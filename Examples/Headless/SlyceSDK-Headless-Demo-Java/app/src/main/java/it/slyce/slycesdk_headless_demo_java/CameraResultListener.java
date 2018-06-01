package it.slyce.slycesdk_headless_demo_java;

public interface CameraResultListener {

    void onSearchCompleted();

    void onSearchStarted();

    void onSearchReceivedUpdate(String header, String update);
}
