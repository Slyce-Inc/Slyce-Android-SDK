package com.android.slyce.communication;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.android.slyce.communication.utils.AuthFailureError;
import com.android.slyce.communication.utils.BasicNetwork;
import com.android.slyce.communication.utils.HttpHeaderParser;
import com.android.slyce.communication.utils.HttpStack;
import com.android.slyce.communication.utils.HurlStack;
import com.android.slyce.communication.utils.JsonObjectRequest;
import com.android.slyce.communication.utils.Network;
import com.android.slyce.communication.utils.NetworkResponse;
import com.android.slyce.communication.utils.Request;
import com.android.slyce.communication.utils.Response;
import com.android.slyce.communication.utils.VolleyError;
import com.android.slyce.requests.AuthRequest;
import com.android.slyce.utils.Constants;
import com.android.slyce.utils.SharedPrefHelper;
import com.android.slyce.utils.SlyceLog;
import com.android.slyce.utils.Utils;
import com.moodstocks.android.MoodstocksError;
import com.moodstocks.android.Result;
import com.moodstocks.android.Scanner;
import com.moodstocks.android.advanced.ApiSearcher;
import com.moodstocks.android.advanced.Image;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

public class ComManager {

    private static final String TAG = ComManager.class.getSimpleName();

    public static ComManager mInstance;

    private HttpStack stack;
    private Network network;

    private NetworkResponse networkResponse;

    public interface OnResponseListener{
        void onResponse(JSONObject jsonResponse);
    }

    public interface On2DSearchListener {
        void onResponse(String irid, String error);
    }

    public interface OnExtendedInfoListener{
        void onExtendedInfo(JSONArray result);
        void onExtenedInfoError();
    }

    private ComManager(){
        stack = new HurlStack();
        network = new BasicNetwork(stack);
    }

    public static ComManager getInstance(){

        if(mInstance == null){
            mInstance = new ComManager();
        }
        return mInstance;
    }

    public void getClientIDInfo(final String clientID, final OnResponseListener listener){

        new Thread(new Runnable() {

            @Override
            public void run() {

                // Create URL
                StringBuilder requestURLBuilder = new StringBuilder();
                requestURLBuilder.append(Constants.POUNCE_BASE_URL).append(Constants.POUNCE_USERS_SDK_API).
                        append(Constants.POUNCE_CID).append("=").append(clientID);

                // Create request
                JsonObjectRequest request = createRequest(requestURLBuilder.toString());

                // Perform request
                JSONObject response = null;
                try {
                    response = parseJsonObjectResponse(performRequest(request));
                } catch (AuthFailureError authFailureError) {}

                listener.onResponse(response);

            }

        }).start();
    }

    /* Requesting for products related to the base64 irid */
    public void getProductsFromIRID(final String irid, final OnExtendedInfoListener listener){

        new Thread(new Runnable() {

            @Override
            public void run() {

                StringBuilder requestURLBuilder = new StringBuilder();
                requestURLBuilder.
                        append(Constants.POUNCE_BASE_URL).
                        append(Constants.POUNCE_PRODUCTS_API).
                        append(irid);

                // Create request
                JsonObjectRequest request = createRequest(requestURLBuilder.toString());

                // Perform request
                String response = null;
                try {
                    response = performRequest(request);
                } catch (AuthFailureError authFailureError) {}

                JSONArray result = parseJsonArrayResponse(response);

                if(result != null && result.length() > 0){
                    listener.onExtendedInfo(result);
                }else{
                    listener.onExtenedInfoError();
                }
            }

        }).start();
    }

    public void search2DImageURL(final Context context, final String imageUrl, final On2DSearchListener listener){

        new Thread(new Runnable() {

            @Override
            public void run() {

                // Get MS Api Key, Api Secret
                SharedPrefHelper sharedPrefHelper = SharedPrefHelper.getInstance(context);
                String key = sharedPrefHelper.getMSkey();
                String secret = sharedPrefHelper.getMSsecret();


                HttpClientHelper http = new HttpClientHelper(key, secret);
                try {
                    HttpGet request = http.createImageUrlSearchRequest(HttpClientHelper.SEARCH_API, imageUrl);
                    String response = http.dispatch(request);
                    http.clean();

                    JSONObject jsonObjectResponse = parseJsonObjectResponse(response);

                    handle2DResponse(jsonObjectResponse, listener);

                } catch (IOException | URISyntaxException e) {
                    SlyceLog.i(TAG, "Exception on 2D search image url");
                }
            }
        }).start();
    }

    public void search2DImageFile(final Bitmap bitmap, final On2DSearchListener listener){

        // Get Scanner
        Scanner scanner = null;
        try {
            scanner = Scanner.get();
        } catch (MoodstocksError moodstocksError) {
            SlyceLog.e(TAG, "Error at Scanner.get()");
            return;
        }

        // Create Image
        Image img;
        try {
            img = new Image(bitmap);
        } catch (IllegalArgumentException | MoodstocksError e) {
            SlyceLog.i(TAG, "Fail to create Image object");
            return;
        }

        // 1. Local search
        try {
            Result result = scanner.search(img, Scanner.SearchOption.DEFAULT, Result.Extra.NONE);
            if (result != null) {

                SlyceLog.d(TAG, "[2D Local search] Result found: "+result.getValue());

                handle2DResponse(result.getValue(), "", listener);
                return;
            }
            else {
                SlyceLog.d(TAG, "[2D Local search] No result found");
            }
        } catch (MoodstocksError e) {
            SlyceLog.i(TAG, "Failed on 2D local search");
        }

        // 2. Server search
        try {
            ApiSearcher searcher = new ApiSearcher(scanner);
            Result result = searcher.search(img);
            if (result != null) {

                SlyceLog.d(TAG, "[2D Server-side search] Result found: "+result.getValue());

                handle2DResponse(result.getValue(), "", listener);
            }
            else {
                SlyceLog.d(TAG, "[2D Server-side search] No result found");
                handle2DResponse(null, "No image found", listener);
            }
        } catch (MoodstocksError e) {
            SlyceLog.i(TAG, "Failed on 2D server search");
        }

        // 3. Release the `Image` object!
        if(img != null){
            img.release();
        }
    }

    private void handle2DResponse(JSONObject response, On2DSearchListener listener){

        String irId = "";
        String error = "";

        if(response != null){
            error = response.optString(Constants.MS_ERROR);
            irId = response.optString(Constants.MS_ID);
        }

        listener.onResponse(irId, error);
    }

    private void handle2DResponse(String irId, String error, On2DSearchListener listener){
        listener.onResponse(irId, error);
    }

    private JsonObjectRequest createRequest(String url){

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });

        request.setShouldCache(false);

        return request;
    }

    private String performRequest(JsonObjectRequest request) throws AuthFailureError{

        String response = null;

        try {

            networkResponse = network.performRequest(request);

            response = new String(networkResponse.data, HttpHeaderParser.parseCharset(networkResponse.headers));

        } catch (VolleyError volleyError) {
            volleyError.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            SlyceLog.e(TAG, "UnsupportedEncodingException");
        }

        return response;
    }

    private JSONObject parseJsonObjectResponse(String response){

        JSONObject object = null;

        try{
            object = new JSONObject(response);
        }catch (JSONException e){
            SlyceLog.e(TAG, "JSONException");
        }

        return object;
    }

    private JSONArray parseJsonArrayResponse(String response){

        JSONArray object = null;

        try{
            object = new JSONArray(response);
        }catch (JSONException e){
            SlyceLog.e(TAG, "JSONException");
        }

        return object;
    }

//    @Deprecated
//    public void getMSAuth(final Context context,  final OnResponseListener listener){
//
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//
//                // Get MS Api Key, Api Secret
//                SharedPrefHelper sharedPrefHelper = SharedPrefHelper.getInstance(context);
//                String key = sharedPrefHelper.getMSkey();
//                String secret = sharedPrefHelper.getMSsecret();
//
//                StringBuilder url = new StringBuilder();
//                url.append("http://").append(key).append(":").append(secret).append(Constants.MS_ECHO_API);
//
//                AuthRequest request = new AuthRequest(
//                        Request.Method.GET,
//                        url.toString(),
//                        null ,
//                        new Response.Listener<JSONObject>() {
//                            @Override
//                            public void onResponse(JSONObject response) {
//                            }
//                        },
//                        new Response.ErrorListener() {
//                            @Override
//                            public void onErrorResponse(VolleyError error) {
//                            }
//                        });
//
//                request.setShouldCache(false);
//
//                JSONObject response = null;
//                try {
//                    response = parseJsonObjectResponse(performRequest(request));
//                } catch (AuthFailureError authFailureError) {
//                }
//
//                listener.onResponse(response);
//
//            }
//
//        }).start();
//    }

    //    @Deprecated
//    public void search2DImageURL(final Context context, final String imageUrl, final On2DSearchListener listener){
//
//        new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//
//                // Get MS Api Key, Api Secret
//                SharedPrefHelper sharedPrefHelper = SharedPrefHelper.getInstance(context);
//                String key = sharedPrefHelper.getMSkey();
//                String secret = sharedPrefHelper.getMSsecret();
//
//                StringBuilder url = new StringBuilder();
//                url.append("http://").
//                        append(key).
//                        append(":").
//                        append(secret).
//                        append(Constants.MS_SEARCH_API).
//                        append("?").
//                        append(Constants.MS_IMAGE_URL).
//                        append("=").
//                        append(imageUrl);
//
//                AuthRequest request = new AuthRequest(
//                        Request.Method.GET,
//                        url.toString(),
//                        null ,
//                        new Response.Listener<JSONObject>() {
//                            @Override
//                            public void onResponse(JSONObject response) {
//                            }
//                        },
//                        new Response.ErrorListener() {
//                            @Override
//                            public void onErrorResponse(VolleyError error) {
//                            }
//                        });
//
//                request.setShouldCache(false);
//
//                JSONObject response = null;
//                try {
//
//                    response = parseJsonObjectResponse(performRequest(request));
//
//                } catch (AuthFailureError authFailureError) {
//
//                }
//
//                handle2DResponse(response, listener);
//            }
//
//        }).start();
//    }

    //    @Deprecated
//    public void search2DImageFile(final Context context, final Bitmap bitmap, final On2DSearchListener listener){
//
//        // Using MS Http API
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//
//                // Get MS Api Key, Api Secret
//                SharedPrefHelper sharedPrefHelper = SharedPrefHelper.getInstance(context);
//                String key = sharedPrefHelper.getMSkey();
//                String secret = sharedPrefHelper.getMSsecret();
//
//                StringBuilder url = new StringBuilder();
//                url.append("http://").
//                        append(key).
//                        append(":").
//                        append(secret).
//                        append(Constants.MS_SEARCH_API);
//
//                Bitmap scaledBitmap = Utils.scaleDown(bitmap);
//
//                JSONObject response = Utils.uploadBitmapToMS(url.toString(), scaledBitmap, key, secret);
//
//                handle2DResponse(response, listener);
//
//            }
//        }).start();
//    }
}
