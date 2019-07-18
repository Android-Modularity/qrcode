package com.zfy.qrcode.utils;

import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import com.google.zxing.Result;
import com.zfy.qrcode.QrCode;
import com.zfy.qrcode.R;
import com.zfy.qrcode.camera.CameraManager;
import com.zfy.qrcode.decode.DecodeThread;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * CreateAt : 2017.08.17
 * Describe : 为扫描页面提供管理类，使扫描页面只关注UI绘制，逻辑在这里处理
 *
 * @author march
 */
public class CaptureUIManager implements LifecycleObserver {

    private static final String TAG = CaptureUIManager.class.getSimpleName();


    public interface ICaptureView {

        void onError();

        Rect getCropFrameRect();

        void onCameraReady();

        void onResult(String text);
    }


    private CameraManager          mCameraManager; // Camera 管理
    private CaptureHandler         mCaptureHandler; // 视频流处理
    private InactivityTimer        mInactivityTimer; // 电量
    private BeepManager            mBeepManager; // 扫描到后的效果
    private Rect                   mCropRect; // 视频流截取
    // UI
    private Activity               mActivity; // 当前 Activity
    private SurfaceView            mSurfaceView; // 预览

    private SurfaceHolder.Callback mCallback;
    //
    private ICaptureView           mOnUIHandler; // 处理结果
    private boolean                mIsHasSurface;


    public CaptureUIManager(Activity activity, SurfaceView surfaceView, ICaptureView onUIHandler) {
        // 保持屏幕常亮
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mActivity = activity;
        mInactivityTimer = new InactivityTimer(activity);
        mBeepManager = new BeepManager(activity);
        mSurfaceView = surfaceView;
        mOnUIHandler = onUIHandler;

        mCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (holder == null) {
                    Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
                }
                if (!mIsHasSurface) {
                    mIsHasSurface = true;
                    initCamera(holder);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mIsHasSurface = false;
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }
        };

    }

    ///////////////////////////////////////////////////////////////////////////
    // api
    ///////////////////////////////////////////////////////////////////////////

    public Rect getCropRect() {
        if (mCropRect == null) {
            mCropRect = mOnUIHandler.getCropFrameRect();
        }
        return mCropRect;
    }

    public CaptureHandler getCaptureHandler() {
        return mCaptureHandler;
    }

    public CameraManager getCameraManager() {
        return mCameraManager;
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (mCaptureHandler != null) {
            mCaptureHandler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
    }


    public int getStatusBarHeight() {
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return mActivity.getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    ///////////////////////////////////////////////////////////////////////////
    // 兼容 Activity 声明周期
    ///////////////////////////////////////////////////////////////////////////
    public void onResume() {
        // CameraManager must be initialized here, not in onCreate(). This is
        // necessary because we don't
        // want to open the camera driver and measure the screen size if we're
        // going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the
        // wrong size and partially
        // off screen.
        mCameraManager = new CameraManager(mActivity);

        mCaptureHandler = null;

        if (mIsHasSurface) {
            // The activity was paused but not stopped, so the surface still
            // exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(mSurfaceView.getHolder());
        } else {
            // Install the callback and wait for surfaceCreated() to init the
            // camera.
            mSurfaceView.getHolder().addCallback(mCallback);
        }
        mInactivityTimer.onResume();
    }


    public void onPause() {
        if (mCaptureHandler != null) {
            mCaptureHandler.quitSynchronously();
            mCaptureHandler = null;
        }
        mInactivityTimer.onPause();
        mBeepManager.close();
        mCameraManager.closeDriver();
        if (!mIsHasSurface) {
            mSurfaceView.getHolder().removeCallback(mCallback);
        }
    }

    public void onDestroy() {
        mInactivityTimer.shutdown();
        if (mCaptureHandler != null) {
            mCaptureHandler.removeCallbacksAndMessages(null);
        }
    }


    void handleDecode(Result rawResult, Bundle bundle) {
        mInactivityTimer.onActivity();
        mBeepManager.playBeepSoundAndVibrate();
        mOnUIHandler.onResult(rawResult.getText());
    }


    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (mCameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            mCameraManager.openDriver(surfaceHolder);
            // Creating the mCaptureHandler starts the preview, which can also throw a
            // RuntimeException.
            if (mCaptureHandler == null) {
                mCaptureHandler = new CaptureHandler(this, mCameraManager, DecodeThread.ALL_MODE);
            }
            if (QrCode.QR_DECODE_CAPTURE_BY_ZBAR) {
                mCropRect = mOnUIHandler.getCropFrameRect();
            }
            mOnUIHandler.onCameraReady();
        } catch (IOException ioe) {
            mOnUIHandler.onError();
        } catch (RuntimeException e) {
            mOnUIHandler.onError();
        }
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onOwnerResume() {
        onResume();
        restartPreviewAfterDelay(0);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onOwnerPause() {
        onPause();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onOwnerDestroy() {
        onDestroy();
    }


}
