package com.permissionx.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.permissionx.guolindev.PermissionX
import com.permissionx.guolindev.callback.RequestCallback
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        makeCallBtn.setOnClickListener {
            PermissionX.init(this)
                .permissions(Manifest.permission.CAMERA, Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE)
                .explainReasonBeforeRequest()
                .onExplainRequestReason { scope, deniedList, beforeRequest ->
//                    if (beforeRequest) {
                        scope.showRequestReasonDialog(deniedList, "为了保证程序正常工作，请您同意以下权限申请", "我已明白")
//                    } else {
//                        val filteredList = deniedList.filter {
//                            it == Manifest.permission.CAMERA
//                        }
//                        scope.showRequestReasonDialog(filteredList, "摄像机权限是程序必须依赖的权限", "我已明白")
//                    }
                }
                .onForwardToSettings { scope, deniedList ->
                    scope.showForwardToSettingsDialog(deniedList, "您需要去应用程序设置当中手动开启权限", "我已明白")
                }
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        Toast.makeText(this, "所有申请的权限都已通过", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "您拒绝了如下权限：$deniedList", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun call() {
        try {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:10086")
            startActivity(intent)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

}
