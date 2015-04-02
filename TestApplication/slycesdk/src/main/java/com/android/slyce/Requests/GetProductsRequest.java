package com.android.slyce.Requests;

import android.graphics.Bitmap;
import android.util.Log;

import com.android.slyce.Slyce;
import com.android.slyce.SlyceRequest;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.socket.WSConnection;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by davidsvilem on 3/31/15.
 */
public final class GetProductsRequest extends SlyceRequest implements WSConnection.OnTokenListener{

    private final String TAG = GetProductsRequest.class.getSimpleName();

    private AtomicBoolean oneShotexecution = new AtomicBoolean(false);

    private WSConnection.MethodType type;

    private String token;

    public GetProductsRequest(Slyce slyce, OnSlyceRequestListener listener, String imageUrl) {
        super(slyce, listener);

        connection.setOnTokenListener(this);

        type = WSConnection.MethodType.SEND_IMAGE_URL;
        connection.setImageUrl(imageUrl);
    }

    public GetProductsRequest(Slyce slyce, OnSlyceRequestListener listener, Bitmap image) {
        super(slyce, listener);

        connection.setOnTokenListener(this);

        type = WSConnection.MethodType.SEND_IMAGE;
        connection.setBitmap(image);
    }

    public void execute(){
        if(!oneShotexecution.compareAndSet(false, true)){

            Log.e(TAG, "execute can be called only once, please create another instance of GetProductsRequest");
            return;
        }

        if(connection == null){
            Log.e(TAG, "Please call GetProductsRequest(...) C'tor before execute()");
            return;
        }

        connection.connect(type);
    }

    public String getToken(){
        return token;
    }

    @Override
    public void onTokenReceived(String token) {
        this.token = token;
    }
}
