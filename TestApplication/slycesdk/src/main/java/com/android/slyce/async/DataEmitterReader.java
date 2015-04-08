package com.android.slyce.async;

public class DataEmitterReader implements com.android.slyce.async.callback.DataCallback {
    com.android.slyce.async.callback.DataCallback mPendingRead;
    int mPendingReadLength;
    com.android.slyce.async.ByteBufferList mPendingData = new com.android.slyce.async.ByteBufferList();

    public void read(int count, com.android.slyce.async.callback.DataCallback callback) {
        assert mPendingRead == null;
        mPendingReadLength = count;
        mPendingRead = callback;
        assert !mPendingData.hasRemaining();
        mPendingData.recycle();
    }

    private boolean handlePendingData(com.android.slyce.async.DataEmitter emitter) {
        if (mPendingReadLength > mPendingData.remaining())
            return false;

        com.android.slyce.async.callback.DataCallback pendingRead = mPendingRead;
        mPendingRead = null;
        pendingRead.onDataAvailable(emitter, mPendingData);
        assert !mPendingData.hasRemaining();

        return true;
    }

    public DataEmitterReader() {
    }
    @Override
    public void onDataAvailable(com.android.slyce.async.DataEmitter emitter, com.android.slyce.async.ByteBufferList bb) {
        // if we're registered for data, we must be waiting for a read
        assert mPendingRead != null;
        do {
            int need = Math.min(bb.remaining(), mPendingReadLength - mPendingData.remaining());
            bb.get(mPendingData, need);
            bb.remaining();
        }
        while (handlePendingData(emitter) && mPendingRead != null);
        bb.remaining();
    }
}
