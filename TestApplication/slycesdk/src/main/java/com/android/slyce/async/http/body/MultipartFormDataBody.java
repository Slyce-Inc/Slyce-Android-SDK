package com.android.slyce.async.http.body;

import com.android.slyce.async.Util;
import com.android.slyce.async.LineEmitter.StringCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

public class MultipartFormDataBody extends com.android.slyce.async.http.server.BoundaryEmitter implements com.android.slyce.async.http.body.AsyncHttpRequestBody<com.android.slyce.async.http.Multimap> {
    com.android.slyce.async.LineEmitter liner;
    com.android.slyce.async.http.Headers formData;
    com.android.slyce.async.ByteBufferList last;
    String lastName;

    public interface MultipartCallback {
        public void onPart(com.android.slyce.async.http.body.Part part);
    }

    @Override
    public void parse(com.android.slyce.async.DataEmitter emitter, final com.android.slyce.async.callback.CompletedCallback completed) {
        setDataEmitter(emitter);
        setEndCallback(completed);
    }

    void handleLast() {
        if (last == null)
            return;

        if (formData == null)
            formData = new com.android.slyce.async.http.Headers();

        formData.add(lastName, last.peekString());

        lastName = null;
        last = null;
    }

    public String getField(String name) {
        if (formData == null)
            return null;
        return formData.get(name);
    }

    @Override
    protected void onBoundaryEnd() {
        super.onBoundaryEnd();
        handleLast();
    }

    @Override
    protected void onBoundaryStart() {
        final com.android.slyce.async.http.Headers headers = new com.android.slyce.async.http.Headers();
        liner = new com.android.slyce.async.LineEmitter();
        liner.setLineCallback(new StringCallback() {
            @Override
            public void onStringAvailable(String s) {
                if (!"\r".equals(s)){
                    headers.addLine(s);
                }
                else {
                    handleLast();

                    liner = null;
                    setDataCallback(null);
                    com.android.slyce.async.http.body.Part part = new com.android.slyce.async.http.body.Part(headers);
                    if (mCallback != null)
                        mCallback.onPart(part);
                    if (getDataCallback() == null) {
                        if (part.isFile()) {
                            setDataCallback(new NullDataCallback());
                            return;
                        }

                        lastName = part.getName();
                        last = new com.android.slyce.async.ByteBufferList();
                        setDataCallback(new com.android.slyce.async.callback.DataCallback() {
                            @Override
                            public void onDataAvailable(com.android.slyce.async.DataEmitter emitter, com.android.slyce.async.ByteBufferList bb) {
                                bb.get(last);
                            }
                        });
                    }
                }
            }
        });
        setDataCallback(liner);
    }

    public static final String CONTENT_TYPE = "multipart/form-data";
    String contentType = CONTENT_TYPE;
    public MultipartFormDataBody(String[] values) {
        for (String value: values) {
            String[] splits = value.split("=");
            if (splits.length != 2)
                continue;
            if (!"boundary".equals(splits[0]))
                continue;
            setBoundary(splits[1]);
            return;
        }
        report(new Exception ("No boundary found for multipart/form-data"));
    }

    MultipartCallback mCallback;
    public void setMultipartCallback(MultipartCallback callback) {
        mCallback = callback;
    }

    public MultipartCallback getMultipartCallback() {
        return mCallback;
    }

    int written;
    @Override
    public void write(com.android.slyce.async.http.AsyncHttpRequest request, final com.android.slyce.async.DataSink sink, final com.android.slyce.async.callback.CompletedCallback completed) {
        if (mParts == null)
            return;

        com.android.slyce.async.future.Continuation c = new com.android.slyce.async.future.Continuation(new com.android.slyce.async.callback.CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                completed.onCompleted(ex);
//                if (ex == null)
//                    sink.end();
//                else
//                    sink.close();
            }
        });

        for (final com.android.slyce.async.http.body.Part part: mParts) {
            c.add(new com.android.slyce.async.callback.ContinuationCallback() {
                @Override
                public void onContinue(com.android.slyce.async.future.Continuation continuation, com.android.slyce.async.callback.CompletedCallback next) throws Exception {
                    byte[] bytes = part.getRawHeaders().toPrefixString(getBoundaryStart()).getBytes();
                    Util.writeAll(sink, bytes, next);
                    written += bytes.length;
                }
            })
            .add(new com.android.slyce.async.callback.ContinuationCallback() {
                @Override
                public void onContinue(com.android.slyce.async.future.Continuation continuation, com.android.slyce.async.callback.CompletedCallback next) throws Exception {
                    long partLength = part.length();
                    if (partLength >= 0)
                        written += partLength;
                    part.write(sink, next);
                }
            })
            .add(new com.android.slyce.async.callback.ContinuationCallback() {
                @Override
                public void onContinue(com.android.slyce.async.future.Continuation continuation, com.android.slyce.async.callback.CompletedCallback next) throws Exception {
                    byte[] bytes = "\r\n".getBytes();
                    Util.writeAll(sink, bytes, next);
                    written += bytes.length;
                }
            });
        }
        c.add(new com.android.slyce.async.callback.ContinuationCallback() {
            @Override
            public void onContinue(com.android.slyce.async.future.Continuation continuation, com.android.slyce.async.callback.CompletedCallback next) throws Exception {
                byte[] bytes = (getBoundaryEnd()).getBytes();
                Util.writeAll(sink, bytes, next);
                written += bytes.length;

                assert written == totalToWrite;
            }
        });
        c.start();
    }

    @Override
    public String getContentType() {
        if (getBoundary() == null) {
            setBoundary("----------------------------" + UUID.randomUUID().toString().replace("-", ""));
        }
        return contentType + "; boundary=" + getBoundary();
    }

    @Override
    public boolean readFullyOnRequest() {
        return false;
    }

    int totalToWrite;
    @Override
    public int length() {
        if (getBoundary() == null) {
            setBoundary("----------------------------" + UUID.randomUUID().toString().replace("-", ""));
        }

        int length = 0;
        for (final com.android.slyce.async.http.body.Part part: mParts) {
            String partHeader = part.getRawHeaders().toPrefixString(getBoundaryStart());
            if (part.length() == -1)
                return -1;
            length += part.length() + partHeader.getBytes().length + "\r\n".length();
        }
        length += (getBoundaryEnd()).getBytes().length;
        return totalToWrite = length;
    }

    public MultipartFormDataBody() {
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void addFilePart(String name, File file) {
        addPart(new com.android.slyce.async.http.body.FilePart(name, file));
    }

    public void addStringPart(String name, String value) {
        addPart(new com.android.slyce.async.http.body.StringPart(name, value));
    }

    private ArrayList<com.android.slyce.async.http.body.Part> mParts;
    public void addPart(com.android.slyce.async.http.body.Part part) {
        if (mParts == null)
            mParts = new ArrayList<com.android.slyce.async.http.body.Part>();
        mParts.add(part);
    }

    @Override
    public com.android.slyce.async.http.Multimap get() {
        return new com.android.slyce.async.http.Multimap(formData.getMultiMap());
    }
}
