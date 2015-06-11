package com.android.slyce.interfaces;

public interface SlyceCameraInterface {

    void snap();
    void start();
    void stop();
    void resume();
    void pause();
    void cancel();
    void focusAtPoint(float x, float y);
    void setContinuousRecognition(boolean value);
    void turnFlash();
    void shouldResumeScanner(boolean value);
}
