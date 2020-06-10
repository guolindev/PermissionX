package com.permissionx.guolindev.request;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.ChainedRequestCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class InvisibleFragment extends Fragment {

    /**
     * Code for request permissions.
     */
    public static final int REQUEST_NORMAL_PERMISSIONS = 1;

    public static final int REQUEST_BACKGROUND_LOCATION_PERMISSION = 2;

    /**
     * Code for forward to settings page of current app.
     */
    public static final int FORWARD_TO_SETTINGS = 2;

    /**
     * Instance of PermissionBuilder.
     */
    private PermissionBuilder pb;

    private ChainedRequestCallback cb;

    /**
     * Request permissions at once by calling {@link Fragment#requestPermissions(String[], int)}, and handle request result in {@link androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback}.
     *
     * @param permissionBuilder The instance of PermissionBuilder.
     * @param permissions       Permissions that you want to request.
     */
    void requestNow(PermissionBuilder permissionBuilder, Set<String> permissions, ChainedRequestCallback callback) {
        pb = permissionBuilder;
        cb = callback;
        requestPermissions(permissions.toArray(new String[0]), REQUEST_NORMAL_PERMISSIONS);
    }

    void requestBackgroundLocationPermissionNow(PermissionBuilder permissionBuilder, ChainedRequestCallback callback) {
        pb = permissionBuilder;
        cb = callback;
        if (Build.VERSION.SDK_INT >= 29) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQUEST_BACKGROUND_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_NORMAL_PERMISSIONS) {
            onRequestNormalPermissionsResult();
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
        if (requestCode == FORWARD_TO_SETTINGS) {
            // When user switch back from settings, just request again.
            if (pb != null) { // On some phones, when switch back from settings, permissionBuilder may become null
                pb.requestAgain(new ArrayList<>(pb.forwardPermissions));
            } else {
                Log.w("PermissionX", "permissionBuilder should not be null at this time, so we can do nothing in this case.");
            }
        }
    }

    private void onRequestNormalPermissionsResult() {
        // We can never holds granted permissions for safety, because user may turn some permissions off in settings.
        // So every time request, must request the already granted permissions again and refresh the granted permission set.
        pb.grantedPermissions.clear();
        for (String permission : pb.normalPermissions) {
            if (PermissionX.isGranted(getContext(), permission)) {
                pb.grantedPermissions.add(permission);
                // Remove granted permissions from deniedPermissions and permanentDeniedPermissions set in PermissionBuilder.
                pb.deniedPermissions.remove(permission);
                pb.permanentDeniedPermissions.remove(permission);
            } else {
                // Denied permission can turn into permanent denied permissions, but permanent denied permission can not turn into denied permissions.
                boolean shouldShowRationale = shouldShowRequestPermissionRationale(permission);
                if (shouldShowRationale) {
                    pb.deniedPermissions.add(permission);
                    // So there's no need to remove the current permission from permanentDeniedPermissions because it won't be there.
                } else {
                    pb.permanentDeniedPermissions.add(permission);
                    // We must remove the current permission from deniedPermissions because it is permanent denied permission now.
                    pb.deniedPermissions.remove(permission);
                }
            }
        }
        boolean allGranted = pb.grantedPermissions.size() == pb.normalPermissions.size();
        if (allGranted) { // If all permissions are granted, call ChainedRequestCallback directly.
            cb.onResult();
        } else {
            boolean goesToRequestCallback = true; // If there's need goes to ChainedRequestCallback
            // If explainReasonCallback is not null and there're denied permissions. Try the ExplainReasonCallback.
            if ((pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null) && !pb.deniedPermissions.isEmpty()) {
                goesToRequestCallback = false; // No need cause ExplainReasonCallback handles it
                if (pb.explainReasonCallbackWithBeforeParam != null) {
                    // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                    pb.explainReasonCallbackWithBeforeParam.onExplainReason(cb.getExplainScope(), new ArrayList<>(pb.deniedPermissions), false);
                } else {
                    pb.explainReasonCallback.onExplainReason(cb.getExplainScope(), new ArrayList<>(pb.deniedPermissions));
                }
            }
            // If forwardToSettingsCallback is not null and there're permanently denied permissions. Try the ForwardToSettingsCallback.
            else if (pb.forwardToSettingsCallback != null && !pb.permanentDeniedPermissions.isEmpty()) {
                goesToRequestCallback = false; // No need cause ForwardToSettingsCallback handles it
                pb.forwardToSettingsCallback.onForwardToSettings(cb.getForwardScope(), new ArrayList<>(pb.permanentDeniedPermissions));
            }
            // If showRequestReasonDialog or showForwardToSettingsDialog is not called. Try the ChainedRequestCallback.
            // There's case that ExplainReasonCallback or ForwardToSettingsCallback is called, but developer didn't invoke
            // showRequestReasonDialog or showForwardToSettingsDialog in the callback.
            // At this case and all other cases, ChainedRequestCallback will be called.
            if (goesToRequestCallback || !pb.showDialogCalled) {
                cb.onResult();
            }
        }
    }

    private void onRequestBackgroundLocationPermissionResult() {
        if (Build.VERSION.SDK_INT >= 29) {
            String accessBackgroundLocation = Manifest.permission.ACCESS_BACKGROUND_LOCATION;
            if (PermissionX.isGranted(getContext(), accessBackgroundLocation)) {
                pb.grantedPermissions.add(accessBackgroundLocation);
                // Remove granted permissions from deniedPermissions and permanentDeniedPermissions set in PermissionBuilder.
                pb.deniedPermissions.remove(accessBackgroundLocation);
                pb.permanentDeniedPermissions.remove(accessBackgroundLocation);
                cb.onResult();
            } else {
                boolean goesToRequestCallback = true; // If there's need goes to RequestCallback
                boolean shouldShowRationale = shouldShowRequestPermissionRationale(accessBackgroundLocation);
                // If explainReasonCallback is not null and there're denied permissions. Try the ExplainReasonCallback.
                if ((pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null) && shouldShowRationale) {
                    goesToRequestCallback = false; // No need cause ExplainReasonCallback handles it
                    List<String> permissionsToExplain = new ArrayList<>();
                    permissionsToExplain.add(accessBackgroundLocation);
                    if (pb.explainReasonCallbackWithBeforeParam != null) {
                        // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                        pb.explainReasonCallbackWithBeforeParam.onExplainReason(cb.getExplainScope(), permissionsToExplain, false);
                    } else {
                        pb.explainReasonCallback.onExplainReason(cb.getExplainScope(), permissionsToExplain);
                    }
                }
                // If forwardToSettingsCallback is not null and there're permanently denied permissions. Try the ForwardToSettingsCallback.
                else if (pb.forwardToSettingsCallback != null && !pb.permanentDeniedPermissions.isEmpty()) {
                    goesToRequestCallback = false; // No need cause ForwardToSettingsCallback handles it
                    List<String> permissionsToForward = new ArrayList<>();
                    permissionsToForward.add(accessBackgroundLocation);
                    pb.forwardToSettingsCallback.onForwardToSettings(cb.getForwardScope(), permissionsToForward);
                }
                // If showRequestReasonDialog or showForwardToSettingsDialog is not called. Try the RequestCallback.
                // There's case that ExplainReasonCallback or ForwardToSettingsCallback is called, but developer didn't invoke
                // showRequestReasonDialog or showForwardToSettingsDialog in the callback.
                // At this case and all other cases, RequestCallback will be called.
                if (goesToRequestCallback || !pb.showDialogCalled) {
                    cb.onResult();
                }
            }
        }
    }

}
