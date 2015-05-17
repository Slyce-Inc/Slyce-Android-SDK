package com.android.slyce.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import com.android.slyce.Slyce;
import com.android.slyce.camera.SlyceCamera;
import com.android.slyce.listeners.OnSlyceCameraListener;
import com.android.slyce.listeners.OnSlyceOpenListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.models.SlyceBarcode;
import com.android.slycesdk.R;

import org.json.JSONArray;

public class SlyceActivity extends Activity implements OnSlyceCameraListener{

    private final String TAG = SlyceActivity.class.getSimpleName();

    public static final String SLYCE_CLIENT_ID = "slyce_client_id";

    /* Slyce SDK object */
    private Slyce mSlyce;

    /* Slyce Camera object */
    private SlyceCamera mSlyceCamera;

    /* views */
    private SurfaceView mPreview;

    /* SLyce parameters */
    private String mClientID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slyce);

        initViews();

        handleIntent(getIntent());

        openSlyceSDK();

        createSlyceCamera();
    }

    private void initViews(){
        mPreview = (SurfaceView) findViewById(R.id.preview);
    }

    private void handleIntent(Intent intent){

        Bundle bundle = intent.getExtras();

        if(bundle == null){
            Toast.makeText(this, "Bundle is null", Toast.LENGTH_SHORT).show();
            return;
        }

        // Extract intent fields
        if(bundle.containsKey(SLYCE_CLIENT_ID)){
            mClientID = bundle.getString(SLYCE_CLIENT_ID);
        }else{
            Toast.makeText(this, "Client ID does not exist at the bundle", Toast.LENGTH_SHORT).show();
        }
    }

    private void openSlyceSDK(){

        mSlyce = Slyce.getInstance(this, mClientID);

        mSlyce.open(new OnSlyceOpenListener() {
            @Override
            public void onOpenSuccess() {
                Log.i(TAG, "Slyce SDK opened");
                mSlyceCamera.start();
            }

            @Override
            public void onOpenFail(String message) {
                Log.i(TAG, "Slyce SDK failed to open");
            }
        });
    }

    private void createSlyceCamera(){
        mSlyceCamera = new SlyceCamera(this, mSlyce, mPreview, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mSlyceCamera != null){
            mSlyceCamera.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mSlyceCamera != null){
            mSlyceCamera.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mSlyce != null){
            mSlyce.close();
        }
    }

    @Override
    public void onCamera3DRecognition(JSONArray products) {
        Toast.makeText(this, "onCamera3DRecognition" ,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraBarcodeRecognition(SlyceBarcode barcode) {
        Toast.makeText(this, "onCameraBarcodeRecognition" ,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCamera2DRecognition(String irId, String productInfo) {
        Toast.makeText(this, "onCamera2DRecognition" ,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCamera2DExtendedRecognition(JSONArray products) {
        Toast.makeText(this, "onCamera2DExtendedRecognition" ,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraSlyceProgress(long progress, String message, String id) {
        Toast.makeText(this, "onCameraSlyceProgress" ,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraStageLevelFinish(OnSlyceRequestListener.StageMessage message) {
        Toast.makeText(this, "onCameraStageLevelFinish" ,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSlyceCameraError(String message) {
        Toast.makeText(this, "onSlyceCameraError" ,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onImageStartRequest(Bitmap bitmap) {
        Toast.makeText(this, "onImageStartRequest" ,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTap(float x, float y) {
        Toast.makeText(this, "onTap" ,Toast.LENGTH_SHORT).show();
    }
}
