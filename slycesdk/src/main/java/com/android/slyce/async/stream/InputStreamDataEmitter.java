package com.android.slyce.async.stream;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by koush on 5/22/13.
 */
public class InputStreamDataEmitter implements com.android.slyce.async.DataEmitter {
    com.android.slyce.async.AsyncServer server;
    InputStream inputStream;
    public InputStreamDataEmitter(com.android.slyce.async.AsyncServer server, InputStream inputStream) {
        this.server = server;
        this.inputStream = inputStream;
        doResume();
    }

    com.android.slyce.async.callback.DataCallback callback;
    @Override
    public void setDataCallback(com.android.slyce.async.callback.DataCallback callback) {
        this.callback = callback;
    }

    @Override
    public com.android.slyce.async.callback.DataCallback getDataCallback() {
        return callback;
    }

    @Override
    public boolean isChunked() {
        return false;
    }

    boolean paused;
    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public void resume() {
        paused = false;
        doResume();
    }

    private void report(final Exception e) {
        getServer().post(new Runnable() {
            @Override
            public void run() {
                Exception ex = e;
                try {
                    inputStream.close();
                }
                catch (Exception e) {
                    ex = e;
                }
                if (endCallback != null)
                    endCallback.onCompleted(ex);
            }
        });
    }

    int mToAlloc = 0;
    com.android.slyce.async.ByteBufferList pending = new com.android.slyce.async.ByteBufferList();
    Runnable pumper = new Runnable() {
        @Override
        public void run() {
            try {
                if (!pending.isEmpty()) {
                    getServer().run(new Runnable() {
                        @Override
                        public void run() {
                            com.android.slyce.async.Util.emitAllData(InputStreamDataEmitter.this, pending);
                        }
                    });
                    if (!pending.isEmpty())
                        return;
                }
                ByteBuffer b;
                do {
                    b = com.android.slyce.async.ByteBufferList.obtain(Math.min(Math.max(mToAlloc, 2 << 11), 256 * 1024));
                    int read;
                    if (-1 == (read = inputStream.read(b.array()))) {
                        report(null);
                        return;
                    }
                    mToAlloc = read * 2;
                    b.limit(read);
                    pending.add(b);
                    getServer().run(new Runnable() {
                        @Override
                        public void run() {
                            com.android.slyce.async.Util.emitAllData(InputStreamDataEmitter.this, pending);
                        }
                    });
                }
                while (pending.remaining() == 0 && !isPaused());
            }
            catch (Exception e) {
                report(e);
            }
        }
    };

    private void doResume() {
        new Thread(pumper).start();
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    com.android.slyce.async.callback.CompletedCallback endCallback;
    @Override
    public void setEndCallback(com.android.slyce.async.callback.CompletedCallback callback) {
        endCallback = callback;
    }

    @Override
    public com.android.slyce.async.callback.CompletedCallback getEndCallback() {
        return endCallback;
    }

    @Override
    public com.android.slyce.async.AsyncServer getServer() {
        return server;
    }

    @Override
    public void close() {
        report(null);
        try {
            inputStream.close();
        }
        catch (Exception e) {
        }
    }

    @Override
    public String charset() {
        return null;
    }
}
