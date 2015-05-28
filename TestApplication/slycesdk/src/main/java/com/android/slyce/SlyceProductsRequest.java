package com.android.slyce;

import android.graphics.Bitmap;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.requests.SlyceBaseRequest;
import com.android.slyce.socket.WSConnection;
import org.json.JSONObject;

/**
 * <p>Provides a way to perform visual search without using SDK’s integral camera functionality.
 * Set of asynchronous methods that allow developers to send a request for recognition by providing an Bitmap or an image URL to the image.</p>
 */
public final class SlyceProductsRequest extends SlyceBaseRequest {

    private final String TAG = SlyceProductsRequest.class.getSimpleName();

    /**
     * Searching products given an image Url
     *
     * @param slyce Slyce SDK object. Must be opened before using it.
     * @param listener the {@link OnSlyceRequestListener} to notify of results and errors.
     * @param imageUrl the image Url.
     */
    public SlyceProductsRequest(Slyce slyce, OnSlyceRequestListener listener, String imageUrl) {
        super(slyce, listener, WSConnection.MethodType.SEND_IMAGE_URL);

        wsConnection.setImageUrl(imageUrl);
    }

    /**
     * Searching products given an image (Bitmap)
     *
     * @param slyce Slyce SDK object. Must be opened before using it.
     * @param listener the {@link OnSlyceRequestListener} to notify of results and errors.
     * @param image the image Bitmap.
     */
    public SlyceProductsRequest(Slyce slyce, OnSlyceRequestListener listener, Bitmap image) {
        super(slyce, listener, WSConnection.MethodType.SEND_IMAGE);

        wsConnection.setBitmap(image);
    }

    /**
     * @param options use this JSONObject to pass properties to Slyce servers. Can be null.
     */
    public SlyceProductsRequest(Slyce slyce, OnSlyceRequestListener listener, String imageUrl, JSONObject options) {
        this(slyce, listener, imageUrl);

        wsConnection.setOptions(options);
    }

    /**
     * @param options use this JSONObject to pass properties to Slyce servers. Can be null.
     */
    public SlyceProductsRequest(Slyce slyce, OnSlyceRequestListener listener, Bitmap image, JSONObject options) {
        this(slyce, listener, image);

        wsConnection.setOptions(options);
    }
}
