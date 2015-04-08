package com.android.slyce.async;

import com.android.slyce.async.callback.WritableCallback;

public interface DataSink {
    public void write(ByteBufferList bb);
    public void setWriteableCallback(WritableCallback handler);
    public WritableCallback getWriteableCallback();
    
    public boolean isOpen();
    public void end();
    public void setClosedCallback(com.android.slyce.async.callback.CompletedCallback handler);
    public com.android.slyce.async.callback.CompletedCallback getClosedCallback();
    public com.android.slyce.async.AsyncServer getServer();
}
