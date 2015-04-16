package com.android.slyce.async.http;


public interface WebSocket extends com.android.slyce.async.AsyncSocket {
    static public interface StringCallback {
        public void onStringAvailable(String s);
    }
    static public interface PongCallback {
        public void onPongReceived(String s);
    }

    public void send(byte[] bytes);
    public void send(String string);
    public void send(byte[] bytes, int offset, int len);
    public void ping(String message);
    
    public void setStringCallback(StringCallback callback);
    public StringCallback getStringCallback();

    public void setPongCallback(PongCallback callback);
    public PongCallback getPongCallback();

    public boolean isBuffering();
    
    public com.android.slyce.async.AsyncSocket getSocket();
}
