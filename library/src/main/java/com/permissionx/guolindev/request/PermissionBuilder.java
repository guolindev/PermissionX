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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.ChainedRequestCallback;
import com.permissionx.guolindev.callback.ExplainReasonCallback;
import com.permissionx.guolindev.callback.ExplainReasonCallbackWithBeforeParam;
import com.permissionx.guolindev.callback.ForwardToSettingsCallback;
import com.permissionx.guolindev.callback.RequestCallback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * More APIs for developers to control PermissionX functions.
 *
 * @author guolin
 * @since 2019/11/17
 */
public class PermissionBuilder {

    /**
     * TAG of InvisibleFragment to find and create.
     */
    private static final String FRAGMENT_TAG = "InvisibleFragment";

    /**
     * Instance of activity for everything.
     */
    FragmentActivity activity;

    private String currentProcess;

    /**
     * Normal runtime permissions that app want to request.
     */
    Set<String> normalPermissions;

    boolean requireBackgroundLocationPermission;

    Set<String> permissionsWontRequest;

    /**
     * Indicates should PermissionX explain request reason before request.
     */
    boolean explainReasonBeforeRequest = false;

    /**
     * Indicates {@link ExplainScope#showRequestReasonDialog(List, String, String)} or {@link ForwardScope#showForwardToSettingsDialog(List, String, String)} is called in {@link #onExplainRequestReason(ExplainReasonCallback)} or {@link #onForwardToSettings(ForwardToSettingsCallback)} callback.
     * If not called, requestCallback will be called by PermissionX automatically.
     */
    boolean showDialogCalled = false;

    /**
     * Holds permissions that have already granted in the requested permissions.
     */
    Set<String> grantedPermissions = new HashSet<>();

    /**
     * Holds permissions that have been denied in the requested permissions.
     */
    Set<String> deniedPermissions = new HashSet<>();

    /**
     * Holds permissions that have been permanently denied in the requested permissions. (Deny and never ask again)
     */
    Set<String> permanentDeniedPermissions = new HashSet<>();

    /**
     * Holds permissions which should forward to Settings to allow them.
     * Not all permanently denied permissions should forward to Settings. Only the ones developer think they are necessary should.
     */
    Set<String> forwardPermissions = new HashSet<>();

    /**
     * The callback for {@link #request(RequestCallback)} method. Can not be null.
     */
    RequestCallback requestCallback;

    /**
     * The callback for {@link #onExplainRequestReason(ExplainReasonCallback)} method. Maybe null.
     */
    ExplainReasonCallback explainReasonCallback;

    /**
     * The callback for {@link #onExplainRequestReason(ExplainReasonCallbackWithBeforeParam)} method, but with beforeRequest param. Maybe null.
     */
    ExplainReasonCallbackWithBeforeParam explainReasonCallbackWithBeforeParam;

    /**
     * The callback for {@link #onForwardToSettings(ForwardToSettingsCallback)} method. Maybe null.
     */
    ForwardToSettingsCallback forwardToSettingsCallback;

    PermissionBuilder(FragmentActivity activity, Set<String> normalPermissions, boolean requireBackgroundLocationPermission, Set<String> permissionsWontRequest) {
        this.activity = activity;
        this.normalPermissions = normalPermissions;
        this.requireBackgroundLocationPermission = requireBackgroundLocationPermission;
        this.permissionsWontRequest = permissionsWontRequest;
    }

    /**
     * Called when permissions need to explain request reason.
     * Typically every time user denies your request would call this method.
     * If you chained {@link #explainReasonBeforeRequest()}, this method might run before permission request.
     *
     * @param callback Callback with permissions denied by user.
     */
    public PermissionBuilder onExplainRequestReason(ExplainReasonCallback callback) {
        explainReasonCallback = callback;
        return this;
    }

    /**
     * Called when permissions need to explain request reason.
     * Typically every time user denies your request would call this method.
     * If you chained {@link #explainReasonBeforeRequest()}, this method might run before permission request.
     * beforeRequest param would tell you this method is currently before or after permission request.
     *
     * @param callback Callback with permissions denied by user.
     */
    public PermissionBuilder onExplainRequestReason(ExplainReasonCallbackWithBeforeParam callback) {
        explainReasonCallbackWithBeforeParam = callback;
        return this;
    }

    /**
     * Called when permissions need to forward to Settings for allowing.
     * Typically user denies your request and checked never ask again would call this method.
     * Remember {@link #onExplainRequestReason(ExplainReasonCallback)} is always prior to this method.
     * If {@link #onExplainRequestReason(ExplainReasonCallback)} is called, this method will not be called in the same request time.
     *
     * @param callback Callback with permissions denied and checked never ask again by user.
     */
    public PermissionBuilder onForwardToSettings(ForwardToSettingsCallback callback) {
        forwardToSettingsCallback = callback;
        return this;
    }

    /**
     * If you need to show request permission rationale, chain this method in your request syntax.
     * {@link #onExplainRequestReason(ExplainReasonCallback)} will be called before permission request.
     */
    public PermissionBuilder explainReasonBeforeRequest() {
        explainReasonBeforeRequest = true;
        return this;
    }

    /**
     * Request permissions at once, and handle request result in the callback.
     *
     * @param callback Callback with 3 params. allGranted, grantedList, deniedList.
     */
    public void request(RequestCallback callback) {
        requestCallback = callback;
        requestNormalPermissions();
    }

    /**
     * This method is internal, and should not be called by developer.
     * <p>
     * If permission is denied by user and {@link ExplainScope#showRequestReasonDialog(List, String, String)} or {@link ForwardScope#showForwardToSettingsDialog(List, String, String)} is called,
     * when user clicked positive button, will call this [requestAgain] method.
     *
     * @param permissions Permissions to request again.
     */
    void requestAgain(List<String> permissions) {
        if (permissions.isEmpty()) {
            onPermissionDialogCancel();
            return;
        }
        Set<String> requestAgainPermissions = new HashSet<>(grantedPermissions);
        requestAgainPermissions.addAll(permissions);
        requestNow(new ArrayList<>(requestAgainPermissions), new ChainedRequestCallback() {
            @Override
            public void onResult() {
                notifyResult();
            }
        });
//        if (requestCallback != null) {
//            if (currentProcess.equals("requestNormalPermissions")) {
//                // when request again, put all granted permissions into permission list again, in case user turn them off in settings.
//                Set<String> requestAgainPermissions = new HashSet<>(grantedPermissions);
//                requestAgainPermissions.addAll(permissions);
//                requestNow(new ArrayList<>(requestAgainPermissions), new ChainedRequestCallback() {
//                    @Override
//                    public void onResult() {
//                        notifyResult();
//                    }
//                });
//            } else if (currentProcess.equals("requestBackgroundLocationPermissions")) {
//                requestBackgroundLocationPermissionNow(new ChainedRequestCallback() {
//                    @Override
//                    public void onResult() {
//                        notifyResult();
//                    }
//                });
//            }
//        }
    }

    /**
     * This method is internal, and should not be called by developer.
     * <p>
     * Show a dialog to user and  explain why these permissions are necessary.
     *
     * @param showReasonOrGoSettings Indicates should show explain reason or forward to Settings.
     * @param permissions            Permissions to request again.
     * @param message                Message that explain to user why these permissions are necessary.
     * @param positiveText           Positive text on the positive button to request again.
     * @param negativeText           Negative text on the negative button. Maybe null if this dialog should not be canceled.
     */
    void showHandlePermissionDialog(ChainTask chainTask, final boolean showReasonOrGoSettings, final List<String> permissions, String message, String positiveText, String negativeText) {
        showDialogCalled = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message);
        builder.setCancelable(!TextUtils.isEmpty(negativeText));
        builder.setPositiveButton(positiveText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (showReasonOrGoSettings) {
                    requestAgain(permissions);
                } else {
                    forwardToSettings(permissions);
                }
            }
        });
        if (!TextUtils.isEmpty(negativeText)) {
            builder.setNegativeButton(negativeText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onPermissionDialogCancel();
                }
            });
        }
        builder.show();
    }

    private void requestNormalPermissions() {
//        currentProcess = "requestNormalPermissions";
//        List<String> requestList = new ArrayList<>();
//        for (String permission : normalPermissions) {
//            if (PermissionX.isGranted(activity, permission)) {
//                grantedPermissions.add(permission); // already granted
//            } else {
//                requestList.add(permission); // still need to request
//            }
//        }
//        if (requestList.isEmpty()) { // all permissions are granted
//            notifyResult();
//            return;
//        }
//        if (explainReasonBeforeRequest && (explainReasonCallback != null || explainReasonCallbackWithBeforeParam != null)) {
//            explainReasonBeforeRequest = false;
//            deniedPermissions.addAll(requestList);
//            if (explainReasonCallbackWithBeforeParam != null) {
//                // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
//                explainReasonCallbackWithBeforeParam.onExplainReason(explainReasonScope, requestList, true);
//            } else {
//                explainReasonCallback.onExplainReason(explainReasonScope, requestList);
//            }
//        } else {
//            // Do the request at once. Always request all permissions no matter they are already granted or not, in case user turn them off in Settings.
//            requestNow(new ArrayList<>(normalPermissions), new ChainedRequestCallback() {
//                @Override
//                public void onResult() {
//                    notifyResult();
//                }
//            });
//        }
    }

    private void requestBackgroundLocationPermissions() {
        currentProcess = "requestBackgroundLocationPermissions";
        if (requireBackgroundLocationPermission && Build.VERSION.SDK_INT >= 29) {
            if (explainReasonCallback != null || explainReasonCallbackWithBeforeParam != null) {
                List<String> requestList =  new ArrayList<>();
                requestList.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                if (explainReasonCallbackWithBeforeParam != null) {
                    // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                    explainReasonCallbackWithBeforeParam.onExplainReason(explainReasonScope, requestList, true);
                } else {
                    explainReasonCallback.onExplainReason(explainReasonScope, requestList);
                }
                return;
            }
        }
        notifyResult();
    }

    /**
     * Request permissions at once in the fragment.
     *
     * @param permissions Permissions that you want to request.
     */
    void requestNow(Set<String> permissions, ChainedRequestCallback callback) {
        getInvisibleFragment().requestNow(this, permissions, callback);
    }

    void requestBackgroundLocationPermissionNow(ChainedRequestCallback callback) {
        getInvisibleFragment().requestBackgroundLocationPermissionNow(this, callback);
    }

    /**
     * Get the invisible fragment in activity for request permissions.
     * If there is no invisible fragment, add one into activity.
     * Don't worry. This is very lightweight.
     */
    private InvisibleFragment getInvisibleFragment() {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        Fragment existedFragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG);
        if (existedFragment != null) {
            return (InvisibleFragment) existedFragment;
        } else {
            InvisibleFragment invisibleFragment = new InvisibleFragment();
            fragmentManager.beginTransaction().add(invisibleFragment, FRAGMENT_TAG).commitNow();
            return invisibleFragment;
        }
    }

    /**
     * Go to your app's Settings page to let user turn on the necessary permissions.
     *
     * @param permissions Permissions which are necessary.
     */
    private void forwardToSettings(List<String> permissions) {
        forwardPermissions.clear();
        forwardPermissions.addAll(permissions);
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        getInvisibleFragment().startActivityForResult(intent, InvisibleFragment.FORWARD_TO_SETTINGS);
    }

    /**
     * If permission is denied by user and {@link ExplainScope#showRequestReasonDialog(List, String, String)} or {@link ForwardScope#showForwardToSettingsDialog(List, String, String)} is called,
     * when user clicked negative button, will call this method.
     */
    private void onPermissionDialogCancel() {
        notifyResult();
    }

    private void notifyResult() {
        if (currentProcess.equals("requestNormalPermissions")) {
            requestBackgroundLocationPermissions();
        } else if (currentProcess.equals("requestBackgroundLocationPermissions")) {
            List<String> deniedList = new ArrayList<>();
            deniedList.addAll(deniedPermissions);
            deniedList.addAll(permanentDeniedPermissions);
            deniedList.addAll(permissionsWontRequest);
            if (requestCallback != null) {
                requestCallback.onResult(deniedList.isEmpty(), new ArrayList<>(grantedPermissions), deniedList);
            }
        }
    }

}