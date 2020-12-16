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

import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.permissionx.guolindev.PermissionX;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.permissionx.guolindev.request.RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION;

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
     * the permission requests. requestXX(...) may be invoked more than once,
     * and the field pb/task will be override by behind call.So we need a map to
     * save request
     */
    private final Map<Integer, RequestInfo> requestInfoMap = new HashMap<>();

    private int version = 0;

    /**
     * Code for forward to settings page of current app.
     */
    public static final int FORWARD_TO_SETTINGS = 3;

    public static final int REQUEST_MASK = 10;

    /**
     * Request permissions at once by calling {@link Fragment#requestPermissions(String[], int)}, and handle request result in ActivityCompat.OnRequestPermissionsResultCallback.
     *
     * @param permissionBuilder The instance of PermissionBuilder.
     * @param permissions       Permissions that you want to request.
     * @param chainTask         Instance of current task.
     */
    void requestNow(PermissionBuilder permissionBuilder, Set<String> permissions, ChainTask chainTask) {
        int requestCode = getRequestCode(REQUEST_NORMAL_PERMISSIONS);
        requestInfoMap.put(requestCode, new RequestInfo(permissionBuilder, chainTask));
        requestPermissions(permissions.toArray(new String[0]), requestCode);
    }

    /**
     * Request ACCESS_BACKGROUND_LOCATION at once by calling {@link Fragment#requestPermissions(String[], int)}, and handle request result in ActivityCompat.OnRequestPermissionsResultCallback.
     *
     * @param permissionBuilder The instance of PermissionBuilder.
     * @param chainTask         Instance of current task.
     */
    void requestAccessBackgroundLocationNow(PermissionBuilder permissionBuilder, ChainTask chainTask) {
        int requestCode = getRequestCode(REQUEST_BACKGROUND_LOCATION_PERMISSION);
        requestInfoMap.put(requestCode, new RequestInfo(permissionBuilder, chainTask));
        requestPermissions(new String[]{ACCESS_BACKGROUND_LOCATION}, requestCode);
    }

    void requestToSettingPage(Intent intent, int requestType, PermissionBuilder pb, ChainTask task) {
        int requestCode = getRequestCode(requestType);
        requestInfoMap.put(requestCode, new RequestInfo(pb, task));
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        int requestType = getRequestType(requestCode);
        if (requestType == REQUEST_NORMAL_PERMISSIONS) {
            onRequestNormalPermissionsResult(permissions, grantResults, requestCode);
        } else if (requestType == REQUEST_BACKGROUND_LOCATION_PERMISSION) {
            onRequestBackgroundLocationPermissionResult(requestCode);
        }
    }

    /**
     * Handle the request result when user switch back from Settings.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int requestType = getRequestType(requestCode);
        if (requestType == FORWARD_TO_SETTINGS) {
            RequestInfo requestInfo = requestInfoMap.get(requestCode);
            if (requestInfo == null) {
                Log.w("PermissionX", "PermissionBuilder and ChainTask should not be null at this time, so we can do nothing in this case.");
            } else {
                ChainTask task = requestInfo.getTask();
                PermissionBuilder pb = requestInfo.getPb();
                task.requestAgain(new ArrayList<>(pb.forwardPermissions));
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (Integer key : requestInfoMap.keySet()) {
            RequestInfo info = requestInfoMap.get(key);
            if (info != null) {
                PermissionBuilder pb = info.getPb();
                if (pb.currentDialog != null && pb.currentDialog.isShowing()) {
                    pb.currentDialog.dismiss();
                }
            }
        }
    }

    /**
     * Handle result of normal permissions request.
     */
    private void onRequestNormalPermissionsResult(@NonNull String[] permissions, @NonNull int[] grantResults, int requestCode) {
        RequestInfo requestInfo = requestInfoMap.get(requestCode);
        if (requestInfo == null) {
            Log.w("PermissionX", "PermissionBuilder and ChainTask should not be null at this time, so we can do nothing in this case.");
        } else {
            // We can never holds granted permissions for safety, because user may turn some permissions off in settings.
            // So every time request, must request the already granted permissions again and refresh the granted permission set.
            PermissionBuilder pb = requestInfo.getPb();
            ChainTask task = requestInfo.getTask();
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
    private void onRequestBackgroundLocationPermissionResult(int requestCode) {
        RequestInfo requestInfo = requestInfoMap.get(requestCode);
        if (requestInfo == null) {
            Log.w("PermissionX", "PermissionBuilder and ChainTask should not be null at this time, so we can do nothing in this case.");
        } else {
            PermissionBuilder pb = requestInfo.getPb();
            ChainTask task = requestInfo.getTask();
            if (PermissionX.isGranted(getContext(), ACCESS_BACKGROUND_LOCATION)) {
                pb.grantedPermissions.add(ACCESS_BACKGROUND_LOCATION);
                // Remove granted permissions from deniedPermissions and permanentDeniedPermissions set in PermissionBuilder.
                pb.deniedPermissions.remove(ACCESS_BACKGROUND_LOCATION);
                pb.permanentDeniedPermissions.remove(ACCESS_BACKGROUND_LOCATION);
                task.finish();
            } else {
                boolean goesToRequestCallback = true; // Indicate if we should finish the task
                boolean shouldShowRationale = shouldShowRequestPermissionRationale(ACCESS_BACKGROUND_LOCATION);
                // If explainReasonCallback is not null and we should show rationale. Try the ExplainReasonCallback.
                if ((pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null) && shouldShowRationale) {
                    goesToRequestCallback = false; // shouldn't because ExplainReasonCallback handles it
                    List<String> permissionsToExplain = new ArrayList<>();
                    permissionsToExplain.add(ACCESS_BACKGROUND_LOCATION);
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
                    permissionsToForward.add(ACCESS_BACKGROUND_LOCATION);
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

    private int getRequestCode(int requestType) {
        int requestCode;
        if (requestType == REQUEST_NORMAL_PERMISSIONS) {
            requestCode = (REQUEST_NORMAL_PERMISSIONS << REQUEST_MASK) + version++;
        } else if (requestType == REQUEST_BACKGROUND_LOCATION_PERMISSION) {
            requestCode = (REQUEST_BACKGROUND_LOCATION_PERMISSION << REQUEST_MASK) + version++;
        } else {
            requestCode = (FORWARD_TO_SETTINGS << REQUEST_MASK) + version++;
        }
        return requestCode;
    }

    private int getRequestType(int requestCode) {
        return requestCode >> REQUEST_MASK;
    }
}
