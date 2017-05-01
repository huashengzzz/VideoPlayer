package com.vientiane.asset.ui.activity.login;


import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vientiane.asset.R;
import com.vientiane.asset.utils.CommonUtils;
import com.vientiane.asset.utils.FileUtils;
import com.vientiane.asset.utils.ToastHelper;

import java.io.File;
import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fm.jiecao.jcvideoplayer_lib.JCBuriedPointStandard;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayerStandard;



public class RecordActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.camera_show_view)
    SurfaceView cameraShowView;
    @BindView(R.id.video_flash_light)
    ImageView videoFlashLight;
    @BindView(R.id.video_time)
    Chronometer videoTime;
    @BindView(R.id.swicth_camera)
    ImageView swicthCamera;
    @BindView(R.id.record_button)
    ImageView recordButton;
    @BindView(R.id.camera_translat_view)
    View cameraTranslatView;
    @BindView(R.id.tv_agreement)
    TextView tvAgreement;
    @BindView(R.id.rl_camera)
    RelativeLayout rlCamera;
    @BindView(R.id.rl_video)
    RelativeLayout rlVideo;
    @BindView(R.id.custom_videoplayer_standard)
    JCVideoPlayerStandard videoPlayer;
    @BindView(R.id.rl_video_bottom)
    RelativeLayout rlVideoBottom;
    @BindView(R.id.linear_upload)
    LinearLayout linearUpload;
    @BindView(R.id.tv_upload_again)
    TextView tvUploadAgain;
    @BindView(R.id.tv_upload)
    TextView tvUpload;
    @BindView(R.id.tv_progress)
    TextView tvProgress;
    @BindView(R.id.tv_progress_bg)
    TextView tvProgressBg;
    @BindView(R.id.linear_head)
    LinearLayout linearHead;
    @BindView(R.id.tv_title)
    TextView tvTitle;

    MediaRecorder recorder;

    SurfaceHolder surfaceHolder;

    Camera camera;

    OrientationEventListener orientationEventListener;

    File videoFile;

    int rotationRecord = 90;

    int rotationFlag = 90;

    int flashType;

    int frontRotate;

    int frontOri;

    int cameraType = 1;//默认初始时为前置

    int cameraFlag = 0; //0为前置 1为后置

    boolean flagRecord = false;//是否正在录像

    private String recordTime = "02:00";//2分钟自动截止
    private int surfaceViewHeight = 340;//surface的高度单位是dp
    private int progressBgWidth = 184; //上传进度条的宽度dp
    public final static int SIZE_1 = 640;
    public final static int SIZE_2 = 480;

    private String videoPath;
    private boolean canCamera;//是否有牌照的权限
    private boolean isActive=true;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        ButterKnife.bind(this);
        initToobar();
        initRecord();
    }





    private void initToobar() {

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationIcon(R.drawable.icon_btn_back);
        title.setText("上传视频认证");
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }




    /**
     * 初始化到开始录制的状态
     */
    private void initRecord() {
        //recordButton.setEnabled(false);//置相机录制按钮不可点击需要申请权限后才可点击
        requestCameraPermission();//申请相机权限如果申请成功就置录制按钮为可点
        doStartSize();              //设置surface和覆盖上面的view的尺寸大小
        SurfaceHolder holder = cameraShowView.getHolder();
        holder.addCallback(this);
        // setType必须设置，要不出错.
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        rotationUIListener();
    }


    @OnClick({R.id.video_flash_light, R.id.swicth_camera, R.id.record_button,
            R.id.tv_upload_again, R.id.tv_upload})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.video_flash_light:
                clickFlash();
                break;
            case R.id.swicth_camera:
                switchCamera();
                break;
            case R.id.record_button:
                clickRecord();
                break;
            case R.id.tv_upload_again://重新录制
                //显示视频播放内容
                rlCamera.setVisibility(View.VISIBLE);
                rlVideo.setVisibility(View.GONE);
                if (!flagRecord) {
                    startRecord();
                    startRecordUI();
                }
                break;
            case R.id.tv_upload://上传视频

                break;

        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceHolder = holder;
        cameraTranslatView.setVisibility(View.VISIBLE);

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        surfaceHolder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        endRecord();
        releaseCamera();

    }


    /**
     * 开始录制时候的状态
     */
    private void startRecordUI() {
        swicthCamera.setVisibility(View.GONE); // 旋转摄像头关闭
        videoFlashLight.setVisibility(View.GONE); //闪光灯关闭
        recordButton.setImageResource(R.drawable.img_record_start);   //录制按钮变成待停止


    }

    /**
     * 停止录制时候的状态
     */
    private void endRecordUI() {
        swicthCamera.setVisibility(View.VISIBLE); // 旋转摄像头关闭
        videoFlashLight.setVisibility(View.VISIBLE); //闪光灯关闭
        recordButton.setImageResource(R.drawable.img_record_pause);   //录制按钮变成待停止
    }

    /**
     * 录制按键
     */
    private void clickRecord() {
        if (!canCamera) {
            ToastHelper.shortToast(this, "相机权限被禁止，请在设置中打开");
            return;
        }
        if (!flagRecord) {
            if (startRecord()) {
                startRecordUI();
            }
        } else {
            endRecord();
            //显示视频播放内容
            rlCamera.setVisibility(View.GONE);
            rlVideo.setVisibility(View.VISIBLE);
            rlVideoBottom.setVisibility(View.VISIBLE);
            linearUpload.setVisibility(View.VISIBLE);
            tvProgress.setVisibility(View.GONE);
            tvProgressBg.setVisibility(View.GONE);
            initVideo();
        }
    }

    /**
     * 旋转界面UI
     */
    private void rotationUIListener() {
        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int rotation) {
                if (!flagRecord) {
                    if (((rotation >= 0) && (rotation <= 30)) || (rotation >= 330)) {
                        // 竖屏拍摄
                        if (rotationFlag != 0) {
                            //旋转logo
                            rotationAnimation(rotationFlag, 0);
                            //这是竖屏视频需要的角度
                            rotationRecord = 90;
                            //这是记录当前角度的flag
                            rotationFlag = 0;
                        }
                    } else if (((rotation >= 230) && (rotation <= 310))) {
                        // 横屏拍摄
                        if (rotationFlag != 90) {
                            //旋转logo
                            rotationAnimation(rotationFlag, 90);
                            //这是正横屏视频需要的角度
                            rotationRecord = 0;
                            //这是记录当前角度的flag
                            rotationFlag = 90;
                        }
                    } else if (rotation > 30 && rotation < 95) {
                        // 反横屏拍摄
                        if (rotationFlag != 270) {
                            //旋转logo
                            rotationAnimation(rotationFlag, 270);
                            //这是反横屏视频需要的角度
                            rotationRecord = 180;
                            //这是记录当前角度的flag
                            rotationFlag = 270;
                        }
                    }
                }
            }
        };
        orientationEventListener.enable();
    }


    private void rotationAnimation(int from, int to) {
        if (isActive) {
            ValueAnimator progressAnimator = ValueAnimator.ofInt(from, to);
            progressAnimator.setDuration(300);
            progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int currentAngle = (int) animation.getAnimatedValue();
                    videoFlashLight.setRotation(currentAngle);
                    videoTime.setRotation(currentAngle);
                    swicthCamera.setRotation(currentAngle);
                }
            });
            progressAnimator.start();
        }

    }

    /**
     * 因为录制改分辨率的比例可能和屏幕比例一直，所以需要调整比例显示
     */
    private void doStartSize() {
        CommonUtils.setViewSize(cameraShowView, CommonUtils.dp2px(this, surfaceViewHeight) * SIZE_2 / SIZE_1, CommonUtils.dp2px(this, surfaceViewHeight));
        CommonUtils.setViewSize(cameraTranslatView, CommonUtils.dp2px(this, surfaceViewHeight) * SIZE_2 / SIZE_1, CommonUtils.dp2px(this, surfaceViewHeight));
        CommonUtils.setViewSize(tvProgressBg, CommonUtils.dp2px(this, progressBgWidth), 0);
    }

    /**
     * 申请相机权限
     */
    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            } else {//成功申请
                //recordButton.setEnabled(true);
                canCamera = true;
            }
        } else {//6.0以下无需申请权限
            //recordButton.setEnabled(true);
            canCamera = true;

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //recordButton.setEnabled(true);
            canCamera = true;
        } else {
            //用户勾选了不再询问，提示用户进入手机设置打开相关权限
            ToastHelper.shortToast(this, "相机权限已被禁止");
            // recordButton.setEnabled(true);
            canCamera = false;
        }
    }


    /**
     * 初始化相机
     *
     * @param type    前后的类型
     * @param flashDo 赏光灯是否工作
     */
    private void initCamera(int type, boolean flashDo) {

        if (camera != null) {
            //如果已经初始化过，就先释放
            releaseCamera();
        }

        try {
            camera = Camera.open(type);
            if (camera == null) {
                //  showCameraPermission();
                return;
            }
            camera.lock();

            //Point screen = new Point(getScreenWidth(this), getScreenHeight(this));
            //现在不用获取最高的显示效果
            //Point show = getBestCameraShow(camera.getParameters(), screen);

            Camera.Parameters parameters = camera.getParameters();
            if (type == 0) {
                //基本是都支持这个比例
                Camera.Size closelyPreSize = getCloselyPreSize(CommonUtils.dp2px(this, surfaceViewHeight) * SIZE_2 / SIZE_1, CommonUtils.dp2px(this, surfaceViewHeight));
                parameters.setPreviewSize(closelyPreSize.width, closelyPreSize.height);
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1连续对焦
                camera.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
            }
            camera.setParameters(parameters);
            FlashLogic(camera.getParameters(), flashType, flashDo);
            if (cameraType == 1) {
                frontCameraRotate();
                camera.setDisplayOrientation(frontRotate);
            } else {
                camera.setDisplayOrientation(90);
            }
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
            camera.unlock();
        } catch (Exception e) {
            e.printStackTrace();
            releaseCamera();
        }
    }

    private boolean startRecord() {
        cameraTranslatView.setVisibility(View.GONE);
        //创建好视频文件用来保存
        videoDir();
        if (videoFile == null) {
            ToastHelper.shortToast(this, "SD卡没有挂载，无法录制视频");
            return false;
        }

        //懒人模式，根据闪光灯和摄像头前后重新初始化一遍，开启闪光灯工作模式
        initCamera(cameraType, true);

        if (recorder == null) {
            recorder = new MediaRecorder();
        }

        try {

            recorder.setCamera(camera);
            // 这两项需要放在setOutputFormat之前
            recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            // Set output file format，输出格式
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            //必须在setEncoder之前
            recorder.setVideoFrameRate(15);  //帧数  一分钟帧，15帧就够了
            // 设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
            recorder.setVideoSize(SIZE_1, SIZE_2);//这个大小就够了

            // 这两项需要放在setOutputFormat之后
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);//设置音频编码录音比特率
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);//设置视频编码录音比特率

            recorder.setVideoEncodingBitRate(2 * SIZE_1 * SIZE_2);//第一个数字越大，清晰度就越高，考虑文件大小的缘故，就调整为1
            int frontRotation;
            if (rotationRecord == 180) {
                //反向的前置
                frontRotation = 180;
            } else {
                //正向的前置
                frontRotation = (rotationRecord == 0) ? 270 - frontOri : frontOri; //录制下来的视屏选择角度，此处为前置
            }
            recorder.setOrientationHint((cameraType == 1) ? frontRotation : rotationRecord);
            //把摄像头的画面给它
            recorder.setPreviewDisplay(surfaceHolder.getSurface());

            //设置创建好的输入路径
            recorder.setOutputFile(videoFile.getPath());
            recorder.prepare();
            recorder.start();
            //不能旋转啦
            orientationEventListener.disable();
            flagRecord = true;
            //录制时间的设置
            videoTime.setBase(SystemClock.elapsedRealtime());
            videoTime.start();
            videoTime.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                @Override
                public void onChronometerTick(Chronometer chronometer) {
                    //最大录制时长
                    if (chronometer.getText().equals(recordTime)) {
                        if (flagRecord) {
                            endRecord();
                            //显示视频播放内容
                            rlCamera.setVisibility(View.GONE);
                            rlVideo.setVisibility(View.VISIBLE);
                            rlVideoBottom.setVisibility(View.VISIBLE);
                            linearUpload.setVisibility(View.VISIBLE);
                            tvProgress.setVisibility(View.GONE);
                            tvProgressBg.setVisibility(View.GONE);
                            initVideo();
                        }
                    }
                }
            });

        } catch (Exception e) {
            //一般没有录制权限或者录制参数出现问题都走这里
            e.printStackTrace();
            //还是没权限啊
            recorder.reset();
            recorder.release();
            recorder = null;
            FileUtils.deleteFile(videoFile.getPath());
            return false;
        }
        return true;

    }

    private void endRecord() {
        //反正多次进入，比如surface的destroy和界面onPause
        if (!flagRecord) {
            return;
        }
        flagRecord = false;
        try {
            if (recorder != null) {
                recorder.stop();
                recorder.reset();
                recorder.release();
                orientationEventListener.disable();
                recorder = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        recordButton.setImageResource(R.drawable.img_record_pause);
        videoTime.stop();
        videoTime.setBase(SystemClock.elapsedRealtime());
        releaseCamera();
//        Intent intent = new Intent(this, PlayActivity.class);
//        intent.putExtra(PlayActivity.DATA, videoFile.getAbsolutePath());
//        startActivityForResult(intent, 2222);
        // overridePendingTransition(R.anim.fab_in, R.anim.fab_out);
    }

    public void clickFlash() {
        if (camera == null) {
            return;
        }
        camera.lock();
        Camera.Parameters p = camera.getParameters();
        if (flashType == 0) {
            FlashLogic(p, 1, false);
        } else {
            FlashLogic(p, 0, false);

        }
        camera.unlock();
    }

    /**
     * 释放摄像头资源
     */
    private void releaseCamera() {
        try {
            if (camera != null) {
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.lock();
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 闪光灯逻辑
     *
     * @param p    相机参数
     * @param type 打开还是关闭
     * @param isOn 是否启动
     */
    private void FlashLogic(Camera.Parameters p, int type, boolean isOn) {
        flashType = type;
        if (type == 0) {
            if (isOn) {
                p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(p);
            }
            videoFlashLight.setImageResource(R.drawable.flash_off);
        } else {
            if (isOn) {
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(p);
            }
            videoFlashLight.setImageResource(R.drawable.flash);
        }
        if (cameraFlag == 0) {
            videoFlashLight.setVisibility(View.GONE);
        } else {
            videoFlashLight.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();//得到摄像头的个数0或者1;

        try {
            for (int i = 0; i < cameraCount; i++) {
                Camera.getCameraInfo(i, cameraInfo);//得到每一个摄像头的信息
                if (cameraFlag == 1) {
                    //后置到前置
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                        frontCameraRotate();//前置旋转摄像头度数
                        switchCameraLogic(i, 0, frontRotate);
                        break;
                    }
                } else {
                    //前置到后置
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                        switchCameraLogic(i, 1, 90);
                        break;
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /***
     * 处理摄像头切换逻辑
     *
     * @param i           哪一个，前置还是后置
     * @param flag        切换后的标志
     * @param orientation 旋转的角度
     */
    private void switchCameraLogic(int i, int flag, int orientation) {
        if (camera != null) {
            camera.lock();
        }
        endRecordUI();
        releaseCamera();
        camera = Camera.open(i);//打开当前选中的摄像头
        try {
            camera.setDisplayOrientation(orientation);
            camera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        cameraFlag = flag;
        FlashLogic(camera.getParameters(), 0, false);
        camera.startPreview();
        cameraType = i;
        camera.unlock();
    }

    /**
     * 旋转前置摄像头为正的
     */
    private void frontCameraRotate() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(1, info);
        int degrees = getDisplayRotation(this);
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        frontOri = info.orientation;
        frontRotate = result;
    }

    /**
     * 获取旋转角度
     */
    private int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }


    public String videoDir() {
        //保证每个录制文件始终只有一个
        if (!TextUtils.isEmpty(FileUtils.getVideoDirPath(this))) {
            FileUtils.deleteFile(FileUtils.getVideoDirPath(this));
        }
        File sampleDir = FileUtils.getVideoDir(this);
        if (sampleDir != null) {
            if (!sampleDir.exists()) {
                sampleDir.mkdirs();
            }
            File vecordDir = sampleDir;

            // 创建文件
            try {
                videoFile = File.createTempFile("recording", ".mp4", vecordDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            videoFile = null;
        }

        return null;
    }

    /**
     * 获取和屏幕比例最相近的，质量最高的显示
     */
    private Point getBestCameraShow(Camera.Parameters parameters, Point screenResolution) {
        float tmpSize;
        float minDiffSize = 100f;
        float scale = (float) screenResolution.x / (float) screenResolution.y;
        Camera.Size best = null;
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        for (Camera.Size s : supportedPreviewSizes) {
            tmpSize = Math.abs(((float) s.height / (float) s.width) - scale);
            if (tmpSize < minDiffSize) {
                minDiffSize = tmpSize;
                best = s;
            }
        }
        return new Point(best.width, best.height);
    }

    protected Camera.Size getCloselyPreSize(int surfaceWidth, int surfaceHeight) {
        List<Camera.Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();
        int ReqTmpWidth;
        int ReqTmpHeight;
        // 当屏幕为垂直的时候需要把宽高值进行调换，保证宽大于高

        ReqTmpWidth = surfaceHeight;
        ReqTmpHeight = surfaceWidth;

        //先查找preview中是否存在与surfaceview相同宽高的尺寸
        for (Camera.Size size : previewSizes) {
            if ((size.width == ReqTmpWidth) && (size.height == ReqTmpHeight)) {
                return size;
            }
        }

        // 得到与传入的宽高比最接近的size
        float reqRatio = ((float) ReqTmpWidth) / ReqTmpHeight;
        float curRatio, deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE;
        Camera.Size retSize = null;
        for (Camera.Size size : previewSizes) {
            curRatio = ((float) size.width) / size.height;
            deltaRatio = Math.abs(reqRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
        }

        return retSize;
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (flagRecord) {
                endRecord();
                if (camera != null && cameraType == 0) {
                    //关闭后置摄像头闪光灯
                    camera.lock();
                    FlashLogic(camera.getParameters(), 0, true);
                    camera.unlock();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        JCVideoPlayer.releaseAllVideos();
    }


    @Override
    protected void onDestroy() {
        endRecord();
        releaseCamera();
        if (orientationEventListener != null) {
            orientationEventListener.disable();
            orientationEventListener = null;
        }
        isActive=false;
        super.onDestroy();

    }

    private void initVideo() {
        File videoDir = FileUtils.getVideoDir(this);
        if (!videoDir.exists()) {
            return;
        }
        File[] files = videoDir.listFiles();
        if (files != null && files.length > 0) {
            videoPath = files[0].getPath();
            videoPlayer.setUp(videoPath, "视频认证");
        }
//        else {
//            videoPlayer.setUp("url"), "视频认证");
//        }
        JCVideoPlayerStandard.setJcBuriedPointStandard(jcBuriedPointStandard);
    }

    public JCBuriedPointStandard jcBuriedPointStandard = new JCBuriedPointStandard() {
        @Override
        public void onClickStartThumb(String url, Object... objects) {

         //   Logger.d("Buried_Point", "onClickStartThumb" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " mUrl is : " + url);
        }

        @Override
        public void onClickBlank(String url, Object... objects) {
          //  Logger.d("Buried_Point", "onClickBlank" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " mUrl is : " + url);

        }

        @Override
        public void onClickBlankFullscreen(String url, Object... objects) {
           // Logger.d("Buried_Point", "onClickBlankFullscreen" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " mUrl is : " + url);
        }

        @Override
        public void onClickStartIcon(String url, Object... objects) {
            //Logger.d("Buried_Point", "onClickStartIcon" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " mUrl is : " + url);
        }

        @Override
        public void onClickStartError(String url, Object... objects) {

            //Logger.d("Buried_Point", "onClickStartError" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " mUrl is : " + url);
        }

        @Override
        public void onClickStop(String url, Object... objects) {

           // Logger.d("Buried_Point", "onClickStop" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " mUrl is : " + url);
        }

        @Override
        public void onClickStopFullscreen(String url, Object... objects) {
           // Logger.d("Buried_Point", "onClickStopFullscreen" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " mUrl is : " + url);
        }

        @Override
        public void onClickResume(String url, Object... objects) {
           // Logger.d("Buried_Point", "onClickResume" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " mUrl is : " + url);
        }

        @Override
        public void onClickResumeFullscreen(String url, Object... objects) {
           // Logger.d("Buried_Point", "onClickResumeFullscreen" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " mUrl is : " + url);
        }

        @Override
        public void onClickSeekbar(String url, Object... objects) {
          //  Logger.d("Buried_Point", "onClickSeekbar" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " mUrl is : " + url);
        }

        @Override
        public void onClickSeekbarFullscreen(String url, Object... objects) {
          //  Logger.d("Buried_Point", "onClickSeekbarFullscreen" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " mUrl is : " + url);
        }

        @Override
        public void onAutoComplete(String url, Object... objects) {
           /// Logger.d("Buried_Point", "onAutoComplete" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " mUrl is : " + url);
        }

        @Override
        public void onAutoCompleteFullscreen(String url, Object... objects) {
           // Logger.d("Buried_Point", "onAutoCompleteFullscreen" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " mUrl is : " + url);
        }

        @Override
        public void onEnterFullscreen(String url, Object... objects) {
           // Logger.d("Buried_Point", "onEnterFullscreen" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " mUrl is : " + url);
        }

        @Override
        public void onQuitFullscreen(String url, Object... objects) {
           // Logger.d("Buried_Point", "onQuitFullscreen" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " mUrl is : " + url);
        }

        @Override
        public void onTouchScreenSeekVolume(String url, Object... objects) {
           // Logger.d("Buried_Point", "onTouchScreenSeekVolume" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " mUrl is : " + url);
        }

        @Override
        public void onTouchScreenSeekPosition(String url, Object... objects) {
          //  Logger.d("Buried_Point", "onTouchScreenSeekVolume" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " mUrl is : " + url);
        }
    };

}
