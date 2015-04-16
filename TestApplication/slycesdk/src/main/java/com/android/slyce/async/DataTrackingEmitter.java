package com.android.slyce.async;

/**
 * Created by koush on 5/28/13.
 */
public interface DataTrackingEmitter extends com.android.slyce.async.DataEmitter {
    public interface DataTracker {
        void onData(int totalBytesRead);
    }
    void setDataTracker(DataTracker tracker);
    DataTracker getDataTracker();
    int getBytesRead();
    void setDataEmitter(com.android.slyce.async.DataEmitter emitter);
}
