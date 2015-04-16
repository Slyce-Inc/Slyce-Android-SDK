package com.android.slyce.async.http.body;

import org.apache.http.NameValuePair;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

public class UrlEncodedFormBody implements com.android.slyce.async.http.body.AsyncHttpRequestBody<com.android.slyce.async.http.Multimap> {
    private com.android.slyce.async.http.Multimap mParameters;
    private byte[] mBodyBytes;

    public UrlEncodedFormBody(com.android.slyce.async.http.Multimap parameters) {
        mParameters = parameters;
    }

    public UrlEncodedFormBody(List<NameValuePair> parameters) {
        mParameters = new com.android.slyce.async.http.Multimap(parameters);
    }

    private void buildData() {
        boolean first = true;
        StringBuilder b = new StringBuilder();
        try {
            for (NameValuePair pair: mParameters) {
                if (pair.getValue() == null)
                    continue;
                if (!first)
                    b.append('&');
                first = false;

                b.append(URLEncoder.encode(pair.getName(), "UTF-8"));
                b.append('=');
                b.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
            }
            mBodyBytes = b.toString().getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }
    
    @Override
    public void write(com.android.slyce.async.http.AsyncHttpRequest request, final com.android.slyce.async.DataSink response, final com.android.slyce.async.callback.CompletedCallback completed) {
        if (mBodyBytes == null)
            buildData();
        com.android.slyce.async.Util.writeAll(response, mBodyBytes, completed);
    }

    public static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
    @Override
    public String getContentType() {
        return CONTENT_TYPE + "; charset=utf-8";
    }

    @Override
    public void parse(com.android.slyce.async.DataEmitter emitter, final com.android.slyce.async.callback.CompletedCallback completed) {
        final com.android.slyce.async.ByteBufferList data = new com.android.slyce.async.ByteBufferList();
        emitter.setDataCallback(new com.android.slyce.async.callback.DataCallback() {
            @Override
            public void onDataAvailable(com.android.slyce.async.DataEmitter emitter, com.android.slyce.async.ByteBufferList bb) {
                bb.get(data);
            }
        });
        emitter.setEndCallback(new com.android.slyce.async.callback.CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                if (ex != null) {
                    completed.onCompleted(ex);
                    return;
                }
                try {
                    mParameters = com.android.slyce.async.http.Multimap.parseUrlEncoded(data.readString());
                    completed.onCompleted(null);
                }
                catch (Exception e) {
                    completed.onCompleted(e);
                }
            }
        });
    }

    public UrlEncodedFormBody() {
    }

    @Override
    public boolean readFullyOnRequest() {
        return true;
    }

    @Override
    public int length() {
        if (mBodyBytes == null)
            buildData();
        return mBodyBytes.length;
    }

    @Override
    public com.android.slyce.async.http.Multimap get() {
        return mParameters;
    }
}
