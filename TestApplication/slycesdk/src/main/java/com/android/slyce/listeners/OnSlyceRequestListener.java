package com.android.slyce.listeners;

import com.android.slyce.SlyceBarcode;
import com.android.slyce.enums.SlyceRequestStage;

import org.json.JSONArray;
import org.json.JSONObject;

public interface OnSlyceRequestListener {

    /** Called when 3D products are found
     *  @param products a JSONObject of products.
     *  */
    void on3DRecognition(JSONObject products); // onResultsReceived

    /** Called when barcode is found
     * @param barcode a barcode object.
     * */
    void onBarcodeRecognition(SlyceBarcode barcode); // onBarcodeDetected

    /** Called when 2D products are found
     *  @param irId representing the recognized 2D products in base64 format. Can be en empty string in case no match has been found.
     *  @param productInfo representing a short info about the matched 2D products. Can be empty in case no match has been found.
     *  */
    void on2DRecognition(String irId, String productInfo); // onImageDetected

    /** Called when additional info for the previously recognized 2D product is found.
     *  @param products a JSONArray of additional info.
     *  */
    void on2DExtendedRecognition(JSONArray products); // onImageInfoReceived

    /** Reporting the stage currently being processed.
     *  @param message current stage.
     *  */
    void onSlyceRequestStage(SlyceRequestStage message);

    /** Reporting a numeric value and informative message.
     *  @param progress a value from 0-100
     *  @param message a short description of the current search stage.
     *  @param id a unique request id
     *  */
    void onSlyceProgress(long progress, String message, String id);

    /** Called when an error occured
     *  @param message the error description
     *  */
    void onError(String message);

    /** Called when Slyce search process ended
     *
     */
    void onFinished();
}
