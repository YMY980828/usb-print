package com.example.gw.usbprint.task;

import com.example.gw.usbprint.common.base.BaseRequestor;
import com.example.gw.usbprint.common.http.CommnAction;

import okhttp3.FormBody;
import okhttp3.RequestBody;

/**
 * Created by gw on 2018/8/29.
 */

public class Task_GetAreasByParAreaCode extends BaseRequestor {
    public String areaCode;

    @Override
    public Object execute() {
        RequestBody body = new FormBody.Builder()
                .add("areaCode", areaCode)
                .build();
        return CommnAction.request(body, "dic/getAreasByParAreaCode.do");
    }
}
