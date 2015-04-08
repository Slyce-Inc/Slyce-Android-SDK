package com.android.slyce.async.parser;

import org.json.JSONObject;

import java.lang.reflect.Type;

/**
 * Created by koush on 5/27/13.
 */
public class JSONObjectParser implements AsyncParser<JSONObject> {
    @Override
    public com.android.slyce.async.future.Future<JSONObject> parse(com.android.slyce.async.DataEmitter emitter) {
        return new com.android.slyce.async.parser.StringParser().parse(emitter)
        .then(new com.android.slyce.async.future.TransformFuture<JSONObject, String>() {
            @Override
            protected void transform(String result) throws Exception {
                setComplete(new JSONObject(result));
            }
        });
    }

    @Override
    public void write(com.android.slyce.async.DataSink sink, JSONObject value, com.android.slyce.async.callback.CompletedCallback completed) {
        new com.android.slyce.async.parser.StringParser().write(sink, value.toString(), completed);
    }

    @Override
    public Type getType() {
        return JSONObject.class;
    }
}
