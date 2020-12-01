package com.example.gw.usbprint.task;

import android.util.Log;


import com.example.gw.usbprint.common.base.BaseInfo;
import com.example.gw.usbprint.common.base.BaseRequestor;
import com.example.gw.usbprint.common.db.DBManager;
import com.example.gw.usbprint.common.db.FrmConfigKeys;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Task_CompareFace extends BaseRequestor {
    public String featureDataString;
    public String BaseUrl = BaseInfo.getJsonURL();
    //  public String BaseUrl = "http://192.168.1.106:8084/webapp_war_exploded/webapp/";

    @Override
    public Object execute() {

        RequestBody body = RequestBody.create(MediaType.parse("application/json;charset=utf-8"),featureDataString);
        String result = "";
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        Request request = new Request.Builder()
                .url(BaseUrl+"auth/getTokenForFace.do")
                .addHeader("X-Token", DBManager.getOtherConfig(FrmConfigKeys.token))
                .post(body)
                .build();
        Call call = okHttpClient.newCall(request);
        try {
            Response response = call.execute();
            result = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
    public static class CompareFeature{
        public String regionCode;
        public byte[] faceFeature;;
    }



}
