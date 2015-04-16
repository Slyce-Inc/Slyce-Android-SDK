package com.android.slyce.async.http.server;

import com.android.slyce.async.LineEmitter.StringCallback;

import java.util.regex.Matcher;

public abstract class AsyncHttpServerRequestImpl extends com.android.slyce.async.FilteredDataEmitter implements AsyncHttpServerRequest, com.android.slyce.async.callback.CompletedCallback {
    private String statusLine;
    private com.android.slyce.async.http.Headers mRawHeaders = new com.android.slyce.async.http.Headers();
    com.android.slyce.async.AsyncSocket mSocket;
    Matcher mMatcher;

    public String getStatusLine() {
        return statusLine;
    }

    private com.android.slyce.async.callback.CompletedCallback mReporter = new com.android.slyce.async.callback.CompletedCallback() {
        @Override
        public void onCompleted(Exception error) {
            AsyncHttpServerRequestImpl.this.onCompleted(error);
        }
    };

    @Override
    public void onCompleted(Exception e) {
//        if (mBody != null)
//            mBody.onCompleted(e);
        report(e);
    }

    abstract protected void onHeadersReceived();
    
    protected void onNotHttp() {
        System.out.println("not http!");
    }

    protected com.android.slyce.async.http.body.AsyncHttpRequestBody onUnknownBody(com.android.slyce.async.http.Headers headers) {
        return null;
    }
    
    StringCallback mHeaderCallback = new StringCallback() {
        @Override
        public void onStringAvailable(String s) {
            try {
                if (statusLine == null) {
                    statusLine = s;
                    if (!statusLine.contains("HTTP/")) {
                        onNotHttp();
                        mSocket.setDataCallback(null);
                    }
                }
                else if (!"\r".equals(s)){
                    mRawHeaders.addLine(s);
                }
                else {
                    com.android.slyce.async.DataEmitter emitter = com.android.slyce.async.http.HttpUtil.getBodyDecoder(mSocket, com.android.slyce.async.http.Protocol.HTTP_1_1, mRawHeaders, true);
//                    emitter.setEndCallback(mReporter);
                    mBody = com.android.slyce.async.http.HttpUtil.getBody(emitter, mReporter, mRawHeaders);
                    if (mBody == null) {
                        mBody = onUnknownBody(mRawHeaders);
                        if (mBody == null)
                            mBody = new com.android.slyce.async.http.server.UnknownRequestBody(mRawHeaders.get("Content-Type"));
                    }
                    mBody.parse(emitter, mReporter);
                    onHeadersReceived();
                }
            }
            catch (Exception ex) {
                onCompleted(ex);
            }
        }
    };

    String method;
    @Override
    public String getMethod() {
        return method;
    }
    
    void setSocket(com.android.slyce.async.AsyncSocket socket) {
        mSocket = socket;

        com.android.slyce.async.LineEmitter liner = new com.android.slyce.async.LineEmitter();
        mSocket.setDataCallback(liner);
        liner.setLineCallback(mHeaderCallback);
        mSocket.setEndCallback(new NullCompletedCallback());
    }
    
    @Override
    public com.android.slyce.async.AsyncSocket getSocket() {
        return mSocket;
    }

    @Override
    public com.android.slyce.async.http.Headers getHeaders() {
        return mRawHeaders;
    }

    @Override
    public void setDataCallback(com.android.slyce.async.callback.DataCallback callback) {
        mSocket.setDataCallback(callback);
    }

    @Override
    public com.android.slyce.async.callback.DataCallback getDataCallback() {
        return mSocket.getDataCallback();
    }

    @Override
    public boolean isChunked() {
        return mSocket.isChunked();
    }

    @Override
    public Matcher getMatcher() {
        return mMatcher;
    }

    com.android.slyce.async.http.body.AsyncHttpRequestBody mBody;
    @Override
    public com.android.slyce.async.http.body.AsyncHttpRequestBody getBody() {
        return mBody;
    }

    @Override
    public void pause() {
        mSocket.pause();
    }

    @Override
    public void resume() {
        mSocket.resume();
    }

    @Override
    public boolean isPaused() {
        return mSocket.isPaused();
    }

    @Override
    public String toString() {
        if (mRawHeaders == null)
            return super.toString();
        return mRawHeaders.toPrefixString(statusLine);
    }
}
