package com.android.slyce.async.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class OutputStreamDataCallback implements com.android.slyce.async.callback.DataCallback, com.android.slyce.async.callback.CompletedCallback {
    private OutputStream mOutput;
    public OutputStreamDataCallback(OutputStream os) {
        mOutput = os;
    }

    public OutputStream getOutputStream() {
        return mOutput;
    }

    @Override
    public void onDataAvailable(com.android.slyce.async.DataEmitter emitter, com.android.slyce.async.ByteBufferList bb) {
        try {
            while (bb.size() > 0) {
                ByteBuffer b = bb.remove();
                mOutput.write(b.array(), b.arrayOffset() + b.position(), b.remaining());
                com.android.slyce.async.ByteBufferList.reclaim(b);
            }
        }
        catch (Exception ex) {
            onCompleted(ex);
        }
        finally {
            bb.recycle();
        }
    }
    
    public void close() {
        try {
            mOutput.close();
        }
        catch (IOException e) {
            onCompleted(e);
        }
    }

    @Override
    public void onCompleted(Exception error) {
        error.printStackTrace();       
    }
}
