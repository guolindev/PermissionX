/*
 * Copyright (C) guolin, PermissionX Open Source Project
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
import android.os.Build;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation for request android.permission.WRITE_SETTINGS.
 *
 * @author guolin
 * @since 2021/2/21
 */
public class RequestWriteSettingsPermission extends BaseTask {

    RequestWriteSettingsPermission(PermissionBuilder permissionBuilder) {
        super(permissionBuilder);
    }

    @Override
    public void request() {
        if (pb.shouldRequestWriteSettingsPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pb.getTargetSdkVersion() >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(pb.activity)) {
                    // WRITE_SETTINGS permission has already granted, we can finish this task now.
                    finish();
                    return;
                }
                if (pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null) {
                    List<String> requestList = new ArrayList<>();
                    requestList.add(Manifest.permission.WRITE_SETTINGS);
                    if (pb.explainReasonCallbackWithBeforeParam != null) {
                        // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                        pb.explainReasonCallbackWithBeforeParam.onExplainReason(explainReasonScope, requestList, true);
                    } else {
                        pb.explainReasonCallback.onExplainReason(explainReasonScope, requestList);
                    }
                } else {
                    // No implementation of explainReasonCallback, we can't request
                    // WRITE_SETTINGS permission at this time, because user won't understand why.
                    finish();
                }
            } else {
                // WRITE_SETTINGS permission is automatically granted below Android M.
                pb.grantedPermissions.add(Manifest.permission.WRITE_SETTINGS);
                // At this time, WRITE_SETTINGS permission shouldn't be special treated anymore.
                pb.specialPermissions.remove(Manifest.permission.WRITE_SETTINGS);
                finish();
            }
        } else {
            // shouldn't request WRITE_SETTINGS permission at this time, so we call finish() to finish this task.
            finish();
        }
    }

    @Override
    public void requestAgain(List<String> permissions) {
        // don't care what the permissions param is, always request WRITE_SETTINGS permission.
        pb.requestWriteSettingsPermissionNow(this);
    }

}