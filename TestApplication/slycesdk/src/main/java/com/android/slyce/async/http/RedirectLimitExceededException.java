package com.android.slyce.async.http;

public class RedirectLimitExceededException extends Exception {
    public RedirectLimitExceededException(String message) {
        super(message);
    }
}
