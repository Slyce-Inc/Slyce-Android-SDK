package com.android.slyce;

import android.util.Log;

import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.socket.WSConnection;

/**
 * Created by davidsvilem on 3/23/15.
 */
public class SlyceRequest {

    private static String TAG = SlyceRequest.class.getSimpleName();

    protected WSConnection connection;

    public SlyceRequest(Slyce slyce, OnSlyceRequestListener listener){

        if(slyce == null){
            Log.e(TAG,"Slyce object is null");
            return;
        }
        connection = new WSConnection(slyce.getContext() , slyce.getClientID(), listener);
    }
}
