package com.android.slyce.listeners;

import com.android.slyce.SlyceBarcode;

import org.json.JSONArray;
import org.json.JSONObject;


public interface OnSlyceCameraFragmentListener {

    /** Called when barcode is found
     * @param barcode a barcode object.
     * */
    void onCameraFragmentBarcodeDetected(SlyceBarcode barcode);

    /** Called when 2D products are found
     *  @param productInfo representing a short info about the matched 2D products. Can be empty in case no match has been found.
     *
     *  <p> This method will be called upon a successful match either from a live video preview (automatic scanner) or from an image captured (by pressing snap button)
     *      or picked from the gallery<p/>
     *  */
    void onCameraFragmentImageDetected(String productInfo);

    /** Called when additional info for the previously recognized 2D product is found.
     *  @param products a JSONArray of additional info.
     *  */
    void onCameraFragmentImageInfoReceived(JSONArray products);

    /** Called when 3D products are found
     *  @param products a JSONObject of products. Can be empty in case no match was found.
     *  */
    void onCameraFragmentResultsReceived(JSONObject products);

    /** Called when an error occured
     *  @param message the error description
     *  */
    void onCameraFragmentError(String message);

    /** Called when Slyce search process ended
     *
     */
    void onCameraFragmentFinished();
}
