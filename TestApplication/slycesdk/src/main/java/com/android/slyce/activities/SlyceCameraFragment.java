package com.android.slyce.activities;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.slyce.Slyce;
import com.android.slyce.camera.SlyceCamera;
import com.android.slyce.listeners.OnSlyceCameraFragmentListener;
import com.android.slyce.listeners.OnSlyceCameraListener;
import com.android.slyce.listeners.OnSlyceOpenListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.models.SlyceBarcode;
import com.android.slycesdk.R;

import org.json.JSONArray;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnSlyceCameraFragmentListener} interface
 * to handle interaction events.
 * Use the {@link SlyceCameraFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SlyceCameraFragment extends Fragment implements OnSlyceCameraListener{

    private final String TAG = SlyceCameraFragment.class.getSimpleName();

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_CLIENT_ID = "arg_client_id";

    private String mClientID;

    private OnSlyceCameraFragmentListener mListener;

    /* Camera surface view */
    private SurfaceView mPreview;

    /* Slyce SDK object */
    private Slyce mSlyce;

    /* Slyce Camera object */
    private SlyceCamera mSlyceCamera;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param clientID Parameter 1.
     * @return A new instance of fragment SlyceFragment.
     */
    public static SlyceCameraFragment newInstance(String clientID) {
        SlyceCameraFragment fragment = new SlyceCameraFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CLIENT_ID, clientID);
        fragment.setArguments(args);
        return fragment;
    }

    public SlyceCameraFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mClientID = getArguments().getString(ARG_CLIENT_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_slyce, container, false);

        mPreview = (SurfaceView) root.findViewById(R.id.preview);

        // Create SlyceCamera object
        createSlyceCamera();

        return root;
    }

    private void openSlyceSDK(){

        mSlyce = Slyce.getInstance(getActivity().getApplicationContext(), mClientID);

        mSlyce.open(new OnSlyceOpenListener() {
            @Override
            public void onOpenSuccess() {
                Log.i(TAG, "Slyce SDK opened");
            }

            @Override
            public void onOpenFail(String message) {
                Log.i(TAG, "Slyce SDK failed to open");
            }
        });
    }

    private void createSlyceCamera(){
        mSlyceCamera = new SlyceCamera(getActivity(), Slyce.get(), mPreview, null, this);
    }

        @Override
    public void onResume() {
        super.onResume();
            Log.i(TAG, "onResume");
        if(mSlyceCamera != null){
            mSlyceCamera.start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        if(mSlyceCamera != null){
            mSlyceCamera.stop();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnSlyceCameraFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSlyceCameraFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCamera3DRecognition(JSONArray products) {
        mListener.onCameraFragment3DRecognition(products);
    }

    @Override
    public void onCameraBarcodeRecognition(SlyceBarcode barcode) {
        mListener.onCameraFragmentBarcodeRecognition(barcode);
    }

    @Override
    public void onCamera2DRecognition(String irId, String productInfo) {
        mListener.onCameraFragment2DRecognition(irId, productInfo);
    }

    @Override
    public void onCamera2DExtendedRecognition(JSONArray products) {
        mListener.onCameraFragment2DExtendedRecognition(products);
    }

    @Override
    public void onCameraSlyceProgress(long progress, String message, String id) {
        mListener.onCameraFragmentSlyceProgress(progress, message, id);
    }

    @Override
    public void onCameraStageLevelFinish(OnSlyceRequestListener.StageMessage message) {
        mListener.onCameraFragmentStageLevelFinish(message);
    }

    @Override
    public void onSlyceCameraError(String message) {
        mListener.onSlyceCameraFragmentError(message);
    }

    @Override
    public void onImageStartRequest(Bitmap bitmap) {
        mListener.onImageStartRequest(bitmap);
    }

    @Override
    public void onTap(float x, float y) {
        mListener.onTap(x, y);
    }
}
