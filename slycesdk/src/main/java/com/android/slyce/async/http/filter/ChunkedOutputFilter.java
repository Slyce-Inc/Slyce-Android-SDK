package com.android.slyce.async.http.filter;

import java.nio.ByteBuffer;

public class ChunkedOutputFilter extends com.android.slyce.async.FilteredDataSink {
    public ChunkedOutputFilter(com.android.slyce.async.DataSink sink) {
        super(sink);
    }

    @Override
    public com.android.slyce.async.ByteBufferList filter(com.android.slyce.async.ByteBufferList bb) {
        String chunkLen = Integer.toString(bb.remaining(), 16) + "\r\n";
        bb.addFirst(ByteBuffer.wrap(chunkLen.getBytes()));
        bb.add(ByteBuffer.wrap("\r\n".getBytes()));
        return bb;
    }

    @Override
    public void end() {
        setMaxBuffer(Integer.MAX_VALUE);
        com.android.slyce.async.ByteBufferList fin = new com.android.slyce.async.ByteBufferList();
        write(fin);
        setMaxBuffer(0);
        // do NOT call through to super.end, as chunking is a framing protocol.
        // we don't want to close the underlying transport.
    }
}
