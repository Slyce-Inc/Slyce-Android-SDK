package com.android.slyce.async.parser;

import java.lang.reflect.Type;

/**
 * Created by koush on 5/27/13.
 */
public class ByteBufferListParser implements com.android.slyce.async.parser.AsyncParser<com.android.slyce.async.ByteBufferList> {
    @Override
    public com.android.slyce.async.future.Future<com.android.slyce.async.ByteBufferList> parse(final com.android.slyce.async.DataEmitter emitter) {
        final com.android.slyce.async.ByteBufferList bb = new com.android.slyce.async.ByteBufferList();
        final com.android.slyce.async.future.SimpleFuture<com.android.slyce.async.ByteBufferList> ret = new com.android.slyce.async.future.SimpleFuture<com.android.slyce.async.ByteBufferList>() {
            @Override
            protected void cancelCleanup() {
                emitter.close();
            }
        };
        emitter.setDataCallback(new com.android.slyce.async.callback.DataCallback() {
            @Override
            public void onDataAvailable(com.android.slyce.async.DataEmitter emitter, com.android.slyce.async.ByteBufferList data) {
                data.get(bb);
            }
        });

        emitter.setEndCallback(new com.android.slyce.async.callback.CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                if (ex != null) {
                    ret.setComplete(ex);
                    return;
                }

                try {
                    ret.setComplete(bb);
                }
                catch (Exception e) {
                    ret.setComplete(e);
                }
            }
        });

        return ret;
    }

    @Override
    public void write(com.android.slyce.async.DataSink sink, com.android.slyce.async.ByteBufferList value, com.android.slyce.async.callback.CompletedCallback completed) {
        com.android.slyce.async.Util.writeAll(sink, value, completed);
    }

    @Override
    public Type getType() {
        return com.android.slyce.async.ByteBufferList.class;
    }
}
