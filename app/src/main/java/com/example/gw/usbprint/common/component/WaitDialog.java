package com.example.gw.usbprint.common.component;

import android.app.Dialog;
import android.content.Context;

import com.example.gw.usbprint.R;


/**
 * 自定义加载对话框
 * Created by gw on 15/9/21.
 */
public class WaitDialog extends Dialog {


    public WaitDialog(Context context, int themeResId) {
        super(context, themeResId);
        /**设置对话框背景透明*/
        setContentView(R.layout.dialog_transparent);
        setCanceledOnTouchOutside(false);
    }
}