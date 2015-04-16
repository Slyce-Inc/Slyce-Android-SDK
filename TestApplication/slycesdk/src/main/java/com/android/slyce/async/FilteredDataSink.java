package com.android.slyce.async;

public class FilteredDataSink extends com.android.slyce.async.BufferedDataSink {
    public FilteredDataSink(com.android.slyce.async.DataSink sink) {
        super(sink);
        setMaxBuffer(0);
    }
    
    public com.android.slyce.async.ByteBufferList filter(com.android.slyce.async.ByteBufferList bb) {
        return bb;
    }

    @Override
    public final void write(com.android.slyce.async.ByteBufferList bb) {
        // don't filter and write if currently buffering, unless we know
        // that the buffer can fit the entirety of the filtered result
        if (isBuffering() && getMaxBuffer() != Integer.MAX_VALUE)
            return;
        com.android.slyce.async.ByteBufferList filtered = filter(bb);
        assert bb == null || filtered == bb || bb.isEmpty();
        super.write(filtered, true);
        if (bb != null)
            bb.recycle();
    }
}
