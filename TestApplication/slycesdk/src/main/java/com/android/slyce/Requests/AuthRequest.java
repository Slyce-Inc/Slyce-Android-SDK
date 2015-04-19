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

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return createBasicAuthHeader("user", "password");
    }

    Map<String, String> createBasicAuthHeader(String username, String password) {
        Map<String, String> headerMap = new HashMap<String, String>();

        String digest = "Digest username=\"3jygvjimebpivrohfxyf\", realm=\"Moodstocks API\", nonce=\"MTQyOTQyNjAwMiBjZTU2YjFjNmE0MWZmMmFlN2M5M2MzMjE0N2RhNzJmZg==\", uri=\"/v2/search?image_url=http://pouncewidgetsnaps.s3.amazonaws.com/JCP4.jpg\", response=\"a180cf5885b25dfaaa05c3fb4e2a69d9\", opaque=\"b1a8d1044b0de768f7905b15aa7f95de\", qop=auth, nc=00000001, cnonce=\"f95bc0587a20a2de\"";

        headerMap.put("Authorization", digest);
        return headerMap;
    }
}
