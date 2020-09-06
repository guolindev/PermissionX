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

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;

import com.permissionx.guolindev.callback.ExplainReasonCallback;
import com.permissionx.guolindev.callback.ExplainReasonCallbackWithBeforeParam;
import com.permissionx.guolindev.callback.ForwardToSettingsCallback;
import com.permissionx.guolindev.callback.RequestCallback;
import com.permissionx.guolindev.dialog.DefaultDialog;
import com.permissionx.guolindev.dialog.RationaleDialog;
import com.permissionx.guolindev.dialog.RationaleDialogFragment;

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

    /**
     * Instance of fragment for everything as an alternative choice for activity.
     */
    Fragment fragment;

    /**
     * Instance of the current dialog that shows to user. We need to dismiss this dialog when InvisibleFragment destroyed.
     */
    Dialog currentDialog;

    /**
     * Normal runtime permissions that app want to request.
     */
    Set<String> normalPermissions;

    /**
     * Some permissions shouldn't request will be stored here. And notify back to user when request finished.
     */
    Set<String> permissionsWontRequest;

    /**
     * Indicate if we should require background location permission alone.
     * If app run on Android R and targetSdkVersion is R, when request ACCESS_BACKGROUND_LOCATION, this should be true.
     */
    boolean requireBackgroundLocationPermission;

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
     * The custom tint color to set on the DefaultDialog in light theme.
     */
    int lightColor = -1;

    /**
     * The custom tint color to set on the DefaultDialog in dark theme.
     */
    int darkColor = -1;

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
     * When we request multiple permissions. Some are denied, some are permanently denied. Denied permissions will be callback first.
     * And the permanently denied permissions will store in this tempPermanentDeniedPermissions. They will be callback once no more
     * denied permissions exist.
     */
    Set<String> tempPermanentDeniedPermissions = new HashSet<>();

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

    public PermissionBuilder(FragmentActivity activity, Fragment fragment, Set<String> normalPermissions, boolean requireBackgroundLocationPermission, Set<String> permissionsWontRequest) {
        // activity and fragment must not be null at same time
        this.activity = activity;
        this.fragment = fragment;
        if (activity == null && fragment != null) {
            this.activity = fragment.getActivity();
        }
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
     * @return PermissionBuilder itself.
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
     * @return PermissionBuilder itself.
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
     * @return PermissionBuilder itself.
     */
    public PermissionBuilder onForwardToSettings(ForwardToSettingsCallback callback) {
        forwardToSettingsCallback = callback;
        return this;
    }

    /**
     * If you need to show request permission rationale, chain this method in your request syntax.
     * {@link #onExplainRequestReason(ExplainReasonCallback)} will be called before permission request.
     *
     * @return PermissionBuilder itself.
     */
    public PermissionBuilder explainReasonBeforeRequest() {
        explainReasonBeforeRequest = true;
        return this;
    }

    /**
     * Set the tint color to the default rationale dialog.
     * @param lightColor
     *          Used in light theme. A color value in the form 0xAARRGGBB. Do not pass a resource ID. To get a color value from a resource ID, call getColor.
     * @param darkColor
     *          Used in dark theme. A color value in the form 0xAARRGGBB. Do not pass a resource ID. To get a color value from a resource ID, call getColor.
     * @return PermissionBuilder itself.
     */
    public PermissionBuilder setDialogTintColor(int lightColor, int darkColor) {
        this.lightColor = lightColor;
        this.darkColor = darkColor;
        return this;
    }

    /**
     * Request permissions at once, and handle request result in the callback.
     *
     * @param callback Callback with 3 params. allGranted, grantedList, deniedList.
     */
    public void request(RequestCallback callback) {
        requestCallback = callback;
        // Build the request chain.
        // RequestNormalPermissions runs first.
        // Then RequestBackgroundLocationPermission runs.
        RequestChain requestChain = new RequestChain();
        requestChain.addTaskToChain(new RequestNormalPermissions(this));
        requestChain.addTaskToChain(new RequestBackgroundLocationPermission(this));
        requestChain.runTask();
    }

    /**
     * This method is internal, and should not be called by developer.
     * <p>
     * Show a dialog to user and  explain why these permissions are necessary.
     *
     * @param chainTask              Instance of current task.
     * @param showReasonOrGoSettings Indicates should show explain reason or forward to Settings.
     * @param permissions            Permissions to request again.
     * @param message                Message that explain to user why these permissions are necessary.
     * @param positiveText           Positive text on the positive button to request again.
     * @param negativeText           Negative text on the negative button. Maybe null if this dialog should not be canceled.
     */
    void showHandlePermissionDialog(final ChainTask chainTask, final boolean showReasonOrGoSettings, final List<String> permissions, String message, String positiveText, String negativeText) {
        DefaultDialog defaultDialog = new DefaultDialog(activity, permissions, message, positiveText, negativeText, lightColor, darkColor);
        showHandlePermissionDialog(chainTask, showReasonOrGoSettings, defaultDialog);
    }

    /**
     * This method is internal, and should not be called by developer.
     * <p>
     * Show a dialog to user and  explain why these permissions are necessary.
     *
     * @param chainTask              Instance of current task.
     * @param showReasonOrGoSettings Indicates should show explain reason or forward to Settings.
     * @param dialog                 Dialog to explain to user why these permissions are necessary.
     */
    void showHandlePermissionDialog(final ChainTask chainTask, final boolean showReasonOrGoSettings, @NonNull final RationaleDialog dialog) {
        showDialogCalled = true;
        final List<String> permissions = dialog.getPermissionsToRequest();
        if (permissions.isEmpty()) {
            chainTask.finish();
            return;
        }
        currentDialog = dialog;
        dialog.show();
        View positiveButton = dialog.getPositiveButton();
        View negativeButton = dialog.getNegativeButton();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        positiveButton.setClickable(true);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                if (showReasonOrGoSettings) {
                    chainTask.requestAgain(permissions);
                } else {
                    forwardToSettings(permissions);
                }
            }
        });
        if (negativeButton != null) {
            negativeButton.setClickable(true);
            negativeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    chainTask.finish();
                }
            });
        }
        currentDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                currentDialog = null;
            }
        });
    }

    /**
     * This method is internal, and should not be called by developer.
     * <p>
     * Show a DialogFragment to user and  explain why these permissions are necessary.
     *
     * @param chainTask              Instance of current task.
     * @param showReasonOrGoSettings Indicates should show explain reason or forward to Settings.
     * @param dialogFragment         DialogFragment to explain to user why these permissions are necessary.
     */
    void showHandlePermissionDialog(final ChainTask chainTask, final boolean showReasonOrGoSettings, @NonNull final RationaleDialogFragment dialogFragment) {
        showDialogCalled = true;
        final List<String> permissions = dialogFragment.getPermissionsToRequest();
        if (permissions.isEmpty()) {
            chainTask.finish();
            return;
        }
        dialogFragment.showNow(getFragmentManager(), "PermissionXRationaleDialogFragment");
        View positiveButton = dialogFragment.getPositiveButton();
        View negativeButton = dialogFragment.getNegativeButton();
        dialogFragment.setCancelable(false);
        positiveButton.setClickable(true);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogFragment.dismiss();
                if (showReasonOrGoSettings) {
                    chainTask.requestAgain(permissions);
                } else {
                    forwardToSettings(permissions);
                }
            }
        });
        if (negativeButton != null) {
            negativeButton.setClickable(true);
            negativeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialogFragment.dismiss();
                    chainTask.finish();
                }
            });
        }
    }

    /**
     * Request permissions at once in the fragment.
     *
     * @param permissions Permissions that you want to request.
     * @param chainTask   Instance of current task.
     */
    void requestNow(Set<String> permissions, ChainTask chainTask) {
        getInvisibleFragment().requestNow(this, permissions, chainTask);
    }

    /**
     * Request ACCESS_BACKGROUND_LOCATION at once in the fragment.
     *
     * @param chainTask Instance of current task.
     */
    void requestAccessBackgroundLocationNow(ChainTask chainTask) {
        getInvisibleFragment().requestAccessBackgroundLocationNow(this, chainTask);
    }

    /**
     * Get the FragmentManager if it's in Activity, or the ChildFragmentManager if it's in Fragment.
     * @return The FragmentManager to operate Fragment.
     */
    FragmentManager getFragmentManager() {
        FragmentManager fragmentManager;
        if (fragment != null) {
            fragmentManager = fragment.getChildFragmentManager();
        } else {
            fragmentManager = activity.getSupportFragmentManager();
        }
        return fragmentManager;
    }

    /**
     * Get the invisible fragment in activity for request permissions.
     * If there is no invisible fragment, add one into activity.
     * Don't worry. This is very lightweight.
     */
    private InvisibleFragment getInvisibleFragment() {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment existedFragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG);
        if (existedFragment != null) {
            return (InvisibleFragment) existedFragment;
        } else {
            InvisibleFragment invisibleFragment = new InvisibleFragment();
            fragmentManager.beginTransaction().add(invisibleFragment, FRAGMENT_TAG).commitNowAllowingStateLoss();
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

}