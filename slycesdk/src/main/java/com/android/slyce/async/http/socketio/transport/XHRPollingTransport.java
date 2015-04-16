package com.android.slyce.async.http.socketio.transport;

import android.net.Uri;

public class XHRPollingTransport implements com.android.slyce.async.http.socketio.transport.SocketIOTransport {
    private com.android.slyce.async.http.AsyncHttpClient client;
    private Uri sessionUrl;
    private StringCallback stringCallback;
    private com.android.slyce.async.callback.CompletedCallback closedCallback;
    private boolean connected;
	private String sessionId;

    private static final String SEPARATOR = "\ufffd";

    public XHRPollingTransport(com.android.slyce.async.http.AsyncHttpClient client, String sessionUrl, String sessionId) {
        this.client = client;
        this.sessionUrl = Uri.parse(sessionUrl);
        this.sessionId = sessionId;
        
        doLongPolling();
        connected = true;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void setClosedCallback(com.android.slyce.async.callback.CompletedCallback handler) {
        closedCallback = handler;
    }

    @Override
    public void disconnect() {
        connected = false;
        close(null);
    }

    private void close(Exception ex) {
        if (closedCallback != null)
            closedCallback.onCompleted(ex);
    }

    @Override
    public com.android.slyce.async.AsyncServer getServer() {
        return client.getServer();
    }

    @Override
    public void send(String message) {
        if (message.startsWith("5")) {
            postMessage(message);
            return;
        }

        com.android.slyce.async.http.AsyncHttpRequest request = new com.android.slyce.async.http.AsyncHttpPost(computedRequestUrl());
        request.setBody(new com.android.slyce.async.http.body.StringBody(message));

        client.executeString(request, new com.android.slyce.async.http.AsyncHttpClient.StringCallback() {
            @Override
            public void onCompleted(Exception e, com.android.slyce.async.http.AsyncHttpResponse source, String result) {
                if (e != null) {
                    close(e);
                    return;
                }

                sendResult(result);
            }
        });
    }

    private void postMessage(String message) {
        if (!message.startsWith("5"))
            return;

        com.android.slyce.async.http.AsyncHttpRequest request = new com.android.slyce.async.http.AsyncHttpPost(computedRequestUrl());
        request.setBody(new com.android.slyce.async.http.body.StringBody(message));
        client.executeString(request, null);
    }

    private void doLongPolling() {
        this.client.executeString(new com.android.slyce.async.http.AsyncHttpGet(computedRequestUrl()), new com.android.slyce.async.http.AsyncHttpClient.StringCallback() {
            @Override
            public void onCompleted(Exception e, com.android.slyce.async.http.AsyncHttpResponse source, String result) {
                if (e != null) {
                    close(e);
                    return;
                }

                sendResult(result);
                doLongPolling();
            }
        });
    }

    private void sendResult(String result) {
        if (stringCallback == null)
            return;

        if (!result.contains(SEPARATOR)) {
            stringCallback.onStringAvailable(result);
            return;
        }

        String [] results = result.split(SEPARATOR);
        for (int i = 1; i < results.length; i = i + 2) {
            stringCallback.onStringAvailable(results[i+1]);
        }
    }

    /**
     * Return an url with a time-based parameter to avoid caching issues
     */
    private String computedRequestUrl() {
        String currentTime = String.valueOf(System.currentTimeMillis());
        return sessionUrl.buildUpon().appendQueryParameter("t", currentTime)
                .build().toString();
    }

    @Override
    public void setStringCallback(StringCallback callback) {
        stringCallback = callback;
    }

    @Override
    public boolean heartbeats() {
        return false;
    }

	@Override
	public String getSessionId() {
		return this.sessionId;
	}
}
