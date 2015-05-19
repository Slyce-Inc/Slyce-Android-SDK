package com.android.slyce.activities;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.android.slyce.Slyce;
import com.android.slyce.camera.SlyceCamera;
import com.android.slyce.listeners.OnSlyceCameraFragmentListener;
import com.android.slyce.listeners.OnSlyceCameraListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.models.SlyceBarcode;
import com.android.slyce.utils.SlyceLog;
import com.android.slyce.utils.Utils;
import com.android.slycesdk.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnSlyceCameraFragmentListener} interface
 * to handle interaction events.
 * Use the {@link SlyceCameraFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SlyceCameraFragment extends Fragment implements OnSlyceCameraListener, OnClickListener{

    private static final String TAG = SlyceCameraFragment.class.getSimpleName();

    private static final int RESULT_LOAD_IMG = 1;

    // the fragment initialization parameters
    private static final String ARG_CLIENT_ID = "arg_client_id";
    private static final String ARG_OPTION_JSON = "arg_option_json";

    private String mClientID;
    private JSONObject mOptionsJson;

    /* Listeners */
    private OnSlyceCameraFragmentListener mListener;

    /* Notify the ImageProcessFragment on events */
    private static OnImageProcessListener mOnImageProcessListener;

    /* Camera surface view */
    private SurfaceView mPreview;

    /* Views */
    private Button mCloseButton;
    private ImageButton mScanTipsButton;
    private ImageButton mGalleryButton;
    private CheckBox mFlashButton;
    private ImageButton mSnapButton;

    private ImageView mOnTapView;

    /* Slyce SDK object */
    private Slyce mSlyce;

    /* Slyce Camera object */
    private SlyceCamera mSlyceCamera;

    private ImageProcessFragment mImageProcessFragment;

    public interface OnImageProcessListener{

        /** Bitmap is ready from {@link OnSlyceCameraListener}*/
        void onSnap(Bitmap bitmap);

        /** Bitmap uploaded to the server */
        void onImageStartRequest();

        /** Notify on search progress */
        void onProgress(long progress, String message);

        /** Notify on error */
        void onError(String message);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param clientID Parameter 1.
     * @param options  Parameter 2.
     * @return A new instance of fragment SlyceCameraFragment.
     */
    public static SlyceCameraFragment newInstance(String clientID, JSONObject options) {
        SlyceCameraFragment fragment = new SlyceCameraFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CLIENT_ID, clientID);

        if(options != null){
            args.putString(ARG_OPTION_JSON, options.toString());
        }

        fragment.setArguments(args);
        return fragment;
    }

    public SlyceCameraFragment() {
        // Required empty public constructor

        // Create ImageProcessFragment
        mImageProcessFragment = ImageProcessFragment.newInstance();

        // Set the listener so messages will be sent to ImageProcessFragment
        mOnImageProcessListener = mImageProcessFragment;
    }

    public void setContinuousRecognition(boolean value){
        mSlyceCamera.setContinuousRecognition(value);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentArguments();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_slyce, container, false);

        // Initialize views
        initViews(root);

        // Create SlyceCamera object
        createSlyceCamera();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mSlyceCamera != null){
            mSlyceCamera.start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
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
        // Notify the host application of found products
        mListener.onCameraFragment3DRecognition(products);
    }

    @Override
    public void onCameraBarcodeRecognition(SlyceBarcode barcode) {
        // Notify the host application of barcode recognition
        mListener.onCameraFragmentBarcodeRecognition(barcode);
    }

    @Override
    public void onCamera2DRecognition(String irId, String productInfo) {
        // Notify the host application of MS recognition
        mListener.onCameraFragment2DRecognition(irId, productInfo);
    }

    @Override
    public void onCamera2DExtendedRecognition(JSONArray products) {
        // Notify the host application of extra products details
        mListener.onCameraFragment2DExtendedRecognition(products);
    }

    @Override
    public void onCameraSlyceProgress(long progress, String message, String id) {
        // Notify ImageProcessFragment for searching progress
        mOnImageProcessListener.onProgress(progress, message);
    }

    @Override
    public void onCameraStageLevelFinish(OnSlyceRequestListener.StageMessage message) {

    }

    @Override
    public void onSlyceCameraError(String message) {
        // Notify host application
        mListener.onSlyceCameraFragmentError(message);

        // Notify ImageProcessFragment
        mOnImageProcessListener.onError(message);
    }

    @Override
    public void onImageStartRequest(Bitmap bitmap) {
        // Notify ImageProcessFragment for bitmap was uploaded to server
        mOnImageProcessListener.onImageStartRequest();
    }

    @Override
    public void onSnap(Bitmap bitmap) {
        // Notify ImageProcessFragment that bitmap is ready
        mOnImageProcessListener.onSnap(bitmap);
    }

    @Override
    public void onTap(float x, float y) {
        // Displays the touch point
        Utils.performAlphaAnimation(mOnTapView, x, y);
    }

    private void createSlyceCamera(){
        mSlyceCamera = new SlyceCamera(getActivity(), Slyce.get(), mPreview, mOptionsJson, this);
    }

    private void initViews(View view){
        mPreview = (SurfaceView) view.findViewById(R.id.preview);
        mCloseButton = (Button) view.findViewById(R.id.close_button);
        mScanTipsButton = (ImageButton) view.findViewById(R.id.scan_tips_button);
        mGalleryButton = (ImageButton) view.findViewById(R.id.gallery_button);
        mFlashButton = (CheckBox) view.findViewById(R.id.flash_button);
        mSnapButton = (ImageButton) view.findViewById(R.id.snap_button);

        mOnTapView = (ImageView) view.findViewById(R.id.on_tap_view);

        mCloseButton.setOnClickListener(this);
        mScanTipsButton.setOnClickListener(this);
        mGalleryButton.setOnClickListener(this);
        mFlashButton.setOnClickListener(this);
        mSnapButton.setOnClickListener(this);
    }

    private void getFragmentArguments(){
        if (getArguments() != null) {

            // Parameter 1. Set Client ID
            mClientID = getArguments().getString(ARG_CLIENT_ID);

            // Parameter 2. Set Options Json
            String options = getArguments().getString(ARG_OPTION_JSON);
            if(!TextUtils.isEmpty(options)){
                try {
                    mOptionsJson = new JSONObject(options);
                } catch (JSONException e) {
                    SlyceLog.i(TAG, "Failed to create options Json");
                }
            }

            // Parameter 3.
        }
    }

    private void close(){
        getActivity().getFragmentManager().beginTransaction().remove(this).commit();
    }

    @Override
    public void onClick(View v) {

        int id = v.getId();

        if(id == R.id.close_button){

            close();

        }else if(id == R.id.scan_tips_button){

        }else if(id == R.id.gallery_button){

            Utils.loadImageFromGallery(this, RESULT_LOAD_IMG);

        }else if(id == R.id.flash_button){

            mSlyceCamera.turnFlash();

        }else if(id == R.id.snap_button){

            attachFragment(mImageProcessFragment);

            // Take a picture using SlyceCamera object
            mSlyceCamera.snap();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // When an Image is picked
        if (requestCode == RESULT_LOAD_IMG && resultCode == getActivity().RESULT_OK && null != data) {

            // Extract Image String
            String pickedImageString  = Utils.getImageDecodableString(data, getActivity().getApplicationContext());

            if(TextUtils.isEmpty(pickedImageString)){

                SlyceLog.i(TAG, "Error occurred while picking an Image");

            }else{

                mImageProcessFragment.setImageDecodableString(pickedImageString);
                attachFragment(mImageProcessFragment);
            }

        } else {
            SlyceLog.i(TAG, "You haven't picked Image");
        }
    }

    /** Displays ImageProcessFragment {@link ImageProcessFragment}
     *  @param fragment to display */
    private void attachFragment(Fragment fragment){

        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.image_process_fragment_container, fragment);
        transaction.commitAllowingStateLoss();
    }
}
