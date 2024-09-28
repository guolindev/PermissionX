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
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.permissionx.guolindev.PermissionX
import com.permissionx.guolindev.callback.ExplainReasonCallback
import com.permissionx.guolindev.callback.ExplainReasonCallbackWithBeforeParam
import com.permissionx.guolindev.callback.ForwardToSettingsCallback
import com.permissionx.guolindev.callback.RequestCallback
import com.permissionx.guolindev.dialog.DefaultDialog
import com.permissionx.guolindev.dialog.RationaleDialog
import com.permissionx.guolindev.dialog.RationaleDialogFragment
import com.permissionx.guolindev.patch.PermissionDelegateHolder
import java.util.*

/**
 * More APIs for developers to control PermissionX functions.
 *
 * @author guolin
 * @since 2019/11/17
 */
class PermissionBuilder(
    fragmentActivity: FragmentActivity?,
    fragment: Fragment?,
    normalPermissions: MutableSet<String>,
    specialPermissions: MutableSet<String>
) {

    /**
     * Instance of activity for everything.
     */
    lateinit var activity: FragmentActivity

    /**
     * Instance of fragment for everything as an alternative choice for activity.
     */
    private var fragment: Fragment? = null

    /**
     * The custom tint color to set on the DefaultDialog in light theme.
     */
    private var lightColor = -1

    /**
     * The custom tint color to set on the DefaultDialog in dark theme.
     */
    private var darkColor = -1

    /**
     * The origin request orientation of the current Activity. We need to restore it when
     * permission request finished.
     */
    private var originRequestOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    /**
     * Get the FragmentManager if it's in Activity, or the ChildFragmentManager if it's in Fragment.
     * @return The FragmentManager to operate Fragment.
     */
    private val fragmentManager: FragmentManager
        get() {
            return fragment?.childFragmentManager ?: activity.supportFragmentManager
        }

    /**
     * Get the invisible fragment in activity for request permissions.
     * If there is no invisible fragment, add one into activity.
     * Don't worry. This is very lightweight.
     */
    private val invisibleFragment: InvisibleFragment
        get() {
            val existedFragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG)
            return if (existedFragment != null) {
                existedFragment as InvisibleFragment
            } else {
                val invisibleFragment = InvisibleFragment()
                fragmentManager.beginTransaction()
                    .add(invisibleFragment, FRAGMENT_TAG)
                    .commitNowAllowingStateLoss()
                invisibleFragment
            }
        }

    /**
     * Instance of the current dialog that shows to user.
     * We need to dismiss this dialog when InvisibleFragment destroyed.
     */
    @JvmField
    var currentDialog: Dialog? = null

    /**
     * Normal runtime permissions that app want to request.
     */
    @JvmField
    var normalPermissions: MutableSet<String>

    /**
     * Special permissions that we need to handle by special case.
     * Such as SYSTEM_ALERT_WINDOW, WRITE_SETTINGS and MANAGE_EXTERNAL_STORAGE.
     */
    @JvmField
    var specialPermissions: MutableSet<String>

    /**
     * Indicates should PermissionX explain request reason before request.
     */
    @JvmField
    var explainReasonBeforeRequest = false

    /**
     * Indicates [ExplainScope.showRequestReasonDialog] or [ForwardScope.showForwardToSettingsDialog]
     * is called in [.onExplainRequestReason] or [.onForwardToSettings] callback.
     * If not called, requestCallback will be called by PermissionX automatically.
     */
    @JvmField
    var showDialogCalled = false

    /**
     * Some permissions shouldn't request will be stored here. And notify back to user when request finished.
     */
    @JvmField
    var permissionsWontRequest: MutableSet<String> = LinkedHashSet()

    /**
     * Holds permissions that have already granted in the requested permissions.
     */
    @JvmField
    var grantedPermissions: MutableSet<String> = LinkedHashSet()

    /**
     * Holds permissions that have been denied in the requested permissions.
     */
    @JvmField
    var deniedPermissions: MutableSet<String> = LinkedHashSet()

    /**
     * Holds permissions that have been permanently denied in the requested permissions.
     * (Deny and never ask again)
     */
    @JvmField
    var permanentDeniedPermissions: MutableSet<String> = LinkedHashSet()

    /**
     * When we request multiple permissions. Some are denied, some are permanently denied.
     * Denied permissions will be callback first.
     * And the permanently denied permissions will store in this tempPermanentDeniedPermissions.
     * They will be callback once no more denied permissions exist.
     */
    @JvmField
    var tempPermanentDeniedPermissions: MutableSet<String> = LinkedHashSet()

    @JvmField
    var tempReadMediaPermissions: MutableSet<String> = LinkedHashSet()

    /**
     * Holds permissions which should forward to Settings to allow them.
     * Not all permanently denied permissions should forward to Settings.
     * Only the ones developer think they are necessary should.
     */
    @JvmField
    var forwardPermissions: MutableSet<String> = LinkedHashSet()

    /**
     * The callback for [.request] method. Can not be null.
     */
    @JvmField
    var requestCallback: RequestCallback? = null

    /**
     * The callback for [.onExplainRequestReason] method. Maybe null.
     */
    @JvmField
    var explainReasonCallback: ExplainReasonCallback? = null

    /**
     * The callback for [.onExplainRequestReason] method, but with beforeRequest param. Maybe null.
     */
    @JvmField
    var explainReasonCallbackWithBeforeParam: ExplainReasonCallbackWithBeforeParam? = null

    /**
     * The callback for [.onForwardToSettings] method. Maybe null.
     */
    @JvmField
    var forwardToSettingsCallback: ForwardToSettingsCallback? = null

    /**
     * Get the targetSdkVersion of current app.
     *
     * @return The targetSdkVersion of current app.
     */
    val targetSdkVersion: Int
        get() = activity.applicationInfo.targetSdkVersion

    /**
     * Called when permissions need to explain request reason.
     * Typically every time user denies your request would call this method.
     * If you chained [.explainReasonBeforeRequest], this method might run before permission request.
     *
     * @param callback Callback with permissions denied by user.
     * @return PermissionBuilder itself.
     */
    fun onExplainRequestReason(callback: ExplainReasonCallback?): PermissionBuilder {
        explainReasonCallback = callback
        return this
    }

    /**
     * Called when permissions need to explain request reason.
     * Typically every time user denies your request would call this method.
     * If you chained [.explainReasonBeforeRequest], this method might run before permission request.
     * beforeRequest param would tell you this method is currently before or after permission request.
     *
     * @param callback Callback with permissions denied by user.
     * @return PermissionBuilder itself.
     */
    fun onExplainRequestReason(callback: ExplainReasonCallbackWithBeforeParam?): PermissionBuilder {
        explainReasonCallbackWithBeforeParam = callback
        return this
    }

    /**
     * Called when permissions need to forward to Settings for allowing.
     * Typically user denies your request and checked never ask again would call this method.
     * Remember [.onExplainRequestReason] is always prior to this method.
     * If [.onExplainRequestReason] is called, this method will not be called in the same request time.
     *
     * @param callback Callback with permissions denied and checked never ask again by user.
     * @return PermissionBuilder itself.
     */
    fun onForwardToSettings(callback: ForwardToSettingsCallback?): PermissionBuilder {
        forwardToSettingsCallback = callback
        return this
    }

    /**
     * If you need to show request permission rationale, chain this method in your request syntax.
     * [.onExplainRequestReason] will be called before permission request.
     *
     * @return PermissionBuilder itself.
     */
    fun explainReasonBeforeRequest(): PermissionBuilder {
        explainReasonBeforeRequest = true
        return this
    }

    /**
     * Set the tint color to the default rationale dialog.
     * @param lightColor
     * Used in light theme. A color value in the form 0xAARRGGBB. Do not pass a resource ID.
     * To get a color value from a resource ID, call getColor.
     * @param darkColor
     * Used in dark theme. A color value in the form 0xAARRGGBB. Do not pass a resource ID.
     * To get a color value from a resource ID, call getColor.
     * @return PermissionBuilder itself.
     */
    fun setDialogTintColor(lightColor: Int, darkColor: Int): PermissionBuilder {
        this.lightColor = lightColor
        this.darkColor = darkColor
        return this
    }

    /**
     * Request permissions at once, and handle request result in the callback.
     *
     * @param callback Callback with 3 params. allGranted, grantedList, deniedList.
     */
    fun request(callback: RequestCallback?) {
        requestCallback = callback
        startRequest()
    }

    /**
     * This method is internal, and should not be called by developer.
     *
     *
     * Show a dialog to user and  explain why these permissions are necessary.
     *
     * @param chainTask              Instance of current task.
     * @param showReasonOrGoSettings Indicates should show explain reason or forward to Settings.
     * @param permissions            Permissions to request again.
     * @param message                Message that explain to user why these permissions are necessary.
     * @param positiveText           Positive text on the positive button to request again.
     * @param negativeText           Negative text on the negative button. Maybe null if this dialog should not be canceled.
     */
    fun showHandlePermissionDialog(
        chainTask: ChainTask,
        showReasonOrGoSettings: Boolean,
        permissions: List<String>,
        message: String,
        positiveText: String,
        negativeText: String?
    ) {
        val defaultDialog = DefaultDialog(
            activity,
            permissions,
            message,
            positiveText,
            negativeText,
            lightColor,
            darkColor
        )
        showHandlePermissionDialog(chainTask, showReasonOrGoSettings, defaultDialog)
    }

    /**
     * This method is internal, and should not be called by developer.
     *
     *
     * Show a dialog to user and  explain why these permissions are necessary.
     *
     * @param chainTask              Instance of current task.
     * @param showReasonOrGoSettings Indicates should show explain reason or forward to Settings.
     * @param dialog                 Dialog to explain to user why these permissions are necessary.
     */
    fun showHandlePermissionDialog(
        chainTask: ChainTask,
        showReasonOrGoSettings: Boolean,
        dialog: RationaleDialog
    ) {
        showDialogCalled = true
        val permissions = dialog.permissionsToRequest
        if (permissions.isEmpty()) {
            chainTask.finish()
            return
        }
        currentDialog = dialog
        dialog.show()
        if (dialog is DefaultDialog && dialog.isPermissionLayoutEmpty()) {
            // No valid permission to show on the dialog.
            // We call dismiss instead.
            dialog.dismiss()
            chainTask.finish()
        }
        val positiveButton = dialog.positiveButton
        val negativeButton = dialog.negativeButton
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        positiveButton.isClickable = true
        positiveButton.setOnClickListener {
            dialog.dismiss()
            if (showReasonOrGoSettings) {
                chainTask.requestAgain(permissions)
            } else {
                forwardToSettings(permissions)
            }
        }
        if (negativeButton != null) {
            negativeButton.isClickable = true
            negativeButton.setOnClickListener {
                dialog.dismiss()
                chainTask.finish()
            }
        }
        currentDialog?.setOnDismissListener {
            currentDialog = null
        }
    }

    /**
     * This method is internal, and should not be called by developer.
     *
     *
     * Show a DialogFragment to user and  explain why these permissions are necessary.
     *
     * @param chainTask              Instance of current task.
     * @param showReasonOrGoSettings Indicates should show explain reason or forward to Settings.
     * @param dialogFragment         DialogFragment to explain to user why these permissions are necessary.
     */
    fun showHandlePermissionDialog(
        chainTask: ChainTask,
        showReasonOrGoSettings: Boolean,
        dialogFragment: RationaleDialogFragment
    ) {
        showDialogCalled = true
        val permissions = dialogFragment.permissionsToRequest
        if (permissions.isEmpty()) {
            chainTask.finish()
            return
        }
        dialogFragment.showNow(fragmentManager, "PermissionXRationaleDialogFragment")
        val positiveButton = dialogFragment.positiveButton
        val negativeButton = dialogFragment.negativeButton
        dialogFragment.isCancelable = false
        positiveButton.isClickable = true
        positiveButton.setOnClickListener {
            dialogFragment.dismiss()
            if (showReasonOrGoSettings) {
                chainTask.requestAgain(permissions)
            } else {
                forwardToSettings(permissions)
            }
        }
        if (negativeButton != null) {
            negativeButton.isClickable = true
            negativeButton.setOnClickListener(View.OnClickListener {
                dialogFragment.dismiss()
                chainTask.finish()
            })
        }
    }

    /**
     * Request permissions at once in the fragment.
     *
     * @param permissions Permissions that you want to request.
     * @param chainTask   Instance of current task.
     */
    fun requestNow(permissions: Set<String>, chainTask: ChainTask) {
        invisibleFragment.requestNow(this, permissions, chainTask)
    }

    /**
     * Request ACCESS_BACKGROUND_LOCATION permission at once in the fragment.
     *
     * @param chainTask Instance of current task.
     */
    fun requestAccessBackgroundLocationPermissionNow(chainTask: ChainTask) {
        invisibleFragment.requestAccessBackgroundLocationPermissionNow(this, chainTask)
    }

    /**
     * Request SYSTEM_ALERT_WINDOW permission at once in the fragment.
     *
     * @param chainTask Instance of current task.
     */
    fun requestSystemAlertWindowPermissionNow(chainTask: ChainTask) {
        invisibleFragment.requestSystemAlertWindowPermissionNow(this, chainTask)
    }

    /**
     * Request WRITE_SETTINGS permission at once in the fragment.
     *
     * @param chainTask Instance of current task.
     */
    fun requestWriteSettingsPermissionNow(chainTask: ChainTask) {
        invisibleFragment.requestWriteSettingsPermissionNow(this, chainTask)
    }

    /**
     * Request MANAGE_EXTERNAL_STORAGE permission at once in the fragment.
     *
     * @param chainTask Instance of current task.
     */
    fun requestManageExternalStoragePermissionNow(chainTask: ChainTask) {
        invisibleFragment.requestManageExternalStoragePermissionNow(this, chainTask)
    }

    /**
     * Request REQUEST_INSTALL_PACKAGES permission at once in the fragment.
     *
     * @param chainTask Instance of current task.
     */
    fun requestInstallPackagePermissionNow(chainTask: ChainTask) {
        invisibleFragment.requestInstallPackagesPermissionNow(this, chainTask)
    }

    /**
     * Request notification permission at once in the fragment.
     *
     * @param chainTask Instance of current task.
     */
    fun requestNotificationPermissionNow(chainTask: ChainTask) {
        invisibleFragment.requestNotificationPermissionNow(this, chainTask)
    }

    /**
     * Request BODY_SENSORS_BACKGROUND permission at once in the fragment.
     *
     * @param chainTask Instance of current task.
     */
    fun requestBodySensorsBackgroundPermissionNow(chainTask: ChainTask) {
        invisibleFragment.requestBodySensorsBackgroundPermissionNow(this, chainTask)
    }

    /**
     * Should we request ACCESS_BACKGROUND_LOCATION permission or not.
     *
     * @return True if specialPermissions contains ACCESS_BACKGROUND_LOCATION permission, false otherwise.
     */
    fun shouldRequestBackgroundLocationPermission(): Boolean {
        return specialPermissions.contains(RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION)
    }

    /**
     * Should we request SYSTEM_ALERT_WINDOW permission or not.
     *
     * @return True if specialPermissions contains SYSTEM_ALERT_WINDOW permission, false otherwise.
     */
    fun shouldRequestSystemAlertWindowPermission(): Boolean {
        return specialPermissions.contains(Manifest.permission.SYSTEM_ALERT_WINDOW)
    }

    /**
     * Should we request WRITE_SETTINGS permission or not.
     *
     * @return True if specialPermissions contains WRITE_SETTINGS permission, false otherwise.
     */
    fun shouldRequestWriteSettingsPermission(): Boolean {
        return specialPermissions.contains(Manifest.permission.WRITE_SETTINGS)
    }

    /**
     * Should we request MANAGE_EXTERNAL_STORAGE permission or not.
     *
     * @return True if specialPermissions contains MANAGE_EXTERNAL_STORAGE permission, false otherwise.
     */
    fun shouldRequestManageExternalStoragePermission(): Boolean {
        return specialPermissions.contains(RequestManageExternalStoragePermission.MANAGE_EXTERNAL_STORAGE)
    }

    /**
     * Should we request REQUEST_INSTALL_PACKAGES permission or not.
     *
     * @return True if specialPermissions contains REQUEST_INSTALL_PACKAGES permission, false otherwise.
     */
    fun shouldRequestInstallPackagesPermission(): Boolean {
        return specialPermissions.contains(RequestInstallPackagesPermission.REQUEST_INSTALL_PACKAGES)
    }

    /**
     * Should we request the specific special permission or not.
     *
     * @return True if specialPermissions contains POST_NOTIFICATIONS permission, false otherwise.
     */
    fun shouldRequestNotificationPermission(): Boolean {
        return specialPermissions.contains(PermissionX.permission.POST_NOTIFICATIONS)
    }

    /**
     * Should we request the specific special permission or not.
     *
     * @return True if specialPermissions contains BODY_SENSORS_BACKGROUND permission, false otherwise.
     */
    fun shouldRequestBodySensorsBackgroundPermission(): Boolean {
        return specialPermissions.contains(RequestBodySensorsBackgroundPermission.BODY_SENSORS_BACKGROUND)
    }

    private fun startRequest() {
        // Lock the orientation when requesting permissions, or callback maybe missed due to
        // activity destroyed.
        lockOrientation()

        // Build the request chain. RequestNormalPermissions runs first, then RequestBackgroundLocationPermission runs.
        val requestChain = RequestChain()
        requestChain.addTaskToChain(RequestNormalPermissions(this))
        requestChain.addTaskToChain(RequestBackgroundLocationPermission(this))
        requestChain.addTaskToChain(RequestSystemAlertWindowPermission(this))
        requestChain.addTaskToChain(RequestWriteSettingsPermission(this))
        requestChain.addTaskToChain(RequestManageExternalStoragePermission(this))
        requestChain.addTaskToChain(RequestInstallPackagesPermission(this))
        requestChain.addTaskToChain(RequestNotificationPermission(this))
        requestChain.addTaskToChain(RequestBodySensorsBackgroundPermission(this))
        requestChain.runTask()
    }

    /**
     * Remove the InvisibleFragment from current FragmentManager.
     */
    private fun removeInvisibleFragment() {
        // notify that the request is finished.
        fragmentManager.setFragmentResult(PermissionDelegateHolder.REQUEST_KEY, Bundle().apply {
            putBoolean(PermissionDelegateHolder.RESULT_KEY, true)
        })
        val existedFragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG)
        if (existedFragment != null) {
            fragmentManager.beginTransaction().remove(existedFragment).commitNowAllowingStateLoss()
        }
    }

    /**
     * Restore the screen orientation. Activity just behave as before locked.
     * Android O has bug that only full screen activity can request orientation,
     * so we need to exclude Android O.
     */
    private fun restoreOrientation() {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
            activity.requestedOrientation = originRequestOrientation
        }
    }

    /**
     * Lock the screen orientation. Activity couldn't rotate with sensor.
     * Android O has bug that only full screen activity can request orientation,
     * so we need to exclude Android O.
     */
    @SuppressLint("SourceLockedOrientationActivity")
    private fun lockOrientation() {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
            originRequestOrientation = activity.requestedOrientation
            val orientation = activity.resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
        }
    }

    /**
     * Go to your app's Settings page to let user turn on the necessary permissions.
     *
     * @param permissions Permissions which are necessary.
     */
    private fun forwardToSettings(permissions: List<String>) {
        forwardPermissions.clear()
        forwardPermissions.addAll(permissions)
        invisibleFragment.forwardToSettings()
    }

    internal fun endRequest() {
        // Remove the InvisibleFragment from current Activity after request finished.
        removeInvisibleFragment()
        // Restore the orientation after request finished since it's locked before.
        restoreOrientation()
    }

    companion object {
        /**
         * TAG of InvisibleFragment to find and create.
         */
        private const val FRAGMENT_TAG = "InvisibleFragment"
    }

    init {
        if (fragmentActivity != null) {
            activity = fragmentActivity
        }
        // activity and fragment must not be null at same time
        if (fragmentActivity == null && fragment != null) {
            activity = fragment.requireActivity()
        }
        this.fragment = fragment
        this.normalPermissions = normalPermissions
        this.specialPermissions = specialPermissions
    }
}