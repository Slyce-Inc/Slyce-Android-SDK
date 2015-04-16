package com.android.slyce.async.http.body;

import org.w3c.dom.Document;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by koush on 8/30/13.
 */
public class DocumentBody implements com.android.slyce.async.http.body.AsyncHttpRequestBody<Document> {
    public DocumentBody() {
        this(null);
    }

    public DocumentBody(Document document) {
        this.document = document;
    }

    ByteArrayOutputStream bout;
    private void prepare() {
        if (bout != null)
            return;

        try {
            DOMSource source = new DOMSource(document);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            bout = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(bout);
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);
            writer.flush();
        }
        catch (Exception e) {
        }
    }

    @Override
    public void write(com.android.slyce.async.http.AsyncHttpRequest request, com.android.slyce.async.DataSink sink, com.android.slyce.async.callback.CompletedCallback completed) {
        prepare();
        byte[] bytes = bout.toByteArray();
        com.android.slyce.async.Util.writeAll(sink, bytes, completed);
    }

    @Override
    public void parse(com.android.slyce.async.DataEmitter emitter, final com.android.slyce.async.callback.CompletedCallback completed) {
        new com.android.slyce.async.parser.DocumentParser().parse(emitter).setCallback(new com.android.slyce.async.future.FutureCallback<Document>() {
            @Override
            public void onCompleted(Exception e, Document result) {
                document = result;
                completed.onCompleted(e);
            }
        });
    }

    public static final String CONTENT_TYPE = "application/xml";

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public boolean readFullyOnRequest() {
        return true;
    }

    @Override
    public int length() {
        prepare();
        return bout.size();
    }

    Document document;
    @Override
    public Document get() {
        return document;
    }
}
