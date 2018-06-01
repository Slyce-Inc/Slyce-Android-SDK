package it.slyce.slycesdk_headless_demo_java;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import static android.graphics.Typeface.DEFAULT_BOLD;
import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class CameraResultFragment extends Fragment {

    private static final String KEY_IMAGE_RES_ID = "KEY_IMAGE_RES_ID";
    private static final String TAG = CameraResultFragment.class.getSimpleName();

    private static final int INVALID_IMAGE_RES_ID = -1;

    private CameraResultDisplayListener listener;
    private ImageView close;
    private ImageView image;
    private ProgressBar progressBar;
    private ViewGroup container;

    /**
     * Creates a new instance.
     *
     * @return a new instance.
     */
    public static CameraResultFragment newInstance() {
        return new CameraResultFragment();
    }

    /**
     * Creates a new instance with the provided image resource ID.
     *
     * @param imageResId
     * @return a new instance with the provided image resource ID.
     */
    public static CameraResultFragment newInstanceWithImage(int imageResId) {
        CameraResultFragment fragment = new CameraResultFragment();

        Bundle args = new Bundle();
        args.putInt(KEY_IMAGE_RES_ID, imageResId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            listener = (CameraResultDisplayListener) context;
        } catch (ClassCastException e) {
            Log.d(TAG, "onAttach: " + e.getMessage());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        close = view.findViewById(R.id.camera_result_close_button);
        image = view.findViewById(R.id.camera_result_image_view);
        progressBar = view.findViewById(R.id.camera_result_progress_bar);
        container = view.findViewById(R.id.camera_result_container);

        setupImage();

        close.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCameraResultDisplayClosed();
            }
        });
    }

    public void addSearchUpdate(String header, String update) {

        if (header != null && getContext() != null) {
            AppCompatTextView headerText = new AppCompatTextView(getContext());
            headerText.setText(header);
            headerText.setAllCaps(true);
            headerText.setTextColor(Color.BLACK);
            headerText.setTextSize(COMPLEX_UNIT_SP, 16);
            headerText.setTypeface(null, DEFAULT_BOLD.getStyle());
            headerText.setPadding(0, 16, 0, 0);
            container.addView(headerText);
        }

        if (update != null && getContext() != null) {
            AppCompatTextView updateText = new AppCompatTextView(getContext());
            updateText.setText(update);
            updateText.setTextColor(Color.DKGRAY);
            updateText.setTextSize(COMPLEX_UNIT_SP, 12);
            updateText.setPadding(0, 16, 0, 0);
            container.addView(updateText);
        }
    }

    public void notifySearchComplete() {
        if (progressBar != null) {
            progressBar.setVisibility(GONE);
        }
    }

    private void setupImage() {
        int imageResId = getArguments() != null ? getArguments().getInt(KEY_IMAGE_RES_ID, INVALID_IMAGE_RES_ID) : INVALID_IMAGE_RES_ID;
        if (imageResId != INVALID_IMAGE_RES_ID) {
            image.setVisibility(VISIBLE);
            image.setImageResource(imageResId);
        } else {
            image.setVisibility(GONE);
        }
    }
}
