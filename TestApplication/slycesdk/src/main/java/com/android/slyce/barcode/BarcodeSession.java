/*
 * Basic no frills app which integrates the ZBar barcode scanner with
 * the camera.
 * 
 * Created by lisah0 on 2012-02-24
 */
package com.android.slyce.barcode;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.SurfaceView;

import com.android.slyce.scanner.CameraFrame;
import com.android.slyce.scanner.CameraManager;
import com.moodstocks.android.advanced.Tools;
import com.moodstocks.android.core.Loader;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

/* Import ZBar Class files */

public class BarcodeSession implements CameraManager.Listener {

    private final String TAG = BarcodeSession.class.getSimpleName();

    ImageScanner scanner;

    /* Notify SlyceCamera on barcode scan result */
    private OnBarcodeListener listener;

    /* */
    private boolean isSnap = false;

    private CameraManager cameraManager;

    private boolean started = false;

    private boolean paused = false;

    static {
        System.loadLibrary("iconv");
    }

    static {
        Loader.load();
    }

    public BarcodeSession(Activity parent, SurfaceView preview, OnBarcodeListener listener) {

        this.listener = listener;

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        this.cameraManager = new CameraManager(parent, this, preview);
    }

    public void snap(){
        isSnap = true;
    }

    public void start() {
        // TODO: expose front/back choice?
        this.cameraManager.start(true, true);
        this.started = true;
    }

    public void stop() {
        this.started = false;
        this.cameraManager.stop();
    }

    public void resume() {
        this.paused = false;
    }

    public void pause() {
        this.paused = true;
    }

    public boolean turnFlash(){
        return cameraManager.turnFlash();
    }

    public void requestFocus(boolean focusAtPoint, final Rect focusRect){
        cameraManager.requestFocus(focusAtPoint, focusRect);
    }

    /*
     *  CameraManager.Listener call backs
     */
    @Override
    public boolean isListening() {
        return (this.started && !this.paused);
    }

    @Override
    public void onCameraOpenException(Exception e) {
        // TODO: notify the host app on error while open the camera
    }

    @Override
    public void onNewFrameInBackground(CameraFrame f) {

        Image barcode = new Image(f.size.width, f.size.height, "Y800");
        barcode.setData(f.data);

        int result = scanner.scanImage(barcode);

        if (result != 0) {

            if (this.started && !this.paused) {
                pause();
            }

            SymbolSet syms = scanner.getResults();

            for (Symbol sym : syms) {
                String barcodeResult = sym.getData();

                int type = sym.getType();

                listener.onBarcodeResult(type, barcodeResult);
            }
        }

        if(isSnap) {
            isSnap = false;
            Bitmap query = Tools.convertNV21ToBitmap(f.data, f.size.width, f.size.height, 90);
            listener.onBarcodeSnap(query);
        }

        f.release();
    }
    /*
     *
     */

    public interface OnBarcodeListener{
        void onBarcodeResult(int type, String result);
        void onBarcodeSnap(Bitmap bitmap);
    }

}
