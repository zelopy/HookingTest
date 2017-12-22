package com.grepin.hookingtest.common;

import android.content.Context;
import android.provider.Settings;

/**
 * Created by P117842 on 2017-12-20.
 */

public class CheckSecure {

    private static final String TAG = CheckSecure.class.getSimpleName();


    /**
     * Check USB Debugging Mode on Device.
     * @return
     */
    public boolean isDebuggingMode(Context context) {
        int debugMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ADB_ENABLED, 0);
        return debugMode == 1;
    }
}
