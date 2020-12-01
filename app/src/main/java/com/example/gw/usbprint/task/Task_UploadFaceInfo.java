package com.example.gw.usbprint.task;

import android.util.Log;


import com.example.gw.usbprint.common.base.BaseInfo;
import com.example.gw.usbprint.common.base.BaseRequestor;
import com.example.gw.usbprint.common.db.DBManager;
import com.example.gw.usbprint.common.db.FrmConfigKeys;

import org.json.JSONArray;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Task_UploadFaceInfo extends BaseRequestor {
    public String featureDataString;
    public String BaseUrl = BaseInfo.getJsonURL();
    //  public String BaseUrl = "http://192.168.1.106:8084/webapp_war_exploded/webapp/";
    @Override
    public Object execute() {
        RequestBody body = RequestBody.create(MediaType.parse("application/json;charset=utf-8"),featureDataString);
        String result = "";
        OkHttpClient okHttpClient = new OkHttpClient.Builder().addInterceptor(new LoggingInterceptor()).addNetworkInterceptor(new LoggingInterceptor()).build();
        Request request = new Request.Builder()
                .url(BaseUrl+"face/registerCurrFaceFeature.do")
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
        Log.d("ymy",result);
        return result;
    }
    public static class Feature{
       public byte[] featureData;
        public Long faceUserId;
        public Integer userType;
        public String faceName;
        public String faceIdCard;
    }

    class LoggingInterceptor implements Interceptor {
        private static final String TAG = "ymy";
        //当网络请求时就会调用该方法
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            //获取到原来的request，注意这是还没真正的请求服务端
            Request request = chain.request();
            //获取当前时间，并打印日志说要发起请求了
            //同时使用了前面我们也讲到了的headers打印请求头
            long t1 = System.nanoTime();
            //这里打印可以使用自己的日志框架
            Log.d(TAG, String.format("Sending request %s on %s%n%s",
                    request.url(), chain.connection(), request.headers()));
            Response response = chain.proceed(request);
            long t2 = System.nanoTime();
            Log.d(TAG, String.format("Received response for %s in %.1fms%n%s",
                    response.request().url(), (t2 - t1) / 1e6d, response.headers()));
            return response;
        }
    }
}
