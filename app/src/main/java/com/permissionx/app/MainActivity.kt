package com.permissionx.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.permissionx.guolindev.PermissionX
import com.permissionx.guolindev.RequestCallback
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        makeCallBtn.setOnClickListener {
            PermissionX.init(this)
                .permissions(Manifest.permission.READ_CONTACTS, Manifest.permission.CAMERA, Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALENDAR, Manifest.permission.READ_EXTERNAL_STORAGE)
                .shouldExplainRequestReason { deniedList ->
                    val newList = deniedList.filter {
                        it in listOf(Manifest.permission.CAMERA, Manifest.permission.CALL_PHONE)
                        true
                    }
//                    showRequestReasonDialog(newList, "必须同意权限$newList", "OK", "Cancel")
                }
                .shouldForwardToSettings { deniedList ->
                    val newList = deniedList.filter {
                        it == Manifest.permission.CALL_PHONE
                        true
                    }
//                    showForwardToSettingsDialog(newList, "您需要去设置中开启权限$newList" ,"OK", "Cancel")
                }
                .explainReasonBeforeRequest()
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        Toast.makeText(this@MainActivity, "allGranted", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "denied $deniedList", Toast.LENGTH_LONG).show()
                    }
                }
//                .request(object : RequestCallback {
//                    override fun onGranted(allGranted: Boolean, grantedList: List<String>): Boolean {
//                        if (grantedList.contains(Manifest.permission.CALL_PHONE)) {
//                            call()
//                            return true
//                        }
//                        return false
//                    }
//
//                    override fun onDenied(deniedList: List<String>) {
//                        Toast.makeText(this@MainActivity, "denied $deniedList", Toast.LENGTH_LONG).show()
//                    }
//                })
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
