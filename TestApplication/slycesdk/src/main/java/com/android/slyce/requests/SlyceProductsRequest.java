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

    public SlyceProductsRequest(Slyce slyce, OnSlyceRequestListener listener, String imageUrl) {
        super(slyce, listener, WSConnection.MethodType.SEND_IMAGE_URL);

        wsConnection.setImageUrl(imageUrl);
    }

    public SlyceProductsRequest(Slyce slyce, OnSlyceRequestListener listener, Bitmap image) {
        super(slyce, listener, WSConnection.MethodType.SEND_IMAGE);

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
}
