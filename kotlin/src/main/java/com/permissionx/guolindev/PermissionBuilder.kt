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

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.fragment.app.FragmentActivity

/**
 * More APIs for developers to control PermissionX functions.
 *
 * @author guolin
 * @since 2019/11/17
 */
class PermissionBuilder internal constructor(
    private val activity: FragmentActivity,
    internal val allPermissions: List<String>
) {

    /**
     * The callback for onExplainRequestReason() method. Maybe null.
     */
    private var explainReasonCallback: ExplainReasonCallback? = null

    /**
     * The callback for onExplainRequestReason() method with beforeRequest param. Maybe null.
     */
    private var explainReasonCallback2: ExplainReasonCallback2? = null

    /**
     * The callback for onForwardToSettings() method. Maybe null.
     */
    private var forwardToSettingsCallback: ForwardToSettingsCallback? = null

    /**
     * The callback for request() method. Should not be null, but allow it null here for callback not initialized.
     */
    private var requestCallback: RequestCallback? = null

    /**
     * Indicates should PermissionX explain request reason before request.
     */
    private var explainReasonBeforeRequest = false

    /**
     * Provide specific scopes for explainReasonCallback for specific functions to call.
     */
    internal val explainReasonScope = ExplainReasonScope(this)

    /**
     * Provide specific scopes for forwardToSettingsCallback for specific functions to call.
     */
    internal val forwardToSettingsScope = ForwardToSettingsScope(this)

    /**
     * Indicates [ExplainReasonScope.showRequestReasonDialog] or [ForwardToSettingsScope.showForwardToSettingsDialog] is called in [onExplainRequestReason] or [onForwardToSettings] callback.
     * If not called, requestCallback will be called by PermissionX automatically.
     */
    internal var showDialogCalled = false

    /**
     * Holds permissions that have already granted in the requested permissions.
     */
    internal val grantedPermissions = HashSet<String>()

    /**
     * Holds permissions that have been denied in the requested permissions.
     */
    internal val deniedPermissions = HashSet<String>()

    /**
     * Holds permissions that have been permanently denied in the requested permissions. (Deny and never ask again)
     */
    internal val permanentDeniedPermissions = HashSet<String>()

    /**
     * Holds permissions which should forward to Settings to allow them.
     * Not all permanently denied permissions should forward to Settings. Only the ones developer think they are necessary should.
     */
    internal val forwardPermissions = ArrayList<String>()

    /**
     * Called when permissions need to explain request reason.
     * Typically every time user denies your request would call this method.
     * If you chained [explainReasonBeforeRequest], this method might run before permission request.
     *
     * @param callback
     *          Callback with permissions denied by user.
     */
    fun onExplainRequestReason(callback: ExplainReasonCallback): PermissionBuilder {
        explainReasonCallback = callback
        return this
    }

    /**
     * Called when permissions need to explain request reason.
     * Typically every time user denies your request would call this method.
     * If you chained [explainReasonBeforeRequest], this method might run before permission request.
     * beforeRequest param would tell you this method is currently before or after permission request.
     *
     * @param callback
     *          Callback with permissions denied by user.
     */
    fun onExplainRequestReason(callback: ExplainReasonCallback2): PermissionBuilder {
        explainReasonCallback2 = callback
        return this
    }

    /**
     * Called when permissions need to forward to Settings for allowing.
     * Typically user denies your request and checked never ask again would call this method.
     * Remember [onExplainRequestReason] is always prior to [onForwardToSettings].
     * If [onExplainRequestReason] is called, [onForwardToSettings] will not be called in the same request time.
     *
     * @param callback
     *          Callback with permissions denied and checked never ask again by user.
     */
    fun onForwardToSettings(callback: ForwardToSettingsCallback): PermissionBuilder {
        forwardToSettingsCallback = callback
        return this
    }

    /**
     * If you need to show request permission rationale, chain this method in your request syntax.
     * [onExplainRequestReason] will be called before permission request.
     */
    fun explainReasonBeforeRequest(): PermissionBuilder {
        explainReasonBeforeRequest = true
        return this
    }

    /**
     * Request permissions at once, and handle request result in the callback.
     *
     * @param callback
     *          Callback with 3 params. allGranted, grantedList, deniedList.
     */
    fun request(callback: RequestCallback) {
        requestCallback = callback
        val requestList = ArrayList<String>()
        for (permission in allPermissions) {
            if (PermissionX.isGranted(activity, permission)) {
                grantedPermissions.add(permission)
            } else {
                requestList.add(permission)
            }
        }
        if (requestList.isEmpty()) { // all permissions are granted
            callback(true, allPermissions, listOf())
            return
        }
        if (explainReasonBeforeRequest && (explainReasonCallback != null || explainReasonCallback2 != null)) { // should show request permission rationale before request
            explainReasonBeforeRequest = false
            deniedPermissions.addAll(requestList)
            explainReasonCallback2?.let { // callback ExplainReasonCallback2 prior to ExplainReasonCallback
                explainReasonScope.it(requestList, true)
            } ?: explainReasonCallback?.let { explainReasonScope.it(requestList) }
        } else {
            // Do the request at once. Always request all permissions no matter they are already granted or not, in case user turn them off in Settings.
            requestNow(allPermissions, callback)
        }
    }

    /**
     * This method is internal, and should not be called by developer.
     *
     * If permission is denied by user and [ExplainReasonScope.showRequestReasonDialog] or [ForwardToSettingsScope.showForwardToSettingsDialog] is called,
     * when user clicked positive button, will call this [requestAgain] method.
     *
     * @param permissions
     *          Permissions to request again.
     */
    internal fun requestAgain(permissions: List<String>) {
        if (permissions.isEmpty()) {
            onPermissionDialogCancel()
            return
        }
        requestCallback?.let {
            // when request again, put all granted permissions into permission list again, in case user turn them off in settings.
            val permissionSet = HashSet(grantedPermissions)
            permissionSet.addAll(permissions)
            requestNow(permissionSet.toList(), it)
        }
    }

    /**
     * This method is internal, and should not be called by developer.
     *
     * Show a dialog to user and  explain why these permissions are necessary.
     *
     * @param showReasonOrGoSettings
     *          Indicates should show explain reason or forward to Settings.
     * @param permissions
     *          Permissions to request again.
     * @param message
     *          Message that explain to user why these permissions are necessary.
     * @param positiveText
     *          Positive text on the positive button to request again.
     * @param negativeText
     *          Negative text on the negative button. Maybe null if this dialog should not be canceled.
     */
    internal fun showHandlePermissionDialog(
        showReasonOrGoSettings: Boolean,
        permissions: List<String>,
        message: String,
        positiveText: String,
        negativeText: String? = null
    ) {
        showDialogCalled = true
        val filteredPermissions = permissions.filter {
            !grantedPermissions.contains(it) && allPermissions.contains(it)
        }
        if (filteredPermissions.isEmpty()) {
            onPermissionDialogCancel()
            return
        }
        AlertDialog.Builder(activity).apply {
            setMessage(message)
            setCancelable(negativeText.isNullOrBlank())
            setPositiveButton(positiveText) { _, _ ->
                if (showReasonOrGoSettings) {
                    requestAgain(filteredPermissions)
                } else {
                    forwardToSettings(filteredPermissions)
                }
            }
            negativeText?.let {
                setNegativeButton(it) { _, _ ->
                    onPermissionDialogCancel()
                }
            }
            show()
        }
    }

    /**
     * Request permissions at once in the fragment.
     *
     * @param permissions
     *          Permissions that you want to request.
     * @param callback
     *          Callback with 3 params. allGranted, grantedList, deniedList.
     */
    private fun requestNow(permissions: List<String>, callback: RequestCallback) {
        getInvisibleFragment().requestNow(
            this,
            explainReasonCallback,
            explainReasonCallback2,
            forwardToSettingsCallback,
            callback,
            *permissions.toTypedArray()
        )
    }

    /**
     * Get the invisible fragment in activity for request permissions.
     * If there is no invisible fragment, add one into activity.
     * Don't worry. This is very lightweight.
     */
    private fun getInvisibleFragment(): InvisibleFragment {
        val fragmentManager = activity.supportFragmentManager
        val existedFragment = fragmentManager.findFragmentByTag(TAG)
        return if (existedFragment != null) {
            existedFragment as InvisibleFragment
        } else {
            val invisibleFragment = InvisibleFragment()
            fragmentManager.beginTransaction().add(invisibleFragment, TAG).commitNow()
            invisibleFragment
        }
    }

    /**
     * Go to your app's Settings page to let user turn on the necessary permissions.
     *
     * @param permissions
     *          Permissions which are necessary.
     */
    private fun forwardToSettings(permissions: List<String>) {
        forwardPermissions.addAll(permissions)
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        getInvisibleFragment().startActivityForResult(intent, SETTINGS_CODE)
    }

    /**
     * If permission is denied by user and [ExplainReasonScope.showRequestReasonDialog] or [ForwardToSettingsScope.showForwardToSettingsDialog] is called,
     * when user clicked negative button, will call this [onPermissionDialogCancel] method.
     */
    private fun onPermissionDialogCancel() {
        val deniedList = ArrayList<String>()
        deniedList.addAll(deniedPermissions)
        deniedList.addAll(permanentDeniedPermissions)
        requestCallback?.let {
            it(deniedList.isEmpty(), grantedPermissions.toList(), deniedList)
        }
    }

}