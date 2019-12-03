package com.permissionx.guolindev

import android.content.Intent
import android.content.pm.PackageManager
import androidx.fragment.app.Fragment

/**
 * Invisible fragment to embedded into activity for handling permission requests.
 * @author guolin
 * @since 2019/11/2
 */
typealias Callback = PermissionBuilder.(MutableList<String>) -> Unit

typealias RequestCallback = PermissionBuilder.(Boolean, MutableList<String>, MutableList<String>) -> Unit

const val TAG = "InvisibleFragment"

const val PERMISSION_CODE = 2019

const val SETTINGS_CODE = 2020

class InvisibleFragment : Fragment() {

    private lateinit var permissionBuilder: PermissionBuilder

    private var explainReasonCallback: Callback? = null

    private var forwardToSettingsCallback: Callback? = null

    private lateinit var requestCallback: RequestCallback

    private lateinit var permissions: Array<out String>

    fun requestNow(builder: PermissionBuilder,
                   cb1: Callback?,
                   cb2: Callback?,
                   cb3: RequestCallback,
                   vararg p: String) {
        permissionBuilder = builder
        explainReasonCallback = cb1
        forwardToSettingsCallback = cb2
        requestCallback = cb3
        permissions = p
        requestPermissions(permissions, PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_CODE) {
            val grantedList = ArrayList<String>()
            val deniedList = ArrayList<String>()
            val showReasonList = ArrayList<String>()
            val forwardToSettingsList = ArrayList<String>()
            for ((index, result) in grantResults.withIndex()) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    grantedList.add(permissions[index])
                } else {
                    val shouldShowReason = shouldShowRequestPermissionRationale(permissions[index])
                    if (explainReasonCallback != null && shouldShowReason) {
                        showReasonList.add(permissions[index])
                    } else if (forwardToSettingsCallback != null && !shouldShowReason) {
                        forwardToSettingsList.add(permissions[index])
                    } else {
                        deniedList.add(permissions[index])
                    }
                }
            }
            val allGranted = grantedList.size == permissions.size
            if (showReasonList.isNotEmpty()) {
                explainReasonCallback?.let { permissionBuilder.it(showReasonList) }
            }
            if (forwardToSettingsList.isNotEmpty()) {
                forwardToSettingsCallback?.let { permissionBuilder.it(forwardToSettingsList) }
            }
            if (grantResults.isNotEmpty() || deniedList.isNotEmpty()) {
                permissionBuilder.requestCallback(allGranted, grantedList, deniedList)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SETTINGS_CODE) {
            permissionBuilder.requestAgain(permissions.toMutableList())
        }
    }

}