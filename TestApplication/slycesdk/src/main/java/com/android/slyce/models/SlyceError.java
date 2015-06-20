package com.android.slyce.models;

import com.android.slyce.enums.SlyceErrorType;

public class SlyceError {

    private SlyceErrorType type;
    private String message;

    public SlyceError(SlyceErrorType type, String message){
        this.type = type;
        this.message = message;
    }
}
