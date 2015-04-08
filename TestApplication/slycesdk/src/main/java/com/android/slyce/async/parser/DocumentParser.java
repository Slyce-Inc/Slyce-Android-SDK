package com.android.slyce.async.parser;

import org.w3c.dom.Document;

import java.lang.reflect.Type;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by koush on 8/3/13.
 */
public class DocumentParser implements AsyncParser<Document> {
    @Override
    public com.android.slyce.async.future.Future<Document> parse(com.android.slyce.async.DataEmitter emitter) {
        return new com.android.slyce.async.parser.ByteBufferListParser().parse(emitter)
        .then(new com.android.slyce.async.future.TransformFuture<Document, com.android.slyce.async.ByteBufferList>() {
            @Override
            protected void transform(com.android.slyce.async.ByteBufferList result) throws Exception {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                setComplete(db.parse(new com.android.slyce.async.stream.ByteBufferListInputStream(result)));
            }
        });
    }

    @Override
    public void write(com.android.slyce.async.DataSink sink, Document value, com.android.slyce.async.callback.CompletedCallback completed) {
        new com.android.slyce.async.http.body.DocumentBody(value).write(null, sink, completed);
    }

    @Override
    public Type getType() {
        return Document.class;
    }
}
