package com.permissionx.guolindev.dialog

import android.content.Context
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.permissionx.guolindev.R
import kotlinx.android.synthetic.main.permissionx_default_dialog_layout.*
import kotlinx.android.synthetic.main.permissionx_permission_item.view.*

/**
 * Default rationale dialog to show if developers did not implement their own custom rationale dialog.
 *
 * @author guolin
 * @since 2020/8/27
 */
class DefaultDialog(context: Context,
    private val permissions: List<String>,
    private val message: String,
    private val positiveText: String,
    private val negativeText: String?,
    private val lightColor: Int,
    private val darkColor: Int
) : RationaleDialog(context, R.style.PermissionXDefaultDialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.permissionx_default_dialog_layout)
        setupText()
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
     * Setup text and text color on the dialog.
     */
    private fun setupText() {
        messageText.text = message
        positiveBtn.text = positiveText
        if (negativeText != null) {
            negativeLayout.visibility = View.VISIBLE
            negativeBtn.text = negativeText
        } else {
            negativeLayout.visibility = View.GONE
        }
        if (isDarkTheme()) {
            if (darkColor != -1) {
                positiveBtn.setTextColor(darkColor)
                negativeBtn.setTextColor(darkColor)
            }
        } else {
            if (lightColor != -1) {
                positiveBtn.setTextColor(lightColor)
                negativeBtn.setTextColor(lightColor)
            }
        }
    }

    /**
     * Add every permission that need to explain the request reason to the dialog.
     * But we only need to add the permission group. So if there're two permissions belong to one group, only one item will be added to the dialog.
     */
    private fun buildPermissionsLayout() {
        val groupSet = HashSet<String>()
        val currentVersion = Build.VERSION.SDK_INT
        for (permission in permissions) {
            val permissionGroup = when(currentVersion) {
                Build.VERSION_CODES.Q -> {
                    getPermissionMapOnQ()[permission]
                }
                Build.VERSION_CODES.R -> {
                    getPermissionMapOnR()[permission]
                }
                else -> {
                    val permissionInfo = context.packageManager.getPermissionInfo(permission, 0)
                    permissionInfo.group
                }
            }
            if (permissionGroup != null && !groupSet.contains(permissionGroup)) {
                val layout = LayoutInflater.from(context).inflate(R.layout.permissionx_permission_item, permissionsLayout, false) as LinearLayout
                layout.permissionText.text = context.getString(context.packageManager.getPermissionGroupInfo(permissionGroup, 0).labelRes)
                layout.permissionIcon.setImageResource(context.packageManager.getPermissionGroupInfo(permissionGroup, 0).icon)
                if (isDarkTheme()) {
                    if (darkColor != -1) {
                        layout.permissionIcon.setColorFilter(darkColor, PorterDuff.Mode.SRC_ATOP)
                    }
                } else {
                    if (lightColor != -1) {
                        layout.permissionIcon.setColorFilter(lightColor, PorterDuff.Mode.SRC_ATOP)
                    }
                }
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

    /**
     * Currently we are in dark theme or not.
     */
    private fun isDarkTheme(): Boolean {
        val flag = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return flag == Configuration.UI_MODE_NIGHT_YES
    }

}