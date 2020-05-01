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

package com.permissionx.guolindev

import android.content.Intent
import android.content.pm.PackageManager
import androidx.fragment.app.Fragment

/**
 * An invisible fragment to embedded into activity for handling permission requests.
 * This is very lightweight. Will not affect your app's efficiency.
 *
 * @author guolin
 * @since 2019/11/2
 */

/**
 * Callback for [PermissionBuilder.request] method.
 */
typealias RequestCallback = (allGranted: Boolean, grantedList: List<String>, deniedList: List<String>) -> Unit

/**
 * Callback for [PermissionBuilder.onExplainRequestReason] method.
 */
typealias ExplainReasonCallback = ExplainReasonScope.(deniedList: MutableList<String>) -> Unit

/**
 * Callback for [PermissionBuilder.onExplainRequestReason] method, but with beforeRequest param.
 */
typealias ExplainReasonCallback2 = ExplainReasonScope.(deniedList: MutableList<String>, beforeRequest: Boolean) -> Unit

/**
 * Callback for [PermissionBuilder.onForwardToSettings] method.
 */
typealias ForwardToSettingsCallback = ForwardToSettingsScope.(deniedList: MutableList<String>) -> Unit

const val TAG = "InvisibleFragment"

const val PERMISSION_CODE = 1

const val SETTINGS_CODE = 2

class InvisibleFragment : Fragment() {

    /**
     * The instance of PermissionBuilder which holds granted permissions, denied permissions and permanent denied permissions.
     */
    private lateinit var permissionBuilder: PermissionBuilder

    /**
     * The callback for [PermissionBuilder.onExplainRequestReason] method. Maybe null.
     */
    private var explainReasonCallback: ExplainReasonCallback? = null

    /**
     * The callback for [PermissionBuilder.onExplainRequestReason] method, but with beforeRequest param. Maybe null.
     */
    private var explainReasonCallback2: ExplainReasonCallback2? = null

    /**
     * The callback for [PermissionBuilder.onForwardToSettings] method. Maybe null.
     */
    private var forwardToSettingsCallback: ForwardToSettingsCallback? = null

    /**
     * The callback for [PermissionBuilder.request] method. Can not be null.
     */
    private lateinit var requestCallback: RequestCallback

    /**
     * Request permissions at once by calling [Fragment.requestPermissions], and handle request result in [onRequestPermissionsResult].
     *
     * @param builder
     *          The instance of PermissionBuilder.
     * @param cb1
     *          The callback for [PermissionBuilder.onExplainRequestReason] method. Maybe null.
     * @param cb2
     *          The callback for [PermissionBuilder.onExplainRequestReason] method but with beforeRequest param. Maybe null.
     * @param cb3
     *          The callback for [PermissionBuilder.onForwardToSettings] method. Maybe null.
     * @param cb4
     *          The callback for [PermissionBuilder.request] method. Can not be null.
     * @param permissions
     *          Permissions that you want to request.
     */
    fun requestNow(builder: PermissionBuilder, cb1: ExplainReasonCallback?, cb2: ExplainReasonCallback2?, cb3: ForwardToSettingsCallback?, cb4: RequestCallback, vararg permissions: String) {
        permissionBuilder = builder
        explainReasonCallback = cb1
        explainReasonCallback2 = cb2
        forwardToSettingsCallback = cb3
        requestCallback = cb4
        requestPermissions(permissions, PERMISSION_CODE)
    }

    /**
     * Handle the request results.
     * There may be 3 callbacks: [ExplainReasonCallback], [ForwardToSettingsCallback] and [RequestCallback].
     * Only one callback can be called normally(But not always true, see the exception in below codes).
     * The priority is [ExplainReasonCallback] -> [ForwardToSettingsCallback] -> [RequestCallback].
     * Always try to call the higher priority callback. If that's not possible, goes to the lower one.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_CODE) {
            val grantedList = ArrayList<String>() // holds granted permissions in the request permissions
            val showReasonList = ArrayList<String>() // holds denied permissions in the request permissions.
            val forwardList = ArrayList<String>() // hold permanently denied permissions in the request permissions.
            for ((index, result) in grantResults.withIndex()) { // iterate all granted results
                if (result == PackageManager.PERMISSION_GRANTED) {
                    grantedList.add(permissions[index])
                    // Remove granted permissions from deniedPermissions and permanentDeniedPermissions set in PermissionBuilder.
                    permissionBuilder.deniedPermissions.remove(permissions[index])
                    permissionBuilder.permanentDeniedPermissions.remove(permissions[index])
                } else {
                    // Denied permission can turn into permanent denied permissions, but permanent denied permission can not turn into denied permissions.
                    val shouldShowReason = shouldShowRequestPermissionRationale(permissions[index])
                    if (shouldShowReason) {
                        showReasonList.add(permissions[index])
                        permissionBuilder.deniedPermissions.add(permissions[index])
                        // So there's no need to remove the current permission from permanentDeniedPermissions because it won't be there.
                    } else {
                        forwardList.add(permissions[index])
                        permissionBuilder.permanentDeniedPermissions.add(permissions[index])
                        // We must remove the current permission from deniedPermissions because it is permanent denied permission now.
                        permissionBuilder.deniedPermissions.remove(permissions[index])
                    }
                }
            }
            // We can never holds granted permissions for safety, because user may turn some permissions off in Settings.
            // So every time request, must request the already granted permissions again and refresh the granted permission set.
            permissionBuilder.grantedPermissions.clear()
            permissionBuilder.grantedPermissions.addAll(grantedList)
            val allGranted = permissionBuilder.grantedPermissions.size == permissionBuilder.allPermissions.size
            if (allGranted) { // If all permissions are granted, call RequestCallback directly.
                requestCallback(true, permissionBuilder.allPermissions, listOf())
            } else {
                var goesToRequestCallback = true // If there's need goes to RequestCallback
                // If explainReasonCallback is not null and there're denied permissions. Try the ExplainReasonCallback.
                if ((explainReasonCallback != null || explainReasonCallback2 != null) && showReasonList.isNotEmpty()) {
                    goesToRequestCallback = false // No need cause ExplainReasonCallback handles it
                    explainReasonCallback2?.let { // callback ExplainReasonCallback2 prior to ExplainReasonCallback
                        permissionBuilder.explainReasonScope.it(showReasonList, false)
                    } ?:
                    explainReasonCallback?.let { permissionBuilder.explainReasonScope.it(showReasonList) }
                }
                // If forwardToSettingsCallback is not null and there're permanently denied permissions. Try the ForwardToSettingsCallback.
                else if (forwardToSettingsCallback != null && forwardList.isNotEmpty()) {
                    goesToRequestCallback = false // No need cause ForwardToSettingsCallback handles it
                    forwardToSettingsCallback?.let { permissionBuilder.forwardToSettingsScope.it(forwardList) }
                }
                // If showRequestReasonDialog or showForwardToSettingsDialog is not called. Try the RequestCallback.
                // There's case that ExplainReasonCallback or ForwardToSettingsCallback is called, but developer didn't invoke
                // showRequestReasonDialog or showForwardToSettingsDialog in the callback.
                // At this case and all other cases, RequestCallback will be called.
                if (goesToRequestCallback || !permissionBuilder.showDialogCalled) {
                    val deniedList = ArrayList<String>()
                    deniedList.addAll(permissionBuilder.deniedPermissions)
                    deniedList.addAll(permissionBuilder.permanentDeniedPermissions)
                    requestCallback(false, permissionBuilder.grantedPermissions.toList(), deniedList)
                }
            }
        }
    }

    /**
     * Handle the request result when user switch back from Settings.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SETTINGS_CODE) {
            // When user switch back from settings, just request again.
            permissionBuilder.requestAgain(permissionBuilder.forwardPermissions)
        }
    }

}