package com.android.slyce.requests;

import com.android.slyce.Slyce;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.socket.WSConnection;
import com.android.slyce.utils.SlyceLog;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by davidsvilem on 3/23/15.
 */
public class SlyceRequest implements WSConnection.OnTokenListener{

    private static String TAG = SlyceRequest.class.getSimpleName();

    protected WSConnection wsConnection;

    protected Slyce slyce;

    protected String token;

    private AtomicBoolean oneShotexecution = new AtomicBoolean(false);

    public SlyceRequest(Slyce slyce, OnSlyceRequestListener listener, WSConnection.MethodType type){

//        if(slyce == null){
//            Log.e(TAG,"Slyce object is null");
//            return;
//        }

        this.slyce = slyce;

        wsConnection = new WSConnection(slyce.getContext() , slyce.getClientID(), slyce.is2DSearchEnabled(),listener, type);

        wsConnection.setOnTokenListener(this);
    }

    public String getToken(){
        return token;
    }

    public void cancel(){
        wsConnection.close();
    }

    public void execute(){
        if(!oneShotexecution.compareAndSet(false, true)){
            SlyceLog.e(TAG, "execute can be called only once, please create another instance of GetProductsRequest");
            return;
        }

        if(wsConnection == null){
            SlyceLog.e(TAG, "Please call GetProductsRequest(...) C'tor before execute()");
            return;
        }

        if(!slyce.isOpen()){
            SlyceLog.e(TAG, "Slyce SDK is not initialized");
            return;
        }

        wsConnection.connect();
    }

    @Override
    public void onTokenReceived(String token) {
        this.token = token;
    }
}
