/*
 * Copyright (C) guolin, PermissionX Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.permissionx.guolindev.dialog

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import com.permissionx.guolindev.R
import com.permissionx.guolindev.databinding.PermissionxDefaultDialogLayoutBinding
import com.permissionx.guolindev.databinding.PermissionxPermissionItemBinding

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

    private lateinit var binding: PermissionxDefaultDialogLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PermissionxDefaultDialogLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupText()
        buildPermissionsLayout()
        setupWindow()
    }

    /**
     * Provide the positive button instance to continue requesting.
     * @return Positive button instance to continue requesting.
     */
    override fun getPositiveButton(): View {
        return binding.positiveBtn
    }

    /**
     * Provide the negative button instance to abort requesting.
     * This is alternative. If negativeText is null we just return null, means all these permissions are necessary.
     * @return Negative button instance to abort requesting. Or null if all these permissions are necessary.
     */
    override fun getNegativeButton(): View? {
        return negativeText?.let {
            return binding.negativeBtn
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
     * Check if the permission layout if empty.
     * It is possible if all the permissions passed in are invalid permission such as a string named
     * "hello world". We won't add these into permission layout.
     */
    fun isPermissionLayoutEmpty(): Boolean {
        return binding.permissionsLayout.childCount == 0
    }

    /**
     * Setup text and text color on the dialog.
     */
    private fun setupText() {
        binding.messageText.text = message
        binding.positiveBtn.text = positiveText
        if (negativeText != null) {
            binding.negativeLayout.visibility = View.VISIBLE
            binding.negativeBtn.text = negativeText
        } else {
            binding.negativeLayout.visibility = View.GONE
        }
        if (isDarkTheme()) {
            if (darkColor != -1) {
                binding.positiveBtn.setTextColor(darkColor)
                binding.negativeBtn.setTextColor(darkColor)
            }
        } else {
            if (lightColor != -1) {
                binding.positiveBtn.setTextColor(lightColor)
                binding.negativeBtn.setTextColor(lightColor)
            }
        }
    }

    /**
     * Add every permission that need to explain the request reason to the dialog.
     * But we only need to add the permission group. So if there're two permissions belong to one group, only one item will be added to the dialog.
     */
    private fun buildPermissionsLayout() {
        val tempSet = HashSet<String>()
        val currentVersion = Build.VERSION.SDK_INT
        for (permission in permissions) {
            val permissionGroup = when(currentVersion) {
                Build.VERSION_CODES.Q -> permissionMapOnQ[permission]
                Build.VERSION_CODES.R -> permissionMapOnR[permission]
                else -> {
                    try {
                        val permissionInfo = context.packageManager.getPermissionInfo(permission, 0)
                        permissionInfo.group
                    } catch (e: PackageManager.NameNotFoundException) {
                        e.printStackTrace()
                        null
                    }
                }
            }
            if ((permission in allSpecialPermissions && !tempSet.contains(permission))
                || (permissionGroup != null && !tempSet.contains(permissionGroup))) {
                val itemBinding = PermissionxPermissionItemBinding.inflate(layoutInflater, binding.permissionsLayout, false)
                when(permission) {
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION -> {
                        itemBinding.permissionText.text = context.getString(R.string.permissionx_access_background_location)
                        itemBinding.permissionIcon.setImageResource(R.drawable.permissionx_ic_location)
                    }
                    Manifest.permission.SYSTEM_ALERT_WINDOW -> {
                        itemBinding.permissionText.text = context.getString(R.string.permissionx_system_alert_window)
                        itemBinding.permissionIcon.setImageResource(R.drawable.permissionx_ic_alert)
                    }
                    Manifest.permission.WRITE_SETTINGS -> {
                        itemBinding.permissionText.text = context.getString(R.string.permissionx_write_settings)
                        itemBinding.permissionIcon.setImageResource(R.drawable.permissionx_ic_setting)
                    }
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                        itemBinding.permissionText.text = context.getString(R.string.permissionx_manage_external_storage)
                        itemBinding.permissionIcon.setImageResource(R.drawable.permissionx_ic_storage)
                    }
                    else -> {
                        itemBinding.permissionText.text = context.getString(context.packageManager.getPermissionGroupInfo(permissionGroup!!, 0).labelRes)
                        itemBinding.permissionIcon.setImageResource(context.packageManager.getPermissionGroupInfo(permissionGroup, 0).icon)
                    }
                }
                if (isDarkTheme()) {
                    if (darkColor != -1) {
                        itemBinding.permissionIcon.setColorFilter(darkColor, PorterDuff.Mode.SRC_ATOP)
                    }
                } else {
                    if (lightColor != -1) {
                        itemBinding.permissionIcon.setColorFilter(lightColor, PorterDuff.Mode.SRC_ATOP)
                    }
                }
                binding.permissionsLayout.addView(itemBinding.root)
                tempSet.add(permissionGroup ?: permission)
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