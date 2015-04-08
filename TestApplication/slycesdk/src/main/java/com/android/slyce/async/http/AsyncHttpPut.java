package com.android.slyce.async.http;

import android.net.Uri;

public class AsyncHttpPut extends com.android.slyce.async.http.AsyncHttpRequest {
    public static final String METHOD = "PUT";
    
    public AsyncHttpPut(String uri) {
        this(Uri.parse(uri));
    }

    public AsyncHttpPut(Uri uri) {
        super(uri, METHOD);
    }
}
