package com.example.gw.usbprint.task;

import com.example.gw.usbprint.common.base.BaseRequestor;
import com.example.gw.usbprint.common.db.DBManager;
import com.example.gw.usbprint.common.db.FrmConfigKeys;
import com.example.gw.usbprint.common.http.CommnAction;

import okhttp3.FormBody;
import okhttp3.RequestBody;

/**
 * Created by gw on 2020/2/27.
 */

public class Task_SavePrintRecord extends BaseRequestor {
    public String certificateRecordId;
    public String printCount;
    public String macAddr;
    public String deviceName;

    @Override
    public Object execute() {
        RequestBody body = new FormBody.Builder()
                .add("certificateRecordId", certificateRecordId)
                .add("printCount", printCount)
                .add("macAddr", DBManager.getOtherConfig(FrmConfigKeys.macAddr))
                .add("deviceName", DBManager.getOtherConfig(FrmConfigKeys.deviceName))
                .build();
        return CommnAction.request(body, "print/savePrintRecord.do");
    }
}
