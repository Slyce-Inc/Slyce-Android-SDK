package com.android.slyce.activities;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.android.slycesdk.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImageProcessFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImageProcessFragment extends Fragment implements SlyceCameraFragment.OnImageProcessListener{

    private String mImageDecodableString;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        Toast.makeText(getActivity(), "ImageProcessFragment: onSnap", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProgress(long progress, String message) {
        Toast.makeText(getActivity(), "ImageProcessFragment: onProgress", Toast.LENGTH_SHORT).show();
    }
}
