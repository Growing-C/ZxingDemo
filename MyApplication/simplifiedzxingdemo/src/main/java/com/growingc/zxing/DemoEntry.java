package com.growingc.zxing;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.growingc.permission.PermissionDenied;
import com.growingc.permission.PermissionDialog;
import com.growingc.permission.PermissionGranted;
import com.growingc.permission.PermissionManager;
import com.growingc.zxing.originuse.CaptureActivity;
import com.growingc.zxing.simpleuse.SimpleUseDemo;

public class DemoEntry extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_entry);
        //since camera permission is listed to be dangerous permission in android M,
        // we should request camera permission before we start the system camera.also compatible with android versions below 23
        PermissionManager.requestCameraPermission(DemoEntry.this);
    }

    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.origin:
                startActivity(new Intent(DemoEntry.this, CaptureActivity.class));
                break;
            case R.id.simplified:
                startActivity(new Intent(DemoEntry.this, SimpleUseDemo.class));
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionManager.onRequestPermissionsResult(DemoEntry.this, requestCode, permissions, grantResults);
    }

    /**
     * called when camera permission is granted
     */
    @PermissionGranted(requestCode = PermissionManager.REQUEST_CAMERA_PERMISSION)
    public void cameraPermissionGranted() {

    }

    /**
     * called when camera permission is denied
     */
    @PermissionDenied(requestCode = PermissionManager.REQUEST_CAMERA_PERMISSION)
    public void cameraPermissionDenied() {
        new PermissionDialog(DemoEntry.this, getString(R.string.alert_dialog_msg_camera)).show();
    }
}
