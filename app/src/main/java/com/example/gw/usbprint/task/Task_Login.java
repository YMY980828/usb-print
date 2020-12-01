package com.example.gw.usbprint.task;

import com.example.gw.usbprint.common.base.BaseRequestor;
import com.example.gw.usbprint.common.http.CommnAction;

import okhttp3.FormBody;
import okhttp3.RequestBody;

/**
 * Created by gw on 2018/8/30.
 */

public class Task_Login extends BaseRequestor {
    public String loginName;
    public String password;
    public Integer loginType;
//    public String authUserType;

    @Override
    public Object execute() {
        RequestBody body = new FormBody.Builder()
                .add("loginName", loginName)
                .add("password", password)
                .add("loginType", String.valueOf(loginType))
                .build();
            return CommnAction.request(body, "auth/getToken.do");
    }
}