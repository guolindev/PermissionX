package com.permissionx.guolindev

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 *
 * @author guolin
 * @since 2019/11/2
 */
object PermissionX {

    fun init(activity: FragmentActivity) = PermissionCollection(activity)

    fun isGranted(context: Context, permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

}

class PermissionCollection(private val activity: FragmentActivity) {

    fun permissions(vararg permissions: String) = PermissionBuilder(activity, permissions.toList())

}