package com.android.slyce.communication;

import com.android.slyce.async.Util;
import com.android.slyce.communication.utils.AuthFailureError;
import com.android.slyce.communication.utils.BasicNetwork;
import com.android.slyce.communication.utils.HttpHeaderParser;
import com.android.slyce.communication.utils.HttpStack;
import com.android.slyce.communication.utils.HurlStack;
import com.android.slyce.communication.utils.JsonObjectRequest;
import com.android.slyce.communication.utils.JsonRequest;
import com.android.slyce.communication.utils.Network;
import com.android.slyce.communication.utils.NetworkResponse;
import com.android.slyce.communication.utils.Request;
import com.android.slyce.communication.utils.Response;
import com.android.slyce.communication.utils.VolleyError;
import com.android.slyce.report.java_websocket.util.Base64;
import com.android.slyce.requests.AuthRequest;
import com.android.slyce.socket.WSConnection;
import com.android.slyce.utils.SlyceLog;
import com.android.slyce.utils.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by davidsvilem on 3/25/15.
 */
public class ComManager {

    private static final String TAG = ComManager.class.getSimpleName();

    public static ComManager mInstance;

    private HttpStack stack;
    private Network network;

    private String BASE_URL = "http://api.pounce.mobi/v2/";

    private String API_USERS_SDK = "users/sdk?";
    private String API_IRID = "ir?";

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
                requestURLBuilder.append(BASE_URL).append(API_USERS_SDK).
                        append("cid").append("=").append(clientID);

                // Create request
                JsonObjectRequest request = createRequest(requestURLBuilder.toString());

                // Perform request
                JSONObject response = performRequest(request);

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

    public void getIRIDInfo(final String clientID, final String irid){

        new Thread(new Runnable() {

            @Override
            public void run() {

                StringBuilder requestURLBuilder = new StringBuilder();
                requestURLBuilder.append(BASE_URL).append(API_IRID).
                        append("cid").append("=").append(clientID).
                        append("&").
                        append("id").append("=").append(irid);

                // Create request
                JsonObjectRequest request = createRequest(requestURLBuilder.toString());

                // Perform request
                JSONObject response = performRequest(request);

                // Parse response
                String status = response.optString("status");
                JSONArray sku = response.optJSONArray("sku");

            }

        }).start();
    }

    public void getMSAuth(final String apiKey, final String apiSecret, final OnResponseListener listener){

        new Thread(new Runnable() {
            @Override
            public void run() {

                String url = "http://3jygvjimebpivrohfxyf:s9cWbmzuRGjRDYeb@api.moodstocks.com/v2/echo/?foo=bar";

//                String url = "http://3jygvjimebpivrohfxyf:s9cWbmzuRGjRDYeb@api.moodstocks.com/v2/search?image_url=http://pouncewidgetsnaps.s3.amazonaws.com/JCP4.jpg";

                AuthRequest request = new AuthRequest(
                        Request.Method.GET,
                        url,
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

                JSONObject response = performRequest(request);

                listener.onResponse(response);

            }

        }).start();
    }

    public void getMSSImageURL(final String apiKey, final String apiSecret, String imageUrl, final OnResponseListener listener){

        new Thread(new Runnable() {

            @Override
            public void run() {

                String url = "http://3jygvjimebpivrohfxyf:s9cWbmzuRGjRDYeb@api.moodstocks.com/v2/search?image_url=http://pouncewidgetsnaps.s3.amazonaws.com/JCP4.jpg";

                JsonObjectRequest request = createRequest(url);

                JSONObject response = performRequest(request);

                listener.onResponse(response);

            }

        }).start();
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

    private JSONObject performRequest(JsonObjectRequest request){

        JSONObject object = null;

        try {

            networkResponse = network.performRequest(request);

            String response = new String(networkResponse.data, HttpHeaderParser.parseCharset(networkResponse.headers));

            object = new JSONObject(response);

        } catch (VolleyError volleyError) {
            volleyError.printStackTrace();
            String digest = volleyError.networkResponse.headers.get("WWW-Authenticate").toString();
        } catch (UnsupportedEncodingException e) {
            SlyceLog.e(TAG, "UnsupportedEncodingException");
        } catch (JSONException e) {
            SlyceLog.e(TAG, "JSONException");
        }

        return object;
    }

    public interface OnResponseListener{
        public void onResponse(JSONObject jsonResponse);
    }
}
