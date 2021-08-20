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

import com.permissionx.guolindev.PermissionX;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation for request normal permissions.
 * @author guolin
 * @since 2020/6/10
 */
public class RequestNormalPermissions extends BaseTask {

    RequestNormalPermissions(PermissionBuilder permissionBuilder) {
        super(permissionBuilder);
    }

    @Override
    public void request() {
        List<String> requestList = new ArrayList<>();
        for (String permission : pb.normalPermissions) {
            if (PermissionX.isGranted(pb.activity, permission)) {
                pb.grantedPermissions.add(permission); // already granted
            } else {
                requestList.add(permission); // still need to request
            }
        }
        if (requestList.isEmpty()) { // all permissions are granted
            finish();
            return;
        }
        if (pb.explainReasonBeforeRequest && (pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null)) {
            pb.explainReasonBeforeRequest = false;
            pb.deniedPermissions.addAll(requestList);
            if (pb.explainReasonCallbackWithBeforeParam != null) {
                // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                pb.explainReasonCallbackWithBeforeParam.onExplainReason(getExplainScope(), requestList, true);
            } else {
                pb.explainReasonCallback.onExplainReason(getExplainScope(), requestList);
            }
        } else {
            // Do the request at once. Always request all permissions no matter they are already granted or not, in case user turn them off in Settings.
            pb.requestNow(pb.normalPermissions, this);
        }
    }

    /**
     * If permission is denied by user and {@link ExplainScope#showRequestReasonDialog(List, String, String)} or {@link ForwardScope#showForwardToSettingsDialog(List, String, String)} is called,
     * when user clicked positive button, will call this method.
     * @param permissions   permissions to request again.
     */
    @Override
    public void requestAgain(List<String> permissions) {
        Set<String> permissionsToRequestAgain = new HashSet<>(pb.grantedPermissions);
        permissionsToRequestAgain.addAll(permissions);
        if (!permissionsToRequestAgain.isEmpty()) {
            pb.requestNow(permissionsToRequestAgain, this);
        } else {
            finish();
        }
    }

}
