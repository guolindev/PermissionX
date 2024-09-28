package com.permissionx.app

import android.Manifest
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.permissionx.app.databinding.ActivityExampleComponentBinding
import com.permissionx.guolindev.PermissionX
import com.permissionx.guolindev.patch.PermissionDelegateHolder

class ExampleComponentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = ActivityExampleComponentBinding.inflate(layoutInflater)
        layout.makeRequestBtn.setOnClickListener {

            PermissionX.init(this) {
                it.permissions(Manifest.permission.CAMERA)
                    .request { allGranted, grantedList, deniedList ->
                        if (allGranted) {
                            Toast.makeText(this, "All permissions are granted", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            Toast.makeText(
                                this,
                                "The following permissions are denied：$deniedList",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
            // or directly use PermissionDelegateHolder
//            requestPerms()
        }
        setContentView(layout.root)
    }

    fun requestPerms() {
        PermissionDelegateHolder.delegate(this) {
            PermissionX.init(it)
                .permissions(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.RECORD_AUDIO,
//                    Manifest.permission.READ_CALENDAR,
//                    Manifest.permission.READ_CALL_LOG,
//                    Manifest.permission.READ_CONTACTS,
//                    Manifest.permission.READ_PHONE_STATE,
//                    Manifest.permission.BODY_SENSORS,
//                    Manifest.permission.ACTIVITY_RECOGNITION,
//                    Manifest.permission.SEND_SMS,
//                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                .setDialogTintColor(Color.parseColor("#1972e8"), Color.parseColor("#8ab6f5"))
                .onExplainRequestReason { scope, deniedList, beforeRequest ->
                    val message = "PermissionX needs following permissions to continue"
                    scope.showRequestReasonDialog(deniedList, message, "Allow", "Deny")
//                    val message = "Please allow the following permissions in settings"
//                    val dialog = CustomDialogFragment(message, deniedList)
//                    scope.showRequestReasonDialog(dialog)
                }
                .onForwardToSettings { scope, deniedList ->
                    val message = "Please allow following permissions in settings"
                    scope.showForwardToSettingsDialog(deniedList, message, "Allow", "Deny")
                }
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        Toast.makeText(it, "All permissions are granted", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(
                            it,
                            "The following permissions are denied：$deniedList",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }
}