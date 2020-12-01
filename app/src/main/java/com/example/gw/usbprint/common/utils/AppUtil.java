package com.example.gw.usbprint.common.utils;

import android.app.Application;
import android.os.Environment;

import com.example.gw.usbprint.FrmApplication;


/**
 * Created by guwei on 15/4/15.
 */
public class AppUtil {

    public static Application getApplicationContext() {
        return FrmApplication.getInstance();
    }

    public static String getStoragePath() {
        return Environment.getExternalStorageDirectory().getPath() + "/certificate";
    }

}
