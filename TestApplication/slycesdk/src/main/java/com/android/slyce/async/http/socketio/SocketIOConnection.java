package com.android.slyce.async.http.socketio;

import android.net.Uri;
import android.text.TextUtils;

import com.android.slyce.async.http.WebSocket;
import com.android.slyce.async.http.socketio.transport.SocketIOTransport;
import com.android.slyce.async.http.socketio.transport.WebSocketTransport;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;

/**
 * Created by koush on 7/1/13.
 */
class SocketIOConnection {
    com.android.slyce.async.http.AsyncHttpClient httpClient;
    int heartbeat;
    long reconnectDelay;
    ArrayList<com.android.slyce.async.http.socketio.SocketIOClient> clients = new ArrayList<com.android.slyce.async.http.socketio.SocketIOClient>();
    com.android.slyce.async.http.socketio.transport.SocketIOTransport transport;
    SocketIORequest request;

    public SocketIOConnection(com.android.slyce.async.http.AsyncHttpClient httpClient, SocketIORequest request) {
        this.httpClient = httpClient;
        this.request = request;
        this.reconnectDelay = this.request.config.reconnectDelay;
    }

    public boolean isConnected() {
        return transport != null && transport.isConnected();
    }

    Hashtable<String, Acknowledge> acknowledges = new Hashtable<String, Acknowledge>();
    int ackCount;
    public void emitRaw(int type, com.android.slyce.async.http.socketio.SocketIOClient client, String message, Acknowledge acknowledge) {
        String ack = "";
        if (acknowledge != null) {
            String id = "" + ackCount++;
            ack =  id + "+";
            acknowledges.put(id, acknowledge);
        }
        transport.send(String.format("%d:%s:%s:%s", type, ack, client.endpoint, message));
    }

    public void connect(com.android.slyce.async.http.socketio.SocketIOClient client) {
        if (!clients.contains(client))
            clients.add(client);
        transport.send(String.format("1::%s", client.endpoint));
    }

    public void disconnect(com.android.slyce.async.http.socketio.SocketIOClient client) {
        clients.remove(client);

        // see if we can leave this endpoint completely
        boolean needsEndpointDisconnect = true;
        for (com.android.slyce.async.http.socketio.SocketIOClient other: clients) {
            // if this is the default endpoint (which disconnects everything),
            // or another client is using this endpoint,
            // we can't disconnect
            if (TextUtils.equals(other.endpoint, client.endpoint) || TextUtils.isEmpty(client.endpoint)) {
                needsEndpointDisconnect = false;
                break;
            }
        }

        if (needsEndpointDisconnect && transport != null)
            transport.send(String.format("0::%s", client.endpoint));

        // and see if we can disconnect the socket completely
        if (clients.size() > 0 || transport == null)
            return;

        transport.setStringCallback(null);
        transport.setClosedCallback(null);
        transport.disconnect();
        transport = null;
    }

    com.android.slyce.async.future.Cancellable connecting;
    void reconnect(final com.android.slyce.async.future.DependentCancellable child) {
        if (isConnected()) {
            return;
        }

        // if a connection is in progress, just wait.
        if (connecting != null && !connecting.isDone() && !connecting.isCancelled()) {
            if (child != null)
                child.setParent(connecting);
            return;
        }

        request.logi("Reconnecting socket.io");

        connecting = httpClient.executeString(request, null)
        .then(new com.android.slyce.async.future.TransformFuture<com.android.slyce.async.http.socketio.transport.SocketIOTransport, String>() {
            @Override
            protected void transform(String result) throws Exception {
                String[] parts = result.split(":");
                final String sessionId = parts[0];
                if (!"".equals(parts[1]))
                    heartbeat = Integer.parseInt(parts[1]) / 2 * 1000;
                else
                    heartbeat = 0;

                String transportsLine = parts[3];
                String[] transports = transportsLine.split(",");
                HashSet<String> set = new HashSet<String>(Arrays.asList(transports));
                final com.android.slyce.async.future.SimpleFuture<com.android.slyce.async.http.socketio.transport.SocketIOTransport> transport = new com.android.slyce.async.future.SimpleFuture<SocketIOTransport>();

                if (set.contains("websocket")) {
                    final String sessionUrl = Uri.parse(request.getUri().toString()).buildUpon()
                            .appendPath("websocket").appendPath(sessionId)
                            .build().toString();

                    httpClient.websocket(sessionUrl, null, null)
                    .setCallback(new com.android.slyce.async.future.FutureCallback<com.android.slyce.async.http.WebSocket>() {
                        @Override
                        public void onCompleted(Exception e, WebSocket result) {
                            if (e != null) {
                                transport.setComplete(e);
                                return;
                            }
                            transport.setComplete(new WebSocketTransport(result, sessionId));
                        }
                    });
                } else if (set.contains("xhr-polling")) {
                    final String sessionUrl = Uri.parse(request.getUri().toString()).buildUpon()
                            .appendPath("xhr-polling").appendPath(sessionId)
                            .build().toString();
                    com.android.slyce.async.http.socketio.transport.XHRPollingTransport xhrPolling = new com.android.slyce.async.http.socketio.transport.XHRPollingTransport(httpClient, sessionUrl, sessionId);
                    transport.setComplete(xhrPolling);
                } else {
                    throw new com.android.slyce.async.http.socketio.SocketIOException("transport not supported");
                }

                setComplete(transport);
            }
        })
        .setCallback(new com.android.slyce.async.future.FutureCallback<com.android.slyce.async.http.socketio.transport.SocketIOTransport>() {
            @Override
            public void onCompleted(Exception e, com.android.slyce.async.http.socketio.transport.SocketIOTransport result) {
                if (e != null) {
                    reportDisconnect(e);
                    return;
                }

                reconnectDelay = request.config.reconnectDelay;
                com.android.slyce.async.http.socketio.SocketIOConnection.this.transport = result;
                attach();
            }
        });

        if (child != null)
            child.setParent(connecting);
    }

    void setupHeartbeat() {
        final com.android.slyce.async.http.socketio.transport.SocketIOTransport ts = transport;
        Runnable heartbeatRunner = new Runnable() {
            @Override
            public void run() {
                if (heartbeat <= 0 || ts != transport || ts == null || !ts.isConnected())
                    return;
                transport.send("2:::");
                transport.getServer().postDelayed(this, heartbeat);
            }
        };
        heartbeatRunner.run();
    }

    private interface SelectCallback {
        void onSelect(com.android.slyce.async.http.socketio.SocketIOClient client);
    }

    private void select(String endpoint, SelectCallback callback) {
        for (com.android.slyce.async.http.socketio.SocketIOClient client: clients) {
            if (endpoint == null || TextUtils.equals(client.endpoint, endpoint)) {
                callback.onSelect(client);
            }
        }
    }

    private void delayReconnect() {
        if (transport != null || clients.size() == 0)
            return;

        // see if any client has disconnected,
        // and that we need a reconnect
        boolean disconnected = false;
        for (com.android.slyce.async.http.socketio.SocketIOClient client: clients) {
            if (client.disconnected) {
                disconnected = true;
                break;
            }
        }

        if (!disconnected)
            return;

        httpClient.getServer().postDelayed(new Runnable() {
            @Override
            public void run() {
                reconnect(null);
            }
        }, nextReconnectDelay(reconnectDelay));

        reconnectDelay = reconnectDelay * 2;
        if (request.config.reconnectDelayMax > 0L) {
            reconnectDelay = Math.min(reconnectDelay, request.config.reconnectDelayMax);
        }
    }

    private long nextReconnectDelay(long targetDelay) {
        if (targetDelay < 2L || targetDelay > (Long.MAX_VALUE >> 1) ||
            !request.config.randomizeReconnectDelay)
        {
            return targetDelay;
        }
        return (targetDelay >> 1) + (long) (targetDelay * Math.random());
    }

    private void reportDisconnect(final Exception ex) {
        if (ex != null) {
            request.loge("socket.io disconnected", ex);
        }
        else {
            request.logi("socket.io disconnected");
        }
        select(null, new SelectCallback() {
            @Override
            public void onSelect(com.android.slyce.async.http.socketio.SocketIOClient client) {
                if (client.connected) {
                    client.disconnected = true;
                    com.android.slyce.async.http.socketio.DisconnectCallback closed = client.getDisconnectCallback();
                    if (closed != null)
                        closed.onDisconnect(ex);
                }
                else {
                    // client has never connected, this is a initial connect failure
                    com.android.slyce.async.http.socketio.ConnectCallback callback = client.connectCallback;
                    if (callback != null)
                        callback.onConnectCompleted(ex, client);
                }
            }
        });

        delayReconnect();
    }

    private void reportConnect(String endpoint) {
        select(endpoint, new SelectCallback() {
            @Override
            public void onSelect(com.android.slyce.async.http.socketio.SocketIOClient client) {
                if (client.isConnected())
                    return;
                if (!client.connected) {
                    // normal connect
                    client.connected = true;
                    com.android.slyce.async.http.socketio.ConnectCallback callback = client.connectCallback;
                    if (callback != null)
                        callback.onConnectCompleted(null, client);
                }
                else if (client.disconnected) {
                    // reconnect
                    client.disconnected = false;
                    com.android.slyce.async.http.socketio.ReconnectCallback callback = client.reconnectCallback;
                    if (callback != null)
                        callback.onReconnect();
                }
                else {
                    // double connect?
//                    assert false;
                }
            }
        });
    }

    private void reportJson(String endpoint, final JSONObject jsonMessage, final Acknowledge acknowledge) {
        select(endpoint, new SelectCallback() {
            @Override
            public void onSelect(com.android.slyce.async.http.socketio.SocketIOClient client) {
                JSONCallback callback = client.jsonCallback;
                if (callback != null)
                    callback.onJSON(jsonMessage, acknowledge);
            }
        });
    }

    private void reportString(String endpoint, final String string, final Acknowledge acknowledge) {
        select(endpoint, new SelectCallback() {
            @Override
            public void onSelect(com.android.slyce.async.http.socketio.SocketIOClient client) {
                com.android.slyce.async.http.socketio.StringCallback callback = client.stringCallback;
                if (callback != null)
                    callback.onString(string, acknowledge);
            }
        });
    }

    private void reportEvent(String endpoint, final String event, final JSONArray arguments, final Acknowledge acknowledge) {
        select(endpoint, new SelectCallback() {
            @Override
            public void onSelect(com.android.slyce.async.http.socketio.SocketIOClient client) {
                client.onEvent(event, arguments, acknowledge);
            }
        });
    }

    private void reportError(String endpoint, final String error) {
        select(endpoint, new SelectCallback() {
            @Override
            public void onSelect(com.android.slyce.async.http.socketio.SocketIOClient client) {
                com.android.slyce.async.http.socketio.ErrorCallback callback = client.errorCallback;
                if (callback != null)
                    callback.onError(error);
            }
        });
    }

    private Acknowledge acknowledge(final String _messageId, final String endpoint) {
        if (TextUtils.isEmpty(_messageId))
            return null;

        final String messageId = _messageId.replaceAll("\\+$", "");

        return new Acknowledge() {
            @Override
            public void acknowledge(JSONArray arguments) {
                String data = "";
                if (arguments != null)
                    data += "+" + arguments.toString();
                com.android.slyce.async.http.socketio.transport.SocketIOTransport transport = com.android.slyce.async.http.socketio.SocketIOConnection.this.transport;
                if (transport == null) {
                    final Exception e = new com.android.slyce.async.http.socketio.SocketIOException("not connected to server");
                    select(endpoint, new SelectCallback() {
                        @Override
                        public void onSelect(com.android.slyce.async.http.socketio.SocketIOClient client) {
                            com.android.slyce.async.http.socketio.ExceptionCallback callback = client.exceptionCallback;
                            if (callback != null)
                                callback.onException(e);
                        }
                    });
                    return;
                }
                transport.send(String.format("6:::%s%s", messageId, data));
            }
        };
    }

    private void attach() {
        if (transport.heartbeats())
            setupHeartbeat();

        transport.setClosedCallback(new com.android.slyce.async.callback.CompletedCallback() {
            @Override
            public void onCompleted(final Exception ex) {
                transport = null;
                reportDisconnect(ex);
            }
        });

        transport.setStringCallback(new com.android.slyce.async.http.socketio.transport.SocketIOTransport.StringCallback() {
            @Override
            public void onStringAvailable(String message) {
                try {
//                    Log.d(TAG, "Message: " + message);
                    String[] parts = message.split(":", 4);
                    int code = Integer.parseInt(parts[0]);
                    switch (code) {
                        case 0:
                            // disconnect
                            transport.disconnect();
                            reportDisconnect(null);
                            break;
                        case 1:
                            // connect
                            reportConnect(parts[2]);
                            break;
                        case 2:
                            // heartbeat
                            transport.send("2::");
                            break;
                        case 3: {
                            // message
                            reportString(parts[2], parts[3], acknowledge(parts[1], parts[2]));
                            break;
                        }
                        case 4: {
                            //json message
                            final String dataString = parts[3];
                            final JSONObject jsonMessage = new JSONObject(dataString);
                            reportJson(parts[2], jsonMessage, acknowledge(parts[1], parts[2]));
                            break;
                        }
                        case 5: {
                            final String dataString = parts[3];
                            final JSONObject data = new JSONObject(dataString);
                            final String event = data.getString("name");
                            final JSONArray args = data.optJSONArray("args");
                            reportEvent(parts[2], event, args, acknowledge(parts[1], parts[2]));
                            break;
                        }
                        case 6:
                            // ACK
                            final String[] ackParts = parts[3].split("\\+", 2);
                            Acknowledge ack = acknowledges.remove(ackParts[0]);
                            if (ack == null)
                                return;
                            JSONArray arguments = null;
                            if (ackParts.length == 2)
                                arguments = new JSONArray(ackParts[1]);
                            ack.acknowledge(arguments);
                            break;
                        case 7:
                            // error
                            reportError(parts[2], parts[3]);
                            break;
                        case 8:
                            // noop
                            break;
                        default:
                            throw new com.android.slyce.async.http.socketio.SocketIOException("unknown code");
                    }
                }
                catch (Exception ex) {
                    transport.setClosedCallback(null);
                    transport.disconnect();
                    transport = null;
                    reportDisconnect(ex);
                }
            }
        });

        // now reconnect all the sockets that may have been previously connected
        select(null, new SelectCallback() {
            @Override
            public void onSelect(com.android.slyce.async.http.socketio.SocketIOClient client) {
                if (TextUtils.isEmpty(client.endpoint))
                    return;

                connect(client);
            }
        });
    }
}
