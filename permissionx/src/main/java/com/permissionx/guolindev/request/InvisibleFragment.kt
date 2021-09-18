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
import android.annotation.TargetApi
import android.os.Build
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import com.permissionx.guolindev.PermissionX
import java.util.ArrayList

/**
 * An invisible fragment to embedded into activity for handling permission requests.
 * This is very lightweight. Will not affect your app's efficiency.
 *
 * @author guolin
 * @since 2019/11/2
 */
class InvisibleFragment : Fragment() {
    /**
     * Instance of PermissionBuilder.
     */
    private lateinit var pb: PermissionBuilder

    /**
     * Instance of current task.
     */
    private lateinit var task: ChainTask

    /**
     * Request permissions at once by calling [Fragment.requestPermissions],
     * and handle request result in ActivityCompat.OnRequestPermissionsResultCallback.
     *
     * @param permissionBuilder The instance of PermissionBuilder.
     * @param permissions       Permissions that you want to request.
     * @param chainTask         Instance of current task.
     */
    fun requestNow(permissionBuilder: PermissionBuilder, permissions: Set<String>, chainTask: ChainTask) {
        pb = permissionBuilder
        task = chainTask
        requestPermissions(permissions.toTypedArray(), REQUEST_NORMAL_PERMISSIONS)
    }

    /**
     * Request ACCESS_BACKGROUND_LOCATION at once by calling [Fragment.requestPermissions],
     * and handle request result in ActivityCompat.OnRequestPermissionsResultCallback.
     *
     * @param permissionBuilder The instance of PermissionBuilder.
     * @param chainTask         Instance of current task.
     */
    fun requestAccessBackgroundLocationNow(permissionBuilder: PermissionBuilder, chainTask: ChainTask) {
        pb = permissionBuilder
        task = chainTask
        requestPermissions(
            arrayOf(RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION),
            REQUEST_BACKGROUND_LOCATION_PERMISSION
        )
    }

    /**
     * Request SYSTEM_ALERT_WINDOW permission. On Android M and above, it's request by
     * Settings.ACTION_MANAGE_OVERLAY_PERMISSION with Intent.
     */
    @TargetApi(Build.VERSION_CODES.M)
    fun requestSystemAlertWindowPermissionNow(permissionBuilder: PermissionBuilder, chainTask: ChainTask) {
        pb = permissionBuilder
        task = chainTask
        if (!Settings.canDrawOverlays(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:${requireActivity().packageName}")
            startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION)
        } else {
            onRequestSystemAlertWindowPermissionResult()
        }
    }

    /**
     * Request WRITE_SETTINGS permission. On Android M and above, it's request by
     * Settings.ACTION_MANAGE_WRITE_SETTINGS with Intent.
     */
    @TargetApi(Build.VERSION_CODES.M)
    fun requestWriteSettingsPermissionNow(permissionBuilder: PermissionBuilder, chainTask: ChainTask) {
        pb = permissionBuilder
        task = chainTask
        if (!Settings.System.canWrite(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:${requireActivity().packageName}")
            startActivityForResult(intent, ACTION_WRITE_SETTINGS_PERMISSION)
        } else {
            onRequestWriteSettingsPermissionResult()
        }
    }

    /**
     * Request MANAGE_EXTERNAL_STORAGE permission. On Android R and above, it's request by
     * Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION with Intent.
     */
    fun requestManageExternalStoragePermissionNow(permissionBuilder: PermissionBuilder, chainTask: ChainTask) {
        pb = permissionBuilder
        task = chainTask
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivityForResult(intent, ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        } else {
            onRequestManageExternalStoragePermissionResult()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_NORMAL_PERMISSIONS) {
            onRequestNormalPermissionsResult(permissions, grantResults)
        } else if (requestCode == REQUEST_BACKGROUND_LOCATION_PERMISSION) {
            onRequestBackgroundLocationPermissionResult()
        }
    }

    /**
     * Handle the request result when user switch back from Settings.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // When user switch back from settings, just request again.
        if (checkForGC()) {
            when (requestCode) {
                FORWARD_TO_SETTINGS -> task.requestAgain(ArrayList(pb.forwardPermissions))
                ACTION_MANAGE_OVERLAY_PERMISSION -> onRequestSystemAlertWindowPermissionResult()
                ACTION_WRITE_SETTINGS_PERMISSION -> onRequestWriteSettingsPermissionResult()
                ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION -> onRequestManageExternalStoragePermissionResult()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (checkForGC()) {
            // Dismiss the showing dialog when InvisibleFragment destroyed for avoiding window leak problem.
            pb.currentDialog?.let {
                if (it.isShowing) {
                    it.dismiss()
                }
            }
        }
    }

    /**
     * Handle result of normal permissions request.
     */
    private fun onRequestNormalPermissionsResult(permissions: Array<String>?, grantResults: IntArray?) {
        if (checkForGC() && permissions != null && grantResults != null && permissions.size == grantResults.size) {
            // We can never holds granted permissions for safety, because user may turn some permissions off in settings.
            // So every time request, must request the already granted permissions again and refresh the granted permission set.
            pb.grantedPermissions.clear()
            val showReasonList: MutableList<String> = ArrayList() // holds denied permissions in the request permissions.
            val forwardList: MutableList<String> = ArrayList() // hold permanently denied permissions in the request permissions.
            for (i in permissions.indices) {
                val permission = permissions[i]
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    pb.grantedPermissions.add(permission)
                    // Remove granted permissions from deniedPermissions and permanentDeniedPermissions set in PermissionBuilder.
                    pb.deniedPermissions.remove(permission)
                    pb.permanentDeniedPermissions.remove(permission)
                } else {
                    // Denied permission can turn into permanent denied permissions, but permanent denied permission can not turn into denied permissions.
                    val shouldShowRationale = shouldShowRequestPermissionRationale(permission)
                    if (shouldShowRationale) {
                        showReasonList.add(permissions[i])
                        pb.deniedPermissions.add(permission)
                        // So there's no need to remove the current permission from permanentDeniedPermissions because it won't be there.
                    } else {
                        forwardList.add(permissions[i])
                        pb.permanentDeniedPermissions.add(permission)
                        // We must remove the current permission from deniedPermissions because it is permanent denied permission now.
                        pb.deniedPermissions.remove(permission)
                    }
                }
            }
            val deniedPermissions: MutableList<String> = ArrayList() // used to validate the deniedPermissions and permanentDeniedPermissions
            deniedPermissions.addAll(pb.deniedPermissions)
            deniedPermissions.addAll(pb.permanentDeniedPermissions)
            // maybe user can turn some permissions on in settings that we didn't request, so check the denied permissions again for safety.
            for (permission in deniedPermissions) {
                if (PermissionX.isGranted(context, permission)) {
                    pb.deniedPermissions.remove(permission)
                    pb.grantedPermissions.add(permission)
                }
            }
            val allGranted = pb.grantedPermissions.size == pb.normalPermissions.size
            if (allGranted) { // If all permissions are granted, finish current task directly.
                task.finish()
            } else {
                var shouldFinishTheTask = true // Indicate if we should finish the task
                // If explainReasonCallback is not null and there are denied permissions. Try the ExplainReasonCallback.
                if ((pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null) && showReasonList.isNotEmpty()) {
                    shouldFinishTheTask = false // shouldn't because ExplainReasonCallback handles it
                    if (pb.explainReasonCallbackWithBeforeParam != null) {
                        // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                        pb.explainReasonCallbackWithBeforeParam!!.onExplainReason(
                            task.explainScope, ArrayList(pb.deniedPermissions), false)
                    } else {
                        pb.explainReasonCallback!!.onExplainReason(task.explainScope, ArrayList(pb.deniedPermissions))
                    }
                    // store these permanently denied permissions or they will be lost when request again.
                    pb.tempPermanentDeniedPermissions.addAll(forwardList)
                } else if (pb.forwardToSettingsCallback != null && (forwardList.isNotEmpty() || pb.tempPermanentDeniedPermissions.isNotEmpty())) {
                    shouldFinishTheTask = false // shouldn't because ForwardToSettingsCallback handles it
                    pb.tempPermanentDeniedPermissions.clear() // no need to store them anymore once onForwardToSettings callback.
                    pb.forwardToSettingsCallback!!.onForwardToSettings(task.forwardScope, ArrayList(pb.permanentDeniedPermissions))
                }
                // If showRequestReasonDialog or showForwardToSettingsDialog is not called. We should finish the task.
                // There's case that ExplainReasonCallback or ForwardToSettingsCallback is called, but developer didn't invoke
                // showRequestReasonDialog or showForwardToSettingsDialog in the callback.
                // At this case and all other cases, task should be finished.
                if (shouldFinishTheTask || !pb.showDialogCalled) {
                    task.finish()
                }
                // Reset this value after each request. If we don't do this, developer invoke showRequestReasonDialog in ExplainReasonCallback
                // but didn't invoke showForwardToSettingsDialog in ForwardToSettingsCallback, the request process will be lost. Because the
                // previous showDialogCalled affect the next request logic.
                pb.showDialogCalled = false
            }
        }
    }

    /**
     * Handle result of ACCESS_BACKGROUND_LOCATION permission request.
     */
    private fun onRequestBackgroundLocationPermissionResult() {
        if (checkForGC()) {
            if (PermissionX.isGranted(context, RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION)) {
                pb.grantedPermissions.add(RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION)
                // Remove granted permissions from deniedPermissions and permanentDeniedPermissions set in PermissionBuilder.
                pb.deniedPermissions.remove(RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION)
                pb.permanentDeniedPermissions.remove(RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION)
                task.finish()
            } else {
                var goesToRequestCallback = true // Indicate if we should finish the task
                val shouldShowRationale = shouldShowRequestPermissionRationale(RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION)
                // If explainReasonCallback is not null and we should show rationale. Try the ExplainReasonCallback.
                if ((pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null) && shouldShowRationale) {
                    goesToRequestCallback = false // shouldn't because ExplainReasonCallback handles it
                    val permissionsToExplain: MutableList<String> = ArrayList()
                    permissionsToExplain.add(RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION)
                    if (pb.explainReasonCallbackWithBeforeParam != null) {
                        // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                        pb.explainReasonCallbackWithBeforeParam!!.onExplainReason(
                            task.explainScope, permissionsToExplain, false)
                    } else {
                        pb.explainReasonCallback!!.onExplainReason(task.explainScope, permissionsToExplain)
                    }
                } else if (pb.forwardToSettingsCallback != null && !shouldShowRationale) {
                    goesToRequestCallback = false // shouldn't because ForwardToSettingsCallback handles it
                    val permissionsToForward: MutableList<String> = ArrayList()
                    permissionsToForward.add(RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION)
                    pb.forwardToSettingsCallback!!.onForwardToSettings(task.forwardScope, permissionsToForward)
                }
                // If showRequestReasonDialog or showForwardToSettingsDialog is not called. We should finish the task.
                // There's case that ExplainReasonCallback or ForwardToSettingsCallback is called, but developer didn't invoke
                // showRequestReasonDialog or showForwardToSettingsDialog in the callback.
                // At this case and all other cases, task should be finished.
                if (goesToRequestCallback || !pb.showDialogCalled) {
                    task.finish()
                }
            }
        }
    }

    /**
     * Handle result of SYSTEM_ALERT_WINDOW permission request.
     */
    private fun onRequestSystemAlertWindowPermissionResult() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(context)) {
                task.finish()
            } else if (pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null) {
                if (pb.explainReasonCallbackWithBeforeParam != null) {
                    // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                    pb.explainReasonCallbackWithBeforeParam!!.onExplainReason(
                        task.explainScope, listOf(Manifest.permission.SYSTEM_ALERT_WINDOW), false)
                } else {
                    pb.explainReasonCallback!!.onExplainReason(
                        task.explainScope, listOf(Manifest.permission.SYSTEM_ALERT_WINDOW))
                }
            }
        } else {
            task.finish()
        }
    }

    /**
     * Handle result of WRITE_SETTINGS permission request.
     */
    private fun onRequestWriteSettingsPermissionResult() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(context)) {
                task.finish()
            } else if (pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null) {
                if (pb.explainReasonCallbackWithBeforeParam != null) {
                    // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                    pb.explainReasonCallbackWithBeforeParam!!.onExplainReason(
                        task.explainScope, listOf(Manifest.permission.WRITE_SETTINGS), false)
                } else {
                    pb.explainReasonCallback!!.onExplainReason(
                        task.explainScope, listOf(Manifest.permission.WRITE_SETTINGS))
                }
            }
        } else {
            task.finish()
        }
    }

    /**
     * Handle result of MANAGE_EXTERNAL_STORAGE permission request.
     */
    private fun onRequestManageExternalStoragePermissionResult() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                task.finish()
            } else if (pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null) {
                if (pb.explainReasonCallbackWithBeforeParam != null) {
                    // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                    pb.explainReasonCallbackWithBeforeParam!!.onExplainReason(
                        task.explainScope, listOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE), false)
                } else {
                    pb.explainReasonCallback!!.onExplainReason(
                        task.explainScope, listOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE))
                }
            }
        } else {
            task.finish()
        }
    }

    /**
     * On some phones, PermissionBuilder and ChainTask may become null under unpredictable occasions such as GC.
     * They should not be null at this time, so we can do nothing in this case.
     * @return PermissionBuilder and ChainTask are still alive or not. If not, we should not do any further logic.
     */
    private fun checkForGC(): Boolean {
        if (!::pb.isInitialized || !::task.isInitialized) {
            Log.w(
                "PermissionX",
                "PermissionBuilder and ChainTask should not be null at this time, so we can do nothing in this case."
            )
            return false
        }
        return true
    }

    companion object {
        /**
         * Code for request normal permissions.
         */
        const val REQUEST_NORMAL_PERMISSIONS = 1

        /**
         * Code for request ACCESS_BACKGROUND_LOCATION permissions. This permissions can't be requested with others over Android R.
         */
        const val REQUEST_BACKGROUND_LOCATION_PERMISSION = 2

        /**
         * Code for forward to settings page of current app.
         */
        const val FORWARD_TO_SETTINGS = 1

        /**
         * Code for request SYSTEM_ALERT_WINDOW permission.
         */
        const val ACTION_MANAGE_OVERLAY_PERMISSION = 2

        /**
         * Code for request WRITE_SETTINGS permission.
         */
        const val ACTION_WRITE_SETTINGS_PERMISSION = 3

        /**
         * Code for request MANAGE_EXTERNAL_STORAGE permission.
         */
        const val ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION = 4
    }
}