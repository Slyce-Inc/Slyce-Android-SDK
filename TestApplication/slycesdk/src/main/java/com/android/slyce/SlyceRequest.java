package com.android.slyce;

import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.socket.WSConnection;

/**
 * Created by davidsvilem on 3/23/15.
 */
public class SlyceRequest {

    protected WSConnection connection;

    public SlyceRequest(Slyce slyce, OnSlyceRequestListener listener){

        connection = new WSConnection(slyce.getContext() , slyce.getClientID(), listener);
    }
}
