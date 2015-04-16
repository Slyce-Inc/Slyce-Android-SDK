package com.android.slyce.async.http.server;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.text.TextUtils;

import com.android.slyce.async.AsyncSSLSocketWrapper;
import com.android.slyce.async.AsyncServerSocket;
import com.android.slyce.async.Util;
import com.android.slyce.async.callback.CompletedCallback;
import com.android.slyce.async.callback.ListenCallback;
import com.android.slyce.async.http.AsyncHttpGet;
import com.android.slyce.async.http.AsyncHttpHead;
import com.android.slyce.async.http.Multimap;
import com.android.slyce.async.http.Protocol;
import com.android.slyce.async.util.StreamUtility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;

@TargetApi(Build.VERSION_CODES.ECLAIR)
public class AsyncHttpServer {
    ArrayList<com.android.slyce.async.AsyncServerSocket> mListeners = new ArrayList<com.android.slyce.async.AsyncServerSocket>();
    public void stop() {
        if (mListeners != null) {
            for (com.android.slyce.async.AsyncServerSocket listener: mListeners) {
                listener.stop();
            }
        }
    }
    
    protected boolean onRequest(AsyncHttpServerRequest request, com.android.slyce.async.http.server.AsyncHttpServerResponse response) {
        return false;
    }

    protected void onRequest(com.android.slyce.async.http.server.HttpServerRequestCallback callback, AsyncHttpServerRequest request, com.android.slyce.async.http.server.AsyncHttpServerResponse response) {
        if (callback != null)
            callback.onRequest(request, response);
    }

    protected com.android.slyce.async.http.body.AsyncHttpRequestBody onUnknownBody(com.android.slyce.async.http.Headers headers) {
        return new com.android.slyce.async.http.server.UnknownRequestBody(headers.get("Content-Type"));
    }

    com.android.slyce.async.callback.ListenCallback mListenCallback = new ListenCallback() {
        @Override
        public void onAccepted(final com.android.slyce.async.AsyncSocket socket) {
            AsyncHttpServerRequestImpl req = new AsyncHttpServerRequestImpl() {
                com.android.slyce.async.http.server.HttpServerRequestCallback match;
                String fullPath;
                String path;
                boolean responseComplete;
                boolean requestComplete;
                com.android.slyce.async.http.server.AsyncHttpServerResponseImpl res;
                boolean hasContinued;

                @Override
                protected com.android.slyce.async.http.body.AsyncHttpRequestBody onUnknownBody(com.android.slyce.async.http.Headers headers) {
                    return AsyncHttpServer.this.onUnknownBody(headers);
                }

                @Override
                protected void onHeadersReceived() {
                    com.android.slyce.async.http.Headers headers = getHeaders();

                    // should the negotiation of 100 continue be here, or in the request impl?
                    // probably here, so AsyncResponse can negotiate a 100 continue.
                    if (!hasContinued && "100-continue".equals(headers.get("Expect"))) {
                        pause();
//                        System.out.println("continuing...");
                        com.android.slyce.async.Util.writeAll(mSocket, "HTTP/1.1 100 Continue\r\n\r\n".getBytes(), new CompletedCallback() {
                            @Override
                            public void onCompleted(Exception ex) {
                                resume();
                                if (ex != null) {
                                    report(ex);
                                    return;
                                }
                                hasContinued = true;
                                onHeadersReceived();
                            }
                        });
                        return;
                    }
//                    System.out.println(headers.toHeaderString());

                    String statusLine = getStatusLine();
                    String[] parts = statusLine.split(" ");
                    fullPath = parts[1];
                    path = fullPath.split("\\?")[0];
                    method = parts[0];
                    synchronized (mActions) {
                        ArrayList<Pair> pairs = mActions.get(method);
                        if (pairs != null) {
                            for (Pair p: pairs) {
                                Matcher m = p.regex.matcher(path);
                                if (m.matches()) {
                                    mMatcher = m;
                                    match = p.callback;
                                    break;
                                }
                            }
                        }
                    }
                    res = new AsyncHttpServerResponseImpl(socket, this) {
                        @Override
                        protected void report(Exception e) {
                            super.report(e);
                            if (e != null) {
                                socket.setDataCallback(new NullDataCallback());
                                socket.setEndCallback(new NullCompletedCallback());
                                socket.close();
                            }
                        }

                        @Override
                        protected void onEnd() {
                            super.onEnd();
                            mSocket.setEndCallback(null);
                            responseComplete = true;
                            // reuse the socket for a subsequent request.
                            handleOnCompleted();
                        }
                    };

                    boolean handled = onRequest(this, res);

                    if (match == null && !handled) {
                        res.code(404);
                        res.end();
                        return;
                    }

                    if (!getBody().readFullyOnRequest()) {
                        onRequest(match, this, res);
                    }
                    else if (requestComplete) {
                        onRequest(match, this, res);
                    }
                }

                @Override
                public void onCompleted(Exception e) {
                    // if the protocol was switched off http, ignore this request/response.
                    if (res.code() == 101)
                        return;
                    requestComplete = true;
                    super.onCompleted(e);
                    // no http pipelining, gc trashing if the socket dies
                    // while the request is being sent and is paused or something
                    mSocket.setDataCallback(new NullDataCallback() {
                        @Override
                        public void onDataAvailable(com.android.slyce.async.DataEmitter emitter, com.android.slyce.async.ByteBufferList bb) {
                            super.onDataAvailable(emitter, bb);
                            mSocket.close();
                        }
                    });
                    handleOnCompleted();

                    if (getBody().readFullyOnRequest()) {
                        onRequest(match, this, res);
                    }
                }

                private void handleOnCompleted() {
                    if (requestComplete && responseComplete) {
                        if (com.android.slyce.async.http.HttpUtil.isKeepAlive(Protocol.HTTP_1_1, getHeaders())) {
                            onAccepted(socket);
                        }
                        else {
                            socket.close();
                        }
                    }
                }

                @Override
                public String getPath() {
                    return path;
                }

                @Override
                public Multimap getQuery() {
                    String[] parts = fullPath.split("\\?", 2);
                    if (parts.length < 2)
                        return new Multimap();
                    return Multimap.parseQuery(parts[1]);
                }
            };
            req.setSocket(socket);
            socket.resume();
        }

        @Override
        public void onCompleted(Exception error) {
            report(error);
        }

        @Override
        public void onListening(AsyncServerSocket socket) {
            mListeners.add(socket);
        }
    };

    public com.android.slyce.async.AsyncServerSocket listen(com.android.slyce.async.AsyncServer server, int port) {
        return server.listen(null, port, mListenCallback);
    }

    private void report(Exception ex) {
        if (mCompletedCallback != null)
            mCompletedCallback.onCompleted(ex);
    }

    public com.android.slyce.async.AsyncServerSocket listen(int port) {
        return listen(com.android.slyce.async.AsyncServer.getDefault(), port);
    }

    public void listenSecure(final int port, final SSLContext sslContext) {
        com.android.slyce.async.AsyncServer.getDefault().listen(null, port, new ListenCallback() {
            @Override
            public void onAccepted(com.android.slyce.async.AsyncSocket socket) {
                com.android.slyce.async.AsyncSSLSocketWrapper.handshake(socket, null, port, sslContext.createSSLEngine(), null, null, false,
                        new AsyncSSLSocketWrapper.HandshakeCallback() {
                            @Override
                            public void onHandshakeCompleted(Exception e, com.android.slyce.async.AsyncSSLSocket socket) {
                                if (socket != null)
                                    mListenCallback.onAccepted(socket);
                            }
                        });
            }

            @Override
            public void onListening(com.android.slyce.async.AsyncServerSocket socket) {
                mListenCallback.onListening(socket);
            }

            @Override
            public void onCompleted(Exception ex) {
                mListenCallback.onCompleted(ex);
            }
        });
    }

    public com.android.slyce.async.callback.ListenCallback getListenCallback() {
        return mListenCallback;
    }

    com.android.slyce.async.callback.CompletedCallback mCompletedCallback;
    public void setErrorCallback(com.android.slyce.async.callback.CompletedCallback callback) {
        mCompletedCallback = callback;
    }

    public com.android.slyce.async.callback.CompletedCallback getErrorCallback() {
        return mCompletedCallback;
    }

    private static class Pair {
        Pattern regex;
        com.android.slyce.async.http.server.HttpServerRequestCallback callback;
    }

    final Hashtable<String, ArrayList<Pair>> mActions = new Hashtable<String, ArrayList<Pair>>();

    public void addAction(String action, String regex, com.android.slyce.async.http.server.HttpServerRequestCallback callback) {
        Pair p = new Pair();
        p.regex = Pattern.compile("^" + regex);
        p.callback = callback;

        synchronized (mActions) {
            ArrayList<Pair> pairs = mActions.get(action);
            if (pairs == null) {
                pairs = new ArrayList<Pair>();
                mActions.put(action, pairs);
            }
            pairs.add(p);
        }
    }

    public static interface WebSocketRequestCallback {
        public void onConnected(com.android.slyce.async.http.WebSocket webSocket, AsyncHttpServerRequest request);
    }

    public void websocket(String regex, final WebSocketRequestCallback callback) {
        websocket(regex, null, callback);
    }

    public void websocket(String regex, final String protocol, final WebSocketRequestCallback callback) {
        get(regex, new HttpServerRequestCallback() {
            @Override
            public void onRequest(final AsyncHttpServerRequest request, final com.android.slyce.async.http.server.AsyncHttpServerResponse response) {
                boolean hasUpgrade = false;
                String connection = request.getHeaders().get("Connection");
                if (connection != null) {
                    String[] connections = connection.split(",");
                    for (String c: connections) {
                        if ("Upgrade".equalsIgnoreCase(c.trim())) {
                            hasUpgrade = true;
                            break;
                        }
                    }
                }
                if (!"websocket".equalsIgnoreCase(request.getHeaders().get("Upgrade")) || !hasUpgrade) {
                    response.code(404);
                    response.end();
                    return;
                }
                String peerProtocol = request.getHeaders().get("Sec-WebSocket-Protocol");
                if (!TextUtils.equals(protocol, peerProtocol)) {
                    response.code(404);
                    response.end();
                    return;
                }
                callback.onConnected(new com.android.slyce.async.http.WebSocketImpl(request, response), request);
            }
        });
    }

    public void get(String regex, com.android.slyce.async.http.server.HttpServerRequestCallback callback) {
        addAction(com.android.slyce.async.http.AsyncHttpGet.METHOD, regex, callback);
    }

    public void post(String regex, com.android.slyce.async.http.server.HttpServerRequestCallback callback) {
        addAction(com.android.slyce.async.http.AsyncHttpPost.METHOD, regex, callback);
    }

    public static android.util.Pair<Integer, InputStream> getAssetStream(final Context context, String asset) {
        AssetManager am = context.getAssets();
        try {
            InputStream is = am.open(asset);
            return new android.util.Pair<Integer, InputStream>(is.available(), is);
        }
        catch (IOException e) {
            return null;
        }
    }

    static Hashtable<String, String> mContentTypes = new Hashtable<String, String>();
    {
        mContentTypes.put("js", "application/javascript");
        mContentTypes.put("json", "application/json");
        mContentTypes.put("png", "image/png");
        mContentTypes.put("jpg", "image/jpeg");
        mContentTypes.put("html", "text/html");
        mContentTypes.put("css", "text/css");
        mContentTypes.put("mp4", "video/mp4");
        mContentTypes.put("mov", "video/quicktime");
        mContentTypes.put("wmv", "video/x-ms-wmv");
    }

    public static String getContentType(String path) {
        String type = tryGetContentType(path);
        if (type != null)
            return type;
        return "text/plain";
    }

    public static String tryGetContentType(String path) {
        int index = path.lastIndexOf(".");
        if (index != -1) {
            String e = path.substring(index + 1);
            String ct = mContentTypes.get(e);
            if (ct != null)
                return ct;
        }
        return null;
    }

    public void directory(Context context, String regex, final String assetPath) {
        final Context _context = context.getApplicationContext();
        addAction(AsyncHttpGet.METHOD, regex, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                String path = request.getMatcher().replaceAll("");
                android.util.Pair<Integer, InputStream> pair = getAssetStream(_context, assetPath + path);
                if (pair == null || pair.second == null) {
                    response.code(404);
                    response.end();
                    return;
                }
                final InputStream is = pair.second;
                response.getHeaders().set("Content-Length", String.valueOf(pair.first));
                response.code(200);
                response.getHeaders().add("Content-Type", getContentType(assetPath + path));
                Util.pump(is, response, new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        response.end();
                        StreamUtility.closeQuietly(is);
                    }
                });
            }
        });
        addAction(AsyncHttpHead.METHOD, regex, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                String path = request.getMatcher().replaceAll("");
                android.util.Pair<Integer, InputStream> pair = getAssetStream(_context, assetPath + path);
                if (pair == null || pair.second == null) {
                    response.code(404);
                    response.end();
                    return;
                }
                final InputStream is = pair.second;
                StreamUtility.closeQuietly(is);
                response.getHeaders().set("Content-Length", String.valueOf(pair.first));
                response.code(200);
                response.getHeaders().add("Content-Type", getContentType(assetPath + path));
                response.writeHead();
                response.end();
            }
        });
    }

    public void directory(String regex, final File directory) {
        directory(regex, directory, false);
    }
    
    public void directory(String regex, final File directory, final boolean list) {
        assert directory.isDirectory();
        addAction("GET", regex, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                String path = request.getMatcher().replaceAll("");
                File file = new File(directory, path);
                
                if (file.isDirectory() && list) {
                    ArrayList<File> dirs = new ArrayList<File>();
                    ArrayList<File> files = new ArrayList<File>();
                    for (File f: file.listFiles()) {
                        if (f.isDirectory())
                            dirs.add(f);
                        else
                            files.add(f);
                    }
                    
                    Comparator<File> c = new Comparator<File>() {
                        @Override
                        public int compare(File lhs, File rhs) {
                            return lhs.getName().compareTo(rhs.getName());
                        }
                    };
                    
                    Collections.sort(dirs, c);
                    Collections.sort(files, c);
                    
                    files.addAll(0, dirs);
                    
                    return;
                }
                if (!file.isFile()) {
                    response.code(404);
                    response.end();
                    return;
                }
                try {
                    FileInputStream is = new FileInputStream(file);
                    response.code(200);
                    Util.pump(is, response, new CompletedCallback() {
                        @Override
                        public void onCompleted(Exception ex) {
                            response.end();
                        }
                    });
                }
                catch (FileNotFoundException ex) {
                    response.code(404);
                    response.end();
                }
            }
        });
    }

    private static Hashtable<Integer, String> mCodes = new Hashtable<Integer, String>();
    static {
        mCodes.put(200, "OK");
        mCodes.put(206, "Partial Content");
        mCodes.put(101, "Switching Protocols");
        mCodes.put(301, "Moved Permanently");
        mCodes.put(302, "Found");
        mCodes.put(404, "Not Found");
    }
    
    public static String getResponseCodeDescription(int code) {
        String d = mCodes.get(code);
        if (d == null)
            return "Unknown";
        return d;
    }
}
