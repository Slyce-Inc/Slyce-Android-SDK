package com.android.slyce;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.android.slyce.enums.SlyceRequestStage;
import com.android.slyce.fragments.ImageProcessDialogFragment;
import com.android.slyce.fragments.NotFoundDialogFragment;
import com.android.slyce.fragments.ScanningTipsDialogFragment;
import com.android.slyce.listeners.OnSlyceCameraListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.utils.BitmapLoader;
import com.android.slyce.utils.Buzzer;
import com.android.slyce.utils.SlyceLog;
import com.android.slyce.utils.Utils;
import com.android.slycesdk.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.android.slyce.listeners.OnSlyceCameraFragmentListener} interface
 * to handle interaction events.
 * Use the {@link SlyceCameraFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 * SlyceCameraFragment provides an integrated XML layout with all Slyce SDK functionalities.
 */
public class SlyceCameraFragment extends Fragment implements OnClickListener, ImageProcessDialogFragment.OnImageProcessListener{

    private static final String TAG = SlyceCameraFragment.class.getSimpleName();

    private static final String FRAGMENT_TAG = "ImageProcessDialogFragment";

    // the fragment initialization parameters
    private static final String ARG_OPTION_JSON = "arg_option_json";
    private static final String ARG_CONTINUOUS_RECOGNITION = "arg_continuous_recognition";
    private static final String ARG_SHOULD_PAUSE_SCANNER = "arg_should_pause_scanner";

    private static final int RESULT_LOAD_IMG = 1;

    /* Options Json from hosting application */
    private JSONObject mOptionsJson;

    private boolean isAttached;
    private boolean mContinuousRecognition;
    private boolean mShouldPauseScanner;

    /* Listeners */
    private com.android.slyce.listeners.OnSlyceCameraFragmentListener mListener;

    /* Camera surface view */
    private SurfaceView mPreview;

    /* Views */
    private ImageButton mCloseButton;
    private ImageButton mScanTipsButton;
    private ImageButton mGalleryButton;
    private CheckBox mFlashButton;
    private ImageButton mSnapButton;
    private ImageView mOnTapView;
    private RelativeLayout mDialogLayout;

    /* Slyce Camera object */
    private SlyceCamera mSlyceCamera;

    private SlyceProductsRequest mSlyceRequest;

    /*  */
    private ImageProcessDialogFragment mImageProcessDialogFragment;

    private Slyce mSlyce;

    // PUBLIC METHODS
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param options use this JSONObject to pass properties to Slyce servers. Can be null.
     * @param continuousRecognition boolean use this in order to stop the automatic scanner
     * @param shouldPauseScanner boolean use this to resume/pause the automatic scanner after detection
     *
     * @return A new instance of fragment SlyceCameraFragment.
     */
    public static SlyceCameraFragment newInstance(JSONObject options, boolean continuousRecognition, boolean shouldPauseScanner) {
        SlyceCameraFragment fragment = new SlyceCameraFragment();
        Bundle args = new Bundle();

        if(options != null){
            args.putString(ARG_OPTION_JSON, options.toString());
        }

        args.putBoolean(ARG_CONTINUOUS_RECOGNITION, continuousRecognition);
        args.putBoolean(ARG_SHOULD_PAUSE_SCANNER, shouldPauseScanner);

        fragment.setArguments(args);
        return fragment;
    }

    public SlyceCameraFragment() {
    }
    // PUBLIC METHODS END

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentArguments();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_slyce, container, false);

        // Initialize views
        initViews(root);

        mSlyce = Slyce.getInstance(getActivity());

        // Create SlyceCamera object
        createSlyceCamera();

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        isAttached = true;
        try {
            mListener = (com.android.slyce.listeners.OnSlyceCameraFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSlyceCameraFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        isAttached = false;
        mListener = null;
    }

    // PRIVATE METHODS
    private void createSlyceCamera(){
        mSlyceCamera = new SlyceCamera(getActivity(), mSlyce, mPreview, mOptionsJson, new SlyceCameraListener());
        mSlyceCamera.setContinuousRecognition(mContinuousRecognition);
        mSlyceCamera.shouldPauseScanner(mShouldPauseScanner);
    }

    private void initViews(View view){
        mPreview = (SurfaceView) view.findViewById(R.id.preview);
        mCloseButton = (ImageButton) view.findViewById(R.id.close_button);
        mScanTipsButton = (ImageButton) view.findViewById(R.id.scan_tips_button);
        mGalleryButton = (ImageButton) view.findViewById(R.id.gallery_button);
        mFlashButton = (CheckBox) view.findViewById(R.id.flash_button);
        mSnapButton = (ImageButton) view.findViewById(R.id.snap_button);
        mOnTapView = (ImageView) view.findViewById(R.id.on_tap_view);
        mDialogLayout = (RelativeLayout) view.findViewById(R.id.dialog_layout);

        mCloseButton.setOnClickListener(this);
        mScanTipsButton.setOnClickListener(this);
        mGalleryButton.setOnClickListener(this);
        mFlashButton.setOnClickListener(this);
        mSnapButton.setOnClickListener(this);
    }

    private void getFragmentArguments(){
        if (getArguments() != null) {

            // Parameter 1. Set Options Json
            String options = getArguments().getString(ARG_OPTION_JSON);
            if(!TextUtils.isEmpty(options)){
                try {
                    mOptionsJson = new JSONObject(options);
                } catch (JSONException e) {
                    SlyceLog.i(TAG, "Failed to create options Json");
                }
            }

            // Parameter 2.
            mContinuousRecognition = getArguments().getBoolean(ARG_CONTINUOUS_RECOGNITION);

            // Parameter 3.
            mShouldPauseScanner = getArguments().getBoolean(ARG_SHOULD_PAUSE_SCANNER);
        }
    }

    private void close(){
        if(isAttached){
            // getActivity().getFragmentManager().beginTransaction().remove(this).commit();
            getActivity().getFragmentManager().popBackStack();
        }
    }

    private ImageProcessDialogFragment showDialogFragment(
            String processType){

        // Create and show the dialog.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ImageProcessDialogFragment newFragment = ImageProcessDialogFragment.newInstance();
        mImageProcessDialogFragment = newFragment;
        mImageProcessDialogFragment.setOnImageProcessListener(this);
        newFragment.show(ft, FRAGMENT_TAG);

        return newFragment;
    }

    @Override
    public void onCancelClicked() {
        if(mSlyceCamera != null){
            mSlyceCamera.cancel();
        }
        if(mSlyceRequest != null){
            mSlyceRequest.cancel();
        }
    }

    private class SlyceCameraListener implements OnSlyceCameraListener{

        @Override
        public void onCameraResultsReceived(JSONObject products) {
            resultsReceived(products);
        }

        @Override
        public void onCameraBarcodeDetected(SlyceBarcode barcode) {
            barcodeDetected(barcode);
        }

        @Override
        public void onCameraImageDetected(String productInfo) {
            imageDetected(productInfo);
        }

        @Override
        public void onCameraImageInfoReceived(JSONArray products) {
            imageInfoReceived(products);
        }

        @Override
        public void onCameraSlyceProgress(long progress, String message, String id) {
            slyceProgress(progress, message);
        }

        @Override
        public void onCameraSlyceRequestStage(SlyceRequestStage message) {
            slyceRequestStage(message);
        }

        @Override
        public void onSlyceCameraError(String message) {
            onError(message);
        }

        @Override
        public void onSnap(Bitmap bitmap) {
            if(isAttached) {
                // Notify ImageProcessDialogFragment that bitmap is ready
                mImageProcessDialogFragment.onSnap(bitmap);
            }
        }

        @Override
        public void onTap(float x, float y) {
            // Displays the touch point
            Utils.performAlphaAnimation(mOnTapView, x, y);
        }

        @Override
        public void onCameraFinished(){

        }
        // OnSlyceCameraListener callbacks END
    }
    // PRIVATE METHODS END

    @Override
    public void onClick(View v) {

        int id = v.getId();

        if(id == R.id.close_button){

            Buzzer.getInstance().buzz(getActivity(), R.raw.slyce_click_sound, mSlyce.isSoundOn(), false);

            close();

        }else if(id == R.id.scan_tips_button){

            Buzzer.getInstance().buzz(getActivity(), R.raw.slyce_click_sound, mSlyce.isSoundOn(), false);

            ScanningTipsDialogFragment dialogFragment = new ScanningTipsDialogFragment();
            dialogFragment.show(getFragmentManager(), null);

        }else if(id == R.id.gallery_button){

            Buzzer.getInstance().buzz(getActivity(), R.raw.slyce_click_sound, mSlyce.isSoundOn(), false);

            Utils.loadImageFromGallery(this, RESULT_LOAD_IMG);

        }else if(id == R.id.flash_button){

            Buzzer.getInstance().buzz(getActivity(), R.raw.slyce_flash_sound, mSlyce.isSoundOn(), false);

            mSlyceCamera.turnFlash();

        }else if(id == R.id.snap_button){

            Buzzer.getInstance().buzz(getActivity(), R.raw.slyce_click_sound, mSlyce.isSoundOn(), false);

            showDialogFragment(ImageProcessDialogFragment.CAMERA_BITMAP);

            // Take a picture using SlyceCamera object
            mSlyceCamera.setContinuousRecognition(false);
            mSlyceCamera.snap();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // When an Image is picked
        if (requestCode == RESULT_LOAD_IMG && resultCode == getActivity().RESULT_OK && null != data) {

            // Extract Image String
            String imageDecodableString  = Utils.getImageDecodableString(data, getActivity().getApplicationContext());

            if(TextUtils.isEmpty(imageDecodableString)){

                SlyceLog.i(TAG, "Error occurred while picking an Image");

            }else{

//                showDialogFragment(ImageProcessDialogFragment.GALLERY_BITMAP, imageDecodableString, new ImageProcessDialogFragmentListener());

                showDialogFragment(ImageProcessDialogFragment.CAMERA_BITMAP);
                BitmapWorkerTask task = new BitmapWorkerTask();
                task.execute(imageDecodableString);
            }

        } else {
            SlyceLog.i(TAG, "You haven't picked Image");
        }
    }

    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

        private String data;

        @Override
        protected void onPreExecute() {

        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            data = params[0];

            return BitmapLoader.decodeSampledBitmapFromResource(data, 400, 400);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {

            // Bitmap is ready

            // Notifying ImageProcessDialogFragment for the ready bitmap
            mImageProcessDialogFragment.onSnap(bitmap);

            // Start Slyce image search
            mSlyceRequest = createSlyceProductsRequest(bitmap);
            mSlyceRequest.execute();
        }
    }

    private SlyceProductsRequest createSlyceProductsRequest(Bitmap bitmap){

        SlyceProductsRequest request = new SlyceProductsRequest(mSlyce, new OnSlyceRequestListener() {

            @Override
            public void onResultsReceived(JSONObject products) {
                resultsReceived(products);
            }

            @Override
            public void onBarcodeDetected(SlyceBarcode barcode) {
                barcodeDetected(barcode);
            }

            @Override
            public void onImageDetected(String productInfo) {
                imageDetected(productInfo);
            }

            @Override
            public void onImageInfoReceived(JSONArray products) {
                imageInfoReceived(products);
            }

            @Override
            public void onSlyceRequestStage(SlyceRequestStage message) {

               slyceRequestStage(message);
            }

            @Override
            public void onSlyceProgress(long progress, String message, String id) {
                slyceProgress(progress, message);
            }

            @Override
            public void onError(String message) {
                onError(message);
            }

            @Override
            public void onFinished() {

            }

        }, bitmap);

        return request;
    }

    private void showDialogFragment(){

        // Create and show the dialog.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        NotFoundDialogFragment newFragment = NotFoundDialogFragment.newInstance();
        newFragment.show(ft, "NotFoundDialogFragment");
    }

    private void resultsReceived(JSONObject products){
        if(isAttached){
            // Update UI (processing fragment)
            mImageProcessDialogFragment.onResultsReceived();

            if(products.length() > 0){

                // Notify the host application of found products
                mListener.onCameraFragmentResultsReceived(products);

                close();

            }else{

                // Show "Not Found Message" fragment
                showDialogFragment();
            }
        }
    }

    private void barcodeDetected(SlyceBarcode barcode){
        if(isAttached){
            // Update UI (processing fragment)
            mImageProcessDialogFragment.onBarcodeDetected();

            // Notify the host application of found barcode
            mListener.onCameraFragmentBarcodeDetected(barcode);
        }
    }

    private void imageDetected(String productInfo){
        if(isAttached){
            if(mImageProcessDialogFragment != null){
                // Update UI (processing fragment)
                mImageProcessDialogFragment.dismiss();
            }

            // Show progress bar
            mDialogLayout.setVisibility(View.VISIBLE);

            // Notify the host application of found MS product/s
            mListener.onCameraFragmentImageDetected(productInfo);
        }
    }

    private void imageInfoReceived(JSONArray products){
        if(isAttached){
            // Dismiss progress bar
            mDialogLayout.setVisibility(View.GONE);

            // Notify the host application of found extra product/s details
            mListener.onCameraFragmentImageInfoReceived(products);
        }
    }

    private void slyceProgress(long progress, String message){
        if(isAttached){
            // Update UI (processing fragment)
            mImageProcessDialogFragment.onSlyceProgress(progress, message);
        }
    }

    private void slyceRequestStage(SlyceRequestStage message){
        if(isAttached){
            // Update UI (processing fragment)
            mImageProcessDialogFragment.onSlyceRequestStage(message);
        }
    }

    private void onError(String message){
        if(isAttached){
            // Update UI (processing fragment)
            mImageProcessDialogFragment.dismiss();

            mDialogLayout.setVisibility(View.GONE);

            // Show "Not Found Message" fragment
            showDialogFragment();

            // Notify host application
            mListener.onCameraFragmentError(message);
        }
    }
}
