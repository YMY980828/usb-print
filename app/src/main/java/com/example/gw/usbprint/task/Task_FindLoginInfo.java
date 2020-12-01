package com.example.gw.usbprint.task;

import com.example.gw.usbprint.common.base.BaseRequestor;
import com.example.gw.usbprint.common.http.CommnAction;

import okhttp3.FormBody;
import okhttp3.RequestBody;

/**
 * Created by gw on 2018/10/16.
 */

public class Task_FindLoginInfo extends BaseRequestor {
    public String type;
    public String orgName;
    public String legalPersonName;
    public String legalPersonMobile;
    public String newPassword;

    @Override
    public Object execute() {
        RequestBody body = new FormBody.Builder()
                .add("type", type)
                .add("orgName", orgName)
                .add("legalPersonName", legalPersonName)
                .add("legalPersonMobile", legalPersonMobile)
                .add("newPassword", newPassword)
                .build();
        return CommnAction.request(body, "user/findLoginInfo.do");
    }
}
