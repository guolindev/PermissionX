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
        makeCallBtn.setOnClickListener {
            PermissionX.init(this)
                .permissions(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CONTACTS, Manifest.permission.READ_EXTERNAL_STORAGE)
                .explainReasonBeforeRequest()
                .onExplainRequestReason { scope, deniedList, beforeRequest ->
                    val message = "PermissionX需要您同意以下权限才能正常使用"
                    val dialog = CustomDialog(context!!, message, deniedList)
                    scope.showRequestReasonDialog(dialog)
                }
                .onForwardToSettings { scope, deniedList ->
                    val message = "您需要去设置中手动开启以下权限"
                    val dialog = CustomDialog(context!!, message, deniedList)
                    scope.showForwardToSettingsDialog(dialog)
                }
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        Toast.makeText(activity, "所有申请的权限都已通过", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(activity, "您拒绝了如下权限：$deniedList", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

}