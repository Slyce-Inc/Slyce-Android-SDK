package com.android.slyce.async;

public interface DataEmitter {
    public void setDataCallback(com.android.slyce.async.callback.DataCallback callback);
    public com.android.slyce.async.callback.DataCallback getDataCallback();
    public boolean isChunked();
    public void pause();
    public void resume();
    public void close();
    public boolean isPaused();
    public void setEndCallback(com.android.slyce.async.callback.CompletedCallback callback);
    public com.android.slyce.async.callback.CompletedCallback getEndCallback();
    public com.android.slyce.async.AsyncServer getServer();
    public String charset();
}
