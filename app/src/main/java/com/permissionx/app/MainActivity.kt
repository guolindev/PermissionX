package com.permissionx.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        PermissionXHelper.checkPermissions(
            PermissionXHelper.PermissionType.LOCATION,
            this,
            object : PermissionXHelper.PermissionCallback {
                override fun allGranted() {
//                    initLocation()
//                    startLocation()
                }
            })
    }

}
