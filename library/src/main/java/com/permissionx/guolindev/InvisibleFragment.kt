package com.permissionx.guolindev

import android.content.Intent
import android.content.pm.PackageManager
import androidx.fragment.app.Fragment

/**
 * Invisible fragment to embedded into activity for handling permission requests.
 * @author guolin
 * @since 2019/11/2
 */
typealias RequestCallback = PermissionBuilder.(allGranted: Boolean, grantedList: List<String>, deniedList: List<String>) -> Unit

typealias ExplainReasonCallback = ExplainReasonScope.(deniedList: MutableList<String>) -> Unit

typealias ForwardToSettingsCallback = ForwardToSettingsScope.(deniedList: MutableList<String>) -> Unit

const val TAG = "InvisibleFragment"

const val PERMISSION_CODE = 1

const val SETTINGS_CODE = 2

class InvisibleFragment : Fragment() {

    private lateinit var permissionBuilder: PermissionBuilder

    private var explainReasonCallback: ExplainReasonCallback? = null

    private var forwardToSettingsCallback: ForwardToSettingsCallback? = null

    private lateinit var requestCallback: RequestCallback

    private lateinit var permissions: Array<out String>

    fun requestNow(builder: PermissionBuilder, cb1: ExplainReasonCallback?, cb2: ForwardToSettingsCallback?, cb3: RequestCallback, vararg p: String) {
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
            val showReasonList = ArrayList<String>()
            val forwardList = ArrayList<String>()
            for ((index, result) in grantResults.withIndex()) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    grantedList.add(permissions[index])
                    permissionBuilder.deniedPermissions.remove(permissions[index])
                    permissionBuilder.permanentDeniedPermissions.remove(permissions[index])
                } else {
                    val shouldShowReason = shouldShowRequestPermissionRationale(permissions[index])
                    if (shouldShowReason) {
                        // denied permission can turn into permanent denied permissions
                        showReasonList.add(permissions[index])
                        permissionBuilder.deniedPermissions.add(permissions[index])
                    } else {
                        // permanent denied permission can not turn into denied permissions
                        forwardList.add(permissions[index])
                        permissionBuilder.permanentDeniedPermissions.add(permissions[index])
                        permissionBuilder.deniedPermissions.remove(permissions[index])
                    }
                }
            }
            permissionBuilder.grantedPermissions.clear() // clear first in case user turn some permissions off in settings.
            permissionBuilder.grantedPermissions.addAll(grantedList)
            val allGranted = permissionBuilder.grantedPermissions.size == permissionBuilder.allPermissions.size
            if (allGranted) {
                permissionBuilder.requestCallback(true, permissionBuilder.allPermissions, listOf())
            } else {
                if (explainReasonCallback != null && showReasonList.isNotEmpty()) {
                    explainReasonCallback?.let { permissionBuilder.explainReasonScope.it(showReasonList) }
                } else if (forwardToSettingsCallback != null && forwardList.isNotEmpty()) {
                    forwardToSettingsCallback?.let { permissionBuilder.forwardToSettingsScope.it(forwardList) }
                }
                if (!permissionBuilder.showDialogCalled) {
                    val deniedList = ArrayList<String>()
                    deniedList.addAll(permissionBuilder.deniedPermissions)
                    deniedList.addAll(permissionBuilder.permanentDeniedPermissions)
                    permissionBuilder.requestCallback(false, permissionBuilder.grantedPermissions.toList(), deniedList)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SETTINGS_CODE) {
            permissionBuilder.requestAgain(permissionBuilder.forwardPermissions)
        }
    }

}