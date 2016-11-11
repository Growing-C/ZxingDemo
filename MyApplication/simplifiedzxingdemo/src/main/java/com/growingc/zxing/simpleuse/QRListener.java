package com.growingc.zxing.simpleuse;

import android.graphics.Bitmap;

import com.google.zxing.Result;
import com.growingc.zxing.result.ResultHolder;

/**
 * Simple listener used in QRScanner.
 * Created by RB-cgy on 2016/11/8.
 */
public interface QRListener {

    /**
     * invoked when QR code is decoded
     *
     * @param rawResult
     * @param resultHandler
     * @param barcode
     */
    void onGetQRCode(Result rawResult, ResultHolder resultHandler, Bitmap barcode);


    /**
     * invoked when QR scanner starts or restarts.
     */
    void onReset();
}
