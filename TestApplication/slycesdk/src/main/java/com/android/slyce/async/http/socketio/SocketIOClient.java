package com.android.slyce.async.http.socketio;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

@Deprecated
public class SocketIOClient extends EventEmitter {
    boolean connected;
    boolean disconnected;

    private void emitRaw(int type, String message, Acknowledge acknowledge) {
        connection.emitRaw(type, this, message, acknowledge);
    }

    public void emit(String name, JSONArray args) {
        emit(name, args, null);
    }

    public void emit(final String message) {
        emit(message, (Acknowledge)null);
    }

    public void emit(final JSONObject jsonMessage) {
        emit(jsonMessage, null);
    }

    public void emit(String name, JSONArray args, Acknowledge acknowledge) {
        final JSONObject event = new JSONObject();
        try {
            event.put("name", name);
            event.put("args", args);
            emitRaw(5, event.toString(), acknowledge);
        }
        catch (Exception e) {
        }
    }

    public void emit(final String message, Acknowledge acknowledge) {
        emitRaw(3, message, acknowledge);
    }

    public void emit(final JSONObject jsonMessage, Acknowledge acknowledge) {
        emitRaw(4, jsonMessage.toString(), acknowledge);
    }

    public void emitEvent(final String name) {
        emitEvent(name, null);
    }

    public void emitEvent(final String name, Acknowledge acknowledge) {
        final JSONObject event = new JSONObject();
        try {
            event.put("name", name);
            emitRaw(5, event.toString(), acknowledge);
        } catch (Exception e) {

        }
    }

    public static com.android.slyce.async.future.Future<SocketIOClient> connect(final com.android.slyce.async.http.AsyncHttpClient client, String uri, final com.android.slyce.async.http.socketio.ConnectCallback callback) {
        return connect(client, new SocketIORequest(uri), callback);
    }

    com.android.slyce.async.http.socketio.ConnectCallback connectCallback;
    public static com.android.slyce.async.future.Future<SocketIOClient> connect(final com.android.slyce.async.http.AsyncHttpClient client, final SocketIORequest request, final com.android.slyce.async.http.socketio.ConnectCallback callback) {
        final com.android.slyce.async.future.SimpleFuture<SocketIOClient> ret = new com.android.slyce.async.future.SimpleFuture<SocketIOClient>();

        final SocketIOConnection connection = new SocketIOConnection(client, request);

        final com.android.slyce.async.http.socketio.ConnectCallback wrappedCallback = new com.android.slyce.async.http.socketio.ConnectCallback() {
            @Override
            public void onConnectCompleted(Exception ex, SocketIOClient client) {
                if (ex != null || TextUtils.isEmpty(request.getEndpoint())) {
                    if (callback != null)
                        callback.onConnectCompleted(ex, client);
                    ret.setComplete(ex, client);
                    return;
                }

                // remove the root client since that's not actually being used.
                connection.clients.remove(client);

                // connect to the endpoint we want
                client.of(request.getEndpoint(), new com.android.slyce.async.http.socketio.ConnectCallback() {
                    @Override
                    public void onConnectCompleted(Exception ex, SocketIOClient client) {
                        if (callback != null)
                            callback.onConnectCompleted(ex, client);
                        ret.setComplete(ex, client);
                    }
                });
            }
        };

        connection.clients.add(new SocketIOClient(connection, "", wrappedCallback));
        connection.reconnect(ret);

        return ret;
    }

    com.android.slyce.async.http.socketio.ExceptionCallback exceptionCallback;
    public void setExceptionCallback(com.android.slyce.async.http.socketio.ExceptionCallback exceptionCallback) {
        this.exceptionCallback = exceptionCallback;
    }

    public com.android.slyce.async.http.socketio.ExceptionCallback getExceptionCallback() {
        return exceptionCallback;
    }

    com.android.slyce.async.http.socketio.ErrorCallback errorCallback;
    public com.android.slyce.async.http.socketio.ErrorCallback getErrorCallback() {
        return errorCallback;
    }
    public void setErrorCallback(com.android.slyce.async.http.socketio.ErrorCallback callback) {
        errorCallback = callback;
    }

    com.android.slyce.async.http.socketio.DisconnectCallback disconnectCallback;
    public com.android.slyce.async.http.socketio.DisconnectCallback getDisconnectCallback() {
        return disconnectCallback;
    }
    public void setDisconnectCallback(com.android.slyce.async.http.socketio.DisconnectCallback callback) {
        disconnectCallback = callback;
    }

    ReconnectCallback reconnectCallback;
    public ReconnectCallback getReconnectCallback() {
        return reconnectCallback;
    }
    public void setReconnectCallback(ReconnectCallback callback) {
        reconnectCallback = callback;
    }

    JSONCallback jsonCallback;
    public JSONCallback getJSONCallback() {
        return jsonCallback;
    }
    public void setJSONCallback(JSONCallback callback) {
        jsonCallback = callback;
    }

    com.android.slyce.async.http.socketio.StringCallback stringCallback;
    public com.android.slyce.async.http.socketio.StringCallback getStringCallback() {
        return stringCallback;
    }
    public void setStringCallback(com.android.slyce.async.http.socketio.StringCallback callback) {
        stringCallback = callback;
    }

    SocketIOConnection connection;
    String endpoint;
    private SocketIOClient(SocketIOConnection connection, String endpoint, com.android.slyce.async.http.socketio.ConnectCallback callback) {
        this.endpoint = endpoint;
        this.connection = connection;
        this.connectCallback = callback;
    }

    public boolean isConnected() {
        return connected && !disconnected && connection.isConnected();
    }

    public void disconnect() {
        connection.disconnect(this);
        com.android.slyce.async.http.socketio.DisconnectCallback disconnectCallback = this.disconnectCallback;
        if (disconnectCallback != null) {
        	disconnectCallback.onDisconnect(null);
        }
    }

    public void of(String endpoint, com.android.slyce.async.http.socketio.ConnectCallback connectCallback) {
        connection.connect(new SocketIOClient(connection, endpoint, connectCallback));
    }

    public void reconnect() {
        connection.reconnect(null);
    }

    public com.android.slyce.async.http.socketio.transport.SocketIOTransport getTransport() {
        return connection.transport;
    }
}
