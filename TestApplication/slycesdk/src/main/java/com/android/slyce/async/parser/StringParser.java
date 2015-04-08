package com.android.slyce.async.parser;

import java.lang.reflect.Type;
import java.nio.charset.Charset;

/**
 * Created by koush on 5/27/13.
 */
public class StringParser implements AsyncParser<String> {
    Charset forcedCharset;

    public StringParser() {
    }

    public StringParser(Charset charset) {
        this.forcedCharset = charset;
    }

    @Override
    public com.android.slyce.async.future.Future<String> parse(com.android.slyce.async.DataEmitter emitter) {
        final String charset = emitter.charset();
        return new com.android.slyce.async.parser.ByteBufferListParser().parse(emitter)
        .then(new com.android.slyce.async.future.TransformFuture<String, com.android.slyce.async.ByteBufferList>() {
            @Override
            protected void transform(com.android.slyce.async.ByteBufferList result) throws Exception {
                Charset charsetToUse = forcedCharset;
                if (charsetToUse == null && charset != null)
                    charsetToUse = Charset.forName(charset);
                setComplete(result.readString(charsetToUse));
            }
        });
    }

    @Override
    public void write(com.android.slyce.async.DataSink sink, String value, com.android.slyce.async.callback.CompletedCallback completed) {
        new com.android.slyce.async.parser.ByteBufferListParser().write(sink, new com.android.slyce.async.ByteBufferList(value.getBytes()), completed);
    }

    @Override
    public Type getType() {
        return String.class;
    }
}
