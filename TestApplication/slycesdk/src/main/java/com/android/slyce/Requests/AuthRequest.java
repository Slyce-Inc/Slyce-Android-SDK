package com.android.slyce.requests;

import com.android.slyce.communication.utils.AuthFailureError;
import com.android.slyce.communication.utils.JsonObjectRequest;
import com.android.slyce.communication.utils.Response;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by davidsvilem on 4/16/15.
 */
public class AuthRequest extends JsonObjectRequest {

    public AuthRequest(int method, String url, JSONObject jsonRequest,
                       Response.Listener<JSONObject> listener,
                       Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
    }

    public AuthRequest(String url, JSONObject jsonRequest,
                       Response.Listener<JSONObject> listener,
                       Response.ErrorListener errorListener) {
        super(url, jsonRequest, listener, errorListener);
    }

//    @Override
//    public Map<String, String> getHeaders() throws AuthFailureError {
//        return createBasicAuthHeader("user", "password");
//    }

    Map<String, String> createBasicAuthHeader(String username, String password) {
        Map<String, String> headerMap = new HashMap<String, String>();
//        headerMap.put("Authorization", "Digest username=\"3jygvjimebpivrohfxyf\", realm=\"Moodstocks API\", nonce=\"MTQyOTAwNjE4NSA2YzQzMWFjZjJhZDg0OTVlZDY3OGE2Nzk3YzMyNmYzZQ==\", uri=\"/v2/echo/?foo=bar\", response=\"2f3458baa7ce27d7111b03bc3283e0c7\", opaque=\"b1a8d1044b0de768f7905b15aa7f95de\", qop=auth, nc=00000001, cnonce=\"8dd6c0acf8d5d1ca\"");

//        String auth = "Digest username=\"3jygvjimebpivrohfxyf\", realm=\"Moodstocks API\", nonce=\"MTQyOTE4MzAzMyAyNjE2MjAyODI4OTgxYjVjN2MzYzRhOTg3NTMwZTg2MQ==\", uri=\"/v2/search?image_url=http://pouncewidgetsnaps.s3.amazonaws.com/JCP4.jpg\", response=\"78750a84aa14d8e3e018d2bf40bde52f\", opaque=\"b1a8d1044b0de768f7905b15aa7f95de\", qop=auth, nc=00000001, cnonce=\"114b77e978732453\"";
        String auth = "Digest username=\"3jygvjimebpivrohfxyf\", realm=\"Moodstocks API\", nonce=\"MTQyOTAwNjE4NSA2YzQzMWFjZjJhZDg0OTVlZDY3OGE2Nzk3YzMyNmYzZQ==\", uri=\"/v2/echo/?foo=bar\", response=\"2f3458baa7ce27d7111b03bc3283e0c7\", opaque=\"b1a8d1044b0de768f7905b15aa7f95de\", qop=auth, nc=00000001, cnonce=\"8dd6c0acf8d5d1ca\"";
        headerMap.put("Authorization", auth);
        return headerMap;
    }
}
