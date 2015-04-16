package com.android.slyce.async.http.callback;


public interface HttpConnectCallback {
    public void onConnectCompleted(Exception ex, com.android.slyce.async.http.AsyncHttpResponse response);
}
