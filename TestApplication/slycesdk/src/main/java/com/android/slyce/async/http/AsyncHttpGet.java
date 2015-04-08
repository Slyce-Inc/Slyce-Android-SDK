package com.android.slyce.async.http;

import android.net.Uri;

public class AsyncHttpGet extends com.android.slyce.async.http.AsyncHttpRequest {
    public static final String METHOD = "GET";
    
    public AsyncHttpGet(String uri) {
        super(Uri.parse(uri), METHOD);
    }

    public AsyncHttpGet(Uri uri) {
        super(uri, METHOD);
    }
}
