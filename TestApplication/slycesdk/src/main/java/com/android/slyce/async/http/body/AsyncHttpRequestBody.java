package com.android.slyce.async.http.body;

public interface AsyncHttpRequestBody<T> {
    public void write(com.android.slyce.async.http.AsyncHttpRequest request, com.android.slyce.async.DataSink sink, com.android.slyce.async.callback.CompletedCallback completed);
    public void parse(com.android.slyce.async.DataEmitter emitter, com.android.slyce.async.callback.CompletedCallback completed);
    public String getContentType();
    public boolean readFullyOnRequest();
    public int length();
    public T get();
}
