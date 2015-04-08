package com.android.slyce.async.http.callback;

public interface RequestCallback<T> extends com.android.slyce.async.callback.ResultCallback<com.android.slyce.async.http.AsyncHttpResponse, T> {
    public void onConnect(com.android.slyce.async.http.AsyncHttpResponse response);
    public void onProgress(com.android.slyce.async.http.AsyncHttpResponse response, long downloaded, long total);
}
