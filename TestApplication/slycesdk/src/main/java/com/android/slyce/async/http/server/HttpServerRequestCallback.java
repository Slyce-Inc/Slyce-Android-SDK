package com.android.slyce.async.http.server;


public interface HttpServerRequestCallback {
    public void onRequest(com.android.slyce.async.http.server.AsyncHttpServerRequest request, com.android.slyce.async.http.server.AsyncHttpServerResponse response);
}
