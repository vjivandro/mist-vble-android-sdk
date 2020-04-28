package com.mist.sample.wakeup.fragment;

import android.Manifest;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.MessageFilter;
import com.google.android.gms.nearby.messages.MessagesOptions;
import com.google.android.gms.nearby.messages.NearbyMessagesStatusCodes;
import com.google.android.gms.nearby.messages.NearbyPermissions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.mist.android.AppMode;
import com.mist.android.BatteryUsage;
import com.mist.android.MSTCentralManagerIndoorOnlyListener;
import com.mist.android.MSTCentralManagerStatusCode;
import com.mist.android.MSTMap;
import com.mist.android.MSTPoint;
import com.mist.android.MSTVirtualBeacon;
import com.mist.sample.wakeup.R;
import com.mist.sample.wakeup.app.MainApplication;
import com.mist.sample.wakeup.model.OrgData;
import com.mist.sample.wakeup.receiver.NearByBroadCastReceiver;
import com.mist.sample.wakeup.service.NearByJobIntentService;
import com.mist.sample.wakeup.utils.MistManager;
import com.mist.sample.wakeup.utils.SharedPrefUtils;
import com.mist.sample.wakeup.utils.Utils;

import org.json.JSONArray;

import java.util.Date;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MarvisFragment extends Fragment implements MSTCentralManagerIndoorOnlyListener, MistManager.fragmentInteraction,
                                                        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    public static final String TAG = MarvisFragment.class.getSimpleName();
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final String SDK_TOKEN = "sdkToken";

    private MainApplication mainApplication;
    private final String START_STRING = "marvis";

    private String sdkToken;
    private Unbinder unbinder;
    private HandlerThread sdkHandlerThread;
    private Handler sdkHandler;
    private GoogleApiClient googleApiClient;

    public enum AlertType {
        network,
        location
    }

    /*
     *  Fragment Life Cycle
     * */

    public static MarvisFragment newInstance(String sdkToken) {
        Bundle bundle = new Bundle();
        bundle.putString(SDK_TOKEN, sdkToken);
        MarvisFragment marvisFragment = new MarvisFragment();
        marvisFragment.setArguments(bundle);
        return marvisFragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.marvis_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null)
            mainApplication = (MainApplication) getActivity().getApplication();
        if (getArguments() != null)
            sdkToken = getArguments().getString(SDK_TOKEN);
        if (havePermissions()) {
            buildGoogleApiClient();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sdkHandlerThread = new HandlerThread("SDKHandler");
        sdkHandlerThread.start();
        sdkHandler = new Handler(sdkHandlerThread.getLooper());
        MistManager.newInstance(mainApplication,START_STRING).setFragmentInteractionListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        //connecting to the google api client
        if (googleApiClient != null && !googleApiClient.isConnected()) {
            googleApiClient.connect();
        }

        try {
            //stopping the scheduled job when the app comes to the foreground
            Utils.stopScheduledJob(mainApplication);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        //disconnecting the Mist sdk, to make sure there is no prior active instance
        MistManager.newInstance(mainApplication,START_STRING).disconnect();
        MistManager.newInstance(mainApplication,START_STRING).
                setAppMode(Utils.getConfiguredAppModeParams(AppMode.FOREGROUND, BatteryUsage.HIGH_BATTERY_USAGE_HIGH_ACCURACY));

        //initializing the Mist sdk
        initMISTSDK();
        SharedPrefUtils.setShouldShowWelcome(getActivity(), false);
    }

    @Override
    public void onStop() {
        super.onStop();
        //stopping the Mist sdk
        MistManager.newInstance(mainApplication,START_STRING).disconnect();
        MistManager.newInstance(mainApplication,START_STRING).
                setAppMode(Utils.getConfiguredAppModeParams(AppMode.BACKGROUND,BatteryUsage.LOW_BATTERY_USAGE_LOW_ACCURACY));
        sdkHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    //scheduling the job to run Mist sdk in the background
                    Utils.scheduleJob(mainApplication.getApplicationContext());
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }, 500);

        //disconnecting from the google api client
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        SharedPrefUtils.setShouldShowWelcome(getActivity(), true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        sdkHandler = null;

        if (sdkHandlerThread != null) {
            sdkHandlerThread.quitSafely();
            sdkHandlerThread = null;
        }

        try {
            //stopping the scheduled job when the app comes to the foreground
            Utils.stopScheduledJob(mainApplication);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        //disconnecting the Mist sdk, to make sure there is no prior active instance
        MistManager.newInstance(mainApplication, START_STRING).destroy();
    }


    /*
     *  Permisions
     * */

    private void showLocationPermissionDialog() {
        if (getActivity() != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect beacons in the background.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSION_REQUEST_FINE_LOCATION);
                }
            });
            builder.show();
        }
    }

    private void showSettingsAlert(final MarvisFragment.AlertType alertType) {
        if (getActivity() != null) {
            final String sTitle, sButton;
            if (alertType == MarvisFragment.AlertType.network) {
                sTitle = "Network Connection is disabled in your device. Would you like to enable it?";
                sButton = "Goto Settings Page To Enable Network Connection";
            } else {
                sTitle = "Location is disabled in your device. Would you like to enable it?";
                sButton = "Goto Settings Page To Enable Location";
            }

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setMessage(sTitle)
                    .setCancelable(false)
                    .setPositiveButton(sButton,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    Intent intentOpenBluetoothSettings = new Intent();
                                    if (alertType == MarvisFragment.AlertType.network) {
                                        intentOpenBluetoothSettings.setAction(Settings.ACTION_WIFI_SETTINGS);
                                    } else if (alertType == MarvisFragment.AlertType.location) {
                                        intentOpenBluetoothSettings.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                    }

                                    startActivity(intentOpenBluetoothSettings);
                                }
                            });
            alertDialogBuilder.setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            final AlertDialog.Builder builder = new
                                    AlertDialog.Builder(getActivity());
                            builder.setTitle("Functionality won't work");
                            builder.setMessage(sButton);
                            builder.setPositiveButton(android.R.string.ok, null);
                            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                }
                            });
                            builder.show();
                        }
                    });
            AlertDialog alert = alertDialogBuilder.create();
            alert.show();
        }
    }

    /*
     *  MistManager.fragmentInteraction listener methods
     * */

    @Override
    public void onOrgDataReceived() {
        subscribe();
    }


    /*
     * Mist SDK
     * */

    private void initMISTSDK() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getActivity() != null &&
                getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            showLocationPermissionDialog();
        } else {
            startMistSdk();
        }
    }

    private void startMistSdk() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if ( getActivity() != null && Utils.isNetworkAvailable(getActivity()) && Utils.isLocationServiceEnabled(getActivity())) {
            runMISTSDK();
        } else {
            if (getActivity() != null && !Utils.isNetworkAvailable(getActivity())) {
                showSettingsAlert(MarvisFragment.AlertType.network);
            }
            if (getActivity() != null && !Utils.isLocationServiceEnabled(getActivity())) {
                showSettingsAlert(MarvisFragment.AlertType.location);
            }
        }
    }

    //initializing the Mist sdk with sdkToken
    private void runMISTSDK() {
        MistManager mistManager = MistManager.newInstance(mainApplication, START_STRING);
        mistManager.init(sdkToken, this, AppMode.FOREGROUND);
    }



    /**
     * Setting up google client for getting callback from OS for registered beacons
     */
    private synchronized void buildGoogleApiClient() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addApi(Nearby.MESSAGES_API, new MessagesOptions.Builder()
                            .setPermissions(NearbyPermissions.BLE)
                            .build())
                    .addConnectionCallbacks(this)
                    .build();
        }
    }

    //checking for location permission
    private boolean havePermissions() {
        return ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "GoogleApiClient Connected !!");
        subscribe();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, getString(R.string.connection_suspended) + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Snackbar.make(getActivity().findViewById(android.R.id.content),
                getString(R.string.play_service_exception) + connectionResult.getErrorMessage(),
                Snackbar.LENGTH_LONG).show();
    }


    /**
     * Register the iBeacons for which we need the callback from OS when entered the region
     */
    private void subscribe() {

        String orgId = null, subOrgId = null;

        OrgData orgData = SharedPrefUtils.readConfig(getActivity().getApplicationContext(), sdkToken);

        if (orgData != null) {
            orgId = orgData.getOrgId();
            subOrgId = orgId.substring(0, orgId.length() - 2);

            MessageFilter filter = new MessageFilter.Builder()
                    .includeIBeaconIds(UUID.fromString(orgId), null, null)
                    .includeIBeaconIds(UUID.fromString(subOrgId + "00"), null, null)
                    .includeIBeaconIds(UUID.fromString(subOrgId + "01"), null, null)
                    .includeIBeaconIds(UUID.fromString(subOrgId + "02"), null, null)
                    .includeIBeaconIds(UUID.fromString(subOrgId + "03"), null, null)
                    .includeIBeaconIds(UUID.fromString(subOrgId + "04"), null, null)
                    .includeIBeaconIds(UUID.fromString(subOrgId + "05"), null, null)
                    .includeIBeaconIds(UUID.fromString(subOrgId + "06"), null, null)
                    .includeIBeaconIds(UUID.fromString(subOrgId + "07"), null, null)
                    .includeIBeaconIds(UUID.fromString(subOrgId + "08"), null, null)
                    .includeIBeaconIds(UUID.fromString(subOrgId + "0a"), null, null)
                    .includeIBeaconIds(UUID.fromString(subOrgId + "0b"), null, null)
                    .includeIBeaconIds(UUID.fromString(subOrgId + "0c"), null, null)
                    .includeIBeaconIds(UUID.fromString(subOrgId + "0d"), null, null)
                    .build();

            SubscribeOptions options = new SubscribeOptions.Builder()
                    .setStrategy(Strategy.BLE_ONLY)
                    .setFilter(filter)
                    .build();

            Nearby.Messages.subscribe(googleApiClient, getPendingIntent(), options)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.isSuccess()) {
                                Log.d(TAG, "Successfully Subscribed");
                                getActivity().startService(getBackgroundSubscribeServiceIntent());
                            } else {
                                Log.d(TAG, "Operation Failed, Error : " +
                                        NearbyMessagesStatusCodes.getStatusCodeString(status.getStatusCode()));
                            }
                        }
                    });
        }
    }

    private Intent getBackgroundSubscribeServiceIntent() {
        return new Intent(getActivity(), NearByJobIntentService.class);
    }

    private PendingIntent getPendingIntent() {
        Intent nearByIntent = new Intent(getActivity(), NearByBroadCastReceiver.class);
        return PendingIntent.getBroadcast(getActivity(), 0, nearByIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /*
    *
    *  MSTCentralManagerIndoorOnlyListener methods
    *
    * */


    @Override
    public void onBeaconDetected(JSONArray beaconArray, Date dateUpdated) {

    }

    @Override
    public void onRelativeLocationUpdated(MSTPoint relativeLocation, MSTMap[] maps, Date dateUpdated) {

    }

    @Override
    public void onPressureUpdated(double pressure, Date dateUpdated) {

    }

    @Override
    public void onMapUpdated(MSTMap map, Date dateUpdated) {

    }

    @Override
    public void onVirtualBeaconListUpdated(MSTVirtualBeacon[] virtualBeacons, Date dateUpdated) {

    }

    @Override
    public void onNotificationReceived(Date dateReceived, String message) {

    }

    @Override
    public void onClientInformationUpdated(String clientName) {

    }

    @Override
    public void receivedLogMessageForCode(String message, MSTCentralManagerStatusCode code) {

    }

    @Override
    public void receivedVerboseLogMessage(String message) {

    }

    @Override
    public void onMistErrorReceived(String message, Date date) {

    }

    @Override
    public void onMistRecommendedAction(String message) {

    }

}
