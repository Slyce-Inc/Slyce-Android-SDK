package com.android.slyce.async.future;

import java.util.concurrent.ExecutorService;

/**
 * Created by koush on 12/22/13.
 */
public class FutureThread<T> extends com.android.slyce.async.future.SimpleFuture<T> {
    public FutureThread(final com.android.slyce.async.future.FutureRunnable<T> runnable) {
        this(runnable, "FutureThread");
    }

    public FutureThread(final ExecutorService pool, final com.android.slyce.async.future.FutureRunnable<T> runnable) {
        pool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    setComplete(runnable.run());
                }
                catch (Exception e) {
                    setComplete(e);
                }
            }
        });
    }

    public FutureThread(final com.android.slyce.async.future.FutureRunnable<T> runnable, String name) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    setComplete(runnable.run());
                }
                catch (Exception e) {
                    setComplete(e);
                }
            }
        }, name).start();
    }
}
