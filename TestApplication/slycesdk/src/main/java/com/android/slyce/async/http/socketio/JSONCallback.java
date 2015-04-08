package com.android.slyce.async.http.socketio;

import org.json.JSONObject;

public interface JSONCallback {
    public void onJSON(JSONObject json, com.android.slyce.async.http.socketio.Acknowledge acknowledge);
}
    
