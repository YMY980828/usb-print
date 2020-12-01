package com.example.gw.usbprint.common.base;

import com.example.gw.usbprint.BuildConfig;

/**
 * Created by gw on 15/8/18.
 */
public class BaseInfo {

    public static String getJsonURL() {
        //  return "http://jshgz.snzfnm.com/jscertificate/webapp/";
    //  return "http://sdhgz.snzfnm.com/sdcertificate/webapp/";
        //    return "http://192.168.1.106:8084/webapp_war_exploded/webapp/";
           return BuildConfig.BASE_SERVER_URL;
     //return "http://njhgz.snzfnm.com/njcertificate/webapp/";
//        return "http://121.43.198.37:8080/testwebapp/desk/";//测试服务器
//        return "http://192.168.8.170:8080/webapp/desk/";//测试服务器
    }

}
