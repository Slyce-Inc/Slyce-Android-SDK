package com.android.slyce.async;

public class BufferedDataEmitter implements com.android.slyce.async.DataEmitter {
    com.android.slyce.async.DataEmitter mEmitter;
    public BufferedDataEmitter(com.android.slyce.async.DataEmitter emitter) {
        mEmitter = emitter;
        mEmitter.setDataCallback(new com.android.slyce.async.callback.DataCallback() {
            @Override
            public void onDataAvailable(com.android.slyce.async.DataEmitter emitter, com.android.slyce.async.ByteBufferList bb) {
                bb.get(mBuffers);
                BufferedDataEmitter.this.onDataAvailable();
            }
        });

        mEmitter.setEndCallback(new com.android.slyce.async.callback.CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                mEnded = true;
                mEndException = ex;
                if (mBuffers.remaining() == 0 && mEndCallback != null)
                    mEndCallback.onCompleted(ex);
            }
        });
    }

    @Override
    public void close() {
        mEmitter.close();
    }

    boolean mEnded = false;
    Exception mEndException;
    
    public void onDataAvailable() {
        if (mDataCallback != null && !isPaused() && mBuffers.remaining() > 0)
            mDataCallback.onDataAvailable(this, mBuffers);

        if (mEnded && !mBuffers.hasRemaining() && mEndCallback != null)
            mEndCallback.onCompleted(mEndException);
    }
    
    com.android.slyce.async.ByteBufferList mBuffers = new com.android.slyce.async.ByteBufferList();

    com.android.slyce.async.callback.DataCallback mDataCallback;
    @Override
    public void setDataCallback(com.android.slyce.async.callback.DataCallback callback) {
        if (mDataCallback != null)
            throw new RuntimeException("Buffered Data Emitter callback may only be set once");
        mDataCallback = callback;
    }

    @Override
    public com.android.slyce.async.callback.DataCallback getDataCallback() {
        return mDataCallback;
    }

    @Override
    public boolean isChunked() {
        return false;
    }

    @Override
    public void pause() {
        mEmitter.pause();
    }

    @Override
    public void resume() {
        mEmitter.resume();
        onDataAvailable();
    }

    @Override
    public boolean isPaused() {
        return mEmitter.isPaused();
    }


    com.android.slyce.async.callback.CompletedCallback mEndCallback;
    @Override
    public void setEndCallback(com.android.slyce.async.callback.CompletedCallback callback) {
        mEndCallback = callback;
    }

    @Override
    public com.android.slyce.async.callback.CompletedCallback getEndCallback() {
        return mEndCallback;
    }

    @Override
    public com.android.slyce.async.AsyncServer getServer() {
        return mEmitter.getServer();
    }

    @Override
    public String charset() {
        return mEmitter.charset();
    }
}
