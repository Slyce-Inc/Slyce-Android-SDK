package com.android.slyce.async;

import java.security.cert.X509Certificate;

import javax.net.ssl.SSLEngine;

public interface AsyncSSLSocket extends com.android.slyce.async.AsyncSocket {
    public X509Certificate[] getPeerCertificates();
    public SSLEngine getSSLEngine();
}
