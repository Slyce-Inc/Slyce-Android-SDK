package com.android.slyce.interfaces;

/**
 * Created by davidsvilem on 5/12/15.
 */
public interface SlyceRequestInterface {

    String getToken();
    void cancel();
    void execute();

}
