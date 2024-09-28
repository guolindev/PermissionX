package com.permissionx.guolindev.patch

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import java.util.UUID

/**
 * Delegate activity
 *
 * author: knightwood
 */
class DelegateActivity : FragmentActivity() {
    private lateinit var uuid: UUID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT


        uuid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("uuid", UUID::class.java)!!
        } else {
            intent.getSerializableExtra("uuid") as UUID
        }

        supportFragmentManager.setFragmentResultListener(
            PermissionDelegateHolder.REQUEST_KEY,
            this,
        ) { _, bundle ->
            val result = bundle.getBoolean(PermissionDelegateHolder.RESULT_KEY)
            if (result) // Hahaha, in fact it will never be false here
                release()
        }

        //execute permission request
        PermissionDelegateHolder.holder[uuid]!!.invoke(this)
    }

    private fun release() {
        PermissionDelegateHolder.holder.remove(uuid)
        finish()
    }

}