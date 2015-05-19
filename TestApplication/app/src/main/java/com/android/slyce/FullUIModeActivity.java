package com.android.slyce;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.android.slyce.activities.SlyceCameraFragment;
import com.android.slyce.listeners.OnSlyceCameraFragmentListener;
import com.android.slyce.listeners.OnSlyceOpenListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.models.SlyceBarcode;
import org.json.JSONArray;

public class FullUIModeActivity extends Activity implements OnSlyceCameraFragmentListener{

    private final String TAG = FullUIModeActivity.class.getSimpleName();

    private Slyce slyce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_uimode);

        slyce = Slyce.getInstance(this, "jcpenney852");

        slyce.open(new OnSlyceOpenListener() {
            @Override
            public void onOpenSuccess() {

                openSlyceCameraFragment("jcpenney852");
            }

            @Override
            public void onOpenFail(String message) {

            }
        });
    }

    private void openSlyceCameraFragment(String clientID){

        // Add SlyceCameraFragment
        SlyceCameraFragment slyceFragment = SlyceCameraFragment.newInstance(clientID, null);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.slyce_fragment_container, slyceFragment);
        // Commit the transaction
        transaction.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(slyce != null){
            slyce.close();
        }
    }

    @Override
    public void onCameraFragment3DRecognition(JSONArray products) {
        Toast.makeText(this, "onCameraFragment3DRecognition:" + "\n" + products, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraFragmentBarcodeRecognition(SlyceBarcode barcode) {
        Toast.makeText(this, "onCameraFragmentBarcodeRecognition:" + "\n" + barcode.getBarcode(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraFragment2DRecognition(String irId, String productInfo) {
        Toast.makeText(this, "onCameraFragment2DRecognition:" + "\n" + irId + "\n" + productInfo, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraFragment2DExtendedRecognition(JSONArray products) {
        Toast.makeText(this, "onCameraFragment2DExtendedRecognition:" + "\n" + products, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraFragmentStageLevelFinish(OnSlyceRequestListener.StageMessage message) {
        Toast.makeText(this, "onCameraFragmentStageLevelFinish", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSlyceCameraFragmentError(String message) {
        Toast.makeText(this, "onSlyceCameraFragmentError:" + "\n" + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onImageStartRequest(Bitmap bitmap) {
        Toast.makeText(this, "onImageStartRequest", Toast.LENGTH_SHORT).show();
    }
}
