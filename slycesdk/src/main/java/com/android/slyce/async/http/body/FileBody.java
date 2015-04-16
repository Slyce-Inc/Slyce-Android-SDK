package com.android.slyce.async.http.body;

import java.io.File;

/**
 * Created by koush on 10/14/13.
 */
public class FileBody implements AsyncHttpRequestBody<File> {
    public static final String CONTENT_TYPE = "application/binary";

    File file;
    String contentType = CONTENT_TYPE;

    public FileBody(File file) {
        this.file = file;
    }

    public FileBody(File file, String contentType) {
        this.file = file;
        this.contentType = contentType;
    }

    @Override
    public void write(com.android.slyce.async.http.AsyncHttpRequest request, com.android.slyce.async.DataSink sink, com.android.slyce.async.callback.CompletedCallback completed) {
        com.android.slyce.async.Util.pump(file, sink, completed);
    }

    @Override
    public void parse(com.android.slyce.async.DataEmitter emitter, com.android.slyce.async.callback.CompletedCallback completed) {
        throw new AssertionError("not implemented");
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public boolean readFullyOnRequest() {
        throw new AssertionError("not implemented");
    }

    @Override
    public int length() {
        return (int)file.length();
    }

    @Override
    public File get() {
        return file;
    }
}
