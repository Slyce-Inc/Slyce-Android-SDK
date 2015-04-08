package com.android.slyce.async.http;

public interface AsyncHttpResponse extends com.android.slyce.async.DataEmitter {
    public String protocol();
    public String message();
    public int code();
    public com.android.slyce.async.http.Headers headers();
    public com.android.slyce.async.AsyncSocket detachSocket();
    public com.android.slyce.async.http.AsyncHttpRequest getRequest();
}
