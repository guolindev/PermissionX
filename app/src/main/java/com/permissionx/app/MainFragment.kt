package com.permissionx.app

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.permissionx.guolindev.PermissionX
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = context!!
        makeRequestBtn.setOnClickListener {
            PermissionX.init(this)
                .permissions(Manifest.permission.READ_CONTACTS, Manifest.permission.CAMERA, Manifest.permission.CALL_PHONE)
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        Toast.makeText(activity, "All permissions are granted", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(activity, "These permissions are denied: $deniedList", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

}