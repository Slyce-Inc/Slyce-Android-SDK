package com.android.slyce.async.parser;

import org.json.JSONArray;

import java.lang.reflect.Type;

/**
 * Created by koush on 5/27/13.
 */
public class JSONArrayParser implements AsyncParser<JSONArray> {
    @Override
    public com.android.slyce.async.future.Future<JSONArray> parse(com.android.slyce.async.DataEmitter emitter) {
        return new com.android.slyce.async.parser.StringParser().parse(emitter)
        .then(new com.android.slyce.async.future.TransformFuture<JSONArray, String>() {
            @Override
            protected void transform(String result) throws Exception {
                setComplete(new JSONArray(result));
            }
        });
    }

    @Override
    public void write(com.android.slyce.async.DataSink sink, JSONArray value, com.android.slyce.async.callback.CompletedCallback completed) {
        new com.android.slyce.async.parser.StringParser().write(sink, value.toString(), completed);
    }

    @Override
    public Type getType() {
        return JSONArray.class;
    }
}
