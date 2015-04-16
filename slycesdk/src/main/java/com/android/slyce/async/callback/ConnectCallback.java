package com.android.slyce.async.callback;

public interface ConnectCallback {
    public void onConnectCompleted(Exception ex, com.android.slyce.async.AsyncSocket socket);
}
