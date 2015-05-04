package com.android.slyce.requests;

import android.graphics.Bitmap;
import com.android.slyce.Slyce;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.socket.WSConnection;
import com.android.slyce.utils.SlyceLog;

import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by davidsvilem on 3/31/15.
 */
public final class SlyceProductsRequest extends SlyceRequest {

    private final String TAG = SlyceProductsRequest.class.getSimpleName();

    private AtomicBoolean oneShotexecution = new AtomicBoolean(false);

    private WSConnection.MethodType type;

    public SlyceProductsRequest(Slyce slyce, OnSlyceRequestListener listener, String imageUrl) {
        super(slyce, listener);

        type = WSConnection.MethodType.SEND_IMAGE_URL;
        wsConnection.setImageUrl(imageUrl);
    }

    public SlyceProductsRequest(Slyce slyce, OnSlyceRequestListener listener, Bitmap image) {
        super(slyce, listener);

        type = WSConnection.MethodType.SEND_IMAGE;
        wsConnection.setBitmap(image);
    }

    public SlyceProductsRequest(Slyce slyce, OnSlyceRequestListener listener, String imageUrl, JSONObject options) {
        this(slyce, listener, imageUrl);

        wsConnection.setOptions(options);
    }

    public SlyceProductsRequest(Slyce slyce, OnSlyceRequestListener listener, Bitmap image, JSONObject options) {
        this(slyce, listener, image);

        wsConnection.setOptions(options);
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

        wsConnection.connect(type);
    }
}
