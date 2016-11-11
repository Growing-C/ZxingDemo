package com.growingc.zxing.simpleuse;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.Result;
import com.growingc.zxing.R;
import com.growingc.zxing.other.ViewfinderView;
import com.growingc.zxing.result.ResultHolder;

import java.text.DateFormat;
import java.util.Date;

public class SimpleUseDemo extends Activity {
    QRScanner mScanner;
    ViewfinderView mViewfinderView;
    SurfaceView mSurfaceView;

    private View mResultView;//扫码结果展示的view
    private TextView mStatusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//keep screen on
        setContentView(R.layout.capture);

        initView();

        mScanner = new QRScanner(this, mSurfaceView.getHolder(), mViewfinderView);
        mScanner.setQRListener(new QRListener() {
            @Override
            public void onGetQRCode(Result rawResult, ResultHolder resultHandler, Bitmap barcode) {
                //you can handle the QR code your self
                CharSequence displayContents = resultHandler.getDisplayContents();

                System.out.println("-----------------------------");
                System.out.println("displayContents : " + displayContents);

                mStatusView.setVisibility(View.GONE);
                mViewfinderView.setVisibility(View.GONE);
                mResultView.setVisibility(View.VISIBLE);

                ImageView barcodeImageView = (ImageView) findViewById(R.id.barcode_image_view);
                if (barcode == null) {
                    barcodeImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(),
                            R.mipmap.ic_launcher));
                    System.out.println("barcode Img WRONG~~~~~~~~~~~~~~~");
                } else {
                    barcodeImageView.setImageBitmap(barcode);
                    System.out.println("barcode Img OK~~~~~~~~~~~~~~~");
                }

                TextView formatTextView = (TextView) findViewById(R.id.format_text_view);
                formatTextView.setText(rawResult.getBarcodeFormat().toString());
                System.out.println("getBarcodeFormat : " + rawResult.getBarcodeFormat().toString());

                TextView typeTextView = (TextView) findViewById(R.id.type_text_view);
                typeTextView.setText(resultHandler.getType().toString());
                System.out.println("getType : " + resultHandler.getType().toString());

                DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                TextView timeTextView = (TextView) findViewById(R.id.time_text_view);
                timeTextView.setText(formatter.format(new Date(rawResult.getTimestamp())));
                System.out.println("code time : " + formatter.format(new Date(rawResult.getTimestamp())));

                System.out.println("+++++++++++++++++++++");
            }

            @Override
            public void onReset() {


            }
        });
    }

    private void initView() {
        mSurfaceView = (SurfaceView) findViewById(R.id.preview_view);
        mViewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);

        mResultView = findViewById(R.id.result_view);
        mStatusView = (TextView) findViewById(R.id.status_view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mScanner.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScanner.close();
    }


    @Override
    public void onBackPressed() {
        if (mScanner.getLastResult() != null) {//if the last result is shown on screen,press back to restart QR preview
            mScanner.restartPreviewAfterDelay(0l);
            return;
        }
        super.onBackPressed();
    }

}
