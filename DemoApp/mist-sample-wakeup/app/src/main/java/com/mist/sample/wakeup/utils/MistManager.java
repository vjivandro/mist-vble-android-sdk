package com.mist.sample.wakeup.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.mist.android.AppMode;
import com.mist.android.BatteryUsage;
import com.mist.android.MSTCentralManager;
import com.mist.android.MSTCentralManagerIndoorOnlyListener;
import com.mist.android.MSTOrgCredentialsCallback;
import com.mist.android.MSTOrgCredentialsManager;
import com.mist.android.model.AppModeParams;
import com.mist.sample.wakeup.model.OrgData;

import java.lang.ref.WeakReference;
import java.util.Date;

/**
 * Created by anubhava on 26/03/18.
 */

/**
 * This is the interactor class which will interact with Mist SDK for
 * Enrollment
 * starting Mist SDK
 * stopping Mist SDK
 * Reconnection
 * Setting Mode
 */
public class MistManager implements MSTOrgCredentialsCallback {

    private static final String TAG = MistManager.class.getSimpleName();
    private static WeakReference<Context> contextWeakReference;
    private static MistManager mistManager;
    private String sdkToken;
    private String envType;
    private MSTCentralManagerIndoorOnlyListener indoorOnlyListener;
    private AppMode appMode = AppMode.FOREGROUND;
    private static OrgData orgData;
    private volatile MSTCentralManager mstCentralManager;
    private fragmentInteraction fragmentInteractionListener;

    int sendInterval = 1000*30;
    public boolean marvisEnabled;
    public boolean locationEnabled;

    private MistManager() {

    }

    public void setFragmentInteractionListener(fragmentInteraction fragmentInteractionListener) {
        this.fragmentInteractionListener = fragmentInteractionListener;
    }

    public interface fragmentInteraction {
        void onOrgDataReceived();
    }

    /**
     * Custructor for creating singleton instance of the interactor class
     *
     * @param context application instance needed by Mist SDK
     * @return
     */

    public static MistManager mistManagerInstance(Context context, String start) {
        contextWeakReference = new WeakReference<Context>(context);
        if (mistManager == null) {
            mistManager = new MistManager();
        }
        if (start != null) {
            switch (start) {
                case "location":
                    Log.d(TAG, "DebugLog: mistManagerInstance() - location ");
                    mistManager.marvisEnabled = false;
                    mistManager.locationEnabled = true;
                    break;
                case "locationMarvis":
                    Log.d(TAG, "DebugLog: mistManagerInstance() - locationMarvis ");
                    mistManager.marvisEnabled = true;
                    mistManager.locationEnabled = true;
                    break;
                case "marvis":
                    Log.d(TAG, "DebugLog: mistManagerInstance() - marvis ");
                    mistManager.marvisEnabled = true;
                    mistManager.locationEnabled = false;
                    break;
                default:
                    Log.d(TAG, "DebugLog: mistManagerInstance() - locationMarvis (default)");
                    mistManager.marvisEnabled = true;
                    mistManager.locationEnabled = true;
            }
        }

        return mistManager;
    }

    /**
     * This method will enroll the device and start the Mist SDK on successful enrollment, if we already have the deatil of enrollment response detail we can just start the SDK with those details
     *
     * @param sdkToken           Token used for enrollment
     * @param indoorOnlyListener listener on which callback for location,map,notification can be heard
     * @param appMode            mode of the app (Background,Foreground)
     */
    public void init(String sdkToken, MSTCentralManagerIndoorOnlyListener indoorOnlyListener,
                     AppMode appMode) {
        if (sdkToken != null && !sdkToken.isEmpty()) {
            this.sdkToken = sdkToken;
            this.envType = String.valueOf(sdkToken.charAt(0));
            this.indoorOnlyListener = indoorOnlyListener;
            if (appMode != null) {
                this.appMode = appMode;
            }
            orgData = SharedPrefUtils.readConfig(contextWeakReference.get(), sdkToken);
            if (orgData == null || orgData.getSdkSecret() == null || orgData.getSdkSecret().isEmpty()) {
                Log.d(TAG, "DebugLog: Enrolling device using SDK token: "+ sdkToken);
                MSTOrgCredentialsManager.enrollDeviceWithToken(contextWeakReference.get(), sdkToken, this);

            } else {
                connect(indoorOnlyListener, appMode);
            }
        } else {
            Log.d(TAG, "Empty SDK Token");
        }
    }

    /**
     * This method is used to start the Mist SDk
     *
     * @param indoorOnlyListener listener on which callback for location,map,notification can be heard
     * @param appMode            mode of the app (Background,Foreground)
     */
    private synchronized void connect(MSTCentralManagerIndoorOnlyListener indoorOnlyListener, AppMode appMode) {
        if (mstCentralManager == null) {
            mstCentralManager = new MSTCentralManager(contextWeakReference.get(),
                    orgData.getOrgId(), orgData.getSdkSecret());
            mstCentralManager.setEnvironment(Utils.getEnvironment(envType));

//            if (appMode.equals(AppMode.FOREGROUND)) {
//                setAppMode(Utils.getConfiguredAppModeParams(AppMode.FOREGROUND, BatteryUsage.HIGH_BATTERY_USAGE_HIGH_ACCURACY));
//            } else {
//                setAppMode(Utils.getConfiguredAppModeParams(AppMode.BACKGROUND, BatteryUsage.LOW_BATTERY_USAGE_LOW_ACCURACY));
//            }

            if (appMode.equals(AppMode.FOREGROUND)) {
                setAppMode(Utils.getConfiguredAppModeParams(AppMode.FOREGROUND, BatteryUsage.HIGH_BATTERY_USAGE_HIGH_ACCURACY));
            } else {
                setAppMode(new AppModeParams(AppMode.BACKGROUND,
                        BatteryUsage.LOW_BATTERY_USAGE_LOW_ACCURACY,
                        true,
                        0.5,
                        1.0));
            }
            mstCentralManager.setMSTCentralManagerIndoorOnlyListener(indoorOnlyListener);

            if (!locationEnabled) {
                mstCentralManager.disableLocation();
            }
            if (!marvisEnabled) {
                mstCentralManager.disableMarvis();
            }

            mstCentralManager.setMarvisSendInterval(sendInterval);
            mstCentralManager.setMarvisPassiveTestInterval(1000);
            mstCentralManager.setMarvisMaxSavedResultsSizeInKB(1024);
            Log.d(TAG, "DebugLog: MistManager.connect()");
            mstCentralManager.start();
        } else {
            reconnect();
        }
    }

    /**
     * @param appModeParams params to let SDK know about the scanning frequency and the state of the app (background or foreground)
     *                      call this method to switch the mode when app changes the mode between foreground and background
     */
    public void setAppMode(AppModeParams appModeParams) {
        if (this.mstCentralManager != null) {
            this.mstCentralManager.setAppMode(appModeParams);
            this.appMode = appModeParams.getAppMode();
        }
    }

    /**
     * This is the callback method which will receive the following information from the Mist SDK enrollment call
     *
     * @param orgName   name of the token used for the enrollment
     * @param orgID     organization id
     * @param sdkSecret secret needed to start the Mist SDK
     * @param error     error message if any
     * @param envType   envType which will be used to set the environment
     */
    @Override
    public void onReceivedSecret(String orgName, String orgID, String sdkSecret, String error, String envType) {
        if (!TextUtils.isEmpty(sdkSecret) && !TextUtils.isEmpty(orgID) && !TextUtils.isEmpty(sdkSecret)) {
            Log.d(TAG, "DebugLog: Received Org  ID: "+ orgID);
            saveConfig(orgName, orgID, sdkSecret, envType);
            connect(indoorOnlyListener, appMode);
        } else {
            if (!Utils.isEmptyString(error)) {
                if (indoorOnlyListener != null) {
                    indoorOnlyListener.onMistErrorReceived(error, new Date());
                }
            }
        }
    }

    /**
     * This method is saving the following details so that we can use it again for starting Mist SDK without need for enrollment again
     *
     * @param orgName   name of the token used for the enrollment
     * @param orgID     organization id
     * @param sdkSecret secret needed to start the Mist SDK
     * @param envType   envType which will be used to set the environment
     */
    private void saveConfig(String orgName, String orgID, String sdkSecret, String envType) {
        orgData = new OrgData(orgName, orgID, sdkSecret, envType);
        SharedPrefUtils.saveConfig(contextWeakReference.get(), orgData, sdkToken);
        if (fragmentInteractionListener != null) {
            fragmentInteractionListener.onOrgDataReceived();
        }
    }

    /**
     * This method will stop the Mist SDK
     */
    public void disconnect() {
        if (mstCentralManager != null) {
            mstCentralManager.stop();
        }
    }

    /**
     * This method will reconnect the Mist SDK
     */
    private synchronized void reconnect() {
        if (mstCentralManager != null) {
            disconnect();
            if (appMode.equals(AppMode.FOREGROUND)) {
                setAppMode(Utils.getConfiguredAppModeParams(AppMode.FOREGROUND, BatteryUsage.HIGH_BATTERY_USAGE_HIGH_ACCURACY));
            } else {
                ///
                setAppMode(new AppModeParams(AppMode.BACKGROUND,
                        BatteryUsage.LOW_BATTERY_USAGE_LOW_ACCURACY,
                        true,
                        0.5,
                        1.0));
            }
            mstCentralManager.setMSTCentralManagerIndoorOnlyListener(indoorOnlyListener);

            if (!locationEnabled) {
                mstCentralManager.disableLocation();
            }
            if (!marvisEnabled) {
                mstCentralManager.disableMarvis();
            }
            mstCentralManager.setMarvisSendInterval(sendInterval);
            mstCentralManager.setMarvisPassiveTestInterval(1000);
            mstCentralManager.setMarvisMaxSavedResultsSizeInKB(1024);
            Log.d(TAG, "DebugLog: MistManager.reconnect()");
            mstCentralManager.start();
        }
    }

    /**
     * This method will clear/destroy the Mist SDK instance
     */
    public synchronized void destroy() {
        if (mstCentralManager != null) {
            Log.d(TAG, "DebugLog: MSTCentralManager destroyed");
            mstCentralManager.stop();
            mstCentralManager = null;
        }
    }
}
