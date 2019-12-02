package com.permissionx.guolindev

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * Basic interfaces for developers to use PermissionX functions.
 * @author guolin
 * @since 2019/11/17
 */
class PermissionBuilder internal constructor(private val activity: FragmentActivity) {

    private var explainReasonCallback: Callback? = null

    private var forwardToSettingsCallback: Callback? = null

    private var requestCallback: RequestCallback? = null

    private var showRequestReasonAtFirst = false

    fun shouldExplainRequestReason(block: Callback): PermissionBuilder {
        explainReasonCallback = block
        return this
    }

    fun shouldForwardToSettings(block: Callback): PermissionBuilder {
        forwardToSettingsCallback = block
        return this
    }

    fun explainRequestReasonAtFirst(): PermissionBuilder {
        showRequestReasonAtFirst = true
        return this
    }

    fun showRequestReasonDialog(permissions: List<String>, message: String, positiveText: String) {
        AlertDialog.Builder(activity).apply {
            setMessage(message)
            setCancelable(false)
            setPositiveButton(positiveText) { _, _ ->
                requestAgain(permissions)
            }
            show()
        }
    }

    fun showForwardToSettingsDialog(message: String, positiveText: String, negativeText: String? = null) {
        AlertDialog.Builder(activity).apply {
            setMessage(message)
            setCancelable(false)
            setPositiveButton(positiveText) { _, _ ->
                forwardToSettings()
            }
            negativeText?.let {
                setNegativeButton(it, null)
            }
            show()
        }
    }

    fun requestAgain(permissions: List<String>) {
        requestCallback?.let {
            request(*permissions.toTypedArray(), callback = it)
        }
    }

    fun forwardToSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        getInvisibleFragment().startActivityForResult(intent, SETTINGS_CODE)
    }

    fun request(vararg permissions: String, callback: RequestCallback) {
        requestCallback = callback
        if (showRequestReasonAtFirst && explainReasonCallback != null) {
            showRequestReasonAtFirst = false
            explainReasonCallback?.let { it(permissions.toMutableList()) }
        } else {
            getInvisibleFragment().requestNow(this, explainReasonCallback, forwardToSettingsCallback, callback, *permissions)
        }
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

}