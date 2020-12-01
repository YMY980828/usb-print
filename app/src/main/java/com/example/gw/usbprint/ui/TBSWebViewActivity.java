package com.example.gw.usbprint.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.widget.EditText;
import android.widget.Toast;

import com.caysn.autoreplyprint.AutoReplyPrint;
import com.example.gw.usbprint.BuildConfig;
import com.example.gw.usbprint.R;
import com.example.gw.usbprint.common.FileConfig;
import com.example.gw.usbprint.common.action.UpdateAction;
import com.example.gw.usbprint.common.base.BaseActivity;
import com.example.gw.usbprint.common.base.BaseApplication;
import com.example.gw.usbprint.common.base.BaseRequestor;
import com.example.gw.usbprint.common.db.DBManager;
import com.example.gw.usbprint.common.db.FrmConfigKeys;
import com.example.gw.usbprint.common.http.CommnAction;
import com.example.gw.usbprint.common.utils.ToastUtil;
import com.example.gw.usbprint.task.Task_GetFaceInfo;
import com.example.gw.usbprint.task.Task_SavePrintRecord;
import com.example.gw.usbprint.webview.X5WebView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sun.jna.Pointer;
import com.tencent.smtt.export.external.interfaces.SslErrorHandler;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import ZPL.PublicFunction;
import ZPL.ZPLPrinterHelper;
import butterknife.InjectView;
import zxing.activity.CaptureActivity;

/**
 * Created by gw on 2018/8/28.
 */
public class TBSWebViewActivity extends BaseActivity {

    /**
     * 人脸识别参数
     */
    private Long faceUserId;
    private Integer userType;
    private String faceName;
    private String faceIdCard;

    @InjectView(R.id.myWebView)
    X5WebView myWebView;
    //1 注册  0 主页面
    private int flag;
    private String zsUrl;
    //传的字段
    private String type, title1, title2, name, user, phone, code, company, qrCode, num, weight, time, address, certificateRecordId, reverse, shortPrintCode;
    private static final String[] All_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int All_PERMISSIONS_CODE = 1;
    List<String> mPermissionList = new ArrayList<>();
    //usb打印
    private PublicFunction PFun = null;
    private Context thisCon = null;
    private UsbManager mUsbManager = null;
    private UsbDevice device = null;
    private static final String ACTION_USB_PERMISSION = "com.HPRTSDKSample";
    private PendingIntent mPermissionIntent = null;
    private ZPLPrinterHelper zplPrinterHelper;

    @SuppressLint("JavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.tbs_webview_activity);
        getNbBar().hide();
        flag = getIntent().getIntExtra("flag", 0);

        if (flag == 0) {
            /**
             * 大数据签名
             * 4209224294
             * 合格证签名
             * 123456
             */
            zsUrl = BuildConfig.Main_Url;
            //zsUrl = "http://njhgzapp.snzfnm.com/";
            // zsUrl = "http://sdhgzapp.snzfnm.com/";
            //       zsUrl = "http://sdhgzapp.snzfnm.com/";
            //     zsUrl = "http://sxhgzapp.snzfnm.com/";
            // zsUrl = "http://jshgzapp.snzfnm.com/";
            //  zsUrl = "http://js-big-screen-datav.snzfnm.com/";
        } else {
            zsUrl = BuildConfig.Register_Url;
            // zsUrl = "http://jshgzapp.snzfnm.com/#/register";
            //  zsUrl = "http://sdhgzapp.snzfnm.com/#/register";
            //zsUrl = "http://sdhgzapp.snzfnm.com/#/register";
            //  zsUrl = "http://njhgzapp.snzfnm.com/#/register";
        }
        //更新
        UpdateAction.updateAction(this, false);
        initUsb();
        /**1 大屏
         * 2 pos手持
         * 3 手机app
         * 4 农残仪
         *
         */
        myWebView.loadUrl(zsUrl + "?token=" + DBManager.getOtherConfig(FrmConfigKeys.token) + "&uuId=" + UUID.randomUUID().toString() + "&type=1");
        myWebView.addJavascriptInterface(new method(), "method");
        myWebView.setWebChromeClient(new WebChromeClient());
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //加载完成
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                //加载开始
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                //加载失败
            }

            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // 　　//cancel(); 默认的处理方式，WebView变成空白页
                handler.proceed(); // 接受证书
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermission();
        }
        WebSettings webSettings = myWebView.getSettings();
        //自动播放音乐
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            webSettings.setMediaPlaybackRequiresUserGesture(false);
        }

        /**
         * 获取当前信息 用于人脸注册传参
         */

        //     getFaceinfo();

        //注册打印回调接口 白色打印机
        registerCall();

    }

    private Pointer h = Pointer.NULL;

    private void registerCall() {
        AutoReplyPrint.INSTANCE.CP_Port_AddOnPortOpenedEvent(opened_callback, Pointer.NULL);
        AutoReplyPrint.INSTANCE.CP_Port_AddOnPortOpenFailedEvent(openfailed_callback, Pointer.NULL);

    }

    private void getFaceinfo() {
        showLoading();
        Task_GetFaceInfo task = new Task_GetFaceInfo();
        task.refreshHandler = new BaseRequestor.RefreshHandler() {
            @Override
            public void refresh(Object obj) {
                hideLoading();
                if (CommnAction.CheckY(obj, getActivity())) {

                    JsonObject data = new JsonParser().parse(obj.toString()).getAsJsonObject();
                    faceName = data.get("data").getAsJsonObject().get("faceName").getAsString();
                    faceUserId = data.get("data").getAsJsonObject().get("faceUserId").getAsLong();
                    faceIdCard = data.get("data").getAsJsonObject().get("faceIdCard").getAsString();
                    userType = data.get("data").getAsJsonObject().get("userType").getAsInt();
                    Intent intent = new Intent(TBSWebViewActivity.this, RegisterAndRecognizeActivity.class);
                    //true 注册   false 识别
                    intent.putExtra("flag", true);
                    intent.putExtra("faceUserId", faceUserId);
                    intent.putExtra("userType", userType);
                    intent.putExtra("faceName", faceName);
                    intent.putExtra("faceIdCard", faceIdCard);
                    startActivity(intent);

                }
            }
        };
        task.start();
    }

    private void initUsb() {
        thisCon = this.getApplicationContext();
        mPermissionIntent = PendingIntent.getBroadcast(thisCon, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        thisCon.registerReceiver(mUsbReceiver, filter);
        PFun = new PublicFunction(thisCon);
        InitSetting();
        zplPrinterHelper = ZPLPrinterHelper.getZPL(thisCon);
    }

    private void InitSetting() {
        String SettingValue = "";
        SettingValue = PFun.ReadSharedPreferencesData("Codepage");
        if (SettingValue.equals(""))
            PFun.WriteSharedPreferencesData("Codepage", "0,PC437(USA:Standard Europe)");

        SettingValue = PFun.ReadSharedPreferencesData("Cut");
        if (SettingValue.equals(""))
            PFun.WriteSharedPreferencesData("Cut", "0");

        SettingValue = PFun.ReadSharedPreferencesData("Cashdrawer");
        if (SettingValue.equals(""))
            PFun.WriteSharedPreferencesData("Cashdrawer", "0");

        SettingValue = PFun.ReadSharedPreferencesData("Buzzer");
        if (SettingValue.equals(""))
            PFun.WriteSharedPreferencesData("Buzzer", "0");

        SettingValue = PFun.ReadSharedPreferencesData("Feeds");
        if (SettingValue.equals(""))
            PFun.WriteSharedPreferencesData("Feeds", "0");
    }

    //申请权限
    private void requestPermission() {
        // 当API大于 23 时，才动态申请权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mPermissionList.clear();//清空没有通过的权限
            //逐个判断你要的权限是否已经通过
            for (int i = 0; i < All_PERMISSIONS.length; i++) {
                if (ContextCompat.checkSelfPermission(this, All_PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                    mPermissionList.add(All_PERMISSIONS[i]);//添加还未授予的权限
                }
            }
            //申请权限
            if (mPermissionList.size() > 0) {
                //有权限没有通过，需要申请
                ActivityCompat.requestPermissions(this, All_PERMISSIONS, All_PERMISSIONS_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case All_PERMISSIONS_CODE:
                //权限请求失败
                if (grantResults.length == All_PERMISSIONS.length) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            //弹出对话框引导用户去设置
                            showDialog();
                            ToastUtil.showShort("请求权限被拒绝");
                            break;
                        }
                    }
                    //初始化文件夹创建
                    FileConfig.initFolders();
                }
                break;
        }
    }

    //弹出提示框
    private void showDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("合格证系统需要相机和读写权限，是否去设置？")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        goToAppSetting();
                    }
                })
                .setNegativeButton("否", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false)
                .show();
    }

    // 跳转到当前应用的设置界面
    private void goToAppSetting() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    public class method {

        //白色打印机打印
        public void pointerPrint(String json ){
            JsonObject data = new JsonParser().parse(json).getAsJsonObject();
            type = data.get("types").getAsString();
            title1 = data.get("title1").getAsString();
            title2 = data.get("title2").getAsString();
            name = data.get("productName").getAsString();
            user = data.get("person").getAsString();
            phone = data.get("phone").getAsString();
            code = data.get("shortCode").getAsString();
            company = data.get("orgName").getAsString();
            qrCode = data.get("codeUrl").getAsString();
            num = data.get("printNum").getAsString();
            weight = data.get("weightText").getAsString();
            time = data.get("time").getAsString();
            address = data.get("place").getAsString();
            if (address.length() > 10) {
                address = address.substring(0, 9) + "...";
            }
            certificateRecordId = data.get("certificateRecordId").getAsString();
            reverse = data.get("reverse").getAsString();
            EnumPort();


        }

        //人脸识别
        @JavascriptInterface
        public void FaceRegister() {
            getFaceinfo();

        }

        @JavascriptInterface
        public void back() {
            finish();
        }

        @JavascriptInterface
        public void choosePic() {
            Intent intent = new Intent(getActivity(), PhotoActivity.class);
            startActivityForResult(intent, 1001);
        }

        @JavascriptInterface
        public void login() {
            DBManager.setOtherConfig(FrmConfigKeys.token, "");
            DBManager.setOtherConfig(FrmConfigKeys.loginResult, "");
            Intent intent = new Intent(getActivity(), InitLogin.class);
            startActivity(intent);
            finish();
        }

        @JavascriptInterface
        public String getToken() {
            return DBManager.getOtherConfig(FrmConfigKeys.loginResult);
        }

        @JavascriptInterface
        public void quickMark() {
            Intent intent = new Intent(TBSWebViewActivity.this, CaptureActivity.class);//黄色是第三方类库里面的类
            startActivityForResult(intent, 1002);
        }

        @JavascriptInterface
        public String getDBManager(String key) {
            return DBManager.getOtherConfig(key);
        }

        @JavascriptInterface
        public void setDBManager(String key, String value) {
            DBManager.setOtherConfig(key, value);
        }

        @JavascriptInterface
        public void print(String json) {
            JsonObject data = new JsonParser().parse(json).getAsJsonObject();
            type = data.get("types").getAsString();
            title1 = data.get("title1").getAsString();
            title2 = data.get("title2").getAsString();
            name = data.get("productName").getAsString();
            user = data.get("person").getAsString();
            phone = data.get("phone").getAsString();
            code = data.get("shortCode").getAsString();
            company = data.get("orgName").getAsString();
            qrCode = data.get("codeUrl").getAsString();
            num = data.get("printNum").getAsString();
            weight = data.get("weightText").getAsString();
            time = data.get("time").getAsString();
            address = data.get("place").getAsString();
            if (address.length() > 10) {
                address = address.substring(0, 9) + "...";
            }
            certificateRecordId = data.get("certificateRecordId").getAsString();
            reverse = data.get("reverse").getAsString();

            if (true){
                EnumPort();
                return;
            }

            //USB not need call "iniPort"
//            if (zplPrinterHelper.IsOpened()) {
//                savePrintRecord();
//            } else {
//                connectUsb();
//            }
        }

        @JavascriptInterface
        public void switchToCertificate() {
            String APP_PACKAGE_NAME = "com.example.gw.jsprint";
            if (isAppInstalled(TBSWebViewActivity.this, APP_PACKAGE_NAME)) {
                //如果有根据包名跳转
                TBSWebViewActivity.this.startActivity(TBSWebViewActivity.this.getPackageManager().getLaunchIntentForPackage(APP_PACKAGE_NAME));
            }
        }
    }

    //这里是判断APP中是否有相应APP的方法
    private boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void connectUsb() {
        mUsbManager = (UsbManager) thisCon.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        boolean HavePrinter = false;
        while (deviceIterator.hasNext()) {
            device = deviceIterator.next();
            int count = device.getInterfaceCount();
            for (int i = 0; i < count; i++) {
                UsbInterface intf = device.getInterface(i);
                if (intf.getInterfaceClass() == 7) {
                    HavePrinter = true;
                    mUsbManager.requestPermission(device, mPermissionIntent);
                }
            }
        }
        if (!HavePrinter)
            ToastUtil.showShort("请连接USB打印机");
    }

    private void savePrintRecord() {
        Task_SavePrintRecord task = new Task_SavePrintRecord();
        task.certificateRecordId = certificateRecordId;
        task.printCount = num;
        task.refreshHandler = new BaseRequestor.RefreshHandler() {
            @Override
            public void refresh(Object obj) {
                LoginActivity.writeLogToFile(obj.toString());

                if (CommnAction.CheckY(obj, getActivity())) {
                    JsonObject data = new JsonParser().parse(obj.toString()).getAsJsonObject();
                    String printCode = data.get("data").getAsJsonObject().get("printCode").getAsString();
                    shortPrintCode = data.get("data").getAsJsonObject().get("shortPrintCode").getAsString();
                    // qrCode = "http://jshgzapp.snzfnm.com/#/model?c=" + printCode + "&t=" + System.currentTimeMillis();
                    //qrCode = "http://sdhgzapp.snzfnm.com/#/model?c=" + printCode + "&t=" + System.currentTimeMillis();
                    // qrCode = "http://sxhgzapp.snzfnm.com/#/model?c=" + printCode + "&t=" + System.currentTimeMillis();
                    //   qrCode = "http://njhgzapp.snzfnm.com/#/model?c=" + printCode + "&t=" + System.currentTimeMillis();
                    qrCode = BuildConfig.Main_Url + "#/model?c=" + printCode + "&t=" + System.currentTimeMillis();

                    print();
                }
            }
        };
        task.start();
    }

    private void print() {
        try {
            zplPrinterHelper.start();
            //  zplPrinterHelper.WriteData(("^PQ" + num + "," + 0 + "," + 0 + "," + "N" + "\r\n").getBytes(zplPrinterHelper.LanguageEncode));
            zplPrinterHelper.printData("^CI14\r\n");
            if ("1".equals(reverse)) {
                zplPrinterHelper.WriteData(("^PON" + "\r\n").getBytes(zplPrinterHelper.LanguageEncode));
            } else {
                zplPrinterHelper.WriteData(("^POI" + "\r\n").getBytes(zplPrinterHelper.LanguageEncode));
            }

            zplPrinterHelper.WriteData(("^MMC" + "," + "Y" + "\r\n").getBytes(zplPrinterHelper.LanguageEncode));
            /**
             * 80->95
             */
            zplPrinterHelper.WriteData(("^FO" + 165 + "," + 85 + "^A" + "@" + "N" + "," + 25 + "," + 25 + "^FD" + "食用农产品名称：" + name + "^FS\r\n").getBytes(zplPrinterHelper.LanguageEncode));
            zplPrinterHelper.WriteData(("^FO" + 165 + "," + 115 + "^A" + "@" + "N" + "," + 25 + "," + 25 + "^FD" + "数量(重量)：" + weight + "^FS\r\n").getBytes(zplPrinterHelper.LanguageEncode));
            zplPrinterHelper.WriteData(("^FO" + 165 + "," + 145 + "^A" + "@" + "N" + "," + 25 + "," + 25 + "^FD" + "产地：" + address + "^FS\r\n").getBytes(zplPrinterHelper.LanguageEncode));
            zplPrinterHelper.WriteData(("^FO" + 165 + "," + 175 + "^A" + "@" + "N" + "," + 25 + "," + 25 + "^FD" + "联系方式：" + phone + "^FS\r\n").getBytes(zplPrinterHelper.LanguageEncode));
            zplPrinterHelper.WriteData(("^FO" + 165 + "," + 205 + "^A" + "@" + "N" + "," + 25 + "," + 25 + "^FD" + "开具日期：" + time + "^FS\r\n").getBytes(zplPrinterHelper.LanguageEncode));
            zplPrinterHelper.WriteData(("^FO" + 165 + "," + 235 + "^A" + "@" + "N" + "," + 25 + "," + 25 + "^FD" + "生产者：" + company + "^FS\r\n").getBytes(zplPrinterHelper.LanguageEncode));
            zplPrinterHelper.WriteData(("^FO" + 550 + "," + 85 + "\r\n^BQ" + "N" + "," + "2" + "," + "3" + "\r\n^FDQA," + qrCode + "^FS\r\n").getBytes(zplPrinterHelper.LanguageEncode));
            zplPrinterHelper.WriteData(("^FO" + 165 + "," + 265 + "^A" + "@" + "N" + "," + 25 + "," + 25 + "^FD" + "合格证编号：" + shortPrintCode + "^FS\r\n").getBytes(zplPrinterHelper.LanguageEncode));
            zplPrinterHelper.WriteData(("^PQ" + num + "," + num + "," + 1 + "," + "Y" + "\r\n").getBytes(zplPrinterHelper.LanguageEncode));
            zplPrinterHelper.end();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {
                        device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (zplPrinterHelper.PortOpen(device) != 0) {
                                ToastUtil.showShort("连接失败");
                                return;
                            } else {
                                DBManager.setOtherConfig(FrmConfigKeys.macAddr, device.getProductId() + "");
                                DBManager.setOtherConfig(FrmConfigKeys.deviceName, device.getDeviceName());
                                LoginActivity.writeLogToFile(device.getProductId() + "+++" + device.getDeviceName());
                                savePrintRecord();
                                ToastUtil.showShort("连接成功");
                            }
                        } else {
                            return;
                        }
                    }
                }
                if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null) {
                        zplPrinterHelper.PortClose();
                    }
                }
            } catch (Exception e) {
                Log.e("HPRTSDKSample", (new StringBuilder("Activity_Main --> mUsbReceiver ")).append(e.getMessage()).toString());
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            if (resultCode == RESULT_OK) {
                String filePaths = data.getStringExtra("filePaths");
                myWebView.loadUrl("javascript:getPhotos('" + filePaths + "')");
            }
        } else if (requestCode == 1002) {
            if (resultCode == RESULT_OK) {
                String result = data.getStringExtra("result");//这个绿色的result是在第三方类库里面定义的key
                myWebView.loadUrl(result);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (flag == 0) {
                if (myWebView.canGoBack()) {
                    myWebView.goBack();// 返回前一个页面
                } else {
                    ExitApp();
                }
            } else {
                finish();
            }
        }
        return false;
    }

    private long exitTime = 0;

    public void ExitApp() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            ToastUtil.showShort("再按一次退出程序");
            exitTime = System.currentTimeMillis();
        } else {
            FileConfig.clearUpload(BaseApplication.getAppContext());
            finish();
        }
    }


    @Override
    protected void onDestroy() {
        if (myWebView != null) {
            myWebView.destroy();
            myWebView.clearCache(true);
        }
        //移出注册的回调
        AutoReplyPrint.INSTANCE.CP_Port_RemoveOnPortOpenedEvent(opened_callback);
        AutoReplyPrint.INSTANCE.CP_Port_RemoveOnPortOpenFailedEvent(openfailed_callback);
        super.onDestroy();
    }

    AutoReplyPrint.CP_OnPortOpenedEvent_Callback opened_callback = new AutoReplyPrint.CP_OnPortOpenedEvent_Callback() {
        @Override
        public void CP_OnPortOpenedEvent(Pointer handle, String name, Pointer private_data) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!TBSWebViewActivity.this.isFinishing()){
                        Toast.makeText(TBSWebViewActivity.this, "Open Success", Toast.LENGTH_SHORT).show();
                    }
                  Test_Label_DrawImageFromBitmap(h);

                }
            });
        }
    };
    AutoReplyPrint.CP_OnPortOpenFailedEvent_Callback openfailed_callback = new AutoReplyPrint.CP_OnPortOpenFailedEvent_Callback() {
        @Override
        public void CP_OnPortOpenFailedEvent(Pointer handle, String name, Pointer private_data) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!TBSWebViewActivity.this.isFinishing()) {
                        Toast.makeText(TBSWebViewActivity.this, "Open Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    };

    private void EnumPort(){
         final String[] devicePaths = AutoReplyPrint.CP_Port_EnumUsb_Helper.EnumUsb();
       // final String[] devicePaths = {"vid:0x4b43,pid:0x3830","b","c"};
        if (devicePaths!=null){
            CharSequence[] charSequences = devicePaths;
            AlertDialog.Builder builder= new AlertDialog.Builder(TBSWebViewActivity.this);
            builder.setTitle("选择打印端口")
                    .setItems(charSequences, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            h = AutoReplyPrint.INSTANCE.CP_Port_OpenUsb(devicePaths[which], 1);
                        }
                    }).show();
        }
    }

    void Test_Label_DrawImageFromBitmap(Pointer h) {

        // Bitmap bitmap = TestUtils.getImageFromAssetsFile(ctx, "RasterImage/yellowmen.png");
        if (h!=null){
            Bitmap bitmap = UnKnownPrint();
            if ((bitmap == null) || (bitmap.getWidth() == 0) || (bitmap.getHeight() == 0))
                return;
            try {
                int printwidth = 520;
                int dstw = printwidth;
                int dsth = (int) (dstw * ((double) bitmap.getHeight() / bitmap.getWidth()));
                int orientaion = reverse.equals("1") ?0:2;
                // AutoReplyPrint.INSTANCE.CP_Label_PageBegin(h, 0, 0, dstw, dsth, AutoReplyPrint.CP_Label_Rotation_0);
                AutoReplyPrint.INSTANCE.CP_Label_PageBegin(h, 0, 0, dstw, dsth, orientaion);
                AutoReplyPrint.INSTANCE.CP_Label_DrawBox(h, 0, 0, dstw, dsth, 1, AutoReplyPrint.CP_Label_Color_Black);
                AutoReplyPrint.CP_Label_DrawImageFromData_Helper.DrawImageFromBitmap(h, 0, 0, dstw, dsth, bitmap, AutoReplyPrint.CP_ImageBinarizationMethod_ErrorDiffusion, AutoReplyPrint.CP_ImageCompressionMethod_None);
                AutoReplyPrint.INSTANCE.CP_Label_PagePrint(h, Integer.parseInt(num));
                //再添加
                // AutoReplyPrint.INSTANCE.CP_Pos_FeedAndHalfCutPaper(h);
                Test_Pos_QueryPrintResult(h);
                //再添加
                AutoReplyPrint.INSTANCE.CP_Port_Close(h);
            }catch (Exception e){
                Toast.makeText(TBSWebViewActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();
            }

        }


    }

    void Test_Pos_QueryPrintResult(Pointer h) {
        boolean result = AutoReplyPrint.INSTANCE.CP_Pos_QueryPrintResult(h, 30000);
//        if(!MainActivity28.this.isFinishing()){
//            if (!result)
//                Toast.makeText(MainActivity28.this,"Print Failed",Toast.LENGTH_LONG).show();
//            else
//                Toast.makeText(MainActivity28.this,"Print Success",Toast.LENGTH_LONG).show();
//        }
    }
    private Bitmap UnKnownPrint()  {
        // mBluetoothAdapter
        // BluetoothSocket bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        String title1 = "食用农产品合格证: "+name;
        String title2 = "数量(重量): "+weight;
        String title3 = "产地: "+address;
        String title4 = "联系方式: "+phone;
        String title5 = "开具日期: "+time;
        String title6 = "生产者: "+company;
        String title7 = "合格证编号: "+certificateRecordId;
        Bitmap b = Bitmap.createBitmap(520,300, Bitmap.Config.ARGB_8888);
        Bitmap bitmap =  createQRCodeBitmap("http://www.baidu.com", 180, 180,"UTF-8","H", "1", Color.BLACK, Color.WHITE);
        Paint paint =new Paint();
        Paint paint1 =new Paint();
        paint.setTextSize(18);
        Canvas canvas = new Canvas(b);
//                canvas.rotate(-180);
//                canvas.translate(-280, -300);
        canvas.drawColor(Color.WHITE);
        canvas.drawText(title1,35,30,paint);
        canvas.drawText(title2,35,55,paint);
        canvas.drawText(title3,35,80,paint);
        canvas.drawText(title4,35,105,paint);
        canvas.drawText(title5,35,130,paint);
        canvas.drawText(title6,35,155,paint);
        canvas.drawText(title7,35,180,paint);
        canvas.drawText(" ",35,205,paint);
        if (bitmap!=null){
            canvas.drawBitmap(bitmap,295,20,paint1);
        }

        return b;
    }
    public static Bitmap createQRCodeBitmap(String content, int width, int height,
                                            String character_set, String error_correction_level,
                                            String margin, int color_black, int color_white) {
        // 字符串内容判空
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        // 宽和高>=0
        if (width < 0 || height < 0) {
            return null;
        }
        try {
            /** 1.设置二维码相关配置 */
            Hashtable<EncodeHintType, String> hints = new Hashtable<>();
            // 字符转码格式设置
            if (!TextUtils.isEmpty(character_set)) {
                hints.put(EncodeHintType.CHARACTER_SET, character_set);
            }
            // 容错率设置
            if (!TextUtils.isEmpty(error_correction_level)) {
                hints.put(EncodeHintType.ERROR_CORRECTION, error_correction_level);
            }
            // 空白边距设置
            if (!TextUtils.isEmpty(margin)) {
                hints.put(EncodeHintType.MARGIN, margin);
            }
            /** 2.将配置参数传入到QRCodeWriter的encode方法生成BitMatrix(位矩阵)对象 */
            BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            /** 3.创建像素数组,并根据BitMatrix(位矩阵)对象为数组元素赋颜色值 */
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    //bitMatrix.get(x,y)方法返回true是黑色色块，false是白色色块
                    if (bitMatrix.get(x, y)) {
                        pixels[y * width + x] = color_black;//黑色色块像素设置
                    } else {
                        pixels[y * width + x] = color_white;// 白色色块像素设置
                    }
                }
            }
            /** 4.创建Bitmap对象,根据像素数组设置Bitmap每个像素点的颜色值,并返回Bitmap对象 */
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }


}