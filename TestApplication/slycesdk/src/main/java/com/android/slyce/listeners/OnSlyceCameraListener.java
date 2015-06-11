package com.android.slyce.listeners;

import android.graphics.Bitmap;
import com.android.slyce.SlyceBarcode;
import com.android.slyce.enums.SlyceRequestStage;
import org.json.JSONArray;
import org.json.JSONObject;

public interface OnSlyceCameraListener {

    /** Called when 3D products are found
     *  @param products a JSONObject of products.
     *  */
    void onCameraResultsReceived(JSONObject products);

    /** Called when barcode is found
     * @param barcode a barcode object.
     * */
    void onCameraBarcodeDetected(SlyceBarcode barcode);

    /** Called when 2D products are found
     *  @param irId representing the recognized 2D products in base64 format. Can be en empty string in case no match has been found.
     *  @param productInfo representing a short info about the matched 2D products. Can be empty in case no match has been found.
     *  */
    void onCameraImageDetected(String irId, String productInfo);

    /** Called when additional info for the previously recognized 2D product is found.
     *  @param products a JSONArray of additional info.
     *  */
    void onCameraImageInfoReceived(JSONArray products);

    /** Reporting the stage currently being processed.
     *  @param message current stage.
     *  */
    void onCameraSlyceRequestStage(SlyceRequestStage message);

    /** Reporting a numeric value and informative message.
     *  @param progress a value from 0-100
     *  @param message a short description of the current search stage.
     *  @param id a unique request id
     *  */
    void onCameraSlyceProgress(long progress, String message, String id);

    /** Called when an error occured
     *  @param message the error description
     *  */
    void onSlyceCameraError(String message);

    /** Called when the snapped bitmap is ready after SlyceCamera.snap() was invoked
     *  @param bitmap the error description
     *  */
    void onSnap(Bitmap bitmap);

    /** Called when the camera was touched in a specific point.
     *  @param x axis point
     *  @param y axis point
     *  */
    void onTap(float x, float y);

    /** Called when Slyce search process ended
     *
     */
    void onCameraFinished();
}
