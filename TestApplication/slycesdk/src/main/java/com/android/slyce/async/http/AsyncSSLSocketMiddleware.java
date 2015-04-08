package com.android.slyce.async.http;

import android.net.Uri;
import android.text.TextUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;

public class AsyncSSLSocketMiddleware extends com.android.slyce.async.http.AsyncSocketMiddleware {
    public AsyncSSLSocketMiddleware(com.android.slyce.async.http.AsyncHttpClient client) {
        super(client, "https", 443);
    }

    protected SSLContext sslContext;

    public void setSSLContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public SSLContext getSSLContext() {
        return sslContext != null ? sslContext : com.android.slyce.async.AsyncSSLSocketWrapper.getDefaultSSLContext();
    }

    protected TrustManager[] trustManagers;

    public void setTrustManagers(TrustManager[] trustManagers) {
        this.trustManagers = trustManagers;
    }

    protected HostnameVerifier hostnameVerifier;

    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    protected List<com.android.slyce.async.http.AsyncSSLEngineConfigurator> engineConfigurators = new ArrayList<com.android.slyce.async.http.AsyncSSLEngineConfigurator>();

    public void addEngineConfigurator(com.android.slyce.async.http.AsyncSSLEngineConfigurator engineConfigurator) {
        engineConfigurators.add(engineConfigurator);
    }

    public void clearEngineConfigurators() {
        engineConfigurators.clear();
    }

    protected SSLEngine createConfiguredSSLEngine(GetSocketData data, String host, int port) {
        SSLContext sslContext = getSSLContext();
        SSLEngine sslEngine = sslContext.createSSLEngine();

        for (com.android.slyce.async.http.AsyncSSLEngineConfigurator configurator : engineConfigurators) {
            configurator.configureEngine(sslEngine, data, host, port);
        }

        return sslEngine;
    }

    protected com.android.slyce.async.AsyncSSLSocketWrapper.HandshakeCallback createHandshakeCallback(final GetSocketData data, final com.android.slyce.async.callback.ConnectCallback callback) {
        return new com.android.slyce.async.AsyncSSLSocketWrapper.HandshakeCallback() {
            @Override
            public void onHandshakeCompleted(Exception e, com.android.slyce.async.AsyncSSLSocket socket) {
                callback.onConnectCompleted(e, socket);
            }
        };
    }

    protected void tryHandshake(com.android.slyce.async.AsyncSocket socket, GetSocketData data, final Uri uri, final int port, final com.android.slyce.async.callback.ConnectCallback callback) {
        com.android.slyce.async.AsyncSSLSocketWrapper.handshake(socket, uri.getHost(), port,
                createConfiguredSSLEngine(data, uri.getHost(), port),
                trustManagers, hostnameVerifier, true,
                createHandshakeCallback(data, callback));
    }

    @Override
    protected com.android.slyce.async.callback.ConnectCallback wrapCallback(final GetSocketData data, final Uri uri, final int port, final boolean proxied, final com.android.slyce.async.callback.ConnectCallback callback) {
        return new com.android.slyce.async.callback.ConnectCallback() {
            @Override
            public void onConnectCompleted(Exception ex, final com.android.slyce.async.AsyncSocket socket) {
                if (ex != null) {
                    callback.onConnectCompleted(ex, socket);
                    return;
                }

                if (!proxied) {
                    tryHandshake(socket, data, uri, port, callback);
                    return;
                }

                // this SSL connection is proxied, must issue a CONNECT request to the proxy server
                // http://stackoverflow.com/a/6594880/704837
                // some proxies also require 'Host' header, it should be safe to provide it every time
                String connect = String.format("CONNECT %s:%s HTTP/1.1\r\nHost: %s\r\n\r\n", uri.getHost(), port, uri.getHost());
                data.request.logv("Proxying: " + connect);
                com.android.slyce.async.Util.writeAll(socket, connect.getBytes(), new com.android.slyce.async.callback.CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if (ex != null) {
                            callback.onConnectCompleted(ex, socket);
                            return;
                        }

                        com.android.slyce.async.LineEmitter liner = new com.android.slyce.async.LineEmitter();
                        liner.setLineCallback(new com.android.slyce.async.LineEmitter.StringCallback() {
                            String statusLine;

                            @Override
                            public void onStringAvailable(String s) {
                                data.request.logv(s);
                                if (statusLine == null) {
                                    statusLine = s.trim();
                                    if (!statusLine.matches("HTTP/1.\\d 2\\d\\d .*")) { // connect response is allowed to have any 2xx status code
                                        socket.setDataCallback(null);
                                        socket.setEndCallback(null);
                                        callback.onConnectCompleted(new IOException("non 2xx status line: " + statusLine), socket);
                                    }
                                } else if (TextUtils.isEmpty(s.trim())) { // skip all headers, complete handshake once empty line is received
                                    socket.setDataCallback(null);
                                    socket.setEndCallback(null);
                                    tryHandshake(socket, data, uri, port, callback);
                                }
                            }
                        });

                        socket.setDataCallback(liner);

                        socket.setEndCallback(new com.android.slyce.async.callback.CompletedCallback() {
                            @Override
                            public void onCompleted(Exception ex) {
                                if (!socket.isOpen() && ex == null)
                                    ex = new IOException("socket closed before proxy connect response");
                                callback.onConnectCompleted(ex, socket);
                            }
                        });
                    }
                });
            }
        };
    }
}
