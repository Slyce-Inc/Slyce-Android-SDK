package com.android.slyce.async;

import com.android.slyce.async.callback.DataCallback;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by koush on 5/22/13.
 */
public class FileDataEmitter extends com.android.slyce.async.DataEmitterBase {
    AsyncServer server;
    File file;
    public FileDataEmitter(AsyncServer server, File file) {
        this.server = server;
        this.file = file;
        paused = !server.isAffinityThread();
        if (!paused)
            doResume();
    }

    DataCallback callback;
    @Override
    public void setDataCallback(DataCallback callback) {
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

    @Override
    protected void report(Exception e) {
        com.android.slyce.async.util.StreamUtility.closeQuietly(channel);
        super.report(e);
    }

    com.android.slyce.async.ByteBufferList pending = new com.android.slyce.async.ByteBufferList();
    FileChannel channel;
    Runnable pumper = new Runnable() {
        @Override
        public void run() {
            try {
                if (channel == null)
                    channel = new FileInputStream(file).getChannel();
                if (!pending.isEmpty()) {
                    com.android.slyce.async.Util.emitAllData(FileDataEmitter.this, pending);
                    if (!pending.isEmpty())
                        return;
                }
                ByteBuffer b;
                do {
                    b = com.android.slyce.async.ByteBufferList.obtain(8192);
                    if (-1 == channel.read(b)) {
                        report(null);
                        return;
                    }
                    b.flip();
                    pending.add(b);
                    com.android.slyce.async.Util.emitAllData(FileDataEmitter.this, pending);
                }
                while (pending.remaining() == 0 && !isPaused());
            }
            catch (Exception e) {
                report(e);
            }
        }
    };

    private void doResume() {
        server.post(pumper);
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public com.android.slyce.async.AsyncServer getServer() {
        return server;
    }

    @Override
    public void close() {
        try {
            channel.close();
        }
        catch (Exception e) {
        }
    }
}
