package com.example.gw.usbprint.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.gw.usbprint.R;
import com.example.gw.usbprint.common.base.BaseActivity;
import com.example.gw.usbprint.common.db.DBManager;
import com.example.gw.usbprint.common.db.FrmConfigKeys;

import java.util.Timer;
import java.util.TimerTask;


/**
 * 初始化界面
 */
public class InitActivity extends BaseActivity {
    Timer mtimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getNbBar().hide();
        setLayout(R.layout.init);
        if (Build.VERSION.SDK_INT < 17) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("当前系统不支持比特币要求最低版本");
            builder.setTitle("兼容性提示");
            builder.setPositiveButton("退出", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            builder.create().show();
            return;
        }



        mtimer = new Timer();
        mtimer.schedule(new TimerTask() {
            @Override
            public void run() {
//                Intent mintent = new Intent(getActivity(), TBSWebViewActivity.class);
//                startActivity(mintent);
                String token = DBManager.getOtherConfig(FrmConfigKeys.token);
                if (TextUtils.isEmpty(token)) {

                    // Intent mintent = new Intent(getActivity(), LoginActivity.class);
                        Intent mintent = new Intent(getActivity(), InitLogin.class);
                    startActivity(mintent);
                } else {
                    Intent mintent = new Intent(getActivity(), TBSWebViewActivity.class);
                    startActivity(mintent);
                }
                finish();
            }
        }, 3000);
    }
}
