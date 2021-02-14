/*
 * Copyright (C)  guolin, PermissionX Open Source Project
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

package com.permissionx.guolindev.request;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.permissionx.guolindev.PermissionX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An invisible fragment to embedded into activity for handling permission requests.
 * This is very lightweight. Will not affect your app's efficiency.
 *
 * @author guolin
 * @since 2019/11/2
 */
public class InvisibleFragment extends Fragment {

    /**
     * Code for request normal permissions.
     */
    public static final int REQUEST_NORMAL_PERMISSIONS = 1;

    /**
     * Code for request ACCESS_BACKGROUND_LOCATION permissions. This permissions can't be requested with others over Android R.
     */
    public static final int REQUEST_BACKGROUND_LOCATION_PERMISSION = 2;

    /**
     * Code for forward to settings page of current app.
     */
    public static final int FORWARD_TO_SETTINGS = 1;

    public static final int ACTION_MANAGE_OVERLAY_PERMISSION = 2;

    /**
     * Instance of PermissionBuilder.
     */
    private PermissionBuilder pb;

    /**
     * Instance of current task.
     */
    private ChainTask task;

    /**
     * Request permissions at once by calling {@link Fragment#requestPermissions(String[], int)}, and handle request result in ActivityCompat.OnRequestPermissionsResultCallback.
     *
     * @param permissionBuilder The instance of PermissionBuilder.
     * @param permissions       Permissions that you want to request.
     * @param chainTask         Instance of current task.
     */
    void requestNow(PermissionBuilder permissionBuilder, Set<String> permissions, ChainTask chainTask) {
        pb = permissionBuilder;
        task = chainTask;
        requestPermissions(permissions.toArray(new String[0]), REQUEST_NORMAL_PERMISSIONS);
    }

    /**
     * Request ACCESS_BACKGROUND_LOCATION at once by calling {@link Fragment#requestPermissions(String[], int)}, and handle request result in ActivityCompat.OnRequestPermissionsResultCallback.
     *
     * @param permissionBuilder The instance of PermissionBuilder.
     * @param chainTask         Instance of current task.
     */
    void requestAccessBackgroundLocationNow(PermissionBuilder permissionBuilder, ChainTask chainTask) {
        pb = permissionBuilder;
        task = chainTask;
        requestPermissions(new String[]{ RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION }, REQUEST_BACKGROUND_LOCATION_PERMISSION);
    }

    /**
     * Request SYSTEM_ALERT_WINDOW permission. On Android M and above, it's request by
     * Settings.ACTION_MANAGE_OVERLAY_PERMISSION with Intent.
     */
    @TargetApi(Build.VERSION_CODES.M)
    void requestSystemAlertWindowPermissionNow(PermissionBuilder permissionBuilder, ChainTask chainTask) {
        pb = permissionBuilder;
        task = chainTask;
        if (!Settings.canDrawOverlays(getContext())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION);
        } else {
            onRequestSystemAlertWindowPermissionResult();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_NORMAL_PERMISSIONS) {
            onRequestNormalPermissionsResult(permissions, grantResults);
        } else if (requestCode == REQUEST_BACKGROUND_LOCATION_PERMISSION) {
            onRequestBackgroundLocationPermissionResult();
        }
    }

    /**
     * Handle the request result when user switch back from Settings.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // When user switch back from settings, just request again.
        if (checkForGC()) {
            switch (requestCode) {
                case FORWARD_TO_SETTINGS:
                    task.requestAgain(new ArrayList<>(pb.forwardPermissions));
                    break;
                case ACTION_MANAGE_OVERLAY_PERMISSION:
                    onRequestSystemAlertWindowPermissionResult();
                    break;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (checkForGC()) {
            // Dismiss the showing dialog when InvisibleFragment destroyed for avoiding window leak problem.
            if (pb.currentDialog != null && pb.currentDialog.isShowing()) {
                pb.currentDialog.dismiss();
            }
        }
    }

    /**
     * Handle result of normal permissions request.
     */
    private void onRequestNormalPermissionsResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
        if (checkForGC()) {
            // We can never holds granted permissions for safety, because user may turn some permissions off in settings.
            // So every time request, must request the already granted permissions again and refresh the granted permission set.
            pb.grantedPermissions.clear();
            List<String> showReasonList = new ArrayList<>(); // holds denied permissions in the request permissions.
            List<String> forwardList = new ArrayList<>(); // hold permanently denied permissions in the request permissions.
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    pb.grantedPermissions.add(permission);
                    // Remove granted permissions from deniedPermissions and permanentDeniedPermissions set in PermissionBuilder.
                    pb.deniedPermissions.remove(permission);
                    pb.permanentDeniedPermissions.remove(permission);
                } else {
                    // Denied permission can turn into permanent denied permissions, but permanent denied permission can not turn into denied permissions.
                    boolean shouldShowRationale = shouldShowRequestPermissionRationale(permission);
                    if (shouldShowRationale) {
                        showReasonList.add(permissions[i]);
                        pb.deniedPermissions.add(permission);
                        // So there's no need to remove the current permission from permanentDeniedPermissions because it won't be there.
                    } else {
                        forwardList.add(permissions[i]);
                        pb.permanentDeniedPermissions.add(permission);
                        // We must remove the current permission from deniedPermissions because it is permanent denied permission now.
                        pb.deniedPermissions.remove(permission);
                    }
                }
            }
            List<String> deniedPermissions = new ArrayList<>(); // used to validate the deniedPermissions and permanentDeniedPermissions
            deniedPermissions.addAll(pb.deniedPermissions);
            deniedPermissions.addAll(pb.permanentDeniedPermissions);
            // maybe user can turn some permissions on in settings that we didn't request, so check the denied permissions again for safety.
            for (String permission : deniedPermissions) {
                if (PermissionX.isGranted(getContext(), permission)) {
                    pb.deniedPermissions.remove(permission);
                    pb.grantedPermissions.add(permission);
                }
            }
            boolean allGranted = pb.grantedPermissions.size() == pb.normalPermissions.size();
            if (allGranted) { // If all permissions are granted, finish current task directly.
                task.finish();
            } else {
                boolean shouldFinishTheTask = true; // Indicate if we should finish the task
                // If explainReasonCallback is not null and there're denied permissions. Try the ExplainReasonCallback.
                if ((pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null) && !showReasonList.isEmpty()) {
                    shouldFinishTheTask = false; // shouldn't because ExplainReasonCallback handles it
                    if (pb.explainReasonCallbackWithBeforeParam != null) {
                        // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                        pb.explainReasonCallbackWithBeforeParam.onExplainReason(task.getExplainScope(), new ArrayList<>(pb.deniedPermissions), false);
                    } else {
                        pb.explainReasonCallback.onExplainReason(task.getExplainScope(), new ArrayList<>(pb.deniedPermissions));
                    }
                    // store these permanently denied permissions or they will be lost when request again.
                    pb.tempPermanentDeniedPermissions.addAll(forwardList);
                }
                // If forwardToSettingsCallback is not null and there're permanently denied permissions. Try the ForwardToSettingsCallback.
                else if (pb.forwardToSettingsCallback != null && (!forwardList.isEmpty() || !pb.tempPermanentDeniedPermissions.isEmpty())) {
                    shouldFinishTheTask = false; // shouldn't because ForwardToSettingsCallback handles it
                    pb.tempPermanentDeniedPermissions.clear(); // no need to store them anymore once onForwardToSettings callback.
                    pb.forwardToSettingsCallback.onForwardToSettings(task.getForwardScope(), new ArrayList<>(pb.permanentDeniedPermissions));
                }
                // If showRequestReasonDialog or showForwardToSettingsDialog is not called. We should finish the task.
                // There's case that ExplainReasonCallback or ForwardToSettingsCallback is called, but developer didn't invoke
                // showRequestReasonDialog or showForwardToSettingsDialog in the callback.
                // At this case and all other cases, task should be finished.
                if (shouldFinishTheTask || !pb.showDialogCalled) {
                    task.finish();
                }
                // Reset this value after each request. If we don't do this, developer invoke showRequestReasonDialog in ExplainReasonCallback
                // but didn't invoke showForwardToSettingsDialog in ForwardToSettingsCallback, the request process will be lost. Because the
                // previous showDialogCalled affect the next request logic.
                pb.showDialogCalled = false;
            }
        }
    }

    /**
     * Handle result of ACCESS_BACKGROUND_LOCATION permission request.
     */
    private void onRequestBackgroundLocationPermissionResult() {
        if (checkForGC()) {
            if (PermissionX.isGranted(getContext(), RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION)) {
                pb.grantedPermissions.add(RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION);
                // Remove granted permissions from deniedPermissions and permanentDeniedPermissions set in PermissionBuilder.
                pb.deniedPermissions.remove(RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION);
                pb.permanentDeniedPermissions.remove(RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION);
                task.finish();
            } else {
                boolean goesToRequestCallback = true; // Indicate if we should finish the task
                boolean shouldShowRationale = shouldShowRequestPermissionRationale(RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION);
                // If explainReasonCallback is not null and we should show rationale. Try the ExplainReasonCallback.
                if ((pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null) && shouldShowRationale) {
                    goesToRequestCallback = false; // shouldn't because ExplainReasonCallback handles it
                    List<String> permissionsToExplain = new ArrayList<>();
                    permissionsToExplain.add(RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION);
                    if (pb.explainReasonCallbackWithBeforeParam != null) {
                        // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                        pb.explainReasonCallbackWithBeforeParam.onExplainReason(task.getExplainScope(), permissionsToExplain, false);
                    } else {
                        pb.explainReasonCallback.onExplainReason(task.getExplainScope(), permissionsToExplain);
                    }
                }
                // If forwardToSettingsCallback is not null and we shouldn't show rationale. Try the ForwardToSettingsCallback.
                else if (pb.forwardToSettingsCallback != null && !shouldShowRationale) {
                    goesToRequestCallback = false; // shouldn't because ForwardToSettingsCallback handles it
                    List<String> permissionsToForward = new ArrayList<>();
                    permissionsToForward.add(RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION);
                    pb.forwardToSettingsCallback.onForwardToSettings(task.getForwardScope(), permissionsToForward);
                }
                // If showRequestReasonDialog or showForwardToSettingsDialog is not called. We should finish the task.
                // There's case that ExplainReasonCallback or ForwardToSettingsCallback is called, but developer didn't invoke
                // showRequestReasonDialog or showForwardToSettingsDialog in the callback.
                // At this case and all other cases, task should be finished.
                if (goesToRequestCallback || !pb.showDialogCalled) {
                    task.finish();
                }
            }
        }
    }

    /**
     * Handle result of SYSTEM_ALERT_WINDOW permission request.
     */
    private void onRequestSystemAlertWindowPermissionResult() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(getContext())) {
                task.finish();
            } else if (pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null) {
                if (pb.explainReasonCallbackWithBeforeParam != null) {
                    // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                    pb.explainReasonCallbackWithBeforeParam.onExplainReason(task.getExplainScope(), Collections.singletonList(Manifest.permission.SYSTEM_ALERT_WINDOW), false);
                } else {
                    pb.explainReasonCallback.onExplainReason(task.getExplainScope(), Collections.singletonList(Manifest.permission.SYSTEM_ALERT_WINDOW));
                }
            }
        } else {
            task.finish();
        }
    }

    /**
     * On some phones, PermissionBuilder and ChainTask may become null under unpredictable occasions such as GC.
     * They should not be null at this time, so we can do nothing in this case.
     * @return PermissionBuilder and ChainTask are still alive or not. If not, we should not do any further logic.
     */
    private boolean checkForGC() {
        if (pb == null || task == null) {
            Log.w("PermissionX", "PermissionBuilder and ChainTask should not be null at this time, so we can do nothing in this case.");
            return false;
        }
        return true;
    }

}
