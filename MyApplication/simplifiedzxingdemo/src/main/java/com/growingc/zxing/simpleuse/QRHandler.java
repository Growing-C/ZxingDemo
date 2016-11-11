package com.growingc.zxing.simpleuse;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.util.Log;

import com.google.zxing.Result;
import com.growingc.zxing.R;
import com.growingc.zxing.other.ViewfinderResultPointCallback;
import com.growingc.zxing.camera.CameraManager;


/**
 * Created by RB-cgy on 2016/11/3.
 */
public class QRHandler extends Handler {
    private static final String TAG = QRHandler.class.getSimpleName();

    private final Activity activity;
    private final QRScanner mScanner;
    private final MDecodeThread decodeThread;
    private State state;//扫码状态
    private final CameraManager cameraManager;

    private enum State {
        PREVIEW,//预览
        SUCCESS,//扫码成功
        DONE  //扫码结束
    }

    public QRHandler(QRScanner scanner, Activity activity,
                     CameraManager cameraManager) {
        this.mScanner = scanner;
        this.activity = activity;
        decodeThread = new MDecodeThread(scanner,
                new ViewfinderResultPointCallback(scanner.getViewfinderView()));
        decodeThread.start();
        state = State.SUCCESS;

        // Start ourselves capturing previews and decoding.
        this.cameraManager = cameraManager;
        cameraManager.startPreview();
        restartPreviewAndDecode();
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case R.id.restart_preview:
                restartPreviewAndDecode();
                break;
            case R.id.decode_succeeded:
                state = State.SUCCESS;
                Bundle bundle = message.getData();
                Bitmap barcode = null;
                float scaleFactor = 1.0f;
                if (bundle != null) {
                    byte[] compressedBitmap = bundle.getByteArray(MDecodeThread.BARCODE_BITMAP);
                    if (compressedBitmap != null) {
                        barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.length, null);
                        // Mutable copy:
                        barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
                    }
                    scaleFactor = bundle.getFloat(MDecodeThread.BARCODE_SCALED_FACTOR);
                }
                mScanner.handleDecode((Result) message.obj, barcode, scaleFactor);
                break;
            case R.id.decode_failed:
                // We're decoding as fast as possible, so when one decode fails, start another.
                state = State.PREVIEW;
                cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
                break;
            case R.id.return_scan_result:
                activity.setResult(Activity.RESULT_OK, (Intent) message.obj);
                activity.finish();
                break;
            case R.id.launch_product_query:
                String url = (String) message.obj;

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                intent.setData(Uri.parse(url));

                ResolveInfo resolveInfo =
                        activity.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                String browserPackageName = null;
                if (resolveInfo != null && resolveInfo.activityInfo != null) {
                    browserPackageName = resolveInfo.activityInfo.packageName;
                    Log.d(TAG, "Using browser in package " + browserPackageName);
                }

                // Needed for default Android browser / Chrome only apparently
                if ("com.android.browser".equals(browserPackageName) || "com.android.chrome".equals(browserPackageName)) {
                    intent.setPackage(browserPackageName);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, browserPackageName);
                }

                try {
                    activity.startActivity(intent);
                } catch (ActivityNotFoundException ignored) {
                    Log.w(TAG, "Can't find anything to handle VIEW of URI " + url);
                }
                break;
        }
    }

    public void quitSynchronously() {
        state = State.DONE;
        cameraManager.stopPreview();
        Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
        quit.sendToTarget();
        try {
            // Wait at most half a second; should be enough time, and onPause() will timeout quickly
            decodeThread.join(500L);
        } catch (InterruptedException e) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(R.id.decode_succeeded);
        removeMessages(R.id.decode_failed);
    }

    /**
     * 重新开始预览画面 和解码
     */
    private void restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW;
            cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
            mScanner.drawViewfinder();
        }
    }

}
