package com.android.slyce;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.android.slyce.activities.SlyceCameraFragment;
import com.android.slyce.listeners.OnSlyceCameraFragmentListener;
import com.android.slyce.listeners.OnSlyceOpenListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.models.SlyceBarcode;
import org.json.JSONArray;

public class FullUIModeActivity extends Activity implements OnSlyceCameraFragmentListener{

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

        // Add SlyceFragment
        SlyceCameraFragment slyceFragment = SlyceCameraFragment.newInstance(clientID);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
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
        Toast.makeText(this, "onCameraFragment3DRecognition", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraFragmentBarcodeRecognition(SlyceBarcode barcode) {
        Toast.makeText(this, "onCameraFragmentBarcodeRecognition", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraFragment2DRecognition(String irId, String productInfo) {
        Toast.makeText(this, "onCameraFragment2DRecognition", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraFragment2DExtendedRecognition(JSONArray products) {
        Toast.makeText(this, "onCameraFragment2DExtendedRecognition", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraFragmentSlyceProgress(long progress, String message, String id) {
        Toast.makeText(this, "onCameraFragmentSlyceProgress", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraFragmentStageLevelFinish(OnSlyceRequestListener.StageMessage message) {
        Toast.makeText(this, "onCameraFragmentStageLevelFinish", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSlyceCameraFragmentError(String message) {
        Toast.makeText(this, "onSlyceCameraFragmentError", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onImageStartRequest(Bitmap bitmap) {
        Toast.makeText(this, "onImageStartRequest", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTap(float x, float y) {
        Toast.makeText(this, "onTap", Toast.LENGTH_SHORT).show();
    }
}
