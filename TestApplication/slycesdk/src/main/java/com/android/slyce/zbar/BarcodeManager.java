/*
 * Basic no frills app which integrates the ZBar barcode scanner with
 * the camera.
 * 
 * Created by lisah0 on 2012-02-24
 */
package com.android.slyce.zbar;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Environment;
import android.os.Handler;
import android.view.SurfaceView;
import android.widget.Toast;

import com.moodstocks.android.advanced.Tools;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/* Import ZBar Class files */

public class BarcodeManager
{
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;

    ImageScanner scanner;

    private boolean barcodeScanned = false;
    private boolean previewing = true;

    /* Notify SlyceCamera on barcode scan result */
    private OnBarcodeListener listener;

    /* */
    private boolean isSnap = false;

    static {
        System.loadLibrary("iconv");
    } 

    public BarcodeManager(SurfaceView preview, OnBarcodeListener listener) {

        this.listener = listener;

        autoFocusHandler = new Handler();
        mCamera = getCameraInstance();

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        mPreview = new CameraPreview(mCamera, previewCb, autoFocusCB, preview);
    }

    public void pause() {
        releaseCamera();
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e){
        }
        return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Runnable doAutoFocus = new Runnable() {
            public void run() {
                if (previewing)
                    mCamera.autoFocus(autoFocusCB);
            }
        };

    PreviewCallback previewCb = new PreviewCallback() {

            public void onPreviewFrame(byte[] data, Camera camera) {

                Camera.Parameters parameters = camera.getParameters();
                Size size = parameters.getPreviewSize();

                Image barcode = new Image(size.width, size.height, "Y800");
                barcode.setData(data);

                int result = scanner.scanImage(barcode);
                
                if (result != 0) {
                    previewing = false;
                    mCamera.setPreviewCallback(null);
                    mCamera.stopPreview();
                    
                    SymbolSet syms = scanner.getResults();

                    for (Symbol sym : syms) {
                        String barcodeResult = sym.getData();
                        barcodeScanned = true;

                        listener.onBarcodeResult(barcodeResult);
                    }
                }

                if(isSnap) {
                    isSnap = false;
                    Bitmap bitmap = createBitmap(data, camera);
                    listener.onBarcodeSnap(bitmap);
                }
            }
        };

    // Mimic continuous auto-focusing
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
            public void onAutoFocus(boolean success, Camera camera) {
                autoFocusHandler.postDelayed(doAutoFocus, 1000);
            }
        };

    public void resumeScan(){
        if (barcodeScanned) {
            barcodeScanned = false;
            mCamera.setPreviewCallback(previewCb);
            mCamera.startPreview();
            previewing = true;
            mCamera.autoFocus(autoFocusCB);
        }
    }

    private Bitmap createBitmap(byte[] data, Camera camera){

        Camera.Parameters parameters = camera.getParameters();
        Size size = parameters.getPreviewSize();

        Bitmap query = Tools.convertNV21ToBitmap(data, size.width, size.height, 90);

        return query;
    }

    public void snap(){
        isSnap = true;
    }

    public interface OnBarcodeListener{
        void onBarcodeResult(String result);
        void onBarcodeSnap(Bitmap bitmap);
    }

}
