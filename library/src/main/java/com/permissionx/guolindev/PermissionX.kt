package com.permissionx.guolindev

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * An open source Android library that makes handling runtime permissions extremely easy.
 *
 * Calling the following snippet for simple usage:
 * ```kotlin
 *   PermissionX.init(activity)
 *      .permissions(Manifest.permission.READ_CONTACTS, Manifest.permission.CAMERA)
 *      .request { allGranted, grantedList, deniedList ->
 *          // do the logic
 *      }
 *```
 *
 * @author guolin
 * @since 2019/11/2
 */
object PermissionX {

    fun init(activity: FragmentActivity) = PermissionCollection(activity)

    fun isGranted(context: Context, permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

}

class PermissionCollection internal constructor(private val activity: FragmentActivity) {

    fun permissions(vararg permissions: String) = PermissionBuilder(activity, permissions.toList())

}