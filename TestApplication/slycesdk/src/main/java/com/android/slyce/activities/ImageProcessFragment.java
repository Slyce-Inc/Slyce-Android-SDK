package com.android.slyce.activities;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.slycesdk.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImageProcessFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImageProcessFragment extends Fragment implements SlyceCameraFragment.OnImageProcessListener{

    private static final String ARG_IMAGE_STRING = "arg_image_string";

    private String mImgDecodableString;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param imageString picked image String.
     * @return A new instance of fragment ImageProcessFragment.
     */
    public static ImageProcessFragment newInstance(String imageString) {
        ImageProcessFragment fragment = new ImageProcessFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_STRING, imageString);
        fragment.setArguments(args);
        return fragment;
    }

    public ImageProcessFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mImgDecodableString = getArguments().getString(ARG_IMAGE_STRING);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_image_process, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void createBitmapFromString(String imgDecodableString){

//        ImageView imgView = (ImageView) findViewById(R.id.imgView);
//        // Set the Image in ImageView after decoding the String
//        imgView.setImageBitmap(BitmapFactory.decodeFile(imgDecodableString));

    }

    /** {@link SlyceCameraFragment.OnImageProcessListener} */
    @Override
    public void onSnap(Bitmap bitmap) {

    }

    @Override
    public void onProgress(long progress, String message) {

    }
}
