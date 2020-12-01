package com.example.gw.usbprint.task;


import com.example.gw.usbprint.common.base.BaseRequestor;
import com.example.gw.usbprint.common.http.CommnAction;

import okhttp3.FormBody;
import okhttp3.RequestBody;

/**
 * Created by gw on 2018/5/11.
 */

public class Task_UpdateApp extends BaseRequestor {
    public String version;

    @Override
    public Object execute() {
        RequestBody body = new FormBody.Builder()
                .add("version", version)
                .add("type", "4")
                .add("clientType", "2")
                .build();
        return CommnAction.request(body, "comm/checkVersion.do");
    }
}
