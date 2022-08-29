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
package com.permissionx.guolindev.request

import android.Manifest
import android.os.Build
import com.permissionx.guolindev.PermissionX

/**
 * Implementation for request ACCESS_BACKGROUND_LOCATION permission.
 * @author guolin
 * @since 2022/8/26
 */
internal class RequestBodySensorsBackgroundPermission internal constructor(permissionBuilder: PermissionBuilder)
    : BaseTask(permissionBuilder) {

    override fun request() {
        if (pb.shouldRequestBodySensorsBackgroundPermission()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                // If app runs under Android T, there's no BODY_SENSORS_BACKGROUND permissions.
                // We remove it from request list, but will append it to the request callback as denied permission.
                pb.specialPermissions.remove(BODY_SENSORS_BACKGROUND)
                pb.permissionsWontRequest.add(BODY_SENSORS_BACKGROUND)
                finish()
                return
            }
            if (PermissionX.isGranted(pb.activity, BODY_SENSORS_BACKGROUND)) {
                // BODY_SENSORS_BACKGROUND has already granted, we can finish this task now.
                finish()
                return
            }
            val bodySensorGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                PermissionX.isGranted(pb.activity, Manifest.permission.BODY_SENSORS)
            } else {
                false
            }
            if (bodySensorGranted) {
                if (pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null) {
                    val requestList = mutableListOf(BODY_SENSORS_BACKGROUND)
                    if (pb.explainReasonCallbackWithBeforeParam != null) {
                        // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                        pb.explainReasonCallbackWithBeforeParam!!.onExplainReason(explainScope, requestList, true)
                    } else {
                        pb.explainReasonCallback!!.onExplainReason(explainScope, requestList)
                    }
                } else {
                    // No implementation of explainReasonCallback, so we have to request BODY_SENSORS_BACKGROUND without explanation.
                    requestAgain(emptyList())
                }
                return
            }
        }
        // Shouldn't request BODY_SENSORS_BACKGROUND at this time, so we call finish() to finish this task.
        finish()
    }

    override fun requestAgain(permissions: List<String>) {
        // Don't care what the permissions param is, always request BODY_SENSORS_BACKGROUND.
        pb.requestBodySensorsBackgroundPermissionNow(this)
    }

    companion object {
        /**
         * Define the const to compat with system lower than T.
         */
        const val BODY_SENSORS_BACKGROUND = "android.permission.BODY_SENSORS_BACKGROUND"
    }
}