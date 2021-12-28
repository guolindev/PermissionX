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

import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.permissionx.guolindev.PermissionX
import java.util.*
import kotlin.collections.ArrayList

/**
 * Implementation for request normal permissions.
 * @author guolin
 * @since 2020/6/10
 */
internal class RequestNormalPermissions internal constructor(permissionBuilder: PermissionBuilder) :
    BaseTask(permissionBuilder) {

    override fun request() {
        val requestList = ArrayList<String>()
        for (permission in pb.normalPermissions) {
            if (PermissionX.isGranted(pb.activity, permission)) {
                pb.grantedPermissions.add(permission) // already granted
            } else {
                requestList.add(permission) // still need to request
            }
        }
        if (requestList.isEmpty()) { // all permissions are granted
            finish()
            return
        }
        if (shouldShowRequestPermissionRationale(
                pb.activity,
                requestList
            ) && (pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null)
        ) {
            pb.deniedPermissions.addAll(requestList)
            if (pb.explainReasonCallbackWithBeforeParam != null) {
                // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                pb.explainReasonCallbackWithBeforeParam!!.onExplainReason(explainScope, requestList, true)
            } else {
                pb.explainReasonCallback!!.onExplainReason(explainScope, requestList)
            }
        } else {
            // Do the request at once. Always request all permissions no matter they are already granted or not, in case user turn them off in Settings.
            pb.requestNow(pb.normalPermissions, this)
        }
    }

    private fun shouldShowRequestPermissionRationale(
        activity: FragmentActivity,
        requestList: ArrayList<String>
    ): Boolean {
        for (permission in requestList) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true
            }
        }
        return false
    }

    /**
     * If permission is denied by user and [ExplainScope.showRequestReasonDialog] or [ForwardScope.showForwardToSettingsDialog] is called,
     * when user clicked positive button, will call this method.
     * @param permissions   permissions to request again.
     */
    override fun requestAgain(permissions: List<String>) {
        val permissionsToRequestAgain: MutableSet<String> = HashSet(pb.grantedPermissions)
        permissionsToRequestAgain.addAll(permissions)
        if (permissionsToRequestAgain.isNotEmpty()) {
            pb.requestNow(permissionsToRequestAgain, this)
        } else {
            finish()
        }
    }
}