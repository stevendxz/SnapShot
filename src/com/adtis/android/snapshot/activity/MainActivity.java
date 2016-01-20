package com.adtis.android.snapshot.activity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.adtis.android.snapshot.R;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends Activity
        implements View.OnClickListener, SurfaceHolder.Callback, Camera.PictureCallback {

    private static final String LOGTAG = "MainActivity";

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    private Button btnTakePic;
    private TextView txtTakeTime;
    private ImageButton btnTimeChk;

    private Handler timerUpdateHandler;
    private boolean timerRunning = false;
    private int currentTime = 0;
    private PopupMenu popupMenu = null;

    private ImageButton btnSwitchCamera;
    private int cameraPosition = 1;//0代表前置摄像头，1代表后置摄像头

    private ImageButton btnClkCameraView;
    private boolean isCanClkCameraView = false;

    private int timer = 0;

    private Camera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);
        initView();
    }

    public void initView() {
        surfaceView = (SurfaceView) findViewById(R.id.camara_view);
        btnTakePic = (Button) findViewById(R.id.btn_takepic);
        txtTakeTime = (TextView) findViewById(R.id.txt_takepic_time);
        btnTimeChk = (ImageButton) findViewById(R.id.btn_time_chk);

        btnTimeChk.setOnClickListener(this);
        btnTakePic.setOnClickListener(this);

        surfaceView.setFocusable(true);
        surfaceView.setFocusableInTouchMode(true);
        surfaceView.setClickable(true);
        surfaceView.setOnClickListener(this);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(this);

        timerUpdateHandler = new Handler();

        btnSwitchCamera = (ImageButton)findViewById(R.id.btn_switch_camera);
        btnSwitchCamera.setOnClickListener(this);

        btnClkCameraView = (ImageButton)findViewById(R.id.btn_clkview_chk);
        btnClkCameraView.setOnClickListener(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();
        try {
            Camera.Parameters parameters = camera.getParameters();
            setNewAutoOrientation(parameters);
            //setNewColorEffect(parameters);
            //setNewCameraSize(parameters);
            camera.setParameters(parameters);


            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            camera.release();
            Log.v(LOGTAG, e.getMessage());
        }
        camera.startPreview();
    }

    /**
     * 自动根据屏幕显示旋转横竖屏
     *
     * @param parameters
     */
    public void setNewAutoOrientation(Camera.Parameters parameters) {
        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            parameters.set("orientation", "portrait");
            camera.setDisplayOrientation(90);
            parameters.setRotation(90);
        } else {
            parameters.set("orientation", "landscape");
            camera.setDisplayOrientation(0);
            parameters.setRotation(0);
        }
    }

    /**
     * 摄像头过度曝光 参数为EFFECT_SOLARIZE
     * EFFECT_NONE,EFFECT_MONO,EFFECT_NEGATIVE,EFFECT_SOLARIZE,EFFECT_SEPIA...
     * EFFECT_NONE 无效果
     EFFECT_MONO 黑白效果
     EFFECT_NEGATIVE 负面效果
     EFFECT_SOLARIZE 曝光效果
     EFFECT_SEPIA Sephia效果
     EFFECT_POSTERIZE 多色调分色印效果

     EFFECT_WHITEBOARD 白板效果
     EFFECT_BLACKBOARD 黑板效果
     EFFECT_AQUA 浅绿色效果
     *
     * @param parameters
     */

    public void setNewColorEffect(Camera.Parameters parameters) {
        List<String> colorEffects = parameters.getSupportedColorEffects();
        Iterator<String> cei = colorEffects.iterator();
        while (cei.hasNext()) {
            String currentEffect = cei.next();
            Log.v(APPLOGTAG, "Checking " + currentEffect);
            if (currentEffect.equals(Camera.Parameters.EFFECT_SOLARIZE)) {
                Log.v(APPLOGTAG, "Using SOLARIZE");
                parameters.setColorEffect(Camera.Parameters.EFFECT_SOLARIZE);
                break;
            }
        }
        Log.v(APPLOGTAG, "Using Effect:" + parameters.getColorEffect());
    }

    public static final int LARGEST_WIDTH = 1080;
    public static final int LARGEST_HEIGHT = 1920;
    public static final String APPLOGTAG = "SNAPSHOT";

    public void setNewCameraSize(Camera.Parameters parameters) {
        int best_width = 0;
        int best_height = 0;
        //获取设备所支持的所有大小的列表，返回Camera.Size对象的列表
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        if (previewSizes.size() > 1) {
            Iterator<Camera.Size> cei = previewSizes.iterator();
            while (cei.hasNext()) {
                Camera.Size aSize = cei.next();
                //如果列表中的当前大小大于保存的最佳大小
                //并且小于或等于 LARGEST_WIDTH 和 LARGEST_HEIGHT 常量
                //将在 best_width 和 best_height 变量中保存这个高度和宽度病继续检查
                Log.v(APPLOGTAG, "Checking " + aSize.width + " x " + aSize.height);
                if (aSize.width > best_width
                        && aSize.width <= LARGEST_WIDTH
                        && aSize.height > best_height
                        && aSize.height <= LARGEST_HEIGHT) {
                    //迄今为止，他是最大的大小，且不超过屏幕的尺寸
                    best_width = aSize.width;
                    best_height = aSize.height;
                }
            }

            //确保得到了所需要的值
            if (best_width != 0 && best_height != 0) {
                Log.v(APPLOGTAG, "Using " + best_width + " x " + best_height);
                parameters.setPreviewSize(best_width, best_height);
                surfaceView.setLayoutParams(new RelativeLayout.LayoutParams(best_width, best_height));
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Uri imageFileUri = getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, new ContentValues());
        Log.v(APPLOGTAG, Media.EXTERNAL_CONTENT_URI.toString());
        try {
            OutputStream imageFileOS = getContentResolver().openOutputStream(imageFileUri);
            imageFileOS.write(data);
            imageFileOS.flush();
            imageFileOS.close();
        } catch (FileNotFoundException e) {
            Log.v(APPLOGTAG, e.getMessage());
        } catch (IOException e) {
            Log.v(APPLOGTAG, e.getMessage());
        }
        camera.startPreview();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.camara_view:
                if(isCanClkCameraView) {
                    camera.takePicture(null, null, null, this);
                }
                break;
            case R.id.btn_time_chk:
                onPopupButtonClick(v);
                break;
            case R.id.btn_takepic:
                //camera.takePicture(null, null, null, this);
                currentTime = timer;
                if (!timerRunning) {
                    timerRunning = true;
                    timerUpdateHandler.post(timerUpdateTask);
                }
                break;
            case R.id.btn_switch_camera:
                int cameraCount = 0;
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                //得到摄像头的个数
                cameraCount = Camera.getNumberOfCameras();

                for (int i = 0; i < cameraCount; i++) {
                    //得到每一个摄像头的信息
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraPosition == 1) {
                        //现在是后置，变更为前置
                        //代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                            //停掉原来摄像头的预览
                            camera.stopPreview();
                            //释放资源
                            camera.release();
                            //取消原来摄像头
                            camera = null;
                            //打开当前选中的摄像头
                            camera = Camera.open(i);
                            Camera.Parameters parameters = camera.getParameters();
                            setNewAutoOrientation(parameters);
                            try {
                                //通过surfaceview显示取景画面
                                camera.setPreviewDisplay(surfaceHolder);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            //开始预览
                            camera.startPreview();
                            cameraPosition = 0;
                            break;
                        }
                    } else {
                        //现在是前置， 变更为后置
                        //代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                            //停掉原来摄像头的预览
                            camera.stopPreview();
                            //释放资源
                            camera.release();
                            //取消原来摄像头
                            camera = null;
                            //打开当前选中的摄像头
                            camera = Camera.open(i);
                            Camera.Parameters parameters = camera.getParameters();
                            setNewAutoOrientation(parameters);
                            try {
                                //通过surfaceview显示取景画面
                                camera.setPreviewDisplay(surfaceHolder);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            //开始预览
                            camera.startPreview();
                            cameraPosition = 1;
                            break;
                        }
                    }
                }
                break;
            case R.id.btn_clkview_chk:
                if(!isCanClkCameraView) {
                    btnClkCameraView.setBackgroundResource(R.drawable.icon_clkview_open);
                    isCanClkCameraView = true;
                } else {
                    btnClkCameraView.setBackgroundResource(R.drawable.icon_clkview_lock);
                    isCanClkCameraView = false;
                }
                break;
            default:
                break;
        }
    }

    private Camera openFrontFacingCameraGingerbread() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();

        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx);
                    cameraPosition = camIdx;
                } catch (RuntimeException e) {
                    Log.e("CAMERA", "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }

        return cam;
    }

    private Runnable timerUpdateTask = new Runnable() {
        @Override
        public void run() {
            if (currentTime > 1) {
                timerUpdateHandler.postDelayed(timerUpdateTask, 1000);
                currentTime--;
            } else {
                camera.takePicture(null, null, null, MainActivity.this);
                timerRunning = false;
                timer = 0;
                currentTime = 0;
            }
            Log.v("TIME", currentTime + "s");
            txtTakeTime.setVisibility(View.VISIBLE);
            txtTakeTime.setText(currentTime + "s");
            if (currentTime <= 0) {
                txtTakeTime.setVisibility(View.INVISIBLE);
            }
        }
    };

    public void onPopupButtonClick(View view) {
        //创建PopupMenu对象
        popupMenu = new PopupMenu(this, view);
        //将R.menu.popup_menu菜单资源加载到popup菜单中
        getMenuInflater().inflate(R.menu.timer_check, popupMenu.getMenu());
        //为popup菜单的菜单项单击事件绑定事件监听器
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.timer_chk_close:
                        btnTimeChk.setBackgroundResource(R.drawable.icon_notime);
                        timer = 0;
                        //隐藏该对话框
                        popupMenu.dismiss();
                        break;
                    case R.id.timer_chk_five:
                        btnTimeChk.setBackgroundResource(R.drawable.icon_time_five);
                        timer = 5;
                        //隐藏该对话框
                        popupMenu.dismiss();
                        break;
                    case R.id.timer_chk_ten:
                        btnTimeChk.setBackgroundResource(R.drawable.icon_time_ten);
                        timer = 10;
                        //隐藏该对话框
                        popupMenu.dismiss();
                        break;
                    case R.id.timer_chk_fifteen:
                        btnTimeChk.setBackgroundResource(R.drawable.icon_time_fifteen);
                        timer = 15;
                        //隐藏该对话框
                        popupMenu.dismiss();
                        break;
                    case R.id.timer_chk_twenty:
                        btnTimeChk.setBackgroundResource(R.drawable.icon_time_twenty);
                        timer = 20;
                        //隐藏该对话框
                        popupMenu.dismiss();
                        break;
                    default:
                        //使用Toast显示用户单击的菜单项
                        Toast.makeText(MainActivity.this, "您单击了【" + item.getTitle() + "】菜单项", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });
        popupMenu.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.timer_check, menu);
        return true;
    }
}
