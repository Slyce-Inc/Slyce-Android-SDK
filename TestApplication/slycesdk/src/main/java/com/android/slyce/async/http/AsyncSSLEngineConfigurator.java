package com.android.slyce.async.http;

import javax.net.ssl.SSLEngine;

public interface AsyncSSLEngineConfigurator {
    public void configureEngine(SSLEngine engine, com.android.slyce.async.http.AsyncHttpClientMiddleware.GetSocketData data, String host, int port);
}
