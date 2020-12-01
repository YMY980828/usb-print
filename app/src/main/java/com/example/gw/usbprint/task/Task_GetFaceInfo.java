package com.example.gw.usbprint.task;


import com.example.gw.usbprint.BuildConfig;
import com.example.gw.usbprint.common.base.BaseInfo;
import com.example.gw.usbprint.common.base.BaseRequestor;
import com.example.gw.usbprint.common.db.DBManager;
import com.example.gw.usbprint.common.db.FrmConfigKeys;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class Task_GetFaceInfo extends BaseRequestor {
    public String BaseUrl = BaseInfo.getJsonURL();

    @Override
    public Object execute() {

        /**
         *创建请求的参数body
         */
        String result = "";
//        ClearableCookieJar cookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(FrmApplication.applicationContext));

//        OkHttpClient okHttpClient = new OkHttpClient.Builder()
//                .cookieJar(cookieJar)
//                .build();
//        OkHttpClient okHttpClient = new OkHttpClient();
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(BaseUrl+"face/getCurrFaceUserInfo.do")
                //         .url("http://172.20.10.3:5000")
                .addHeader("X-Token", DBManager.getOtherConfig(FrmConfigKeys.token))
                .get()
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
}
