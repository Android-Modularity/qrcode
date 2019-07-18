package com.zfy.qrcode.utils;

/**
 * CreateAt : 8/8/17
 * Describe :
 *
 * @author chendong
 */
public class ZxConstants {

    public static long focusIntervalTime = 1_000L; // 对焦时长

    public static boolean isOnlyQrCode          = true; // 只解码 二维码
    public static boolean isDecodeCaptureByZBar = true; // 使用 zbar 扫描相机预览
    public static boolean isDecodeAlbumByZBar   = true; // 使用 zbar 扫描相册图片


    public static final String KEY_FRONT_LIGHT_MODE = "KEY_FRONT_LIGHT_MODE";
    public static final String KEY_DISABLE_EXPOSURE = "KEY_DISABLE_EXPOSURE";
    public static final String KEY_AUTO_FOCUS = "KEY_AUTO_FOCUS";
    public static final String KEY_DISABLE_CONTINUOUS_FOCUS = "KEY_DISABLE_CONTINUOUS_FOCUS";
    public static final String KEY_INVERT_SCAN = "KEY_INVERT_SCAN";
    public static final String KEY_DISABLE_BARCODE_SCENE_MODE = "KEY_DISABLE_BARCODE_SCENE_MODE";
    public static final String KEY_DISABLE_METERING = "KEY_DISABLE_METERING";
}
