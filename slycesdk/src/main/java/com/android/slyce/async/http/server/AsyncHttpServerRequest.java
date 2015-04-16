package com.android.slyce.async.http.server;

import java.util.regex.Matcher;

public interface AsyncHttpServerRequest extends com.android.slyce.async.DataEmitter {
    public com.android.slyce.async.http.Headers getHeaders();
    public Matcher getMatcher();
    public com.android.slyce.async.http.body.AsyncHttpRequestBody getBody();
    public com.android.slyce.async.AsyncSocket getSocket();
    public String getPath();
    public com.android.slyce.async.http.Multimap getQuery();
    public String getMethod();
}
