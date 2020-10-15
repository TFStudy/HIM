package com.ztf.him.utils;

import android.util.Log;

import com.ztf.him.app.App;

public class LogUtils {
    private static final String TAG = "ztfLog";

    public static void d(String s) {
        if (App.isDebug()){
            Log.d(TAG,s);
        }
    }

}
