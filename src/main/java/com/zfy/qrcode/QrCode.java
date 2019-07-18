package com.zfy.qrcode;

import android.graphics.Bitmap;

import com.duoyi.qrdecode.BarcodeFormat;
import com.duoyi.qrdecode.DecodeEntry;
import com.zfy.qrcode.decode.DecodeHandler;
import com.zfy.qrcode.encoding.EncodingUtils;
import com.zfy.qrcode.utils.ZxImageUtils;

/**
 * CreateAt : 8/8/17
 * Describe : 对外开放类
 * 使用 zBar 对扫码进行扩展，加快速度
 *
 * @author chendong
 */
public class QrCode {

    public static boolean QR_BEEP_ENABLE            = true;
    public static boolean QR_VIBRATE_ENABLE         = true;
    public static boolean QR_DECODE_CAPTURE_BY_ZBAR = true;
    public static boolean QR_DECODE_ALBUM_BY_ZBAR   = true;
    public static boolean QR_ONLY   = true;

    public static final int ZBAR  = 1;
    public static final int ZXING = 2;

    /**
     * 创建有 logo 二维码
     *
     * @param content   内容
     * @param widthPix  w
     * @param heightPix h
     * @param logoBm    logo bitmap
     * @return 二维码 bitmap
     */
    public static Bitmap generateQRCode(String content, int widthPix, int heightPix, Bitmap logoBm) {
        return EncodingUtils.createQRCode(content, widthPix, heightPix, logoBm);
    }

    /**
     * 创建无 logo 二维码
     *
     * @param content   内容
     * @param widthPix  w
     * @param heightPix h
     * @return 二维码 bitmap
     */
    public static Bitmap generateQRCode(String content, int widthPix, int heightPix) {
        return EncodingUtils.createQRCode(content, widthPix, heightPix, null);
    }

    /**
     * 扫描相册二维码
     *
     * @param filePath 二维码路径
     * @return 二维码中的字符串
     */
    public static String scanAlbumQrCode(String filePath) {
        if (QrCode.QR_DECODE_ALBUM_BY_ZBAR) {
            return DecodeEntry.decodeFromFile(filePath, new BarcodeFormat(BarcodeFormat.QR_CODE));
        } else {
            Bitmap bitmap = ZxImageUtils.decodeSampledBitmapFromFile(filePath, 256, 256);
            byte[] yuv420sp = ZxImageUtils.getYUV420sp(bitmap.getWidth(), bitmap.getHeight(), bitmap);
            ZxImageUtils.recyclerBitmaps(bitmap);
            return new DecodeHandler().decodeQrCode(yuv420sp, bitmap.getWidth(), bitmap.getHeight());
        }
    }


}
