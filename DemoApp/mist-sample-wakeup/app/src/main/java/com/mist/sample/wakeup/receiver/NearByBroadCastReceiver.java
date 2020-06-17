package com.mist.sample.wakeup.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.mist.sample.wakeup.service.NearByJobIntentService;

public class NearByBroadCastReceiver extends BroadcastReceiver {

    private static final String TAG = NearByBroadCastReceiver.class.getSimpleName();

    public NearByBroadCastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received Broadcast");

        if (intent != null) {
            NearByJobIntentService.enqueueWork(context, intent);
        }
//        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo netInfo = conMan.getActiveNetworkInfo();
//        if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI)
//            Log.d("WifiReceiver", "DebugLog: Have Wifi Connection");
//        else
//            Log.d("WifiReceiver", "DebugLog: Don't have Wifi Connection");
    }
}
