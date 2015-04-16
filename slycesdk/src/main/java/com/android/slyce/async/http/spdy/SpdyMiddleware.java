package com.android.slyce.async.http.spdy;

import android.net.Uri;
import android.text.TextUtils;

import com.android.slyce.async.AsyncSSLSocketWrapper;
import com.android.slyce.async.callback.ConnectCallback;
import com.android.slyce.async.future.FutureCallback;
import com.android.slyce.async.http.AsyncSSLEngineConfigurator;
import com.android.slyce.async.http.AsyncSSLSocketMiddleware;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

public class SpdyMiddleware extends AsyncSSLSocketMiddleware {
    public SpdyMiddleware(com.android.slyce.async.http.AsyncHttpClient client) {
        super(client);
        addEngineConfigurator(new AsyncSSLEngineConfigurator() {
            @Override
            public void configureEngine(SSLEngine engine, GetSocketData data, String host, int port) {
                configure(engine, data, host, port);
            }
        });
    }

    private void configure(SSLEngine engine, GetSocketData data, String host, int port) {
        if (!initialized && spdyEnabled) {
            initialized = true;
            try {
                peerHost = engine.getClass().getSuperclass().getDeclaredField("peerHost");
                peerPort = engine.getClass().getSuperclass().getDeclaredField("peerPort");
                sslParameters = engine.getClass().getDeclaredField("sslParameters");
                npnProtocols = sslParameters.getType().getDeclaredField("npnProtocols");
                alpnProtocols = sslParameters.getType().getDeclaredField("alpnProtocols");
                useSni = sslParameters.getType().getDeclaredField("useSni");
                sslNativePointer = engine.getClass().getDeclaredField("sslNativePointer");
                String nativeCryptoName = sslParameters.getType().getPackage().getName() + ".NativeCrypto";
                nativeGetNpnNegotiatedProtocol = Class.forName(nativeCryptoName, true, sslParameters.getType().getClassLoader())
                .getDeclaredMethod("SSL_get_npn_negotiated_protocol", long.class);
                nativeGetAlpnNegotiatedProtocol = Class.forName(nativeCryptoName, true, sslParameters.getType().getClassLoader())
                .getDeclaredMethod("SSL_get0_alpn_selected", long.class);

                peerHost.setAccessible(true);
                peerPort.setAccessible(true);
                sslParameters.setAccessible(true);
                npnProtocols.setAccessible(true);
                alpnProtocols.setAccessible(true);
                useSni.setAccessible(true);
                sslNativePointer.setAccessible(true);
                nativeGetNpnNegotiatedProtocol.setAccessible(true);
                nativeGetAlpnNegotiatedProtocol.setAccessible(true);
            }
            catch (Exception e) {
                sslParameters = null;
                npnProtocols = null;
                alpnProtocols = null;
                useSni = null;
                sslNativePointer = null;
                nativeGetNpnNegotiatedProtocol = null;
                nativeGetAlpnNegotiatedProtocol = null;
            }
        }

        // TODO: figure out why POST does not work if sending content-length header
        // see above regarding app engine comment as to why: drive requires content-length
        // but app engine sends a GO_AWAY if it sees a content-length...
        if (!canSpdyRequest(data))
            return;

        if (sslParameters != null) {
            try {
                byte[] protocols = concatLengthPrefixed(
                com.android.slyce.async.http.Protocol.HTTP_1_1,
                com.android.slyce.async.http.Protocol.SPDY_3
                );

                peerHost.set(engine, host);
                peerPort.set(engine, port);
                Object sslp = sslParameters.get(engine);
//                npnProtocols.set(sslp, protocols);
                alpnProtocols.set(sslp, protocols);
                useSni.set(sslp, true);
            }
            catch (Exception e ) {
                e.printStackTrace();
            }
        }
    }

    boolean initialized;
    Field peerHost;
    Field peerPort;
    Field sslParameters;
    Field npnProtocols;
    Field alpnProtocols;
    Field sslNativePointer;
    Field useSni;
    Method nativeGetNpnNegotiatedProtocol;
    Method nativeGetAlpnNegotiatedProtocol;
    Hashtable<String, SpdyConnectionWaiter> connections = new Hashtable<String, SpdyConnectionWaiter>();
    boolean spdyEnabled;

    private static class SpdyConnectionWaiter extends com.android.slyce.async.future.MultiFuture<AsyncSpdyConnection> {
        com.android.slyce.async.future.SimpleCancellable originalCancellable = new com.android.slyce.async.future.SimpleCancellable();
    }

    public boolean getSpdyEnabled() {
        return spdyEnabled;
    }

    public void setSpdyEnabled(boolean enabled) {
        spdyEnabled = enabled;
    }

    @Override
    public void setSSLContext(SSLContext sslContext) {
        super.setSSLContext(sslContext);
        initialized = false;
    }

    static byte[] concatLengthPrefixed(com.android.slyce.async.http.Protocol... protocols) {
        ByteBuffer result = ByteBuffer.allocate(8192);
        for (com.android.slyce.async.http.Protocol protocol: protocols) {
            if (protocol == com.android.slyce.async.http.Protocol.HTTP_1_0) continue; // No HTTP/1.0 for NPN.
            result.put((byte) protocol.toString().length());
            result.put(protocol.toString().getBytes(com.android.slyce.async.util.Charsets.UTF_8));
        }
        result.flip();
        byte[] ret = new com.android.slyce.async.ByteBufferList(result).getAllByteArray();
        return ret;
    }

    private static String requestPath(Uri uri) {
        String pathAndQuery = uri.getEncodedPath();
        if (pathAndQuery == null)
            pathAndQuery = "/";
        else if (!pathAndQuery.startsWith("/"))
            pathAndQuery = "/" + pathAndQuery;
        if (!TextUtils.isEmpty(uri.getEncodedQuery()))
            pathAndQuery += "?" + uri.getEncodedQuery();
        return pathAndQuery;
    }

    private static class NoSpdyException extends Exception {
    }
    private static final NoSpdyException NO_SPDY = new NoSpdyException();

    private void noSpdy(String key) {
        SpdyConnectionWaiter conn = connections.remove(key);
        if (conn != null)
            conn.setComplete(NO_SPDY);
    }

    private void invokeConnect(String key, final com.android.slyce.async.callback.ConnectCallback callback, Exception e, com.android.slyce.async.AsyncSSLSocket socket) {
        SpdyConnectionWaiter waiter = connections.get(key);
        if (waiter == null || waiter.originalCancellable.setComplete())
            callback.onConnectCompleted(e, socket);
    }

    @Override
    protected AsyncSSLSocketWrapper.HandshakeCallback createHandshakeCallback(final GetSocketData data, final com.android.slyce.async.callback.ConnectCallback callback) {
        final String key = data.state.get("spdykey");
        if (key == null)
            return super.createHandshakeCallback(data, callback);

        return new AsyncSSLSocketWrapper.HandshakeCallback() {
            @Override
            public void onHandshakeCompleted(Exception e, com.android.slyce.async.AsyncSSLSocket socket) {
                data.request.logv("checking spdy handshake");
                if (e != null || nativeGetAlpnNegotiatedProtocol == null) {
                    invokeConnect(key, callback, e, socket);
                    noSpdy(key);
                    return;
                }
                String protoString;
                try {
                    long ptr = (Long)sslNativePointer.get(socket.getSSLEngine());
                    byte[] proto = (byte[])nativeGetAlpnNegotiatedProtocol.invoke(null, ptr);
                    if (proto == null) {
                        invokeConnect(key, callback, null, socket);
                        noSpdy(key);
                        return;
                    }
                    protoString = new String(proto);
                    com.android.slyce.async.http.Protocol p = com.android.slyce.async.http.Protocol.get(protoString);
                    if (p == null) {
                        invokeConnect(key, callback, null, socket);
                        noSpdy(key);
                        return;
                    }
                }
                catch (Exception ex) {
                    throw new AssertionError(ex);
                }

                final AsyncSpdyConnection connection = new AsyncSpdyConnection(socket, com.android.slyce.async.http.Protocol.get(protoString)) {
                    boolean hasReceivedSettings;
                    @Override
                    public void settings(boolean clearPrevious, com.android.slyce.async.http.spdy.Settings settings) {
                        super.settings(clearPrevious, settings);
                        if (!hasReceivedSettings) {
                            try {
                                sendConnectionPreface();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                            hasReceivedSettings = true;

                            SpdyConnectionWaiter waiter = connections.get(key);

                            if (waiter.originalCancellable.setComplete()) {
                                data.request.logv("using new spdy connection for host: " + data.request.getUri().getHost());
                                newSocket(data, this, callback);
                            }

                            waiter.setComplete(this);
                        }
                    }
                };
            }
        };
    }

    private void newSocket(GetSocketData data, final AsyncSpdyConnection connection, final com.android.slyce.async.callback.ConnectCallback callback) {
        final com.android.slyce.async.http.AsyncHttpRequest request = data.request;

        data.protocol = connection.protocol.toString();

        final com.android.slyce.async.http.body.AsyncHttpRequestBody requestBody = data.request.getBody();

        // this causes app engine to shit a brick, but if it is missing,
        // drive shits the bed
//        if (requestBody != null) {
//            if (requestBody.length() >= 0) {
//                request.getHeaders().set("Content-Length", String.valueOf(requestBody.length()));
//            }
//        }

        final ArrayList<com.android.slyce.async.http.spdy.Header> headers = new ArrayList<com.android.slyce.async.http.spdy.Header>();
        headers.add(new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.TARGET_METHOD, request.getMethod()));
        headers.add(new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.TARGET_PATH, requestPath(request.getUri())));
        String host = request.getHeaders().get("Host");
        if (com.android.slyce.async.http.Protocol.SPDY_3 == connection.protocol) {
            headers.add(new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.VERSION, "HTTP/1.1"));
            headers.add(new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.TARGET_HOST, host));
        } else if (com.android.slyce.async.http.Protocol.HTTP_2 == connection.protocol) {
            headers.add(new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.TARGET_AUTHORITY, host)); // Optional in HTTP/2
        } else {
            throw new AssertionError();
        }
        headers.add(new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.TARGET_SCHEME, request.getUri().getScheme()));

        final com.android.slyce.async.http.Multimap mm = request.getHeaders().getMultiMap();
        for (String key: mm.keySet()) {
            if (com.android.slyce.async.http.spdy.SpdyTransport.isProhibitedHeader(connection.protocol, key))
                continue;
            for (String value: mm.get(key)) {
                headers.add(new com.android.slyce.async.http.spdy.Header(key.toLowerCase(), value));
            }
        }

        request.logv("\n" + request);
        final AsyncSpdyConnection.SpdySocket spdy = connection.newStream(headers, requestBody != null, true);
        callback.onConnectCompleted(null, spdy);
    }

    private boolean canSpdyRequest(GetSocketData data) {
        // TODO: figure out why POST does not work if sending content-length header
        // see above regarding app engine comment as to why: drive requires content-length
        // but app engine sends a GO_AWAY if it sees a content-length...
        return data.request.getBody() == null;
    }

    @Override
    protected com.android.slyce.async.callback.ConnectCallback wrapCallback(final GetSocketData data, final Uri uri, final int port, final boolean proxied, com.android.slyce.async.callback.ConnectCallback callback) {
        final com.android.slyce.async.callback.ConnectCallback superCallback = super.wrapCallback(data, uri, port, proxied, callback);
        final String key = data.state.get("spdykey");
        if (key == null)
            return superCallback;

        // new outgoing connection, try to make this a spdy connection
        return new ConnectCallback() {
            @Override
            public void onConnectCompleted(Exception ex, com.android.slyce.async.AsyncSocket socket) {
                // an exception here is an ssl or network exception... don't rule spdy out yet, but
                // trigger the waiters
                if (ex != null) {
                    final SpdyConnectionWaiter conn = connections.remove(key);
                    if (conn != null)
                        conn.setComplete(ex);
                }
                superCallback.onConnectCompleted(ex, socket);
            }
        };
    }

    @Override
    public com.android.slyce.async.future.Cancellable getSocket(final GetSocketData data) {
        final Uri uri = data.request.getUri();
        final int port = getSchemePort(data.request.getUri());
        if (port == -1) {
            return null;
        }

        if (!spdyEnabled)
            return super.getSocket(data);

        // TODO: figure out why POST does not work if sending content-length header
        // see above regarding app engine comment as to why: drive requires content-length
        // but app engine sends a GO_AWAY if it sees a content-length...
        if (!canSpdyRequest(data))
            return super.getSocket(data);

        // can we use an existing connection to satisfy this, or do we need a new one?
        String key = uri.getHost() + port;
        SpdyConnectionWaiter conn = connections.get(key);
        if (conn != null) {
            if (conn.tryGetException() instanceof NoSpdyException)
                return super.getSocket(data);

            // dead connection check
            if (conn.tryGet() != null && !conn.tryGet().socket.isOpen()) {
                // old spdy connection is derped, kill it with fire.
                connections.remove(key);
                conn = null;
            }
        }

        if (conn == null) {
            // no connection has ever been attempted (or previous one had a network death), so attempt one
            data.state.put("spdykey", key);
            // if we got something back synchronously, it's a keep alive socket
            com.android.slyce.async.future.Cancellable ret = super.getSocket(data);
            if (ret.isDone() || ret.isCancelled())
                return ret;
            conn = new SpdyConnectionWaiter();
            connections.put(key, conn);
            return conn.originalCancellable;
        }

        data.request.logv("waiting for potential spdy connection for host: " + data.request.getUri().getHost());
        final com.android.slyce.async.future.SimpleCancellable ret = new com.android.slyce.async.future.SimpleCancellable();
        conn.setCallback(new FutureCallback<AsyncSpdyConnection>() {
            @Override
            public void onCompleted(Exception e, AsyncSpdyConnection conn) {
                if (e instanceof NoSpdyException) {
                    data.request.logv("spdy not available");
                    ret.setParent(SpdyMiddleware.super.getSocket(data));
                    return;
                }
                if (e != null) {
                    if (ret.setComplete())
                        data.connectCallback.onConnectCompleted(e, null);
                    return;
                }
                data.request.logv("using existing spdy connection for host: " + data.request.getUri().getHost());
                if (ret.setComplete())
                    newSocket(data, conn, data.connectCallback);
            }
        });

        return ret;
    }

    @Override
    public boolean exchangeHeaders(final OnExchangeHeaderData data) {
        if (!(data.socket instanceof AsyncSpdyConnection.SpdySocket))
            return super.exchangeHeaders(data);

        com.android.slyce.async.http.body.AsyncHttpRequestBody requestBody = data.request.getBody();
        if (requestBody != null) {
            data.response.sink(data.socket);
        }

        // headers were already sent as part of the socket being opened.
        data.sendHeadersCallback.onCompleted(null);

        final AsyncSpdyConnection.SpdySocket spdySocket = (AsyncSpdyConnection.SpdySocket)data.socket;
        spdySocket.headers()
        .then(new com.android.slyce.async.future.TransformFuture<com.android.slyce.async.http.Headers, List<com.android.slyce.async.http.spdy.Header>>() {
            @Override
            protected void transform(List<com.android.slyce.async.http.spdy.Header> result) throws Exception {
                com.android.slyce.async.http.Headers headers = new com.android.slyce.async.http.Headers();
                for (com.android.slyce.async.http.spdy.Header header: result) {
                    String key = header.name.utf8();
                    String value = header.value.utf8();
                    headers.add(key, value);
                }
                String status = headers.remove(com.android.slyce.async.http.spdy.Header.RESPONSE_STATUS.utf8());
                String[] statusParts = status.split(" ", 2);
                data.response.code(Integer.parseInt(statusParts[0]));
                if (statusParts.length == 2)
                    data.response.message(statusParts[1]);
                data.response.protocol(headers.remove(com.android.slyce.async.http.spdy.Header.VERSION.utf8()));
                data.response.headers(headers);
                setComplete(headers);
            }
        })
        .setCallback(new com.android.slyce.async.future.FutureCallback<com.android.slyce.async.http.Headers>() {
            @Override
            public void onCompleted(Exception e, com.android.slyce.async.http.Headers result) {
                data.receiveHeadersCallback.onCompleted(e);
                com.android.slyce.async.DataEmitter emitter = com.android.slyce.async.http.HttpUtil.getBodyDecoder(spdySocket, spdySocket.getConnection().protocol, result, false);
                data.response.emitter(emitter);
            }
        });
        return true;
    }

    @Override
    public void onRequestSent(OnRequestSentData data) {
        if (!(data.socket instanceof AsyncSpdyConnection.SpdySocket))
            return;

        if (data.request.getBody() != null)
            data.response.sink().end();
    }
}