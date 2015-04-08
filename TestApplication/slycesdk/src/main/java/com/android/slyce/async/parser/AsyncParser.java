package com.android.slyce.async.parser;

import java.lang.reflect.Type;

/**
 * Created by koush on 5/27/13.
 */
public interface AsyncParser<T> {
    com.android.slyce.async.future.Future<T> parse(com.android.slyce.async.DataEmitter emitter);
    void write(com.android.slyce.async.DataSink sink, T value, com.android.slyce.async.callback.CompletedCallback completed);
    Type getType();
}
