package com.android.slyce.async.http.socketio;

import org.json.JSONArray;

public interface EventCallback {
    public void onEvent(JSONArray argument, com.android.slyce.async.http.socketio.Acknowledge acknowledge);
}