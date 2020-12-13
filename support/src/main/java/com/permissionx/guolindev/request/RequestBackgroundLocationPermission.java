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

import com.permissionx.guolindev.PermissionX;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation for request ACCESS_BACKGROUND_LOCATION permission.
 * @author guolin
 * @since 2020/6/10
 */
public class RequestBackgroundLocationPermission extends BaseTask {

    /**
     * Define the const to compat with system lower than Q.
     */
    public static final String ACCESS_BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION";

    RequestBackgroundLocationPermission(PermissionBuilder permissionBuilder) {
        super(permissionBuilder);
    }

    @Override
    public void request() {
        if (pb.requireBackgroundLocationPermission) {
            boolean accessFindLocationGranted = PermissionX.isGranted(pb.activity, Manifest.permission.ACCESS_FINE_LOCATION);
            boolean accessCoarseLocationGranted = PermissionX.isGranted(pb.activity, Manifest.permission.ACCESS_COARSE_LOCATION);
            if (accessFindLocationGranted || accessCoarseLocationGranted) {
                if (pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null) {
                    List<String> requestList = new ArrayList<>();
                    requestList.add(ACCESS_BACKGROUND_LOCATION);
                    if (pb.explainReasonCallbackWithBeforeParam != null) {
                        // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                        pb.explainReasonCallbackWithBeforeParam.onExplainReason(explainReasonScope, requestList, true);
                    } else {
                        pb.explainReasonCallback.onExplainReason(explainReasonScope, requestList);
                    }
                } else {
                    // no implementation of explainReasonCallback, so we have to request ACCESS_BACKGROUND_LOCATION without explanation.
                    requestAgain(null);
                }
                return;
            }
        }
        // shouldn't request ACCESS_BACKGROUND_LOCATION at this time, so we call onResult() to finish this task.
        finish();
    }

    @Override
    public void requestAgain(List<String> permissions) {
        // don't care what the permissions param is, always request ACCESS_BACKGROUND_LOCATION
        pb.requestAccessBackgroundLocationNow(this);
    }

}
