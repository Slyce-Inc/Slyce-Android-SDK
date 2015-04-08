package com.android.slyce.async.http.filter;

public class ContentLengthFilter extends com.android.slyce.async.FilteredDataEmitter {
    public ContentLengthFilter(long contentLength) {
        this.contentLength = contentLength;
    }

    @Override
    protected void report(Exception e) {
        if (e == null && totalRead != contentLength)
            e = new com.android.slyce.async.http.filter.PrematureDataEndException("End of data reached before content length was read: " + totalRead + "/" + contentLength + " Paused: " + isPaused());
        super.report(e);
    }

    long contentLength;
    long totalRead;
    com.android.slyce.async.ByteBufferList transformed = new com.android.slyce.async.ByteBufferList();
    @Override
    public void onDataAvailable(com.android.slyce.async.DataEmitter emitter, com.android.slyce.async.ByteBufferList bb) {
        assert totalRead < contentLength;

        int remaining = bb.remaining();
        long toRead = Math.min(contentLength - totalRead, remaining);

        bb.get(transformed, (int)toRead);

        int beforeRead = transformed.remaining();

        super.onDataAvailable(emitter, transformed);

        totalRead += (beforeRead - transformed.remaining());
        transformed.get(bb);

        if (totalRead == contentLength)
            report(null);
    }
}
