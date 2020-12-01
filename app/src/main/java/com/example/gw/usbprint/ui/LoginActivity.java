package com.example.gw.usbprint.ui;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.gw.usbprint.R;
import com.example.gw.usbprint.common.base.BaseActivity;
import com.example.gw.usbprint.common.base.BaseRequestor;
import com.example.gw.usbprint.common.db.DBManager;
import com.example.gw.usbprint.common.db.FrmConfigKeys;
import com.example.gw.usbprint.common.http.CommnAction;
import com.example.gw.usbprint.common.utils.ToastUtil;
import com.example.gw.usbprint.task.Task_Login;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zkteco.android.biometric.core.device.ParameterHelper;
import com.zkteco.android.biometric.core.device.TransportType;
import com.zkteco.android.biometric.core.utils.LogHelper;
import com.zkteco.android.biometric.module.idcard.IDCardReader;
import com.zkteco.android.biometric.module.idcard.IDCardReaderExceptionListener;
import com.zkteco.android.biometric.module.idcard.IDCardReaderFactory;
import com.zkteco.android.biometric.module.idcard.exception.IDCardReaderException;
import com.zkteco.android.biometric.module.idcard.meta.IDCardInfo;
import com.zkteco.android.biometric.module.idcard.meta.IDPRPCardInfo;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import butterknife.InjectView;

/**
 * Created by gw on 2018/8/29.
 */

public class LoginActivity extends BaseActivity implements View.OnClickListener {
    private LinearLayout l1,l2;
    @InjectView(R.id.user_name)
    EditText etName;
    @InjectView(R.id.password)
    EditText etPsw;
    private EditText idCard;
    TextView tvLogin;
//    @InjectView(R.id.tv_register)
//    TextView tvRegister;
    TextView tvCard;
    private boolean currentFlag = false;
    private String loginName, password, authUserType,id;
    private static final int VID = 1024;    //IDR VID
    private static final int PID = 50010;     //IDR PID
    private IDCardReader idCardReader = null;
    private boolean bopen = false;
    private boolean bStoped = false;
    private int mReadCount = 0;
    private CountDownLatch countdownLatch = null;
    private int flag;
    private Context mContext = null;
    private UsbManager musbManager = null;
    private final String ACTION_USB_PERMISSION = "com.example.gw.usbprint.USB_PERMISSION";
    private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        OpenDevice();
                    } else {
                        ToastUtil.showShort("USB未授权");
                        //mTxtReport.setText("USB未授权");
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.activity_login);
        getNbBar().hide();
        initUI();
        initCard();
        l1 = findViewById(R.id.l1);
        l2 = findViewById(R.id.l2);
        flag = getIntent().getIntExtra("flag",0);
        if (flag==0){
            //普通密码

            l2.setVisibility(View.GONE);
        }else{
            //身份证
            l1.setVisibility(View.GONE);
            idCard = findViewById(R.id.id_card);
//            tvCard = findViewById(R.id.tv_card);
//            tvCard.setOnClickListener(this);
            RequestDevicePermission();
        }
        tvLogin = findViewById(R.id.tv_login);
        tvLogin.setOnClickListener(this);
    }

    private void initCard() {
        mContext = this.getApplicationContext();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        mContext.registerReceiver(mUsbReceiver, filter);
    }

    private void initUI() {
        authUserType = "0";
//        tvRegister.setOnClickListener(this);
//        tvLogin.setOnClickListener(this);
//        tvCard.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
//        if (v == tvRegister) {
//            Intent intent = new Intent(this, TBSWebViewActivity.class);
//            intent.putExtra("flag", 1);
//            startActivity(intent);
//        } else
        if (v == tvLogin) {
            if (flag==0){
                loginName = etName.getText().toString().trim();
                password = etPsw.getText().toString().trim();
                if (TextUtils.isEmpty(loginName)) {
                    ToastUtil.showShort("请输入用户名");
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    ToastUtil.showShort("请输入密码");
                    return;
                }
            }else{
                id = idCard.getText().toString().trim();
                if (TextUtils.isEmpty(id)) {
                    ToastUtil.showShort("身份证未识别");
                    return;
                }
            }
            login();
        }
//        else if (v == tvCard) {
//            if (!currentFlag) {
//                if (bopen) {
//                    ToastUtil.showShort("设备已连接");
//                    return;
//                }
//                tvCard.setText("关闭识别");
//                currentFlag = true;
//                RequestDevicePermission();
//            } else {
//                if (!bopen) {
//                    return;
//                }
//                tvCard.setText("开始识别");
//                currentFlag = false;
//                CloseDevice();
//                ToastUtil.showShort("设备断开连接");
//            }
//
//        }
    }

    private void RequestDevicePermission() {
        musbManager = (UsbManager) this.getSystemService(Context.USB_SERVICE);
        for (UsbDevice device : musbManager.getDeviceList().values()) {
            if (device.getVendorId() == VID && device.getProductId() == PID) {
                Intent intent = new Intent(ACTION_USB_PERMISSION);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
                musbManager.requestPermission(device, pendingIntent);
            }
        }
    }

    private void login() {
        Task_Login task = new Task_Login();
        if (flag==0){
            task.loginName = loginName;
            task.password = password;
            task.loginType = 1;
        }else{
            task.loginName=id;
            task.password = " ";
            task.loginType = 2;
        }

//        task.authUserType = authUserType;
        task.refreshHandler = new BaseRequestor.RefreshHandler() {
            @Override
            public void refresh(Object obj) {
                if (CommnAction.CheckY(obj, getActivity())) {
                    JsonObject data = new JsonParser().parse(obj.toString()).getAsJsonObject();
                    String token = data.get("data").getAsJsonObject().get("token").getAsString();
                    DBManager.setOtherConfig(FrmConfigKeys.token, token);
                    DBManager.setOtherConfig(FrmConfigKeys.loginResult, obj.toString());
                    CloseDevice();
                    EventBus.getDefault().post(new SuccessEvent());
                    Intent mintent = new Intent(getActivity(), TBSWebViewActivity.class);
                    startActivity(mintent);
                    finish();
                }
            }
        };
        task.start();
    }

    public void OpenDevice() {
        if (bopen) {
            ToastUtil.showShort("设备已连接");
            return;
        }
        try {
            startIDCardReader();
            IDCardReaderExceptionListener listener = new IDCardReaderExceptionListener() {
                @Override
                public void OnException() {
                    //出现异常，关闭设备
                    CloseDevice();
                    //当前线程为工作线程，若需操作界面，请在UI线程处理
                    runOnUiThread(new Runnable() {
                        public void run() {
                            ToastUtil.showShort("设备发生异常，断开连接！");
                        }
                    });
                }
            };
            idCardReader.open(0);
            idCardReader.setIdCardReaderExceptionListener(listener);
            bStoped = false;
            mReadCount = 0;
            writeLogToFile("连接设备成功");
            ToastUtil.showShort("连接成功");
            bopen = true;
            countdownLatch = new CountDownLatch(1);
            new Thread(new Runnable() {
                public void run() {
                    while (!bStoped) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        boolean ret = false;
                        final long nTickstart = System.currentTimeMillis();
                        try {
                            idCardReader.findCard(0);
                            idCardReader.selectCard(0);
                        } catch (IDCardReaderException e) {
                            //continue;
                        }
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        int retType = 0;
                        try {
                            retType = idCardReader.readCardEx(0, 0);
                        } catch (IDCardReaderException e) {
                            writeLogToFile("读卡失败，错误信息：" + e.getMessage());
                        }
                        if (retType == 1 || retType == 2 || retType == 3) {
                            final long nTickUsed = (System.currentTimeMillis() - nTickstart);
                            final int final_retType = retType;
                            writeLogToFile("读卡成功：" + (++mReadCount) + "次" + "，耗时：" + nTickUsed + "毫秒");
                            writeLogToFile("type: " + final_retType);
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    if (final_retType == 1) {
                                        final IDCardInfo idCardInfo = idCardReader.getLastIDCardInfo();
                                        //姓名adb
                                        String strName = idCardInfo.getName();
                                        //民族
                                        String strNation = idCardInfo.getNation();
                                        //出生日期
                                        String strBorn = idCardInfo.getBirth();
                                        //住址
                                        String strAddr = idCardInfo.getAddress();
                                        //身份证号
                                        String strID = idCardInfo.getId();
                                        //有效期限
                                        String strEffext = idCardInfo.getValidityTime();
                                        //签发机关
                                        String strIssueAt = idCardInfo.getDepart();
                                        writeLogToFile("strID: " + strID);
//                                        if (idCardInfo.getPhotolength() > 0) {
//                                            byte[] buf = new byte[WLTService.imgLength];
//                                            if (1 == WLTService.wlt2Bmp(idCardInfo.getPhoto(), buf)) {
//                                                imageView.setImageBitmap(IDPhotoHelper.Bgr2Bitmap(buf));
//                                            }
//                                        }
                                        idCard.setText(strID);
                                      //  etName.setText(strID);
                                    } else if (final_retType == 2) {
                                        final IDPRPCardInfo idprpCardInfo = idCardReader.getLastPRPIDCardInfo();
                                        //中文名
                                        String strCnName = idprpCardInfo.getCnName();
                                        //英文名
                                        String strEnName = idprpCardInfo.getEnName();
                                        //国家/国家地区代码
                                        String strCountry = idprpCardInfo.getCountry() + "/" + idprpCardInfo.getCountryCode();//国家/国家地区代码
                                        //出生日期
                                        String strBorn = idprpCardInfo.getBirth();
                                        //身份证号
                                        String strID = idprpCardInfo.getId();
                                        //有效期限
                                        String strEffext = idprpCardInfo.getValidityTime();
                                        //签发机关
                                        String strIssueAt = "公安部";
//                                        if (idprpCardInfo.getPhotolength() > 0) {
//                                            byte[] buf = new byte[WLTService.imgLength];
//                                            if (1 == WLTService.wlt2Bmp(idprpCardInfo.getPhoto(), buf)) {
//                                                imageView.setImageBitmap(IDPhotoHelper.Bgr2Bitmap(buf));
//                                            }
//                                        }
                                        idCard.setText(strID);
                                       // etName.setText(strID);
                                    } else {
                                        final IDCardInfo idCardInfo = idCardReader.getLastIDCardInfo();
                                        //姓名
                                        String strName = idCardInfo.getName();
                                        //民族,港澳台不支持该项
                                        String strNation = "";
                                        //出生日期
                                        String strBorn = idCardInfo.getBirth();
                                        //住址
                                        String strAddr = idCardInfo.getAddress();
                                        //身份证号
                                        String strID = idCardInfo.getId();
                                        //有效期限
                                        String strEffext = idCardInfo.getValidityTime();
                                        //签发机关
                                        String strIssueAt = idCardInfo.getDepart();
                                        //通行证号
                                        String strPassNum = idCardInfo.getPassNum();
                                        //签证次数
                                        int visaTimes = idCardInfo.getVisaTimes();
//                                        if (idCardInfo.getPhotolength() > 0) {
//                                            byte[] buf = new byte[WLTService.imgLength];
//                                            if (1 == WLTService.wlt2Bmp(idCardInfo.getPhoto(), buf)) {
//                                                imageView.setImageBitmap(IDPhotoHelper.Bgr2Bitmap(buf));
//                                            }
//                                        }
                                        idCard.setText(strID);
                                       // etName.setText(strID);
                                    }
                                }
                            });
                        }
                    }
                    countdownLatch.countDown();
                }
            }).start();
        } catch (IDCardReaderException e) {
            writeLogToFile("连接设备失败");
            ToastUtil.showShort("开始读卡失败，错误码：" + e.getErrorCode() + "\n错误信息：" + e.getMessage() + "\n内部代码=" + e.getInternalErrorCode());
        }

    }

    private void startIDCardReader() {
        if (null != idCardReader) {
            IDCardReaderFactory.destroy(idCardReader);
            idCardReader = null;
        }
        // Define output log level
        LogHelper.setLevel(Log.VERBOSE);
        // Start fingerprint sensor
        Map idrparams = new HashMap();
        idrparams.put(ParameterHelper.PARAM_KEY_VID, VID);
        idrparams.put(ParameterHelper.PARAM_KEY_PID, PID);
        idCardReader = IDCardReaderFactory.createIDCardReader(this, TransportType.USB, idrparams);
    }

    private void CloseDevice() {
        if (!bopen) {
            return;
        }
        bStoped = true;
        mReadCount = 0;
        if (null != countdownLatch) {
            try {
                countdownLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            idCardReader.close(0);
        } catch (IDCardReaderException e) {
            e.printStackTrace();
        }
        bopen = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CloseDevice();
        IDCardReaderFactory.destroy(idCardReader);
        mContext.unregisterReceiver(mUsbReceiver);
    }



    public static void writeLogToFile(String log) {
        try {
            File dirFile = new File("/sdcard/zkteco/");  //目录转化成文件夹
            if (!dirFile.exists()) {              //如果不存在，那就建立这个文件夹
                dirFile.mkdirs();
            }
            String path = "/sdcard/zkteco/idrlog.txt";
            File file = new File(path);
            if (!file.exists()) {
                File dir = new File(file.getParent());
                dir.mkdirs();
                file.createNewFile();
            }
            FileOutputStream outStream = new FileOutputStream(file, true);
            log += "\r\n";
            outStream.write(log.getBytes());
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
