package com.android.slyce.async;

/**
 * Created by koush on 5/27/13.
 */
public abstract class DataEmitterBase implements com.android.slyce.async.DataEmitter {
    private boolean ended;
    protected void report(Exception e) {
        if (ended)
            return;
        ended = true;
        if (getEndCallback() != null)
            getEndCallback().onCompleted(e);
    }

    @Override
    public final void setEndCallback(com.android.slyce.async.callback.CompletedCallback callback) {
        endCallback = callback;
    }

    com.android.slyce.async.callback.CompletedCallback endCallback;
    @Override
    public final com.android.slyce.async.callback.CompletedCallback getEndCallback() {
        return endCallback;
    }


    com.android.slyce.async.callback.DataCallback mDataCallback;
    @Override
    public void setDataCallback(com.android.slyce.async.callback.DataCallback callback) {
        mDataCallback = callback;
    }

    @Override
    public com.android.slyce.async.callback.DataCallback getDataCallback() {
        return mDataCallback;
    }

    @Override
    public String charset() {
        return null;
    }
}
