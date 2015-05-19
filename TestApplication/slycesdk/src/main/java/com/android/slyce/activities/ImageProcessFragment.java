package com.android.slyce.activities;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.slyce.roundedimage.RoundedImageView;
import com.android.slycesdk.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImageProcessFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImageProcessFragment extends Fragment implements SlyceCameraFragment.OnImageProcessListener {

    private static final int UPLOAD_IMAGE_TOTAL_PROGRESS_TIME = 3000;

    private static final String BEGIN_SENDING_IMAGE = "begin_sending_image";
    private static final String BEGIN_ANALYZE_IMAGE = "begin_analyze_image";
    private static final String FINISH_ANALYZE_IMAGE = "finish_analyze_image";


    private String mImageDecodableString = null;

    private RoundedImageView mImage;

    private ProgressBar horizontalProgressBar;

    private UpdateProgressBarAsyncTask task;

    private TextView progressMsg;

    private ProgressBar progressSendingImage;
    private ProgressBar progressAnalyzeImage;

    private ImageView sendDoneImage;
    private ImageView analyzeDoneImage;

    private TextView sendImageText;
    private TextView analyzeImageText;

    private Button cancelButton;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ImageProcessFragment.
     */
    public static ImageProcessFragment newInstance() {
        ImageProcessFragment fragment = new ImageProcessFragment();
        return fragment;
    }

    public ImageProcessFragment() {
        // Required empty public constructor
    }

    public void setImageDecodableString(String value) {
        mImageDecodableString = value;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View mView = inflater.inflate(R.layout.fragment_image_process, container, false);

        mImage = (RoundedImageView) mView.findViewById(R.id.image);
        mImage.setCornerRadius(getResources().getDimension(R.dimen.fragment_image_process_image_corners_radius));
        mImage.setBorderColor(getResources().getColor(R.color.image_border_color));
        mImage.setBorderWidth(getResources().getDimension(R.dimen.fragment_image_process_image_border_width));
        mImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mImage.setOval(false);

        horizontalProgressBar = (ProgressBar) mView.findViewById(R.id.horizontal_progress_bar);
        horizontalProgressBar.setIndeterminate(false);

        progressMsg = (TextView) mView.findViewById(R.id.scan_status_main);

        progressSendingImage = (ProgressBar) mView.findViewById(R.id.progress_sending_image);
        progressAnalyzeImage = (ProgressBar) mView.findViewById(R.id.progress_analyzing_image);

        sendDoneImage = (ImageView) mView.findViewById(R.id.done_sending_image);
        analyzeDoneImage = (ImageView) mView.findViewById(R.id.done_analyzing_image);

        sendImageText = (TextView) mView.findViewById(R.id.text_sending_image);
        analyzeImageText = (TextView) mView.findViewById(R.id.text_analyzing_image);

        cancelButton = (Button) mView.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeFragment();
            }
        });

        updateProgressInfo("");

        // Set the Image in ImageView after decoding the String
//        mImage.setImageBitmap(BitmapFactory.decodeFile(mImageDecodableString));

        return mView;
    }

    private void removeFragment() {

        getFragmentManager().beginTransaction().remove(this).commit();
        // TODO stop the calls to this OnImageProcessListener
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void createBitmapFromString(String imgDecodableString) {

//        ImageView imgView = (ImageView) findViewById(R.id.imgView);
//        // Set the Image in ImageView after decoding the String
//        imgView.setImageBitmap(BitmapFactory.decodeFile(imgDecodableString));
    }

    private void updateProgressInfo(String progress) {

        Resources resources = getResources();

        switch (progress) {
            case BEGIN_SENDING_IMAGE:
                task = new UpdateProgressBarAsyncTask();
                task.execute();

                progressSendingImage.setVisibility(View.VISIBLE);
                sendDoneImage.setVisibility(View.INVISIBLE);
                progressAnalyzeImage.setVisibility(View.INVISIBLE);
                analyzeDoneImage.setVisibility(View.INVISIBLE);

                sendImageText.setTextColor(resources.getColor(R.color.image_analyse_in_process));
                analyzeImageText.setTextColor(resources.getColor(R.color.image_analyse_pre_process));

                break;

            case BEGIN_ANALYZE_IMAGE:
                task.cancel(true);

                progressSendingImage.setVisibility(View.INVISIBLE);
                sendDoneImage.setVisibility(View.VISIBLE);
                progressAnalyzeImage.setVisibility(View.VISIBLE);
                analyzeDoneImage.setVisibility(View.INVISIBLE);

                sendImageText.setTextColor(resources.getColor(R.color.image_analyse_post_process));
                analyzeImageText.setTextColor(resources.getColor(R.color.image_analyse_in_process));

                break;

            case FINISH_ANALYZE_IMAGE:

                horizontalProgressBar.setProgress(100);

                progressSendingImage.setVisibility(View.INVISIBLE);
                sendDoneImage.setVisibility(View.VISIBLE);
                progressAnalyzeImage.setVisibility(View.INVISIBLE);
                analyzeDoneImage.setVisibility(View.VISIBLE);

                sendImageText.setTextColor(resources.getColor(R.color.image_analyse_post_process));
                analyzeImageText.setTextColor(resources.getColor(R.color.image_analyse_post_process));

                //removeFragment();

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

                sendImageText.setTextColor(resources.getColor(R.color.image_analyse_pre_process));
                analyzeImageText.setTextColor(resources.getColor(R.color.image_analyse_pre_process));

        }
    }

    /**
     * {@link SlyceCameraFragment.OnImageProcessListener}
     */
    @Override
    public void onSnap(Bitmap bitmap) {
//        Toast.makeText(getActivity(), "ImageProcessFragment: onSnap", Toast.LENGTH_SHORT).show();
        mImage.setImageBitmap(bitmap);
        updateProgressInfo(BEGIN_SENDING_IMAGE);


    }

    @Override
    public void onImageStartRequest() {
//        Toast.makeText(getActivity(), "ImageProcessFragment: onImageStartRequest", Toast.LENGTH_SHORT).show();

        updateProgressInfo(BEGIN_ANALYZE_IMAGE);
    }

    @Override
    public void onProgress(long progress, String message) {
//        Toast.makeText(getActivity(), "ImageProcessFragment: onProgress" + "\n" + progress + "\n" + message, Toast.LENGTH_SHORT).show();
        horizontalProgressBar.setProgress(50 + (int) progress / 2);
        progressMsg.setText(message);
    }

    @Override
    public void onCamera3DRecognition() {
        updateProgressInfo(FINISH_ANALYZE_IMAGE);
    }

    @Override
    public void onError(String message) {
        Toast.makeText(getActivity(), "ImageProcessFragment: onError" + "\n" + message, Toast.LENGTH_SHORT).show();
        updateProgressInfo("");
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
}
