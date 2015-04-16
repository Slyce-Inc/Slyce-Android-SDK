package com.android.slyce.async.stream;

import com.android.slyce.async.AsyncServer;
import com.android.slyce.async.DataSink;
import com.android.slyce.async.callback.CompletedCallback;
import com.android.slyce.async.callback.WritableCallback;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class OutputStreamDataSink implements DataSink {
    public OutputStreamDataSink(com.android.slyce.async.AsyncServer server) {
        this(server, null);
    }

    @Override
    public void end() {
        try {
            if (mStream != null)
                mStream.close();
            reportClose(null);
        }
        catch (IOException e) {
            reportClose(e);
        }
    }

    com.android.slyce.async.AsyncServer server;
    public OutputStreamDataSink(com.android.slyce.async.AsyncServer server, OutputStream stream) {
        this.server = server;
        setOutputStream(stream);
    }

    OutputStream mStream;
    public void setOutputStream(OutputStream stream) {
        mStream = stream;
    }
    
    public OutputStream getOutputStream() throws IOException {
        return mStream;
    }

    @Override
    public void write(final com.android.slyce.async.ByteBufferList bb) {
        try {
            while (bb.size() > 0) {
                ByteBuffer b = bb.remove();
                getOutputStream().write(b.array(), b.arrayOffset() + b.position(), b.remaining());
                com.android.slyce.async.ByteBufferList.reclaim(b);
            }
        }
        catch (IOException e) {
            reportClose(e);
        }
        finally {
            bb.recycle();
        }
    }

    com.android.slyce.async.callback.WritableCallback mWritable;
    @Override
    public void setWriteableCallback(com.android.slyce.async.callback.WritableCallback handler) {
        mWritable = handler;        
    }

    @Override
    public com.android.slyce.async.callback.WritableCallback getWriteableCallback() {
        return mWritable;
    }

    @Override
    public boolean isOpen() {
        return closeReported;
    }

    boolean closeReported;
    Exception closeException;
    public void reportClose(Exception ex) {
        if (closeReported)
            return;
        closeReported = true;
        closeException = ex;

        if (mClosedCallback != null)
            mClosedCallback.onCompleted(closeException);
    }
    
    com.android.slyce.async.callback.CompletedCallback mClosedCallback;
    @Override
    public void setClosedCallback(com.android.slyce.async.callback.CompletedCallback handler) {
        mClosedCallback = handler;        
    }

    @Override
    public CompletedCallback getClosedCallback() {
        return mClosedCallback;
    }

    @Override
    public AsyncServer getServer() {
        return server;
    }

    WritableCallback outputStreamCallback;
    public void setOutputStreamWritableCallback(WritableCallback outputStreamCallback) {
        this.outputStreamCallback = outputStreamCallback;
    }
}
