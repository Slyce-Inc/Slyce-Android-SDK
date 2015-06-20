package com.android.slyce.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.slyce.enums.SlyceRequestStage;
import com.android.slyce.roundedimage.RoundedImageView;
import com.android.slyce.utils.SlyceLog;
import com.android.slycesdk.R;

/**
 * Use the {@link ImageProcessDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImageProcessDialogFragment extends DialogFragment implements View.OnClickListener {

    public static final String TAG = ImageProcessDialogFragment.class.getSimpleName();

    public static final String CAMERA_BITMAP = "camera_bitmap";

    private static final String STARTING_REQUEST = "starting_request";
    private static final String SENDING_IMAGE = "sending_image";
    private static final String ANALYZING_IMAGE = "analyzing_image";
    private static final String FINISH_ANALYZING_IMAGE = "finish_analyzing_image";

    private static final int UPLOAD_IMAGE_TOTAL_PROGRESS_TIME = 3000;

    private int screenHeight;
    private int screenWidth;
    private int layoutSize;

    private boolean isAttached;

    /* views */
    private Button cancelButton;

    private RoundedImageView mImage;
    private ImageView sendDoneImage;
    private ImageView analyzeDoneImage;

    private ProgressBar horizontalProgressBar;
    private ProgressBar progressSendingImage;
    private ProgressBar progressAnalyzeImage;

    private TextView progressMsg;
    private TextView sendImageText;
    private TextView analyzeImageText;

    private RelativeLayout topLayout;

    private UpdateProgressBarAsyncTask task;

    private OnImageProcessListener listener;

    public interface OnImageProcessListener{
        void onCancelClicked();
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ImageProcessDialogFragment.
     */
    public static ImageProcessDialogFragment newInstance() {
        ImageProcessDialogFragment fragment = new ImageProcessDialogFragment();
        return fragment;
    }

    public ImageProcessDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        calculateSize();

//        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.SlyceDialogTheme);
        setStyle(2, 0);
        setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_image_process_dialog, container, false);

        initRoundedImage(root);

        topLayout = (RelativeLayout) root.findViewById(R.id.top_layout);

        setSize();

        horizontalProgressBar = (ProgressBar) root.findViewById(R.id.horizontal_progress_bar);
        horizontalProgressBar.setIndeterminate(false);

        progressMsg = (TextView) root.findViewById(R.id.scan_status_main);

        progressSendingImage = (ProgressBar) root.findViewById(R.id.progress_sending_image);
        progressAnalyzeImage = (ProgressBar) root.findViewById(R.id.progress_analyzing_image);

        sendDoneImage = (ImageView) root.findViewById(R.id.done_sending_image);
        analyzeDoneImage = (ImageView) root.findViewById(R.id.done_analyzing_image);

        sendImageText = (TextView) root.findViewById(R.id.text_sending_image);
        analyzeImageText = (TextView) root.findViewById(R.id.text_analyzing_image);

        cancelButton = (Button) root.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(this);

        updateProgressInfo("");

        return root;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        isAttached = true;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        isAttached = false;
    }

    @Override
    public void onClick(View v) {

        int id = v.getId();

        if(id == R.id.cancel_button){

            listener.onCancelClicked();

            dismiss();
        }
    }

    public void setOnImageProcessListener(OnImageProcessListener listener){
        this.listener = listener;
    }

    public void handleStageMessage(SlyceRequestStage message){

        switch (message){

            case StageStartingRequest:

                updateProgressInfo(STARTING_REQUEST);

                break;

            case StageSendingImage:


                break;

            case StageAnalyzingImage:

                updateProgressInfo(ANALYZING_IMAGE);

                break;
        }
    }

    public void onResultsReceived(){
        if(!isAttached){
            SlyceLog.i(TAG, "Can not perform ImageProcessDialogFragment:onResultsReceived fragment is not attached");
            return;
        }

        // Update progress bar
        updateProgressInfo(FINISH_ANALYZING_IMAGE);

        dismiss();
    }

    public void onBarcodeDetected(){
        if(!isAttached){
            SlyceLog.i(TAG, "Can not perform ImageProcessDialogFragment:onBarcodeDetected fragment is not attached");
            return;
        }

        // Update progress bar
        updateProgressInfo(FINISH_ANALYZING_IMAGE);

        dismiss();
    }

    public void onSlyceProgress(long progress, String message){
        if(!isAttached){
            SlyceLog.i(TAG, "Can not perform ImageProcessDialogFragment:onSlyceProgress fragment is not attached");
            return;
        }

        // Update progress bar
        horizontalProgressBar.setProgress(50 + (int) progress / 2);
        progressMsg.setText(message);
    }

    public void onSlyceRequestStage(SlyceRequestStage message){
        if(!isAttached){
            SlyceLog.i(TAG, "Can not perform ImageProcessDialogFragment:onSlyceRequestStage fragment is not attached");
            return;
        }

        handleStageMessage(message);
    }

    public void onSnap(Bitmap bitmap) {
        if(!isAttached){
            SlyceLog.i(TAG, "Can not perform ImageProcessDialogFragment:onSnap fragment is not attached");
            return;
        }
        mImage.setImageBitmap(bitmap);
    }
    /* End */

    /* PRIVATE METHODS */
    private void initRoundedImage(View root){
        mImage = (RoundedImageView) root.findViewById(R.id.image);
        mImage.setCornerRadius(getResources().getDimension(R.dimen.slyce_dimen_25dp));
        mImage.setBorderColor(getResources().getColor(R.color.slyce_color_white));
        mImage.setBorderWidth(getResources().getDimension(R.dimen.slyce_dimen_2dp));
        mImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mImage.setOval(false);
    }

    private void updateProgressInfo(String progress) {

        Resources resources = getResources();

        switch (progress) {
            case STARTING_REQUEST:
                task = new UpdateProgressBarAsyncTask();
                task.execute();

                progressSendingImage.setVisibility(View.VISIBLE);
                sendDoneImage.setVisibility(View.INVISIBLE);
                progressAnalyzeImage.setVisibility(View.INVISIBLE);
                analyzeDoneImage.setVisibility(View.INVISIBLE);

                sendImageText.setTextColor(resources.getColor(R.color.slyce_color_black));
                analyzeImageText.setTextColor(resources.getColor(R.color.slyce_color_grey_2_alpha_80));

                break;

            case ANALYZING_IMAGE:
                task.cancel(true);

                progressSendingImage.setVisibility(View.INVISIBLE);
                sendDoneImage.setVisibility(View.VISIBLE);
                progressAnalyzeImage.setVisibility(View.VISIBLE);
                analyzeDoneImage.setVisibility(View.INVISIBLE);

                sendImageText.setTextColor(resources.getColor(R.color.slyce_color_grey_2));
                analyzeImageText.setTextColor(resources.getColor(R.color.slyce_color_black));

                break;

            case FINISH_ANALYZING_IMAGE:

                horizontalProgressBar.setProgress(100);

                progressSendingImage.setVisibility(View.INVISIBLE);
                sendDoneImage.setVisibility(View.VISIBLE);
                progressAnalyzeImage.setVisibility(View.INVISIBLE);
                analyzeDoneImage.setVisibility(View.VISIBLE);

                sendImageText.setTextColor(resources.getColor(R.color.slyce_color_grey_2));
                analyzeImageText.setTextColor(resources.getColor(R.color.slyce_color_grey_2));

                break;

            default:

                if (task != null) {
                    task.cancel(true);
                }
                horizontalProgressBar.setProgress(0);
                progressSendingImage.setVisibility(View.INVISIBLE);
                sendDoneImage.setVisibility(View.INVISIBLE);
                progressAnalyzeImage.setVisibility(View.INVISIBLE);
                analyzeDoneImage.setVisibility(View.INVISIBLE);

                sendImageText.setTextColor(resources.getColor(R.color.slyce_color_grey_2_alpha_80));
                analyzeImageText.setTextColor(resources.getColor(R.color.slyce_color_grey_2_alpha_80));
        }
    }

    private class UpdateProgressBarAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            int i = 0;
            int updateJump = 5;
            while (i <= 50) {
                if (!isCancelled()) {
                    i = i + updateJump;
                    horizontalProgressBar.setProgress(i);
                    SystemClock.sleep(UPLOAD_IMAGE_TOTAL_PROGRESS_TIME / (50 / updateJump));

                } else {
                    return null;
                }
            }
            return null;
        }
    }
    /* End */

    private void calculateSize(){

        screenHeight = getResources().getDisplayMetrics().heightPixels;
        screenWidth = getResources().getDisplayMetrics().widthPixels;

        int orientation = getActivity().getResources().getConfiguration().orientation;

        if(screenHeight > screenWidth){
            //code for portrait mode
            layoutSize = (screenWidth * 90)/100;

        } else{
            //code for landscape mode
            layoutSize = (screenHeight * 65)/100;
        }
    }

    private void setSize(){
        topLayout.getLayoutParams().height = layoutSize;
        topLayout.getLayoutParams().width = layoutSize;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        calculateSize();
        setSize();

        super.onConfigurationChanged(newConfig);
    }
}
