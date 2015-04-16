package com.android.slyce.async.http.server;

public class UnknownRequestBody implements com.android.slyce.async.http.body.AsyncHttpRequestBody<Void> {
    public UnknownRequestBody(String contentType) {
        mContentType = contentType;
    }

    int length = -1;
    public UnknownRequestBody(com.android.slyce.async.DataEmitter emitter, String contentType, int length) {
        mContentType = contentType;
        this.emitter = emitter;
        this.length = length;
    }

    @Override
    public void write(final com.android.slyce.async.http.AsyncHttpRequest request, com.android.slyce.async.DataSink sink, final com.android.slyce.async.callback.CompletedCallback completed) {
        com.android.slyce.async.Util.pump(emitter, sink, completed);
        if (emitter.isPaused())
            emitter.resume();
    }

    private String mContentType;
    @Override
    public String getContentType() {
        return mContentType;
    }

    @Override
    public boolean readFullyOnRequest() {
        return false;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public Void get() {
        return null;
    }

    @Deprecated
    public void setCallbacks(com.android.slyce.async.callback.DataCallback callback, com.android.slyce.async.callback.CompletedCallback endCallback) {
        emitter.setEndCallback(endCallback);
        emitter.setDataCallback(callback);
    }

    public com.android.slyce.async.DataEmitter getEmitter() {
        return emitter;
    }

    com.android.slyce.async.DataEmitter emitter;
    @Override
    public void parse(com.android.slyce.async.DataEmitter emitter, com.android.slyce.async.callback.CompletedCallback completed) {
        this.emitter = emitter;
        emitter.setEndCallback(completed);
        emitter.setDataCallback(new com.android.slyce.async.callback.DataCallback.NullDataCallback());
    }
}
