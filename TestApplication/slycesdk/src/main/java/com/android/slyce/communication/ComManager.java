package com.android.slyce.communication;

import android.content.Context;
import android.graphics.Bitmap;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;

public class ComManager {

    private static final String TAG = ComManager.class.getSimpleName();

    public static ComManager mInstance;

    private HttpStack stack;
    private Network network;

    private NetworkResponse networkResponse;

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
                JSONObject response = parseJsonObjectResponse(performRequest(request));

                listener.onResponse(response);

                // Json response example
//                {
//                    status: "success",
//                            premium: "true",
//                        ms: {
//                            enabled: "true",
//                            key: "3jygvjimebpivrohfxyf",
//                            secret: "s9cWbmzuRGjRDYeb"
//                }
//                }
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
                JSONArray response = parseJsonArrayResponse(performRequest(request));

                listener.onExtendedInfo(response);

            }

        }).start();
    }

    public void getMSAuth(final Context context,  final OnResponseListener listener){

        new Thread(new Runnable() {
            @Override
            public void run() {

                // Get MS Api Key, Api Secret
                SharedPrefHelper sharedPrefHelper = SharedPrefHelper.getInstance(context);
                String key = sharedPrefHelper.getMSkey();
                String secret = sharedPrefHelper.getMSsecret();

                StringBuilder url = new StringBuilder();
                url.append("http://").append(key).append(":").append(secret).append(Constants.MS_ECHO_API);

                AuthRequest request = new AuthRequest(
                        Request.Method.GET,
                        url.toString(),
                        null ,
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

                JSONObject response = parseJsonObjectResponse(performRequest(request));

                listener.onResponse(response);

            }

        }).start();
    }

    public void seach2DImageURL(final Context context, final String imageUrl, final On2DSearchListener listener){

        new Thread(new Runnable() {

            @Override
            public void run() {

                // Get MS Api Key, Api Secret
                SharedPrefHelper sharedPrefHelper = SharedPrefHelper.getInstance(context);
                String key = sharedPrefHelper.getMSkey();
                String secret = sharedPrefHelper.getMSsecret();

                StringBuilder url = new StringBuilder();
                url.append("http://").
                        append(key).
                        append(":").
                        append(secret).
                        append(Constants.MS_SEARCH_API).
                        append("?").
                        append(Constants.MS_IMAGE_URL).
                        append("=").
                        append(imageUrl);

                AuthRequest request = new AuthRequest(
                        Request.Method.GET,
                        url.toString(),
                        null ,
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

                JSONObject response = parseJsonObjectResponse(performRequest(request));

                handle2DResponse(response, listener);
            }

        }).start();
    }

    public void search2DImageFile(final Context context, final Bitmap bitmap, final On2DSearchListener listener){

        new Thread(new Runnable() {
            @Override
            public void run() {

                // Get MS Api Key, Api Secret
                SharedPrefHelper sharedPrefHelper = SharedPrefHelper.getInstance(context);
                String key = sharedPrefHelper.getMSkey();
                String secret = sharedPrefHelper.getMSsecret();

                StringBuilder url = new StringBuilder();
                url.append("http://").
                        append(key).
                        append(":").
                        append(secret).
                        append(Constants.MS_SEARCH_API);

                Bitmap scaledBitmap = Utils.scaleDown(bitmap);

                JSONObject response = Utils.uploadBitmapToMS(url.toString(), scaledBitmap);

                handle2DResponse(response, listener);

            }
        }).start();
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

    private String performRequest(JsonObjectRequest request){

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

    public interface OnResponseListener{
        void onResponse(JSONObject jsonResponse);
    }

    public interface On2DSearchListener {
        void onResponse(String irid, String error);
    }

    public interface OnExtendedInfoListener{
        void onExtendedInfo(JSONArray result);
    }
}
