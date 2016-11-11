package com.growingc.zxing.simpleuse;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.growingc.zxing.other.AmbientLightManager;
import com.growingc.zxing.other.BeepManager;
import com.growingc.zxing.R;
import com.growingc.zxing.other.ViewfinderView;
import com.growingc.zxing.camera.CameraManager;
import com.growingc.zxing.result.ResultHandlerFactory;
import com.growingc.zxing.result.ResultHolder;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;


/**
 * convenient use of Zxing
 * Created by RB-cgy on 2016/11/3.
 */
public class QRScanner implements IDevice, SurfaceHolder.Callback {

    private boolean hasSurface;//surface是不是创建成功
    private BeepManager beepManager;//滴滴声音
    private AmbientLightManager ambientLightManager;//相机灯
    private Activity mActivity;
    private CameraManager mCameraManager;
    private ViewfinderView mViewfinderView;
    private View mResultView;//扫码结果展示的view
    private TextView mStatusView;
    private Result mLastResult;//上一次的扫码结果
    private QRHandler mHandler;
    SurfaceHolder mSurfaceHolder;

    private QRListener mListener;

    public QRScanner(Activity activity, @NonNull SurfaceHolder surfaceHolder, ViewfinderView viewfinderView) {
        this.mActivity = activity;
        this.mSurfaceHolder = surfaceHolder;
        this.mViewfinderView = viewfinderView;
        System.out.println("surfaceHolder==null? " + (surfaceHolder == null));

        hasSurface = false;
        beepManager = new BeepManager(activity);
        ambientLightManager = new AmbientLightManager(activity);

        //you can delete preference.xml if you decide to use QRScanner.
//        PreferenceManager.setDefaultValues(activity, R.xml.preferences, false);
    }

    public void setQRListener(QRListener listener) {
        this.mListener = listener;
    }

    @Override
    public void start() {
        // CameraManager must be initialized in onResume(), not in onCreate(). This is necessary because we don't
        // want to open the camera driver and measure the screen size if we're going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the wrong size and partially
        // off screen.
        mCameraManager = new CameraManager(mActivity.getApplication());

        mViewfinderView.setCameraManager(mCameraManager);

        mResultView = mActivity.findViewById(R.id.result_view);
        mStatusView = (TextView) mActivity.findViewById(R.id.status_view);

        mHandler = null;
        mLastResult = null;

        resetStatusView();

        beepManager.updatePrefs();
        ambientLightManager.start(mCameraManager);

        //作为开始activity的getIntent会返回一个android..MAIN的action

        System.out.println("QRScanner start  hasSurface : " + hasSurface);
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(mSurfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            mSurfaceHolder.addCallback(this);
        }
    }

    @Override
    public void close() {
        if (mHandler != null) {
            mHandler.quitSynchronously();
            mHandler = null;
        }
        ambientLightManager.stop();
        beepManager.close();
        mCameraManager.closeDriver();
        if (!hasSurface) {
            mSurfaceHolder.removeCallback(this);
        }
    }

    @Override
    public void onSuccess(Object bean) {

    }

    @Override
    public void onFailed(String errorString) {

    }


//    -------------------------扫码所需额外的方法---------------------------

    public Handler getHandler() {
        return mHandler;
    }

    /**
     * 获得上一次扫描的结果
     *
     * @return
     */
    public Result getLastResult() {
        return mLastResult;
    }


    public CameraManager getCameraManager() {
        return mCameraManager;
    }

    public ViewfinderView getViewfinderView() {
        return mViewfinderView;
    }

    /**
     * 初始化相机
     *
     * @param surfaceHolder
     */
    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (mCameraManager.isOpen()) {
            System.out.println("initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            mCameraManager.openDriver(surfaceHolder);//相机初始化
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (mHandler == null) {
                mHandler = new QRHandler(this, mActivity, mCameraManager);//这一步里面会执行预览画面的绘制
            }
        } catch (IOException ioe) {
            System.out.println("******************initCamera IOException");
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            System.out.println("Unexpected error initializing camera");
        }
    }

    /**
     * 状态切换到初始状态，即显示扫码预览画面
     */
    private void resetStatusView() {
        mResultView.setVisibility(View.GONE);
        mStatusView.setText(R.string.msg_default_status);
        mStatusView.setVisibility(View.VISIBLE);
        mViewfinderView.setVisibility(View.VISIBLE);
        mLastResult = null;
    }

    /**
     * 延时 重新开始扫码
     *
     * @param delayMS
     */
    public void restartPreviewAfterDelay(long delayMS) {
        if (mHandler != null) {
            mHandler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
        resetStatusView();
    }

    public void drawViewfinder() {
        mViewfinderView.drawViewfinder();
    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult   The contents of the barcode.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param barcode     A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        mLastResult = rawResult;

        ResultHolder resultHandler = ResultHandlerFactory.makeUpResult(rawResult);

        boolean fromLiveScan = barcode != null;
        if (fromLiveScan) {
            // Then not from history, so beep/vibrate and we have an image to draw on
            beepManager.playBeepSoundAndVibrate();
            drawResultPoints(barcode, scaleFactor, rawResult);
        }

        handleDecodeInternally(rawResult, resultHandler, barcode);
    }

    // Put up our own UI for how to handle the decoded contents.
    private void handleDecodeInternally(Result rawResult, ResultHolder resultHandler, Bitmap barcode) {

        CharSequence displayContents = resultHandler.getDisplayContents();

        System.out.println("-----------------------------");
        System.out.println("displayContents : " + displayContents);

        mStatusView.setVisibility(View.GONE);
        mViewfinderView.setVisibility(View.GONE);
        mResultView.setVisibility(View.VISIBLE);

        ImageView barcodeImageView = (ImageView) mActivity.findViewById(R.id.barcode_image_view);
        if (barcode == null) {
            barcodeImageView.setImageBitmap(BitmapFactory.decodeResource(mActivity.getResources(),
                    R.mipmap.ic_launcher));
            System.out.println("barcode Img WRONG~~~~~~~~~~~~~~~");
        } else {
            barcodeImageView.setImageBitmap(barcode);
            System.out.println("barcode Img OK~~~~~~~~~~~~~~~");
        }

        TextView formatTextView = (TextView) mActivity.findViewById(R.id.format_text_view);
        formatTextView.setText(rawResult.getBarcodeFormat().toString());
        System.out.println("getBarcodeFormat : " + rawResult.getBarcodeFormat().toString());

        TextView typeTextView = (TextView) mActivity.findViewById(R.id.type_text_view);
        typeTextView.setText(resultHandler.getType().toString());
        System.out.println("getType : " + resultHandler.getType().toString());

        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        TextView timeTextView = (TextView) mActivity.findViewById(R.id.time_text_view);
        timeTextView.setText(formatter.format(new Date(rawResult.getTimestamp())));
        System.out.println("code time : " + formatter.format(new Date(rawResult.getTimestamp())));

        System.out.println("+++++++++++++++++++++");
        //TODO: do somethings after the bar code is decoded

        if (mListener != null) {
            mListener.onGetQRCode(rawResult, resultHandler, barcode);
        }
//        if (displayContents != null && displayContents.length() != 0) {
//            Intent resultIntent = new Intent();
//            resultIntent.putExtra("qr", displayContents);
//            mActivity.setResult(Activity.RESULT_OK, resultIntent);
//            mActivity.finish();
//
//            return;
//        }
    }

    /**
     * Superimpose a line for 1D or dots for 2D to highlight the key features of the barcode.
     *
     * @param barcode     A bitmap of the captured image.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param rawResult   The decoded results which contains the points to draw.
     */
    private void drawResultPoints(Bitmap barcode, float scaleFactor, Result rawResult) {
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(mActivity.getResources().getColor(R.color.result_points));
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
            } else if (points.length == 4 &&
                    (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A ||
                            rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and metadata
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
                drawLine(canvas, paint, points[2], points[3], scaleFactor);
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    if (point != null) {
                        canvas.drawPoint(scaleFactor * point.getX(), scaleFactor * point.getY(), paint);
                    }
                }
            }
        }
    }

    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b, float scaleFactor) {
        if (a != null && b != null) {
            canvas.drawLine(scaleFactor * a.getX(),
                    scaleFactor * a.getY(),
                    scaleFactor * b.getX(),
                    scaleFactor * b.getY(),
                    paint);
        }
    }


    //----------surfaceView回调方法--------------
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            System.out.println("*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }
}
