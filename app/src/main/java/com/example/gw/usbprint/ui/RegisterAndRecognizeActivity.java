package com.example.gw.usbprint.ui;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;
import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.arcsoft.face.enums.DetectMode;
import com.example.gw.usbprint.BuildConfig;
import com.example.gw.usbprint.R;
import com.example.gw.usbprint.common.base.BaseRequestor;
import com.example.gw.usbprint.common.db.DBManager;
import com.example.gw.usbprint.common.db.FrmConfigKeys;
import com.example.gw.usbprint.common.http.CommnAction;
import com.example.gw.usbprint.faceserver.CompareResult;
import com.example.gw.usbprint.faceserver.FaceServer;
import com.example.gw.usbprint.model.FaceListener;
import com.example.gw.usbprint.task.Task_CompareFace;
import com.example.gw.usbprint.task.Task_UploadFaceInfo;
import com.example.gw.usbprint.util.CameraHelper;
import com.example.gw.usbprint.util.CameraListener;
import com.example.gw.usbprint.util.ConfigUtil;
import com.example.gw.usbprint.util.DrawHelper;
import com.example.gw.usbprint.util.DrawInfo;
import com.example.gw.usbprint.util.FaceHelper;
import com.example.gw.usbprint.util.FacePreviewInfo;
import com.example.gw.usbprint.util.LivenessType;
import com.example.gw.usbprint.util.RecognizeColor;
import com.example.gw.usbprint.util.RequestFeatureStatus;
import com.example.gw.usbprint.util.RequestLivenessStatus;
import com.example.gw.usbprint.widget.FaceRectView;
import com.example.gw.usbprint.widget.FaceSearchResultAdapter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

import io.reactivex.Observer;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class RegisterAndRecognizeActivity extends AppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener,View.OnClickListener{

    private ImageView back;
    private Long faceUserId;
    private Integer userType;
    private String faceName;
    private String faceIdCard;

    private LinearLayout linearLayout;
    /**
     * 用于存储活体检测出错重试次数
     */
    private ConcurrentHashMap<Integer, Integer> livenessErrorRetryMap = new ConcurrentHashMap<>();
    /**
     * 优先打开的摄像头，本界面主要用于单目RGB摄像头设备，因此默认打开前置
     */
    private Integer rgbCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private CameraHelper cameraHelper;
    /**
     * 绘制人脸框的控件
     */
    private FaceRectView faceRectView;
    private Switch switchLivenessDetect;
    /**
     * 出错重试最大次数
     */
    private static final int MAX_RETRY_TIME = 3;
    /**
     * 用于记录人脸特征提取出错重试次数
     */
    private ConcurrentHashMap<Integer, Integer> extractErrorRetryMap = new ConcurrentHashMap<>();
    private static final int MAX_DETECT_NUM = 10;
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private FaceSearchResultAdapter adapter;
    /**
     * 所需的所有权限信息
     */
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE

    };
    /**
     * 相机预览显示的控件，可为SurfaceView或TextureView
     */
    private View previewView;
    int color = Color.parseColor("#1b315e");
    private boolean flag;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            Window window = getWindow();
            window.setStatusBarColor(color);
            window.setNavigationBarColor(color);
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null){
            actionBar.hide();
        }
        setContentView(R.layout.activity_register_and_recognize);
        //保持亮屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        Intent intent = getIntent();
        //false 为识别 true为注册
        flag = intent.getBooleanExtra("flag",false);
        faceUserId = intent.getLongExtra("faceUserId",0);
        faceName = intent.getStringExtra("faceName");
        faceIdCard = intent.getStringExtra("faceIdCard");
        userType = intent.getIntExtra("userType",-1);


        //本地人脸库初始化
        FaceServer.getInstance().init(this);
        initView();

    }


    public void active() {

        int code = FaceEngine.active(RegisterAndRecognizeActivity.this,"BQSDQD3yRNNSyzcubzDYMaJEvHnmQJKQKqwtTPtrgHnT", "5PYsYeSDHQnuKpBZMW67GQQmhYKRNa8GpmGtodbKjJNT");
        if (code== ErrorInfo.MOK||code==ErrorInfo.MERR_ASF_ALREADY_ACTIVATED){
            initEngine();
        }else {
            Toast.makeText(RegisterAndRecognizeActivity.this,"激活引擎失败 错误码: "+code,Toast.LENGTH_LONG).show();
        }
    }
    private void initView() {
        linearLayout =(LinearLayout) findViewById(R.id.wrapper);
        previewView = findViewById(R.id.single_camera_texture_preview);

//        int sw = getWindowManager().getDefaultDisplay().getWidth();
//        int ratio = sw/1080;
//        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(sw,1920*ratio);
//        previewView.setLayoutParams(params);

        //在布局结束后才做初始化操作
        previewView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        faceRectView = (FaceRectView)findViewById(R.id.single_camera_face_rect_view);
        switchLivenessDetect = (Switch) findViewById(R.id.single_camera_switch_liveness_detect);
        if (!flag){
            linearLayout.setVisibility(View.GONE);
            switchLivenessDetect.setChecked(livenessDetect);
            switchLivenessDetect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    livenessDetect = isChecked;
                }
            });
        }else {
            switchLivenessDetect.setVisibility(View.GONE);
        }

        RecyclerView recyclerShowFaceInfo = (RecyclerView) findViewById(R.id.single_camera_recycler_view_person);
        compareResultList = new ArrayList<>();
        adapter = new FaceSearchResultAdapter(compareResultList, this);
        recyclerShowFaceInfo.setAdapter(adapter);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int spanCount = (int) (dm.widthPixels / (getResources().getDisplayMetrics().density * 100 + 0.5f));
        recyclerShowFaceInfo.setLayoutManager(new GridLayoutManager(this, spanCount));
        recyclerShowFaceInfo.setItemAnimator(new DefaultItemAnimator());
        back =(ImageView)findViewById(R.id.back);
        back.setOnClickListener(this);
    }
    /**
     * 在{@link #previewView}第一次布局完成后，去除该监听，并且进行引擎和相机的初始化
     */
    @Override
    public void onGlobalLayout() {
        previewView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        } else {
            active();
            initCamera();
        }
    }
    private DrawHelper drawHelper;
    private Camera.Size previewSize;
    /**
     * 当FR成功，活体未成功时，FR等待活体的时间
     */
    private static final int WAIT_LIVENESS_INTERVAL = 100;
    /**
     * 活体检测的开关
     */
    private boolean livenessDetect = true;
    private CompositeDisposable getFeatureDelayedDisposables = new CompositeDisposable();
    /**
     * 用于存储活体值
     */
    private ConcurrentHashMap<Integer, Integer> livenessMap = new ConcurrentHashMap<>();
    private void initCamera(){

        //获取屏幕尺寸
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        final FaceListener faceListener = new FaceListener(){

            @Override
            public void onFail(Exception e) {
                showToast(e.getMessage());
            }
            /**
             * 请求人脸特征后的回调
             */
            @Override
            public void onFaceFeatureInfoGet(@Nullable final FaceFeature faceFeature, final Integer requestId, final Integer errorCode) {

                if (faceFeature!=null){
                    Integer liveness = livenessMap.get(requestId);
                    //不做活体检测的情况，直接搜索
                    if (!livenessDetect) {
                        searchFace(faceFeature, requestId);
                    } //活体检测通过，搜索特征
                    else if (liveness != null && liveness == LivenessInfo.ALIVE) {
                        searchFace(faceFeature, requestId);
                    }
                    //活体检测未出结果，或者非活体，延迟执行该函数
                    else {
                        if (requestFeatureStatusMap.containsKey(requestId)) {
                            Observable.timer(WAIT_LIVENESS_INTERVAL, TimeUnit.MILLISECONDS)
                                    .subscribe(new Observer<Long>() {
                                        Disposable disposable;

                                        @Override
                                        public void onSubscribe(Disposable d) {
                                            disposable = d;
                                            getFeatureDelayedDisposables.add(disposable);
                                        }

                                        @Override
                                        public void onNext(Long aLong) {
                                            onFaceFeatureInfoGet(faceFeature, requestId, errorCode);
                                        }

                                        @Override
                                        public void onError(Throwable e) {

                                        }

                                        @Override
                                        public void onComplete() {
                                            getFeatureDelayedDisposables.remove(disposable);
                                        }
                                    });
                        }
                    }
                }
                //特征提取失败
                else {
                    if (increaseAndGetValue(extractErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        extractErrorRetryMap.put(requestId, 0);

                        String msg;
                        // 传入的FaceInfo在指定的图像上无法解析人脸，此处使用的是RGB人脸数据，一般是人脸模糊
                        if (errorCode != null && errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL) {
                            msg = getString(R.string.low_confidence_level);
                        } else {
                            msg = "ExtractCode:" + errorCode;
                        }
                        faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, msg));
                        // 在尝试最大次数后，特征提取仍然失败，则认为识别未通过
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                        retryRecognizeDelayed(requestId);
                    } else {
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.TO_RETRY);
                    }
                }

            }
            /**
             * 请求活体检测后的回调
             */
            @Override
            public void onFaceLivenessInfoGet(@Nullable LivenessInfo livenessInfo, Integer requestId, Integer errorCode) {
                if (livenessInfo != null) {
                    int liveness = livenessInfo.getLiveness();
                    livenessMap.put(requestId, liveness);
                    // 非活体，重试
                    if (liveness == LivenessInfo.NOT_ALIVE) {
                        faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_ALIVE"));
                        // 延迟 FAIL_RETRY_INTERVAL 后，将该人脸状态置为UNKNOWN，帧回调处理时会重新进行活体检测
                        retryLivenessDetectDelayed(requestId);
                    }
                } else {
                    if (increaseAndGetValue(livenessErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        livenessErrorRetryMap.put(requestId, 0);
                        String msg;
                        // 传入的FaceInfo在指定的图像上无法解析人脸，此处使用的是RGB人脸数据，一般是人脸模糊
                        if (errorCode != null && errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL) {
                            msg = getString(R.string.low_confidence_level);
                        } else {
                            msg = "ProcessCode:" + errorCode;
                        }
                        faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, msg));
                        retryLivenessDetectDelayed(requestId);
                    } else {
                        livenessMap.put(requestId, LivenessInfo.UNKNOWN);
                    }
                }

            }
        };
        CameraListener cameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                Camera.Size lastPreviewSize = previewSize;
                previewSize = camera.getParameters().getPreviewSize();
                drawHelper = new DrawHelper(previewSize.width, previewSize.height, previewView.getWidth(), previewView.getHeight(), displayOrientation
                        , cameraId, isMirror, false, false);

                // 切换相机的时候可能会导致预览尺寸发生变化
                if (faceHelper == null ||
                        lastPreviewSize == null ||
                        lastPreviewSize.width != previewSize.width || lastPreviewSize.height != previewSize.height) {
                    Integer trackedFaceCount = null;
                    // 记录切换时的人脸序号
                    if (faceHelper != null) {
                        trackedFaceCount = faceHelper.getTrackedFaceCount();
                        faceHelper.release();
                    }
                    faceHelper = new FaceHelper.Builder()
                            .ftEngine(ftEngine)
                            .frEngine(frEngine)
                            .flEngine(flEngine)
                            .frQueueSize(MAX_DETECT_NUM)
                            .flQueueSize(MAX_DETECT_NUM)
                            .previewSize(previewSize)
                            .faceListener(faceListener)
                            .trackedFaceCount(trackedFaceCount == null ? ConfigUtil.getTrackedFaceCount(RegisterAndRecognizeActivity.this.getApplicationContext()) : trackedFaceCount)
                            .build();
                }
            }
            @Override
            public void onPreview(final byte[] nv21, Camera camera) {
                if (faceRectView != null) {
                    faceRectView.clearFaceInfo();
                }
                List<FacePreviewInfo> facePreviewInfoList = faceHelper.onPreviewFrame(nv21);
                if (facePreviewInfoList != null && faceRectView != null && drawHelper != null) {
                    drawPreviewInfo(facePreviewInfoList);
                }
                registerFace(nv21, facePreviewInfoList);
                clearLeftFace(facePreviewInfoList);

                if (facePreviewInfoList != null && facePreviewInfoList.size() > 0 && previewSize != null) {
                    for (int i = 0; i < facePreviewInfoList.size(); i++) {
                        /**
                         * requestFeatureStatusMap 用于记录人脸识别相关状态
                         */
                        Integer status = requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId());
                        /**
                         * 在活体检测开启，在人脸识别状态不为成功或人脸活体状态不为处理中（ANALYZING）且不为处理完成（ALIVE、NOT_ALIVE）时重新进行活体检测
                         */
                        if (livenessDetect && (status == null || status != RequestFeatureStatus.SUCCEED)) {
                            Integer liveness = livenessMap.get(facePreviewInfoList.get(i).getTrackId());
                            if (liveness == null
                                    || (liveness != LivenessInfo.ALIVE && liveness != LivenessInfo.NOT_ALIVE && liveness != RequestLivenessStatus.ANALYZING)) {
                                livenessMap.put(facePreviewInfoList.get(i).getTrackId(), RequestLivenessStatus.ANALYZING);
                                faceHelper.requestFaceLiveness(nv21, facePreviewInfoList.get(i).getFaceInfo(), previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId(), LivenessType.RGB);
                            }
                        }
                        /**
                         * 对于每个人脸，若状态为空或者为失败，则请求特征提取（可根据需要添加其他判断以限制特征提取次数），
                         * 特征提取回传的人脸特征结果在{@link FaceListener#onFaceFeatureInfoGet(FaceFeature, Integer, Integer)}中回传
                         */
                        if (status == null
                                || status == RequestFeatureStatus.TO_RETRY) {
                            requestFeatureStatusMap.put(facePreviewInfoList.get(i).getTrackId(), RequestFeatureStatus.SEARCHING);
                            faceHelper.requestFaceFeature(nv21, facePreviewInfoList.get(i).getFaceInfo(), previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId());
//                            Log.i(TAG, "onPreview: fr start = " + System.currentTimeMillis() + " trackId = " + facePreviewInfoList.get(i).getTrackedFaceCount());
                        }
                    }
                }
            }

            @Override
            public void onCameraClosed() {

            }

            @Override
            public void onCameraError(Exception e) {

            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                if (drawHelper != null) {
                    drawHelper.setCameraDisplayOrientation(displayOrientation);
                }

            }
        };
        cameraHelper = new CameraHelper.Builder()
                .previewViewSize(new Point(previewView.getMeasuredWidth(), previewView.getMeasuredHeight()))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .specificCameraId(rgbCameraID != null ? rgbCameraID : Camera.CameraInfo.CAMERA_FACING_FRONT)
                .isMirror(false)
                .previewOn(previewView)
                .cameraListener(cameraListener)
                .build();
        cameraHelper.init();
        cameraHelper.start();


    }
    private FaceHelper faceHelper;
    private List<CompareResult> compareResultList;
    /**
     * 识别阈值
     */
    private static final float SIMILAR_THRESHOLD = 0.8F;
    /**
     * 用于记录人脸识别相关状态
     */
    private ConcurrentHashMap<Integer, Integer> requestFeatureStatusMap = new ConcurrentHashMap<>();

    private void    searchFace(final FaceFeature frFace, final Integer requestId){
        if (flag){
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                /**
                 * 测试
                 */
                Task_CompareFace task = new Task_CompareFace();
                Task_CompareFace.CompareFeature taskFeature = new Task_CompareFace.CompareFeature();
                taskFeature.faceFeature = frFace.getFeatureData();
                taskFeature.regionCode = BuildConfig.RegionCode;
                String  jsonString = null;
                try {
                    if (Build.VERSION.SDK_INT>=19){
                        jsonString = new Gson().toJson(taskFeature);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }

                task.featureDataString=jsonString;
                task.refreshHandler = new BaseRequestor.RefreshHandler() {
                    @Override
                    public void refresh(Object obj) {

                        if (CommnAction.CheckY(obj,RegisterAndRecognizeActivity.this)) {

                            JsonObject data = new JsonParser().parse(obj.toString()).getAsJsonObject();
                            String token = data.get("data").getAsJsonObject().get("token").getAsString();
                            DBManager.setOtherConfig(FrmConfigKeys.token, token);
                            DBManager.setOtherConfig(FrmConfigKeys.loginResult, obj.toString());
                            Intent mintent = new Intent(RegisterAndRecognizeActivity.this, TBSWebViewActivity.class);
                            startActivity(mintent);
                            finish();
                        }
                    }
                };
                task.start();
            }
        });


        //  FaceServer.getInstance().getTopOfFaceLib(frFace,RegisterAndRecognizeActivity.this);

//        Observable.create(new ObservableOnSubscribe<CompareResult>() {
//
//            @Override
//            public void subscribe(ObservableEmitter<CompareResult> emitter) throws Exception {
//
//                CompareResult compareResult = FaceServer.getInstance().getTopOfFaceLib(frFace,RegisterAndRecognizeActivity.this);
//                emitter.onNext(compareResult);
//            }
//        })
//                .subscribeOn(Schedulers.computation())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Observer<CompareResult>() {
//                    @Override
//                    public void onSubscribe(Disposable d) {
//
//                    }
//
//                    @Override
//                    public void onNext(CompareResult compareResult) {
//
////                        if (compareResult == null || compareResult.getUserName() == null) {
////                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
////                            faceHelper.setName(requestId, "VISITOR " + requestId);
////                            return;
////                        }
////                        if (compareResult.getSimilar() > SIMILAR_THRESHOLD){
////
////                            boolean isAdded = false;
////                            if (compareResultList == null) {
////                                requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
////                                faceHelper.setName(requestId, "VISITOR " + requestId);
////                                return;
////                            }
////                            for (CompareResult compareResult1 : compareResultList) {
////                                if (compareResult1.getTrackId() == requestId) {
////                                    isAdded = true;
////                                    break;
////                                }
////                            }
////
////                            if (!isAdded) {
////                                //对于多人脸搜索，假如最大显示数量为 MAX_DETECT_NUM 且有新的人脸进入，则以队列的形式移除
////                                if (compareResultList.size() >= MAX_DETECT_NUM) {
////                                    compareResultList.remove(0);
////                                    adapter.notifyItemRemoved(0);
////                                }
////                                //添加显示人员时，保存其trackId
////                                compareResult.setTrackId(requestId);
////                                compareResultList.add(compareResult);
////                                adapter.notifyItemInserted(compareResultList.size() - 1);
////                            }
////
////                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.SUCCEED);
////                            faceHelper.setName(requestId, getString(R.string.recognize_success_notice, compareResult.getUserName()));
////                            if (!flag){
//////                                List<Users> users = DataSupport.where("trackId=?", compareResult.getUserName()).find(Users.class);
////                                List<Users> users = DataSupport.findAll(Users.class);
////                                if (users.size()>0){
////                                   Users user = users.get(0);
////                                   String password = user.getPassword();
////                                   String name = user.getUsername();
////                                    login(password,name);
////                               }
////
////                            }
////                        }else {
////
////                            faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_REGISTERED"));
////                            retryRecognizeDelayed(requestId);
////                        }
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//                        faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_REGISTERED"));
//                        retryRecognizeDelayed(requestId);
//                    }
//
//                    @Override
//                    public void onComplete() {
//
//                    }
//                });
    }


    private void drawPreviewInfo(List<FacePreviewInfo> facePreviewInfoList) {
        List<DrawInfo> drawInfoList = new ArrayList<>();
        for (int i = 0; i < facePreviewInfoList.size(); i++) {
            String name = faceHelper.getName(facePreviewInfoList.get(i).getTrackId());
            Integer liveness = livenessMap.get(facePreviewInfoList.get(i).getTrackId());
            Integer recognizeStatus = requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId());

            // 根据识别结果和活体结果设置颜色
            int color = RecognizeColor.COLOR_UNKNOWN;
            if (recognizeStatus != null) {
                if (recognizeStatus == RequestFeatureStatus.FAILED) {
                    color = RecognizeColor.COLOR_FAILED;
                }
                if (recognizeStatus == RequestFeatureStatus.SUCCEED) {
                    color = RecognizeColor.COLOR_SUCCESS;
                }
            }
            if (liveness != null && liveness == LivenessInfo.NOT_ALIVE) {
                color = RecognizeColor.COLOR_FAILED;
            }

            drawInfoList.add(new DrawInfo(drawHelper.adjustRect(facePreviewInfoList.get(i).getFaceInfo().getRect()),
                    GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE, liveness == null ? LivenessInfo.UNKNOWN : liveness, color,
                    name == null ? String.valueOf(facePreviewInfoList.get(i).getTrackId()) : name));
        }
        drawHelper.draw(faceRectView, drawInfoList);
    }

    /**
     * VIDEO模式人脸检测引擎，用于预览帧人脸追踪
     */
    private FaceEngine ftEngine;
    /**
     * 用于特征提取的引擎
     */
    private FaceEngine frEngine;
    /**
     * IMAGE模式活体检测引擎，用于预览帧人脸活体检测
     */
    private FaceEngine flEngine;
    /**
     * 初始化引擎
     */

    private int ftInitCode = -1;
    private int frInitCode = -1;
    private int flInitCode = -1;
    private void initEngine(){
        ftEngine = new FaceEngine();
        ftInitCode = ftEngine.init(this, DetectMode.ASF_DETECT_MODE_VIDEO, DetectFaceOrientPriority.ASF_OP_ALL_OUT,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_DETECT);

        frEngine = new FaceEngine();
        frInitCode = frEngine.init(this, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION);

        flEngine = new FaceEngine();
        flInitCode = flEngine.init(this, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_LIVENESS);
        if (ftInitCode != ErrorInfo.MOK) {
            String error = "初始化VIDEO模式人脸检测引擎失败 ftInitCode :"+ftInitCode;
            showToast(error);
        }
        if (frInitCode != ErrorInfo.MOK) {
            String error = "初始化用于特征提取的引擎失败 frInitCode"+frInitCode;
            showToast(error);
        }
        if (flInitCode != ErrorInfo.MOK) {
            String error = "初始化IMAGE模式活体检测引擎失败 flInitCode"+flInitCode;
            showToast(error);
        }

    }
    public void showToast(final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RegisterAndRecognizeActivity.this,msg,Toast.LENGTH_SHORT).show();
            }
        });
    }

    public Boolean checkPermissions(String [] permissions){
        for (String permission :permissions ){
            if (  (ContextCompat.checkSelfPermission(RegisterAndRecognizeActivity.this,permission))!= PackageManager.PERMISSION_GRANTED){
                return  false;
            }

        }
        return true;
    }
    /**
     * 失败重试间隔时间（ms）
     */
    private static final long FAIL_RETRY_INTERVAL = 1000;
    /**
     * 延迟 FAIL_RETRY_INTERVAL 重新进行人脸识别
     *
     * @param requestId 人脸ID
     */
    private CompositeDisposable delayFaceTaskCompositeDisposable = new CompositeDisposable();
    private void retryRecognizeDelayed(final Integer requestId) {
        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
        Observable.timer(FAIL_RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribe(new Observer<Long>() {
                    Disposable disposable;

                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                        delayFaceTaskCompositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(Long aLong) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        // 将该人脸特征提取状态置为FAILED，帧回调处理时会重新进行活体检测
                        faceHelper.setName(requestId, Integer.toString(requestId));
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.TO_RETRY);
                        delayFaceTaskCompositeDisposable.remove(disposable);
                    }
                });
    }

    /**
     * 注册人脸状态码，准备注册
     */
    private static final int REGISTER_STATUS_READY = 0;
    /**
     * 注册人脸状态码，注册中
     */
    private static final int REGISTER_STATUS_PROCESSING = 1;


    /**
     * 注册人脸状态码，注册结束（无论成功失败）
     */
    private static final int REGISTER_STATUS_DONE = 2;

    private int registerStatus = REGISTER_STATUS_DONE;
    /**
     * 将准备注册的状态置为 REGISTER_STATUS_READY
     *
     * @param view 注册按钮
     */
    public void register(View view) {
        if (registerStatus == REGISTER_STATUS_DONE) {
            registerStatus = REGISTER_STATUS_READY;
        }
    }


    /**
     * 将map中key对应的value增1回传
     *
     * @param countMap map
     * @param key      key
     * @return 增1后的value
     */
    public int increaseAndGetValue(Map<Integer, Integer> countMap, int key) {
        if (countMap == null) {
            return 0;
        }
        Integer value = countMap.get(key);
        if (value == null) {
            value = 0;
        }
        countMap.put(key, ++value);
        return value;
    }

    private void registerFace(final byte[] nv21, final List<FacePreviewInfo> facePreviewInfoList) {

        if (registerStatus == REGISTER_STATUS_READY && facePreviewInfoList != null && facePreviewInfoList.size() > 0){
            registerStatus = REGISTER_STATUS_PROCESSING;
            FaceEngine faceEngine = FaceServer.faceEngine;

            if (faceEngine == null  || nv21 == null || previewSize.width % 4 != 0 || nv21.length != previewSize.width* previewSize.height * 3 / 2) {

                return ;
            }

            FaceFeature faceFeature = new FaceFeature();
            //特征提取
            int code = faceEngine.extractFaceFeature(nv21, previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(0).getFaceInfo(), faceFeature);
            if (code != ErrorInfo.MOK) {

                return ;
            }
            else {


                /**
                 * 测试
                 */

                Task_UploadFaceInfo task = new Task_UploadFaceInfo();
                Task_UploadFaceInfo.Feature taskFeature = new Task_UploadFaceInfo.Feature();
                taskFeature.featureData = faceFeature.getFeatureData();
                taskFeature.faceUserId = faceUserId;
                taskFeature.userType = userType;
                taskFeature.faceName = faceName;
                taskFeature.faceIdCard = faceIdCard;

                String jsonString = null;
                try {
                    if (Build.VERSION.SDK_INT >= 19) {
                        jsonString = new Gson().toJson(taskFeature);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                task.featureDataString = jsonString;
                task.refreshHandler = new BaseRequestor.RefreshHandler() {
                    @Override
                    public void refresh(Object obj) {
                        if (CommnAction.CheckY(obj, RegisterAndRecognizeActivity.this)) {
                            Toast.makeText(RegisterAndRecognizeActivity.this,"注册成功",Toast.LENGTH_SHORT).show();
                            finish();
                        }else{
                            Toast.makeText(RegisterAndRecognizeActivity.this,"注册失败",Toast.LENGTH_SHORT).show();
                        }
                        registerStatus = REGISTER_STATUS_DONE;
                    }
                };
                task.start();
            }
        }
//                if (registerStatus == REGISTER_STATUS_READY && facePreviewInfoList != null && facePreviewInfoList.size() > 0) {
//            registerStatus = REGISTER_STATUS_PROCESSING;
//            Observable.create(new ObservableOnSubscribe<Boolean>() {
//                @Override
//                public void subscribe(ObservableEmitter<Boolean> emitter) {
//
//                    boolean success = FaceServer.getInstance().registerNv21(RegisterAndRecognizeActivity.this, nv21.clone(), previewSize.width, previewSize.height,
//                            facePreviewInfoList.get(0).getFaceInfo(), faceHelper.getTrackedFaceCount()+"",faceUserId,userType,faceName,faceIdCard
//                    );
//                    emitter.onNext(success);
//
//                }
//            })
//                    .subscribeOn(Schedulers.computation())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(new Observer<Boolean>() {
//                        @Override
//                        public void onSubscribe(Disposable d) {
//
//                        }
//
//                        @Override
//                        public void onNext(Boolean success) {
//                            String result = success ? "注册成功" : "注册失败!";
//                            showToast(result);
//                            registerStatus = REGISTER_STATUS_DONE;
//                            finish();
//                        }
//
//                        @Override
//                        public void onError(Throwable e) {
//                            Log.e("ymy","123");
//                            e.printStackTrace();
//                            showToast("注册失败!");
//                            registerStatus = REGISTER_STATUS_DONE;
////                            finish();
//                        }
//
//                        @Override
//                        public void onComplete() {
//
//                        }
//                    });
//        }
    }
    /**
     * 销毁引擎，faceHelper中可能会有特征提取耗时操作仍在执行，加锁防止crash
     */
    private void unInitEngine() {

        if (ftInitCode == ErrorInfo.MOK && ftEngine != null) {
            synchronized (ftEngine) {
                int ftUnInitCode = ftEngine.unInit();

            }
        }
        if (frInitCode == ErrorInfo.MOK && frEngine != null) {
            synchronized (frEngine) {
                int frUnInitCode = frEngine.unInit();

            }
        }
        if (flInitCode == ErrorInfo.MOK && flEngine != null) {
            synchronized (flEngine) {
                int flUnInitCode = flEngine.unInit();

            }
        }
    }


    @Override
    protected void onDestroy() {

        if (cameraHelper != null) {
            cameraHelper.release();
            cameraHelper = null;
        }

        unInitEngine();
        if (getFeatureDelayedDisposables != null) {
            getFeatureDelayedDisposables.clear();
        }
        if (delayFaceTaskCompositeDisposable != null) {
            delayFaceTaskCompositeDisposable.clear();
        }
        if (faceHelper != null) {
            ConfigUtil.setTrackedFaceCount(this, faceHelper.getTrackedFaceCount());
            faceHelper.release();
            faceHelper = null;
        }

        FaceServer.getInstance().unInit();
        super.onDestroy();
    }
    /**
     * 删除已经离开的人脸
     *
     * @param facePreviewInfoList 人脸和trackId列表
     */
    private void clearLeftFace(List<FacePreviewInfo> facePreviewInfoList) {
        if (compareResultList != null) {
            for (int i = compareResultList.size() - 1; i >= 0; i--) {
                if (!requestFeatureStatusMap.containsKey(compareResultList.get(i).getTrackId())) {
                    compareResultList.remove(i);
                    adapter.notifyItemRemoved(i);
                }
            }
        }
        if (facePreviewInfoList == null || facePreviewInfoList.size() == 0) {
            requestFeatureStatusMap.clear();
            livenessMap.clear();
            livenessErrorRetryMap.clear();
            extractErrorRetryMap.clear();
            if (getFeatureDelayedDisposables != null) {
                getFeatureDelayedDisposables.clear();
            }
            return;
        }
        Enumeration<Integer> keys = requestFeatureStatusMap.keys();
        while (keys.hasMoreElements()) {
            int key = keys.nextElement();
            boolean contained = false;
            for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
                if (facePreviewInfo.getTrackId() == key) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                requestFeatureStatusMap.remove(key);
                livenessMap.remove(key);
                livenessErrorRetryMap.remove(key);
                extractErrorRetryMap.remove(key);
            }
        }


    }
    /**
     * 延迟 FAIL_RETRY_INTERVAL 重新进行活体检测
     *
     * @param requestId 人脸ID
     */
    private void retryLivenessDetectDelayed(final Integer requestId) {
        Observable.timer(FAIL_RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribe(new Observer<Long>() {
                    Disposable disposable;

                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                        delayFaceTaskCompositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(Long aLong) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        // 将该人脸状态置为UNKNOWN，帧回调处理时会重新进行活体检测
                        if (livenessDetect) {
                            faceHelper.setName(requestId, Integer.toString(requestId));
                        }
                        livenessMap.put(requestId, LivenessInfo.UNKNOWN);
                        delayFaceTaskCompositeDisposable.remove(disposable);
                    }
                });
    }
    private boolean isAllGranted= true;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED){
                isAllGranted=false;
                break;
            }
        }
        if (isAllGranted){
            active();
            initCamera();
        }

    }

    /**
     * 切换相机。注意：若切换相机发现检测不到人脸，则极有可能是检测角度导致的，需要销毁引擎重新创建或者在设置界面修改配置的检测角度
     *
     * @param view
     */
    public void switchCamera(View view) {
        if (cameraHelper != null) {
            boolean success = cameraHelper.switchCamera();
            if (!success) {
                showToast(getString(R.string.switch_camera_failed));
            } else {
                showToast(getString(R.string.notice_change_detect_degree));
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.back:
                finish();
                break;
        }
    }
}
