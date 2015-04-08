package com.android.slyce.async.future;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by koush on 12/25/13.
 */
public class HandlerFuture<T> extends com.android.slyce.async.future.SimpleFuture<T> {
    Handler handler;

    public HandlerFuture() {
        Looper looper = Looper.myLooper();
        if (looper == null)
            looper = Looper.getMainLooper();
        handler = new Handler(looper);
    }

    @Override
    public com.android.slyce.async.future.SimpleFuture<T> setCallback(final com.android.slyce.async.future.FutureCallback<T> callback) {
        com.android.slyce.async.future.FutureCallback<T> wrapped = new com.android.slyce.async.future.FutureCallback<T>() {
            @Override
            public void onCompleted(final Exception e, final T result) {
                if (Looper.myLooper() == handler.getLooper()) {
                    callback.onCompleted(e, result);
                    return;
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onCompleted(e, result);
                    }
                });
            }
        };
        return super.setCallback(wrapped);
    }
}
