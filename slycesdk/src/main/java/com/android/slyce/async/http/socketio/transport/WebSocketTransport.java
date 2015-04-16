package com.android.slyce.async.http.socketio.transport;

public class WebSocketTransport implements com.android.slyce.async.http.socketio.transport.SocketIOTransport {
    private com.android.slyce.async.http.WebSocket webSocket;
    private StringCallback stringCallback;
	private String sessionId;

    public WebSocketTransport(com.android.slyce.async.http.WebSocket webSocket, String sessionId) {
        this.webSocket = webSocket;
        this.sessionId = sessionId;
        this.webSocket.setDataCallback(new com.android.slyce.async.callback.DataCallback.NullDataCallback());
    }

    @Override
    public boolean isConnected() {
        return this.webSocket.isOpen();
    }

    @Override
    public void setClosedCallback(com.android.slyce.async.callback.CompletedCallback handler) {
        this.webSocket.setClosedCallback(handler);
    }

    @Override
    public void disconnect() {
        this.webSocket.close();
    }

    @Override
    public com.android.slyce.async.AsyncServer getServer() {
        return this.webSocket.getServer();
    }

    @Override
    public void send(String message) {
        this.webSocket.send(message);
    }

    @Override
    public void setStringCallback(final StringCallback callback) {
        if (this.stringCallback == callback)
            return;

        if (callback == null) {
            this.webSocket.setStringCallback(null);
        } else {
            this.webSocket.setStringCallback(new com.android.slyce.async.http.WebSocket.StringCallback() {
                @Override
                public void onStringAvailable(String s) {
                    callback.onStringAvailable(s);
                }
            });
        }

        this.stringCallback = callback;
    }

    @Override
    public boolean heartbeats() {
        return true;
    }

	@Override
	public String getSessionId() {
		return this.sessionId;
	}
}

