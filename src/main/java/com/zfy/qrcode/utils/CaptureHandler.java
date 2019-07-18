/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zfy.qrcode.utils;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.Result;
import com.zfy.qrcode.R;
import com.zfy.qrcode.camera.CameraManager;
import com.zfy.qrcode.decode.DecodeThread;

/**
 * This class handles all the messaging which comprises the mState machine for
 * capture.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public class CaptureHandler extends Handler {

    private final DecodeThread  mDecodeThread;
    private final CameraManager mCameraManager;
    private       State         mState;

    private CaptureUIManager mCaptureUIManager;

    public CaptureHandler(CaptureUIManager manager, CameraManager cameraManager, int decodeMode) {
        mCaptureUIManager = manager;
        mDecodeThread = new DecodeThread(mCaptureUIManager, decodeMode);
        mDecodeThread.start();
        mState = State.SUCCESS;

        // Start ourselves capturing previews and decoding.
        mCameraManager = cameraManager;
        cameraManager.startPreview();
        restartPreviewAndDecode();
    }

    @Override
    public void handleMessage(Message message) {
        if (message.what == R.id.restart_preview) {
            restartPreviewAndDecode();
        } else if (message.what == R.id.decode_succeeded) {
            mState = State.SUCCESS;
            Bundle bundle = message.getData();
            mCaptureUIManager.handleDecode((Result) message.obj, bundle);
//            postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mCameraManager.requestPreviewFrame(mDecodeThread.getHandler(), R.id.decode);
//                }
//            }, 2000);
        } else if (message.what == R.id.decode_failed) {// We're decoding as fast as possible, so when one
            // decode fails,
            // start another.
            mState = State.PREVIEW;
            mCameraManager.requestPreviewFrame(mDecodeThread.getHandler(), R.id.decode);
        }
    }

    public void quitSynchronously() {
        mState = State.DONE;
        mCameraManager.stopPreview();
        Message quit = Message.obtain(mDecodeThread.getHandler(), R.id.quit);
        quit.sendToTarget();
        try {
            // Wait at most half a second; should be enough time, and onPause()
            // will timeout quickly
            mDecodeThread.join(500L);
        } catch (InterruptedException e) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(R.id.decode_succeeded);
        removeMessages(R.id.decode_failed);
    }

    private void restartPreviewAndDecode() {
        if (mState == State.SUCCESS) {
            mState = State.PREVIEW;
            mCameraManager.requestPreviewFrame(mDecodeThread.getHandler(), R.id.decode);
        }
    }

    private enum State {
        PREVIEW, SUCCESS, DONE
    }

}
