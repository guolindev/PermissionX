package com.permissionx.guolindev.request;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.permissionx.guolindev.PermissionX;

import java.util.ArrayList;
import java.util.List;
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
     * Code for forward to settings page of current app.
     */
    public static final int FORWARD_TO_SETTINGS = 2;

    /**
     * Instance of PermissionBuilder.
     */
    private PermissionBuilder pb;

    /**
     * Instance of current task.
     */
    private ChainTask task;

    /**
     * Request permissions at once by calling {@link Fragment#requestPermissions(String[], int)}, and handle request result in {@link androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback}.
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
     * Request ACCESS_BACKGROUND_LOCATION at once by calling {@link Fragment#requestPermissions(String[], int)}, and handle request result in {@link androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback}.
     *
     * @param permissionBuilder The instance of PermissionBuilder.
     * @param chainTask         Instance of current task.
     */
    void requestAccessBackgroundLocationNow(PermissionBuilder permissionBuilder, ChainTask chainTask) {
        pb = permissionBuilder;
        task = chainTask;
        requestPermissions(new String[]{ ACCESS_BACKGROUND_LOCATION }, REQUEST_BACKGROUND_LOCATION_PERMISSION);
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
            if (task != null && pb != null) { // On some phones, when switch back from settings, permissionBuilder may become null
                task.requestAgain(new ArrayList<>(pb.forwardPermissions));
            } else {
                Log.w("PermissionX", "permissionBuilder should not be null at this time, so we can do nothing in this case.");
            }
        }
    }

    /**
     * Handle result of normal permissions request.
     */
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
        if (allGranted) { // If all permissions are granted, finish current task directly.
            task.finish();
        } else {
            boolean shouldFinishTheTask = true; // Indicate if we should finish the task
            // If explainReasonCallback is not null and there're denied permissions. Try the ExplainReasonCallback.
            if ((pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null) && !pb.deniedPermissions.isEmpty()) {
                shouldFinishTheTask = false; // shouldn't because ExplainReasonCallback handles it
                if (pb.explainReasonCallbackWithBeforeParam != null) {
                    // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                    pb.explainReasonCallbackWithBeforeParam.onExplainReason(task.getExplainScope(), new ArrayList<>(pb.deniedPermissions), false);
                } else {
                    pb.explainReasonCallback.onExplainReason(task.getExplainScope(), new ArrayList<>(pb.deniedPermissions));
                }
            }
            // If forwardToSettingsCallback is not null and there're permanently denied permissions. Try the ForwardToSettingsCallback.
            else if (pb.forwardToSettingsCallback != null && !pb.permanentDeniedPermissions.isEmpty()) {
                shouldFinishTheTask = false; // shouldn't because ForwardToSettingsCallback handles it
                pb.forwardToSettingsCallback.onForwardToSettings(task.getForwardScope(), new ArrayList<>(pb.permanentDeniedPermissions));
            }
            // If showRequestReasonDialog or showForwardToSettingsDialog is not called. We should finish the task.
            // There's case that ExplainReasonCallback or ForwardToSettingsCallback is called, but developer didn't invoke
            // showRequestReasonDialog or showForwardToSettingsDialog in the callback.
            // At this case and all other cases, task should be finished.
            if (shouldFinishTheTask || !pb.showDialogCalled) {
                task.finish();
            }
        }
    }

    /**
     * Handle result of ACCESS_BACKGROUND_LOCATION permission request.
     */
    private void onRequestBackgroundLocationPermissionResult() {
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
