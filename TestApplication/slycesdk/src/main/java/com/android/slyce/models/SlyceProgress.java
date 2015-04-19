package com.android.slyce.models;


/**
 * Created by davidsvilem on 3/26/15.
 */
public class SlyceProgress {

    public long progress;
    public String message;
    public String token;

    public SlyceProgress(long progress, String message, String token){

        this.progress = progress;
        this.message = message;
        this.token = token;
    }


}
