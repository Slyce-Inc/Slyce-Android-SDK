package com.android.slyce;

import android.graphics.Bitmap;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.requests.SlyceBaseRequest;
import com.android.slyce.socket.WSConnection;
import org.json.JSONObject;

public final class SlyceProductsRequest extends SlyceBaseRequest {

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
