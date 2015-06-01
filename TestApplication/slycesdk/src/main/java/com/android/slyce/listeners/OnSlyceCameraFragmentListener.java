package com.android.slyce.listeners;

import com.android.slyce.SlyceBarcode;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by davidsvilem on 5/17/15.
 */
public interface OnSlyceCameraFragmentListener {

    /** Called when barcode is found
     * @param barcode a barcode object.
     * */
    void onCameraFragmentBarcodeRecognition(SlyceBarcode barcode);

    /** Called when 2D products are found
     *  @param irId representing the recognized 2D products in base64 format. Can be en empty string in case no match has been found.
     *  @param productInfo representing a short info about the matched 2D products. Can be empty in case no match has been found.
     *
     *  <p> This method will be called upon a successful match either from a live video preview (automatic scanner) or from an image captured (by pressing snap button)
     *      or picked from the gallery<p/>
     *  */
    void onCameraFragment2DRecognition(String irId, String productInfo);

    /** Called when additional info for the previously recognized 2D product is found.
     *  @param products a JsonArray of additional info.
     *  */
    void onCameraFragment2DExtendedRecognition(JSONObject products);

    /** Called when 3D products are found
     *  @param products a JsonArray of products. Can be empty in case no match was found.
     *  */
    void onCameraFragment3DRecognition(JSONArray products);

    /** Called when an error occured
     *  @param message the error description
     *  */
    void onCameraFragmentError(String message);
}
