package com.android.slyce.async.callback;

import com.android.slyce.async.AsyncServerSocket;
import com.android.slyce.async.AsyncSocket;


public interface ListenCallback extends CompletedCallback {
    public void onAccepted(AsyncSocket socket);
    public void onListening(AsyncServerSocket socket);
}
