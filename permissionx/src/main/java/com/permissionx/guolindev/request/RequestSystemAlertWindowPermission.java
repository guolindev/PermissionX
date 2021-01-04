package com.permissionx.guolindev.request;

import android.Manifest;
import android.os.Build;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation for request android.permission.SYSTEM_ALERT_WINDOW.
 *
 * @author guolin
 * @since 2020/12/28
 */
public class RequestSystemAlertWindowPermission extends BaseTask {

    RequestSystemAlertWindowPermission(PermissionBuilder permissionBuilder) {
        super(permissionBuilder);
    }

    @Override
    public void request() {
        if (pb.requireSystemAlertWindowPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(pb.activity)) {
                // MANAGE_OVERLAY_PERMISSION has already granted, we can finish this task now.
                finish();
                return;
            }
            if (pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null) {
                List<String> requestList = new ArrayList<>();
                requestList.add(Manifest.permission.SYSTEM_ALERT_WINDOW);
                if (pb.explainReasonCallbackWithBeforeParam != null) {
                    // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                    pb.explainReasonCallbackWithBeforeParam.onExplainReason(explainReasonScope, requestList, true);
                } else {
                    pb.explainReasonCallback.onExplainReason(explainReasonScope, requestList);
                }
            } else {
                // No implementation of explainReasonCallback, we can't request
                // Settings.ACTION_MANAGE_OVERLAY_PERMISSION at this time, because user won't understand why.
                finish();
            }
        } else {
            // shouldn't request Settings.ACTION_MANAGE_OVERLAY_PERMISSION at this time, so we call finish() to finish this task.
            finish();
        }
    }

    @Override
    public void requestAgain(List<String> permissions) {
        // don't care what the permissions param is, always request Settings.ACTION_MANAGE_OVERLAY_PERMISSION
        pb.requestOverlayPermissionNow(this);
    }

}