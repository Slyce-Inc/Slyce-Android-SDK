package com.android.slyce.requests;

import com.android.slyce.Slyce;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.socket.WSConnection;

/**
 * Created by davidsvilem on 3/23/15.
 */
public class SlyceRequest implements WSConnection.OnTokenListener{

    private static String TAG = SlyceRequest.class.getSimpleName();

    protected WSConnection wsConnection;

    protected Slyce slyce;

    protected String token;

    public SlyceRequest(Slyce slyce, OnSlyceRequestListener listener){

//        if(slyce == null){
//            Log.e(TAG,"Slyce object is null");
//            return;
//        }

        this.slyce = slyce;

        wsConnection = new WSConnection(slyce.getContext() , slyce.getClientID(), slyce.is2DSearchEnabled(),listener);

        wsConnection.setOnTokenListener(this);
    }

    protected String getToken(){
        return token;
    }

    @Override
    public void onTokenReceived(String token) {
        this.token = token;
    }

    public void cancel(){
        wsConnection.close();
    }
}
