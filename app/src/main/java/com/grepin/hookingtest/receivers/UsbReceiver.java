package com.grepin.hookingtest.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.grepin.hookingtest.common.Consts;

/**
 * Created by P117842 on 2017-12-22.
 */

public class UsbReceiver extends BroadcastReceiver {

    private static final String TAG = UsbReceiver.class.getSimpleName();


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w(TAG, "onReceive() " + intent.getAction());
        Consts.USB_CONNECTION = intent.getAction();
    }
}
