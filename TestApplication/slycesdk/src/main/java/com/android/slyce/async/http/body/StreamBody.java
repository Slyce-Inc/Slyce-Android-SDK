package com.android.slyce.async.http.body;

import com.android.slyce.async.DataEmitter;
import com.android.slyce.async.DataSink;
import com.android.slyce.async.Util;
import com.android.slyce.async.callback.CompletedCallback;
import com.android.slyce.async.http.AsyncHttpRequest;

import java.io.InputStream;

public class StreamBody implements AsyncHttpRequestBody<InputStream> {
    InputStream stream;
    int length;

    /**
     * Construct an http body from a stream
     * @param stream
     * @param length Length of stream to read, or value < 0 to read to end
     */
    public StreamBody(InputStream stream, int length) {
        this.stream = stream;
        this.length = length;
    }

    @Override
    public void write(AsyncHttpRequest request, DataSink sink, CompletedCallback completed) {
        Util.pump(stream, length < 0 ? Integer.MAX_VALUE : length, sink, completed);
    }

    @Override
    public void parse(DataEmitter emitter, CompletedCallback completed) {
        throw new AssertionError("not implemented");
    }

    public static final String CONTENT_TYPE = "application/binary";
    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public boolean readFullyOnRequest() {
        throw new AssertionError("not implemented");
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public InputStream get() {
        return stream;
    }
}
