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
    private String SLYCE_MOODSTOCKS_MODE = "slyce_moodstocks_mode";
    private String SLYCE_MOODSTOCKS_SECRET = "slyce_moodstocks_secret";
    private String SLYCE_MOODSTOCKS_KEY = "slyce_moodstocks_key";
    private String SLYCE_CLIENT_ID = "SLYCE_CLIENT_ID";

    public static SharedPrefHelper getInstance(Context context) {

        mSharedPreferences = context.getSharedPreferences("com.android.slyce", Context.MODE_PRIVATE);

        return ourInstance;
    }

    private SharedPrefHelper() {

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

        return mSharedPreferences.getBoolean(SLYCE_MOODSTOCKS_MODE, false);
    }

    public void setMSEnabled(String value){

        boolean isMSEnabled;

        SharedPreferences.Editor editor = mSharedPreferences.edit();

        isMSEnabled = value.trim().toLowerCase().equalsIgnoreCase("true") ? true : false;

        editor.putBoolean(SLYCE_MOODSTOCKS_MODE, isMSEnabled);

        editor.commit();
    }

    public void setMSsecret(String value){

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(SLYCE_MOODSTOCKS_SECRET, value);
        editor.commit();
    }

    public String getMSsecret(){
       return  mSharedPreferences.getString(SLYCE_MOODSTOCKS_SECRET, null);
    }

    public void setMSkey(String value){

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(SLYCE_MOODSTOCKS_KEY, value);
        editor.commit();
    }

    public String getMSkey(){
        return  mSharedPreferences.getString(SLYCE_MOODSTOCKS_KEY, null);
    }

    public void setClientID(String clientID){
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(SLYCE_CLIENT_ID, clientID);
        editor.commit();
    }

    public String getClientID(){
        return mSharedPreferences.getString(SLYCE_CLIENT_ID, null);
    }
}
