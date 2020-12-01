package com.example.gw.usbprint.common;

import android.app.Activity;

import com.example.gw.usbprint.R;
import com.example.gw.usbprint.common.base.BaseNavigationBar;


/**
 * Created by guwei on 17/5/17.
 * 主题配置类
 */
public class ThemeConfig {

    public static void setDefaultViewConfig(BaseNavigationBar nbBar, Activity activity) {
        nbBar.nbBack.setImageResource(R.drawable.arrow_left);
        nbBar.setNBBackground(R.color.main_color);
        nbBar.nbTitle.setTextColor(activity.getResources().getColor(R.color.white));
    }


}
