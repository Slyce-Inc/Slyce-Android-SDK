package com.android.slyce.async.http.socketio;

import org.json.JSONArray;

import java.util.Iterator;
import java.util.List;

/**
 * Created by koush on 7/1/13.
 */
public class EventEmitter {
    interface OnceCallback extends com.android.slyce.async.http.socketio.EventCallback {
    }

    com.android.slyce.async.util.HashList<com.android.slyce.async.http.socketio.EventCallback> callbacks = new com.android.slyce.async.util.HashList<com.android.slyce.async.http.socketio.EventCallback>();
    void onEvent(String event, JSONArray arguments, Acknowledge acknowledge) {
        List<com.android.slyce.async.http.socketio.EventCallback> list = callbacks.get(event);
        if (list == null)
            return;
        Iterator<com.android.slyce.async.http.socketio.EventCallback> iter = list.iterator();
        while (iter.hasNext()) {
            com.android.slyce.async.http.socketio.EventCallback cb = iter.next();
            cb.onEvent(arguments, acknowledge);
            if (cb instanceof OnceCallback)
                iter.remove();
        }
    }

    public void addListener(String event, com.android.slyce.async.http.socketio.EventCallback callback) {
        on(event, callback);
    }

    public void once(final String event, final com.android.slyce.async.http.socketio.EventCallback callback) {
        on(event, new OnceCallback() {
            @Override
            public void onEvent(JSONArray arguments, Acknowledge acknowledge) {
                callback.onEvent(arguments, acknowledge);
            }
        });
    }

    public void on(String event, com.android.slyce.async.http.socketio.EventCallback callback) {
        callbacks.add(event, callback);
    }

    public void removeListener(String event, com.android.slyce.async.http.socketio.EventCallback callback) {
        List<com.android.slyce.async.http.socketio.EventCallback> list = callbacks.get(event);
        if (list == null)
            return;
        list.remove(callback);
    }
}
