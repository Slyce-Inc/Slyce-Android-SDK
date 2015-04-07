package com.android.slyce.utils;

import android.util.Log;

/**
 * Created by davidsvilem on 4/7/15.
 */
public class SlyceLog {

    public static void i(String tag, String message){
        if(Constants.ShouldLog){
            Log.i(tag, message);
        }
    }

    public static void e(String tag, String message){
        if(Constants.ShouldLog){
            Log.e(tag, message);
        }
    }

    public static void w(String tag, String message){
        if(Constants.ShouldLog){
            Log.w(tag, message);
        }
    }
}
