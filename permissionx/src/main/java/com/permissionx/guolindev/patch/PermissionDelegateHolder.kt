package com.permissionx.guolindev.patch

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import java.util.UUID

/**
 * Permission Delegate Holder
 *
 * author: knightwood
 *
 * in component activity, you can call this method to request permissions.
 */
object PermissionDelegateHolder {
    const val REQUEST_KEY = "RequestFinish"
    const val RESULT_KEY = "Result"

    internal val holder: MutableMap<UUID, (activity: FragmentActivity) -> Unit> = HashMap()

    /**
     * request permissions in a component activity.
     *
     * kotlin example:
     * ```
     * PermissionDelegateHolder.delegate(this) { activity ->
     *   PermissionX.init(activity)// use activity parameter to init PermissionX, do not use outside activity
     *      .permissions(Manifest.permission.READ_CONTACTS, Manifest.permission.CAMERA)
     *      .request { allGranted, grantedList, deniedList ->
     *          // handling the logic
     *      }
     * }
     *
     * ```
     *
     * java example:
     * ```
     * PermissionDelegateHolder.delegate(this, activity -> {
     *     PermissionX.init(activity) // use activity parameter to init PermissionX, do not use outside activity
     *        .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
     *        .request((allGranted, grantedList, deniedList) -> {
     *            if (allGranted) {
     *                Toast.makeText(activity, "All permissions are granted", Toast.LENGTH_SHORT).show();
     *            } else {
     *                Toast.makeText(activity, "The following permissions are denied：" + deniedList, Toast.LENGTH_SHORT).show();
     *            }
     *     });
     *     return null;
     * });
     * ```
     * @param ctx the context which can launch a new activity
     * @param block the request permissions block to be executed
     */
    @JvmStatic
    fun delegate(ctx: Context, block: (activity: FragmentActivity) -> Unit) {
        val uuid = UUID.randomUUID()
        holder[uuid] = block
        val intent = Intent(ctx, DelegateActivity::class.java)
        intent.putExtra("uuid", uuid)
        ctx.startActivity(intent)
    }
}