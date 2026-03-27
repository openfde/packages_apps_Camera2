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

package com.android.camera.util;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.hardware.Camera;

import com.android.camera.debug.Log;
import com.android.camera.one.OneCameraException;
import com.android.camera.one.OneCameraManager;
import com.android.camera.one.OneCameraModule;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraAvailabilityChecker {
    private static final Log.Tag TAG = new Log.Tag("CameraAvailabilityChecker");
    private static final ExecutorService sExecutor = Executors.newSingleThreadExecutor();

    public static class CameraAvailabilityResult implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        public enum Status {
            AVAILABLE,
            NO_CAMERA_HARDWARE,
            CAMERA_DISABLED,
            CAMERA_IN_USE,
            UNKNOWN_ERROR
        }

        private final Status mStatus;
        private final String mMessage;
        private final String mExceptionMessage;

        public CameraAvailabilityResult(Status status, String message, Exception exception) {
            mStatus = status;
            mMessage = message;
            mExceptionMessage = exception != null ? exception.getMessage() : null;
        }

        public Status getStatus() { return mStatus; }
        public String getMessage() { return mMessage; }
        public String getExceptionMessage() { return mExceptionMessage; }
        public boolean isAvailable() { return mStatus == Status.AVAILABLE; }

        @Override
        public String toString() {
            return "CameraAvailabilityResult{" +
                    "status=" + mStatus +
                    ", message='" + mMessage + '\'' +
                    ", exceptionMessage='" + mExceptionMessage + '\'' +
                    '}';
        }
    }

    public static boolean hasCameraHardware(Context context) {
        try {
            if (ApiHelper.HAS_CAMERA_2_API) {
                CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                if (cameraManager != null) {
                    String[] cameraIds = cameraManager.getCameraIdList();
                    return cameraIds != null && cameraIds.length > 0;
                }
            } else {
                int numberOfCameras = Camera.getNumberOfCameras();
                return numberOfCameras > 0;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking camera hardware", e);
            return false;
        }
        return false;
    }

    public static CameraAvailabilityResult checkCameraAvailability(Context context) {
        try {        
            if (!hasCameraHardware(context)) {
                return new CameraAvailabilityResult(
                    CameraAvailabilityResult.Status.NO_CAMERA_HARDWARE,
                    "No camera hardware found on this device",
                    null
                );
            }
            OneCameraManager cameraManager = OneCameraModule.provideOneCameraManager();
            if (cameraManager == null || !cameraManager.hasCamera()) {
                return new CameraAvailabilityResult(
                    CameraAvailabilityResult.Status.NO_CAMERA_HARDWARE,
                    "Camera manager reports no camera available",
                    null
                );
            }
            return new CameraAvailabilityResult(
                CameraAvailabilityResult.Status.AVAILABLE,
                "Camera is available",
                null
            );
            
        } catch (OneCameraException e) {
            Log.e(TAG, "OneCameraException during availability check", e);
            return new CameraAvailabilityResult(
                CameraAvailabilityResult.Status.UNKNOWN_ERROR,
                "Failed to initialize camera manager: " + e.getMessage(),
                e
            );
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during availability check", e);
            return new CameraAvailabilityResult(
                CameraAvailabilityResult.Status.UNKNOWN_ERROR,
                "Unexpected error: " + e.getMessage(),
                e
            );
        }
    }
}