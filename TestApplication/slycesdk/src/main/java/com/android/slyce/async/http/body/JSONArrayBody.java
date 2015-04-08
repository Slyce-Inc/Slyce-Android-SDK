package com.android.slyce.async.http.body;

import org.json.JSONArray;

public class JSONArrayBody implements com.android.slyce.async.http.body.AsyncHttpRequestBody<JSONArray> {
    public JSONArrayBody() {
    }

    byte[] mBodyBytes;
    JSONArray json;
    public JSONArrayBody(JSONArray json) {
        this();
        this.json = json;
    }

    @Override
    public void parse(com.android.slyce.async.DataEmitter emitter, final com.android.slyce.async.callback.CompletedCallback completed) {
        new com.android.slyce.async.parser.JSONArrayParser().parse(emitter).setCallback(new com.android.slyce.async.future.FutureCallback<JSONArray>() {
            @Override
            public void onCompleted(Exception e, JSONArray result) {
                json = result;
                completed.onCompleted(e);
            }
        });
    }

    @Override
    public void write(com.android.slyce.async.http.AsyncHttpRequest request, com.android.slyce.async.DataSink sink, final com.android.slyce.async.callback.CompletedCallback completed) {
        com.android.slyce.async.Util.writeAll(sink, mBodyBytes, completed);
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public boolean readFullyOnRequest() {
        return true;
    }

    @Override
    public int length() {
        mBodyBytes = json.toString().getBytes();
        return mBodyBytes.length;
    }

    public static final String CONTENT_TYPE = "application/json";

    @Override
    public JSONArray get() {
        return json;
    }
}

