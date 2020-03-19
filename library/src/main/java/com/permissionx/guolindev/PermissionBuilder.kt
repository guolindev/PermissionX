package com.permissionx.guolindev

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.fragment.app.FragmentActivity

/**
 * Basic interfaces for developers to use PermissionX functions.
 * @author guolin
 * @since 2019/11/17
 */
class PermissionBuilder internal constructor(private val activity: FragmentActivity, internal val allPermissions: List<String>) {

    private var explainReasonCallback: ExplainReasonCallback? = null

    private var forwardToSettingsCallback: ForwardToSettingsCallback? = null

    private var explainReasonBeforeRequest = false

    private var requestCallback: RequestCallback? = null

    internal val explainReasonScope = ExplainReasonScope(this)

    internal val forwardToSettingsScope = ForwardToSettingsScope(this)

    internal var showDialogCalled = false

    internal val grantedPermissions = HashSet<String>()

    internal val deniedPermissions = HashSet<String>()

    internal val permanentDeniedPermissions = HashSet<String>()

    internal val forwardPermissions = ArrayList<String>()

    fun onExplainRequestReason(block: ExplainReasonCallback): PermissionBuilder {
        explainReasonCallback = block
        return this
    }

    fun onForwardToSettings(block: ForwardToSettingsCallback): PermissionBuilder {
        forwardToSettingsCallback = block
        return this
    }

    fun explainReasonBeforeRequest(): PermissionBuilder {
        explainReasonBeforeRequest = true
        return this
    }

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
        if (requestList.isEmpty()) {
            callback(true, allPermissions, listOf())
            return
        }
        if (explainReasonBeforeRequest && explainReasonCallback != null) {
            explainReasonBeforeRequest = false
            deniedPermissions.addAll(requestList)
            explainReasonCallback?.let { explainReasonScope.it(requestList) }
        } else {
            request(allPermissions, callback)
        }
    }

    internal fun requestAgain(permissions: List<String>) {
        if (permissions.isEmpty()) {
            onPermissionDialogCancel()
            return
        }
        requestCallback?.let {
            // when request again, put all granted permissions into permission list again, in case user turn them off in settings.
            val permissionSet = HashSet(grantedPermissions)
            permissionSet.addAll(permissions)
            request(permissionSet.toList(), it)
        }
    }

    internal fun showHandlePermissionDialog(showReasonOrGoSettings: Boolean, permissions: List<String>, message: String, positiveText: String, negativeText: String? = null) {
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

    private fun request(permissions: List<String>, callback: RequestCallback) {
        getInvisibleFragment().requestNow(this, explainReasonCallback, forwardToSettingsCallback, callback, *permissions.toTypedArray())
    }

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

    private fun forwardToSettings(permissions: List<String>) {
        forwardPermissions.addAll(permissions)
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        getInvisibleFragment().startActivityForResult(intent, SETTINGS_CODE)
    }

    private fun onPermissionDialogCancel() {
        val deniedList = ArrayList<String>()
        deniedList.addAll(deniedPermissions)
        deniedList.addAll(permanentDeniedPermissions)
        requestCallback?.let {
            it(deniedList.isEmpty(), grantedPermissions.toList(), deniedList)
        }
    }

}