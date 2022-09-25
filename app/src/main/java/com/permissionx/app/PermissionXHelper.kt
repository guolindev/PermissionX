package com.permissionx.app

import android.Manifest
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.permissionx.guolindev.PermissionX
import com.permissionx.guolindev.request.ExplainScope
import com.permissionx.guolindev.request.ForwardScope

/**
 * @author jaydroid
 * @version 1.0
 * @date 3/4/21
 */
object PermissionXHelper {

    private const val TAG = "PermissionXHelper"

    private val permissionMap = HashMap<String, List<String>>()

    init {
        initPermissionMap()
    }


    fun checkPermissions(
        permissionType: String,
        activity: FragmentActivity,
        callback: PermissionCallback
    ) {
        val per = getPermissionsByType(permissionType)
        if (per.isNullOrEmpty()) {
            return
        }
        PermissionX.init(activity)
            .permissions(per)
            .onExplainRequestReason { scope, deniedList ->
                Log.d(TAG, "onExplainRequestReason,deniedList:$deniedList")
                Log.d(TAG, "onExplainRequestReason,permissionType:$permissionType")
                if (permissionType == PermissionType.LOCATION) {

                    val isLocationPermissionGranted = isLocationGranted(activity)
                    Log.d(
                        TAG,
                        "onExplainRequestReason,isLocationPermissionGranted:$isLocationPermissionGranted"
                    )
                    //定位权限RequestReasonDialog二次确认才提示
//                    if (!isLocationPermissionGranted) {
                    setRequestReasonDialog(scope, deniedList)
//                    }

                } else {
                    setRequestReasonDialog(scope, deniedList)
                }
            }
            .onForwardToSettings { scope, deniedList ->
                Log.d(TAG, "onForwardToSettings,deniedList:$deniedList")
                val isLocationPermissionGranted = isLocationGranted(activity)
                Log.d(
                    TAG,
                    "onForwardToSettings,isLocationPermissionGranted:$isLocationPermissionGranted"
                )
                setForwardToSettingsDialog(scope, deniedList)
            }
            .request { allGranted, grantedList, deniedList ->
                Log.d(TAG, "request,grantedList:$grantedList")
                Log.d(TAG, "request,deniedList:$deniedList")
                if (allGranted) {
                    callback.allGranted()
                }

                if (allGranted) {
                    Toast.makeText(activity, "All permissions are granted", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(
                        activity,
                        "The following permissions are denied：$deniedList",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun getPermissionsByType(permissionType: String): List<String>? {
        return permissionMap[permissionType]
    }


    private fun setForwardToSettingsDialog(
        scope: ForwardScope,
        deniedList: List<String>?
    ) {
        scope.showForwardToSettingsDialog(
            deniedList,
            "您需要在设置中手动允许必要的权限",
            "确定",
            "取消"
        )
    }

    private fun setRequestReasonDialog(scope: ExplainScope, deniedList: List<String>?) {
        scope.showRequestReasonDialog(
            deniedList,
            "APP需要这些权限，请授权并继续",
            "确定",
            "取消"
        )
    }

    interface PermissionCallback {
        fun allGranted()
    }


    private fun initPermissionMap() {

        permissionMap[PermissionType.CAMERA] = listOf(Manifest.permission.CAMERA)

        permissionMap[PermissionType.SMS] = listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
        )

        permissionMap[PermissionType.PHONE] = listOf(Manifest.permission.CALL_PHONE)

        permissionMap[PermissionType.STORAGE] = listOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )

        permissionMap[PermissionType.STORAGE_CAMERA] = listOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
        //位置
        val locationList = arrayListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            locationList.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        //https://developer.android.com/training/location/permissions?hl=zh-cn
        permissionMap[PermissionType.LOCATION] = locationList


        //读写+相机+拨打电话
        val allList = arrayListOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.CALL_PHONE
        )
//        //定位
//        allList.addAll(locationList)

        permissionMap[PermissionType.ALL_NEED] = allList


    }

    fun isGranted(permission: String, activity: FragmentActivity): Boolean {
        return PermissionX.isGranted(activity, permission)
    }


    fun isLocationGranted(activity: Context): Boolean {
        var isAllGranted = true
        val per = getPermissionsByType(PermissionType.LOCATION)
        if (per.isNullOrEmpty()) {
            return false
        }
        per.forEach {
            val isGranted = PermissionX.isGranted(activity, it)
            Log.d(TAG, "isLocationGranted,it:$it")
            Log.d(TAG, "isLocationGranted,isGranted:$isGranted")
            isAllGranted = isGranted
        }
        return isAllGranted
    }

    /**
     * 请求类型
     */
    object PermissionType {

        /**
         * 电话
         */
        const val PHONE = "PHONE"

        /**
         * 短信
         */
        const val SMS = "SMS"

        /**
         * 相机
         */
        const val CAMERA = "CAMERA"

        /**
         * 读写
         */
        const val STORAGE = "STORAGE"

        /**
         * 读写+相机
         */
        const val STORAGE_CAMERA = "STORAGE_CAMERA"

        /**
         * 定位
         */
        const val LOCATION = "LOCATION"


        const val ALL_NEED = "ALL"

    }
}