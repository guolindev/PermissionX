package com.permissionx.guolindev;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class InvisibleFragment extends Fragment {

    /**
     * Code for request permissions.
     */
    public static final int PERMISSION_CODE = 1;

    /**
     * Code for forward to settings page of current app.
     */
    public static final int SETTINGS_CODE = 2;

    /**
     * Instance of PermissionBuilder.
     */
    private PermissionBuilder pb;

    /**
     * Request permissions at once by calling {@link Fragment#requestPermissions(String[], int)}, and handle request result in {@link androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback}.
     *
     * @param permissionBuilder
     *          The instance of PermissionBuilder.
     * @param permissions
     *          Permissions that you want to request.
     */
    void requestNow(PermissionBuilder permissionBuilder, String... permissions) {
        pb = permissionBuilder;
        requestPermissions(permissions, PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            List<String> grantedList = new ArrayList<>(); // holds granted permissions in the request permissions
            List<String> showReasonList = new ArrayList<String>(); // holds denied permissions in the request permissions.
            List<String> forwardList = new ArrayList<String>(); // hold permanently denied permissions in the request permissions.
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    grantedList.add(permissions[i]);
                    // Remove granted permissions from deniedPermissions and permanentDeniedPermissions set in PermissionBuilder.
                    pb.deniedPermissions.remove(permissions[i]);
                    pb.permanentDeniedPermissions.remove(permissions[i]);
                } else {
                    // Denied permission can turn into permanent denied permissions, but permanent denied permission can not turn into denied permissions.
                    boolean shouldShowReason = shouldShowRequestPermissionRationale(permissions[i]);
                    if (shouldShowReason) {
                        showReasonList.add(permissions[i]);
                        pb.deniedPermissions.add(permissions[i]);
                        // So there's no need to remove the current permission from permanentDeniedPermissions because it won't be there.
                    } else {
                        forwardList.add(permissions[i]);
                        pb.permanentDeniedPermissions.add(permissions[i]);
                        // We must remove the current permission from deniedPermissions because it is permanent denied permission now.
                        pb.deniedPermissions.remove(permissions[i]);
                    }
                }
            }
            // We can never holds granted permissions for safety, because user may turn some permissions off in settings.
            // So every time request, must request the already granted permissions again and refresh the granted permission set.
            pb.grantedPermissions.clear();
            pb.grantedPermissions.addAll(grantedList);
            boolean allGranted = pb.grantedPermissions.size() == pb.allPermissions.size();
            if (allGranted) { // If all permissions are granted, call RequestCallback directly.
                if (pb.requestCallback != null) {
                    pb.requestCallback.onResult(true, pb.allPermissions, new ArrayList<String>());
                }
            } else {
                boolean goesToRequestCallback = true; // If there's need goes to RequestCallback
                // If explainReasonCallback is not null and there're denied permissions. Try the ExplainReasonCallback.
                if ((pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null) && !showReasonList.isEmpty()) {
                    goesToRequestCallback = false; // No need cause ExplainReasonCallback handles it
                    if (pb.explainReasonCallbackWithBeforeParam != null) {
                        // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                        pb.explainReasonCallbackWithBeforeParam.onExplainReason(pb.explainReasonScope, showReasonList, false);
                    } else {
                        pb.explainReasonCallback.onExplainReason(pb.explainReasonScope, showReasonList);
                    }
                }
                // If forwardToSettingsCallback is not null and there're permanently denied permissions. Try the ForwardToSettingsCallback.
                else if (pb.forwardToSettingsCallback != null && !forwardList.isEmpty()) {
                    goesToRequestCallback = false; // No need cause ForwardToSettingsCallback handles it
                    pb.forwardToSettingsCallback.onForwardToSettings(pb.forwardToSettingsScope, forwardList);
                }
                // If showRequestReasonDialog or showForwardToSettingsDialog is not called. Try the RequestCallback.
                // There's case that ExplainReasonCallback or ForwardToSettingsCallback is called, but developer didn't invoke
                // showRequestReasonDialog or showForwardToSettingsDialog in the callback.
                // At this case and all other cases, RequestCallback will be called.
                if (goesToRequestCallback || !pb.showDialogCalled) {
                    List<String> deniedList = new ArrayList<>();
                    deniedList.addAll(pb.deniedPermissions);
                    deniedList.addAll(pb.permanentDeniedPermissions);
                    if (pb.requestCallback != null) {
                        pb.requestCallback.onResult(false, new ArrayList<String>(pb.grantedPermissions), deniedList);
                    }
                }
            }
        }
    }

    /**
     * Handle the request result when user switch back from Settings.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_CODE) {
            // When user switch back from settings, just request again.
            if (pb != null) { // On some phones, when switch back from settings, permissionBuilder may become null
                pb.requestAgain(pb.forwardPermissions);
            } else {
                Log.w("PermissionX", "permissionBuilder should not be null at this time, so we can do nothing in this case.");
            }
        }
    }
}
