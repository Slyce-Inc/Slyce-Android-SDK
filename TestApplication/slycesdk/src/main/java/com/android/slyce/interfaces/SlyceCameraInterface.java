package com.android.slyce.interfaces;

/**
 * Created by davidsvilem on 5/12/15.
 */
public interface SlyceCameraInterface {

    void snap();
    void start();
    void stop();
    void focusAtPoint(float x, float y);
    void setContinuousRecognition(boolean value);
    void turnFlash();

}
