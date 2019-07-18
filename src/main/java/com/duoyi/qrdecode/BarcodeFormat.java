package com.duoyi.qrdecode;


public class BarcodeFormat {

    private int             reqCode = 0;
    /**
     * 条形码
     */
    public static final int QR_CODE = 1;
    /**
     * 二维码
     */
    public static final int BARCODE = 2;


    public BarcodeFormat(int reqCode) {
        this.reqCode = reqCode;
    }

    public void add(int code) {
        reqCode = reqCode | code;
    }

    public void set(int code) {
        reqCode = code;
    }

    public int get() {
        return reqCode;
    }
}
