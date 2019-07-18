/*
 * Copyright (C) 2010 ZXing authors
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

package com.zfy.qrcode.decode;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.duoyi.qrdecode.BarcodeFormat;
import com.duoyi.qrdecode.DecodeEntry;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.zfy.qrcode.QrCode;
import com.zfy.qrcode.R;
import com.zfy.qrcode.utils.CaptureUIManager;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class DecodeHandler extends Handler {

    public static final String TAG = DecodeHandler.class.getSimpleName();

    private boolean running = true;
    private MultiFormatReader           multiFormatReader;
    private QRCodeReader                mQrCodeReader;
    private Map<DecodeHintType, Object> hints;
    private CaptureUIManager mCaptureUIManager;

    public DecodeHandler( CaptureUIManager captureUIManager,Map<DecodeHintType, Object> hints) {
        this.hints = hints;
        this.mCaptureUIManager = captureUIManager;
        initReader();
    }

    public DecodeHandler() {
        initReader();
    }

    // 初始化 reader
    private void initReader() {
        if (QrCode.QR_ONLY) {
            mQrCodeReader = new QRCodeReader();
            hints = new HashMap<>();
            hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, com.google.zxing.BarcodeFormat.QR_CODE);
        } else {
            multiFormatReader = new MultiFormatReader();
            multiFormatReader.setHints(hints);
        }
    }

    private Result decode(BinaryBitmap bitmap) throws FormatException, ChecksumException, NotFoundException {
        if (QrCode.QR_ONLY) {
            if (mQrCodeReader != null)
                return mQrCodeReader.decode(bitmap);
        } else {
            if (multiFormatReader != null)
                return multiFormatReader.decodeWithState(bitmap);
        }
        return null;
    }

    private void resetReader() {
        if (QrCode.QR_ONLY) {
            if (mQrCodeReader != null) mQrCodeReader.reset();
        } else {
            if (multiFormatReader != null) multiFormatReader.reset();
        }
    }

    private static void bundleThumbnail(PlanarYUVLuminanceSource source, Bundle bundle) {
        int[] pixels = source.renderThumbnail();
        int width = source.getThumbnailWidth();
        int height = source.getThumbnailHeight();
        Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
    }

    @Override
    public void handleMessage(Message message) {
        if (!running) {
            return;
        }
        if (message.what == R.id.decode) {
            decode((byte[]) message.obj, message.arg1, message.arg2);
        } else if (message.what == R.id.quit) {
            running = false;
            if (Looper.myLooper() != null) {
                Looper.myLooper().quit();
            }
        }
    }



    private Rect mRect;

    private Rect zoomRect(Rect rect, float scale) {
        int newW = (int) (rect.width() * scale);
        int newH = (int) (rect.height() * scale);
        int centerX = rect.centerX();
        int centerY = rect.centerY();
        return new Rect(centerX - newW / 2, centerY - newH / 2, centerX + newW / 2, centerY + newH / 2);
    }

    private void decodeByZBar(byte[] data, int width, int height) {
        BarcodeFormat barcodeFormat = new BarcodeFormat(BarcodeFormat.QR_CODE);
        if(mRect ==null) {
            Rect cropRect = mCaptureUIManager.getCropRect();
            mRect = zoomRect(cropRect, 1.1f);
        }
        Log.e(TAG, mRect.toString());
        Log.e(TAG, data.length + " " + width + "  " + height);
        String result = DecodeEntry.getDecodeResult(barcodeFormat, data, width, height, mRect.left, mRect.top, mRect.width(), mRect.height());
        Log.e(TAG, "result = " + result);
        if (!TextUtils.isEmpty(result)) {
            Result rawResult = new Result(result, null, null, null);
            publishResult(rawResult);
        } else {
            publishResult(null);
        }
    }

    static class Size {
        int width;
        int height;

        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
    /**
     * Decode the data within the viewfinder rectangle, and time how long it
     * took. For efficiency, reuse the same reader objects from one decode to
     * the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    private void decode(byte[] data, int width, int height) {
        Size size = new Size(width,height);

        if (QrCode.QR_DECODE_CAPTURE_BY_ZBAR) {
            decodeByZBar(data, size.width, size.height);
            return;
        }

        // 这里需要将获取的data翻转一下，因为相机默认拿的的横屏的数据
        byte[] rotatedData = new byte[data.length];
        for (int y = 0; y < size.height; y++) {
            for (int x = 0; x < size.width; x++)
                rotatedData[x * size.height + size.height - y - 1] = data[x + y * size.width];
        }
        // 宽高也要调整
        int tmp = size.width;
        size.width = size.height;
        size.height = tmp;

        Result rawResult = null;
        PlanarYUVLuminanceSource source = buildLuminanceSource(rotatedData, size.width, size.height);
        if (source != null) {
            // BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            // 更快，但是识别度低
            BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
            try {
                rawResult = decode(bitmap);
            } catch (Exception re) {
                // continue
            } finally {
                resetReader();
            }
        }
        publishResult(rawResult);
    }


    private void publishResult(Result result) {
        Handler handler = mCaptureUIManager.getCaptureHandler();
        if (result != null) {
            // Don't log the barcode contents for security.
            if (handler != null) {
                Message message = Message.obtain(handler, R.id.decode_succeeded, result);
                Bundle bundle = new Bundle();
                // bundleThumbnail(source, bundle);
                message.setData(bundle);
                message.sendToTarget();
            }
        } else {
            if (handler != null) {
                Message message = Message.obtain(handler, R.id.decode_failed);
                message.sendToTarget();
            }
        }
    }


    public String decodeQrCode(byte[] data, int width, int height) {
        Result rawResult = null;
        PlanarYUVLuminanceSource source = buildLuminanceSource(data, width, height);
        if (source != null) {
            // BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            // 更快，但是识别度低
            BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
            try {
                rawResult = decode(bitmap);
            } catch (Exception re) {
                // continue
            } finally {
                resetReader();
            }
        }
        return rawResult == null ? "" : rawResult.getText();
    }

    /**
     * A factory method to build the appropriate LuminanceSource object based on
     * the format of the preview buffers, as described by Camera.Parameters.
     *
     * @param data   A preview frame.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @return A PlanarYUVLuminanceSource instance.
     */
    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
//        Rect rect = activity.getCropRect();
//        if (rect == null) {
//            return null;
//        }
//        // Go ahead and assume it's YUV rather than die.
//        return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(), rect
//                .height(), false);
        // 不裁剪，增加识别灵敏度
        return new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false);
    }

}
