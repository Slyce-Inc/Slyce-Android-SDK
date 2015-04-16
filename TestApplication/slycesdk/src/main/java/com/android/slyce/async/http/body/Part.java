package com.android.slyce.async.http.body;

import com.android.slyce.async.http.Headers;
import com.android.slyce.async.http.Multimap;

import org.apache.http.NameValuePair;

import java.io.File;
import java.util.List;

public class Part {
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    
    Headers mHeaders;
    Multimap mContentDisposition;
    public Part(Headers headers) {
        mHeaders = headers;
        mContentDisposition = com.android.slyce.async.http.Multimap.parseSemicolonDelimited(mHeaders.get(CONTENT_DISPOSITION));
    }
    
    public String getName() {
        return mContentDisposition.getString("name");
    }
    
    private long length = -1;
    public Part(String name, long length, List<NameValuePair> contentDisposition) {
        this.length = length;
        mHeaders = new Headers();
        StringBuilder builder = new StringBuilder(String.format("form-data; name=\"%s\"", name));
        if (contentDisposition != null) {
            for (NameValuePair pair: contentDisposition) {
                builder.append(String.format("; %s=\"%s\"", pair.getName(), pair.getValue()));
            }
        }
        mHeaders.set(CONTENT_DISPOSITION, builder.toString());
        mContentDisposition = Multimap.parseSemicolonDelimited(mHeaders.get(CONTENT_DISPOSITION));
    }

    public com.android.slyce.async.http.Headers getRawHeaders() {
        return mHeaders;
    }

    public String getContentType() {
        return mHeaders.get("Content-Type");
    }

    public void setContentType(String contentType) {
        mHeaders.set("Content-Type", contentType);
    }

    public String getFilename() {
        String file = mContentDisposition.getString("filename");
        if (file == null)
            return null;
        return new File(file).getName();
    }

    public boolean isFile() {
        return mContentDisposition.containsKey("filename");
    }
    
    public long length() {
        return length;
    }
    
    public void write(com.android.slyce.async.DataSink sink, com.android.slyce.async.callback.CompletedCallback callback) {
        assert false;
    }
}
