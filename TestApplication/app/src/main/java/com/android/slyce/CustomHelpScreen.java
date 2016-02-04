package com.android.slyce;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.slyce.fragments.BaseDialogFragment;

/**
 * Created by user on 28/01/2016.
 */
public class CustomHelpScreen extends BaseDialogFragment {


    private final static String DIALOG_TYPE = "dialog_type";
    public final static String NOT_FOUND_DIALOG = "not_found_dialog";
    public final static String SCAN_TIPS_DIALOG = "scan_tips_dialog";
    public final static String GENERAL_DIALOG = "general_dialog";

    private int mLayoutId;

    String dialogType;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment {@link CustomHelpScreen}.
     */
    public static CustomHelpScreen newInstance(String dialogType) {
        CustomHelpScreen fragment = new CustomHelpScreen();
        Bundle args = new Bundle();

        args.putString(DIALOG_TYPE, dialogType);

        fragment.setArguments(args);
        return fragment;
    }

    public CustomHelpScreen() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dialogType = getArguments().getString(DIALOG_TYPE);

        switch(dialogType){

            case NOT_FOUND_DIALOG:

                setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

                mLayoutId = R.layout.custom_help_screen;// replace with your custom layout for "Scanning Tips" screen

                break;

            case SCAN_TIPS_DIALOG:

                setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

                mLayoutId = R.layout.custom_help_screen;// replace with your custom layout for "Not Found" screen

                break;
            case GENERAL_DIALOG:

                setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

                mLayoutId = R.layout.custom_help_screen;// replace with your custom layout for a general screen, that will be shown upon click of the custom button

                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View root = inflater.inflate(mLayoutId, container, false);
        ((TextView)root.findViewById(R.id.text)).setText(dialogType);
        return root;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mListener != null) {
            mListener.onBaseDialogFragmentDismiss(null, false);
        }
    }


}
