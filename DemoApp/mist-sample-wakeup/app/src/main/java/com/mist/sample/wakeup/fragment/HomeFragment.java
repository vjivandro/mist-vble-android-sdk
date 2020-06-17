package com.mist.sample.wakeup.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionMenu;
import com.mist.sample.wakeup.R;
import com.mist.sample.wakeup.utils.SharedPrefUtils;
import com.mist.sample.wakeup.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class HomeFragment extends Fragment {

    private static final String TOKEN_PREF_KEY_NAME = "sdkToken";
    private static final String START_STRING = "startString";
    public static final String TAG = HomeFragment.class.getSimpleName();
    // you can replace this text with your sdk token
    //public static String sdkToken = "";


    //Salambao Staging
    //public static String sdkToken = "Sjn-dhuDQDOJme6YjfUZHxcABuJt-mRA";

    //Deeyo Solutions (Production)
    //public static String sdkToken ="Pn-7Tgw2zlqFPC0yTO8PM5f4HXTWr94o";

    //THE TRUE MIST OFFICE (Production - GCP)
    //public static String sdkToken ="GXaXAQ4dNIYKuSwCIvQF5K1NjD9Iduhx";

    //THE TRUE MIST OFFICE (Staging - GCP)
    //public static String sdkToken ="Tp9UfMSJZNzwl4gCAIpZBcl1CbDBs-0H";
    //public static String sdkToken ="gjNU8ReS6f4O9larOBGDA9Yv9Q7fG2Ix";

    //Mist Office Staging
    //public static String sdkToken = "SCdBjQYYUyExpLXTiujfxKmH0dt4hPWw";


    // Mist Office
    //public static String sdkToken = "PTqTykTl4QJDwFEn9EjevfPRvci41xRi";

    //Live Demo
    public static String sdkToken = "P4XdE1dLFQrmW7JU5Qkqplp6vJWTmRPH";

    //Kevin
    //public static String sdkToken = "Pkd9KWOJpPsh9UbYpNDquDTjPy8x7cIM";

    //Deeyo - Remove / Comment out for Walmart Zibra
    @BindView(R.id.token_menu)

    FloatingActionMenu fabTokenMenu;

    private Unbinder unbinder;
    private SdkTokenReceivedListener sdkTokenReceivedListener;

    //returns an instance of the HomeFragment
    public static Fragment newInstance() {
        return new HomeFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);

        return view;
    }

    //checking if the interface is implemented by the parent activity
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            sdkTokenReceivedListener = (SdkTokenReceivedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement SdkTokenReceivedListener");
        }
    }

    @OnClick(R.id.btn_location)
    public void onClickLocation() {
        sdkToken = TextUtils.isEmpty(SharedPrefUtils.readSdkToken(getActivity(), TOKEN_PREF_KEY_NAME)) ? sdkToken : SharedPrefUtils.readSdkToken(getActivity(), TOKEN_PREF_KEY_NAME);
        SharedPrefUtils.saveSdkToken(getActivity(), TOKEN_PREF_KEY_NAME, sdkToken);
        SharedPrefUtils.saveSdkToken(getActivity(), START_STRING, "location");
        if (Utils.isEmptyString(sdkToken) && getActivity() != null) {
            Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.enter_sdk_token, Snackbar.LENGTH_LONG).show();
        } else if (sdkToken.toUpperCase().charAt(0) == 'P'
                || sdkToken.toUpperCase().charAt(0) == 'S'
                || sdkToken.toUpperCase().charAt(0) == 'E'
                || sdkToken.charAt(0) == 'G'
                || sdkToken.charAt(0) == 'g') {
            sdkTokenReceivedListener.OnSdkTokenReceived(sdkToken,"location");
        } else {
            Toast.makeText(getActivity(), R.string.valid_sdk_token, Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.btn_loaction_marvis)
    public void onClickLocationMarvis() {
        sdkToken = TextUtils.isEmpty(SharedPrefUtils.readSdkToken(getActivity(), TOKEN_PREF_KEY_NAME)) ? sdkToken : SharedPrefUtils.readSdkToken(getActivity(), TOKEN_PREF_KEY_NAME);
        SharedPrefUtils.saveSdkToken(getActivity(), TOKEN_PREF_KEY_NAME, sdkToken);
        SharedPrefUtils.saveSdkToken(getActivity(), START_STRING, "locationMarvis");
        if (Utils.isEmptyString(sdkToken) && getActivity() != null) {
            Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.enter_sdk_token, Snackbar.LENGTH_LONG).show();
        } else if (sdkToken.toUpperCase().charAt(0) == 'P'
                || sdkToken.toUpperCase().charAt(0) == 'S'
                || sdkToken.toUpperCase().charAt(0) == 'E'
                || sdkToken.charAt(0) == 'G'
                || sdkToken.charAt(0) == 'g') {
            sdkTokenReceivedListener.OnSdkTokenReceived(sdkToken, "locationMarvis");
        } else {
            Toast.makeText(getActivity(), R.string.valid_sdk_token, Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.btn_marvis)
    public void onClickMarvis() {
        sdkToken = TextUtils.isEmpty(SharedPrefUtils.readSdkToken(getActivity(), TOKEN_PREF_KEY_NAME)) ? sdkToken : SharedPrefUtils.readSdkToken(getActivity(), TOKEN_PREF_KEY_NAME);
        SharedPrefUtils.saveSdkToken(getActivity(), TOKEN_PREF_KEY_NAME, sdkToken);
        SharedPrefUtils.saveSdkToken(getActivity(), START_STRING, "marvis");
        if (Utils.isEmptyString(sdkToken) && getActivity() != null) {
            Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.enter_sdk_token, Snackbar.LENGTH_LONG).show();
        } else if (sdkToken.toUpperCase().charAt(0) == 'P'
                || sdkToken.toUpperCase().charAt(0) == 'S'
                || sdkToken.toUpperCase().charAt(0) == 'E'
                || sdkToken.charAt(0) == 'G'
                || sdkToken.charAt(0) == 'g') {
            sdkTokenReceivedListener.OnSdkTokenReceived(sdkToken, "marvis");
        } else {
            Toast.makeText(getActivity(), R.string.valid_sdk_token, Toast.LENGTH_SHORT).show();
        }
    }


    //Deeyo - Remove / Comment out for Walmart Zibra
    @OnClick(R.id.add_token_button)
    public void onClickAddTokenButton() {

        AddTokenDialogFragment tokenDialogFragment = AddTokenDialogFragment.newInstance();
        tokenDialogFragment.show(getFragmentManager(), "dialog");
        tokenDialogFragment.setCancelable(false);
        fabTokenMenu.close(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    //interface to send the token to the parent activity
    public interface SdkTokenReceivedListener {
        void OnSdkTokenReceived(String sdkToken, String start);
    }
}
