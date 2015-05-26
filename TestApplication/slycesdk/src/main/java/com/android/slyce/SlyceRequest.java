package com.android.slyce;

import android.graphics.Bitmap;

import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.requests.SlyceBaseRequest;
import com.android.slyce.socket.WSConnection;

import org.json.JSONObject;

/**
 * Created by davidsvilem on 3/31/15.
 */
public final class SlyceRequest extends SlyceBaseRequest {

    private final String TAG = SlyceRequest.class.getSimpleName();



    public SlyceRequest(Slyce slyce, OnSlyceRequestListener listener, String imageUrl) {
        super(slyce, listener, WSConnection.MethodType.SEND_IMAGE_URL);

        wsConnection.setImageUrl(imageUrl);
    }

    public SlyceRequest(Slyce slyce, OnSlyceRequestListener listener, Bitmap image) {
        super(slyce, listener, WSConnection.MethodType.SEND_IMAGE);

        wsConnection.setBitmap(image);
    }

    public SlyceRequest(Slyce slyce, OnSlyceRequestListener listener, String imageUrl, JSONObject options) {
        this(slyce, listener, imageUrl);

        wsConnection.setOptions(options);
    }

    public SlyceRequest(Slyce slyce, OnSlyceRequestListener listener, Bitmap image, JSONObject options) {
        this(slyce, listener, image);

        wsConnection.setOptions(options);
    }
}
