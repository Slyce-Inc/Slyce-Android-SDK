package com.android.slyce.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by davidsvilem on 3/22/15.
 */
public class Ticket {

    public static String createTicket(String event, String data, String dataValue, JSONObject options){

        JSONObject ticket = new JSONObject();

        try {

            JSONObject tokenObj = new JSONObject();
            tokenObj.put(data, dataValue);

            // Added options Json to ticket creation
            if(options != null){
                tokenObj.put("options",options);
            }

            ticket.put("event", event);
            ticket.put("data", tokenObj);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return ticket.toString();
    }

}
