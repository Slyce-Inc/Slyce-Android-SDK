package com.android.slyce.async.http.filter;

import java.nio.ByteBuffer;
import java.util.zip.Inflater;

public class InflaterInputFilter extends com.android.slyce.async.FilteredDataEmitter {
    private Inflater mInflater;

    @Override
    protected void report(Exception e) {
        mInflater.end();
        if (e != null && mInflater.getRemaining() > 0) {
            e = new com.android.slyce.async.http.filter.DataRemainingException("data still remaining in inflater", e);
        }
        super.report(e);
    }

    com.android.slyce.async.ByteBufferList transformed = new com.android.slyce.async.ByteBufferList();
    @Override
    public void onDataAvailable(com.android.slyce.async.DataEmitter emitter, com.android.slyce.async.ByteBufferList bb) {
        try {
            ByteBuffer output = com.android.slyce.async.ByteBufferList.obtain(bb.remaining() * 2);
            int totalRead = 0;
            while (bb.size() > 0) {
                ByteBuffer b = bb.remove();
                if (b.hasRemaining()) {
                    totalRead =+ b.remaining();
                    mInflater.setInput(b.array(), b.arrayOffset() + b.position(), b.remaining());
                    do {
                        int inflated = mInflater.inflate(output.array(), output.arrayOffset() + output.position(), output.remaining());
                        output.position(output.position() + inflated);
                        if (!output.hasRemaining()) {
                            output.flip();
                            transformed.add(output);
                            assert totalRead != 0;
                            int newSize = output.capacity() * 2;
                            output = com.android.slyce.async.ByteBufferList.obtain(newSize);
                        }
                    }
                    while (!mInflater.needsInput() && !mInflater.finished());
                }
                com.android.slyce.async.ByteBufferList.reclaim(b);
            }
            output.flip();
            transformed.add(output);

            com.android.slyce.async.Util.emitAllData(this, transformed);
        }
        catch (Exception ex) {
            report(ex);
        }
    }

    public InflaterInputFilter() {
        this(new Inflater());
    }

    public InflaterInputFilter(Inflater inflater) {
        mInflater = inflater;
    }
}
