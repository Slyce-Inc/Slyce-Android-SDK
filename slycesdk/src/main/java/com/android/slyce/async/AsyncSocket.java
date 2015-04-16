package com.android.slyce.async;


public interface AsyncSocket extends com.android.slyce.async.DataEmitter, DataSink {
    public com.android.slyce.async.AsyncServer getServer();
}
