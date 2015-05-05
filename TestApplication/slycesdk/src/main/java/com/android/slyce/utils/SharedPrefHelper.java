package com.android.slyce.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/**
 * Created by davidsvilem on 3/26/15.
 */
public class SharedPrefHelper {

    private static SharedPrefHelper ourInstance = new SharedPrefHelper();

    private Context mContext;

    private static SharedPreferences mSharedPreferences;

    private String SLYCE_PREMIUM_MODE = "slyce_premium_mode";
    private String SLYCE_2D_MODE = "slyce_2D_mode";
    private String SLYCE_2D_SECRET = "slyce_2D_secret";
    private String SLYCE_2D_KEY = "slyce_2D_key";
    private String SLYCE_CLIENT_ID = "slyce_client_id";
    private String SLYCE_PEOPLE_ANALYTICS_CREATED = "slyce_people_analytics_created";

    public static SharedPrefHelper getInstance(Context context) {

        mSharedPreferences = context.getSharedPreferences("com.android.slyce", Context.MODE_PRIVATE);

        return ourInstance;
    }

    private SharedPrefHelper() {

    }

    public void clear(){
        mSharedPreferences.edit().clear().commit();
    }

    public boolean isPremium(){
        return mSharedPreferences.getBoolean(SLYCE_PREMIUM_MODE, false);
    }

    public void setPremium(String value){

        boolean isPremium;

        SharedPreferences.Editor editor = mSharedPreferences.edit();

        isPremium = value.trim().toLowerCase().equalsIgnoreCase("true") ? true : false;

        editor.putBoolean(SLYCE_PREMIUM_MODE, isPremium);

        editor.commit();
    }

    public boolean isMSEnbaled(){

        return mSharedPreferences.getBoolean(SLYCE_2D_MODE, false);
    }

    public void setMSEnabled(String value){

        boolean isMSEnabled;

        SharedPreferences.Editor editor = mSharedPreferences.edit();

        isMSEnabled = value.trim().toLowerCase().equalsIgnoreCase("true") ? true : false;

        editor.putBoolean(SLYCE_2D_MODE, isMSEnabled);

        editor.commit();
    }

    public void setMSsecret(String value){

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(SLYCE_2D_SECRET, value);
        editor.commit();
    }

    public String getMSsecret(){
       return  mSharedPreferences.getString(SLYCE_2D_SECRET, null);
    }

    public void setMSkey(String value){

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(SLYCE_2D_KEY, value);
        editor.commit();
    }

    public String getMSkey(){
        return  mSharedPreferences.getString(SLYCE_2D_KEY, null);
    }

    public void setClientID(String clientID){
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(SLYCE_CLIENT_ID, clientID);
        editor.commit();
    }

    public String getClientID(){
        return mSharedPreferences.getString(SLYCE_CLIENT_ID, null);
    }

    public String getCreated(){
        return mSharedPreferences.getString(SLYCE_PEOPLE_ANALYTICS_CREATED, null);
    }

    public void setCreated(String value){
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(SLYCE_PEOPLE_ANALYTICS_CREATED, value);
        editor.commit();
    }
}
