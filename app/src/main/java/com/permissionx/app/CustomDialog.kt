package com.permissionx.app

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.permissionx.guolindev.request.RationaleDialog
import kotlinx.android.synthetic.main.custom_dialog_layout.*

@TargetApi(30)
class CustomDialog(context: Context, val message: String, val permissions: List<String>) : RationaleDialog(context, R.style.CustomDialog) {

    private val permissionMap = mapOf(Manifest.permission.READ_CALENDAR to Manifest.permission_group.CALENDAR,
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
        Manifest.permission.BODY_SENSORS to Manifest.permission_group.SENSORS,
        Manifest.permission.SEND_SMS to Manifest.permission_group.SMS,
        Manifest.permission.RECEIVE_SMS to Manifest.permission_group.SMS,
        Manifest.permission.READ_SMS to Manifest.permission_group.SMS,
        Manifest.permission.RECEIVE_WAP_PUSH to Manifest.permission_group.SMS,
        Manifest.permission.RECEIVE_MMS to Manifest.permission_group.SMS,
        Manifest.permission.READ_EXTERNAL_STORAGE to Manifest.permission_group.STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE to Manifest.permission_group.STORAGE
    )

    private val groupMap = mapOf(Manifest.permission_group.CALENDAR to "日历权限",
        Manifest.permission_group.CALL_LOG to "通话记录权限",
        Manifest.permission_group.CAMERA to "摄像头权限",
        Manifest.permission_group.CONTACTS to "联系人权限",
        Manifest.permission_group.LOCATION to "定位权限",
        Manifest.permission_group.MICROPHONE to "麦克风权限",
        Manifest.permission_group.PHONE to "管理通话权限",
        Manifest.permission_group.SENSORS to "传感器权限",
        Manifest.permission_group.SMS to "短彩信权限",
        Manifest.permission_group.STORAGE to "存储权限"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.custom_dialog_layout)
        messageText.text = message
        buildPermissionsLayout()
        window?.let {
            val param = it.attributes
            val width = (context.resources.displayMetrics.widthPixels * 0.8).toInt()
            val height = param.height
            it.setLayout(width, height)
        }
    }

    override fun getNegativeButton(): View? {
        return negativeBtn
    }

    override fun getPositiveButton(): View {
        return positiveBtn
    }

    override fun getPermissionsToRequest(): List<String> {
        return permissions;
    }

    private fun buildPermissionsLayout() {
        for (permission in permissions) {
            val permissionGroup = permissionMap[permission]
            if (permissionGroup != null) {
                val textView = LayoutInflater.from(context).inflate(R.layout.permissions_item, permissionsLayout, false) as TextView
                textView.text = groupMap[permissionGroup]
                permissionsLayout.addView(textView)
            }
        }
    }

}