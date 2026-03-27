/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import com.android.camera.debug.Log;
import com.android.camera.util.CameraAvailabilityChecker;
import com.android.camera2.R;

public class NoCameraActivity extends Activity {
    private static final Log.Tag TAG = new Log.Tag("NoCameraActivity");
    
    private CameraAvailabilityChecker.CameraAvailabilityResult mResult;
    private boolean mMonitoring = true;
    private static final long MONITOR_INTERVAL_MS = 2000;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.no_camera_activity);

        mResult = (CameraAvailabilityChecker.CameraAvailabilityResult)
                getIntent().getSerializableExtra("camera_result");
        setupUI();
        startCameraMonitoring();
    }
    
    private void setupUI() {
        TextView titleText = findViewById(R.id.no_camera_title);
        TextView messageText = findViewById(R.id.no_camera_message);

        if (mResult != null) {
            switch (mResult.getStatus()) {
                case NO_CAMERA_HARDWARE:
                    titleText.setText(R.string.no_camera_title);
                    messageText.setText(R.string.no_camera_message);
                    break;
                case CAMERA_DISABLED:
                    titleText.setText(R.string.camera_disabled_title);
                    messageText.setText(R.string.camera_disabled_message);
                    break; 
                default:
                    titleText.setText(R.string.camera_error_title);
                    messageText.setText(R.string.camera_error_message);
                    break;
            }
        }
    }

    private void startCameraMonitoring() {
        Log.d(TAG, "Starting automatic camera monitoring");
        
        final Handler handler = new Handler(Looper.getMainLooper());
        Runnable monitoringRunnable = new Runnable() {
            @Override
            public void run() {
                if (!mMonitoring) {
                    return;
                }
                CameraAvailabilityChecker.CameraAvailabilityResult result =
                        CameraAvailabilityChecker.checkCameraAvailability(NoCameraActivity.this);
                if (result.isAvailable()) {
                    Log.d(TAG, "Camera is now available, switching to camera activity");
                    mMonitoring = false;
                    Intent intent = new Intent(NoCameraActivity.this, CameraActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    handler.postDelayed(this, MONITOR_INTERVAL_MS);
                }
            }
        };
        handler.post(monitoringRunnable);
    }

    private void stopCameraMonitoring() {
        mMonitoring = false;
        Log.d(TAG, "Stopped camera monitoring");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCameraMonitoring();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCameraMonitoring();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMonitoring) {
            startCameraMonitoring();
        }
    }

    public static void start(Context context, 
                           CameraAvailabilityChecker.CameraAvailabilityResult result) {
        Intent intent = new Intent(context, NoCameraActivity.class);
        intent.putExtra("camera_result", result);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
