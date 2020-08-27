package com.permissionx.guolindev.dialog

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.permissionx.guolindev.R
import com.permissionx.guolindev.request.RationaleDialog
import kotlinx.android.synthetic.main.permissionx_default_dialog_layout.*
import kotlinx.android.synthetic.main.permissionx_permission_item.view.*

/**
 * Default rationale dialog to show if developers did not implement their own custom rationale dialog.
 *
 * @author guolin
 * @since 2020/8/27
 */
@TargetApi(23)
class DefaultDialog(context: Context, val permissions: List<String>, val message: String, val positiveText: String, val negativeText: String?) : RationaleDialog(context, R.style.PermissionXDefaultDialog) {

    private val permissionMap = mapOf(
        Manifest.permission.READ_CALENDAR to Manifest.permission_group.CALENDAR,
        Manifest.permission.WRITE_CALENDAR to Manifest.permission_group.CALENDAR,
        Manifest.permission.READ_CALL_LOG to Manifest.permission_group.CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG to Manifest.permission_group.CALL_LOG,
        Manifest.permission.PROCESS_OUTGOING_CALLS to Manifest.permission_group.CALL_LOG,
        Manifest.permission.CAMERA to Manifest.permission_group.CAMERA,
        Manifest.permission.READ_CONTACTS to Manifest.permission_group.CONTACTS,
        Manifest.permission.WRITE_CONTACTS to Manifest.permission_group.CONTACTS,
        Manifest.permission.GET_ACCOUNTS to Manifest.permission_group.CONTACTS,
        Manifest.permission.ACCESS_FINE_LOCATION to Manifest.permission_group.LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION to Manifest.permission_group.LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION to Manifest.permission_group.LOCATION,
        Manifest.permission.RECORD_AUDIO to Manifest.permission_group.MICROPHONE,
        Manifest.permission.READ_PHONE_STATE to Manifest.permission_group.PHONE,
        Manifest.permission.READ_PHONE_NUMBERS to Manifest.permission_group.PHONE,
        Manifest.permission.CALL_PHONE to Manifest.permission_group.PHONE,
        Manifest.permission.ANSWER_PHONE_CALLS to Manifest.permission_group.PHONE,
        Manifest.permission.ADD_VOICEMAIL to Manifest.permission_group.PHONE,
        Manifest.permission.USE_SIP to Manifest.permission_group.PHONE,
        Manifest.permission.ACCEPT_HANDOVER to Manifest.permission_group.PHONE,
        Manifest.permission.BODY_SENSORS to Manifest.permission_group.SENSORS,
        Manifest.permission.ACTIVITY_RECOGNITION to Manifest.permission_group.ACTIVITY_RECOGNITION,
        Manifest.permission.SEND_SMS to Manifest.permission_group.SMS,
        Manifest.permission.RECEIVE_SMS to Manifest.permission_group.SMS,
        Manifest.permission.READ_SMS to Manifest.permission_group.SMS,
        Manifest.permission.RECEIVE_WAP_PUSH to Manifest.permission_group.SMS,
        Manifest.permission.RECEIVE_MMS to Manifest.permission_group.SMS,
        Manifest.permission.READ_EXTERNAL_STORAGE to Manifest.permission_group.STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE to Manifest.permission_group.STORAGE,
        Manifest.permission.ACCESS_MEDIA_LOCATION to Manifest.permission_group.STORAGE
    )

    private val groupSet = HashSet<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.permissionx_default_dialog_layout)
        messageText.text = message
        positiveBtn.text = positiveText
        if (negativeText != null) {
            negativeLayout.visibility = View.VISIBLE
            negativeBtn.text = negativeText
        } else {
            negativeLayout.visibility = View.GONE
        }
        buildPermissionsLayout()
        setupWindow()
    }

    /**
     * Provide the positive button instance to continue requesting.
     * @return Positive button instance to continue requesting.
     */
    override fun getPositiveButton(): View {
        return positiveBtn
    }

    /**
     * Provide the negative button instance to abort requesting.
     * This is alternative. If negativeText is null we just return null, means all these permissions are necessary.
     * @return Negative button instance to abort requesting. Or null if all these permissions are necessary.
     */
    override fun getNegativeButton(): View? {
        return negativeText?.let {
            return negativeBtn
        }
    }

    /**
     * Provide the permissions to request again.
     * @return Permissions to request again.
     */
    override fun getPermissionsToRequest(): List<String> {
        return permissions
    }

    /**
     * Add every permission that need to explain the request reason to the dialog.
     * But we only need to add the permission group. So if there're two permissions belong to one group, only one item will be added to the dialog.
     */
    private fun buildPermissionsLayout() {
        for (permission in permissions) {
            val permissionGroup = permissionMap[permission]
            if (permissionGroup != null && !groupSet.contains(permissionGroup)) {
                val layout = LayoutInflater.from(context).inflate(R.layout.permissionx_permission_item, permissionsLayout, false) as LinearLayout
                layout.permissionText.text = context.getString(context.packageManager.getPermissionGroupInfo(permissionGroup, 0).labelRes)
                layout.permissionIcon.setImageResource(context.packageManager.getPermissionGroupInfo(permissionGroup, 0).icon)
                permissionsLayout.addView(layout)
                groupSet.add(permissionGroup)
            }
        }
    }

    /**
     * Setup dialog window to show. Control the different window size in portrait and landscape mode.
     */
    private fun setupWindow() {
        val width = context.resources.displayMetrics.widthPixels
        val height = context.resources.displayMetrics.heightPixels
        if (width < height) {
            // now we are in portrait
            window?.let {
                val param = it.attributes
                it.setGravity(Gravity.CENTER)
                param.width = (width * 0.86).toInt()
                it.attributes = param
            }
        } else {
            // now we are in landscape
            window?.let {
                val param = it.attributes
                it.setGravity(Gravity.CENTER)
                param.width = (width * 0.6).toInt()
                it.attributes = param
            }
        }
    }

}