package com.permissionx.guolindev

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * An open source Android library that makes handling runtime permissions extremely easy.
 *
 * The following snippet shows the simple usage:
 * ```kotlin
 *   PermissionX.init(activity)
 *      .permissions(Manifest.permission.READ_CONTACTS, Manifest.permission.CAMERA)
 *      .request { allGranted, grantedList, deniedList ->
 *          // handling the logic
 *      }
 *```
 *
 * @author guolin
 * @since 2019/11/2
 */
object PermissionX {

    /**
     * Init PermissionX to make everything prepare to work.
     *
     * @param activity An instance of FragmentActivity
     */
    fun init(activity: FragmentActivity) = PermissionCollection(activity)

    /**
     *  A helper function to check a permission is granted or not.
     *
     *  @param context Any context, will not be retained.
     *  @param permission Specific permission name to check. e.g. [android.Manifest.permission.CAMERA].
     *  @return True if this permission is granted, False otherwise.
     */
    fun isGranted(context: Context, permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

}

/**
 * An internal class to provide specific scope for passing permissions param.
 */
class PermissionCollection internal constructor(private val activity: FragmentActivity) {

    /**
     * All permissions that you want to request.
     * @param permissions A vararg param to pass permissions.
     */
    fun permissions(vararg permissions: String) = PermissionBuilder(activity, permissions.toList())

}