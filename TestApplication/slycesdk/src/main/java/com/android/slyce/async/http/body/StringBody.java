package com.android.slyce.async.http.body;

public class StringBody implements com.android.slyce.async.http.body.AsyncHttpRequestBody<String> {
    public StringBody() {
    }

    byte[] mBodyBytes;
    String string;
    public StringBody(String string) {
        this();
        this.string = string;
    }

    @Override
    public void parse(com.android.slyce.async.DataEmitter emitter, final com.android.slyce.async.callback.CompletedCallback completed) {
        new com.android.slyce.async.parser.StringParser().parse(emitter).setCallback(new com.android.slyce.async.future.FutureCallback<String>() {
            @Override
            public void onCompleted(Exception e, String result) {
                string = result;
                completed.onCompleted(e);
            }
        });
    }

    public static final String CONTENT_TYPE = "text/plain";

    @Override
    public void write(com.android.slyce.async.http.AsyncHttpRequest request, com.android.slyce.async.DataSink sink, final com.android.slyce.async.callback.CompletedCallback completed) {
        if (mBodyBytes == null)
            mBodyBytes = string.getBytes();
        com.android.slyce.async.Util.writeAll(sink, mBodyBytes, completed);
    }

    @Override
    public String getContentType() {
        return "text/plain";
    }

    @Override
    public boolean readFullyOnRequest() {
        return true;
    }

    @Override
    public int length() {
        if (mBodyBytes == null)
            mBodyBytes = string.getBytes();
        return mBodyBytes.length;
    }

    @Override
    public String toString() {
        return string;
    }

    @Override
    public String get() {
        return toString();
    }
}
