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

package com.permissionx.guolindev

import android.os.Build
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.permissionx.guolindev.dialog.allSpecialPermissions
import com.permissionx.guolindev.request.PermissionBuilder
import com.permissionx.guolindev.request.RequestBackgroundLocationPermission
import java.util.*

/**
 * An internal class to provide specific scope for passing permissions param.
 *
 * @author guolin
 * @since 2019/11/2
 */
class PermissionMediator {

    private var activity: FragmentActivity? = null
    private var fragment: Fragment? = null

    constructor(activity: FragmentActivity) {
        this.activity = activity
    }

    constructor(fragment: Fragment) {
        this.fragment = fragment
    }

    /**
     * All permissions that you want to request.
     *
     * @param permissions A vararg param to pass permissions.
     * @return PermissionBuilder itself.
     */
    fun permissions(permissions: List<String>): PermissionBuilder {
        val normalPermissionSet = HashSet<String>()
        val specialPermissionSet = HashSet<String>()
        val osVersion = Build.VERSION.SDK_INT
        val targetSdkVersion = if (activity != null) {
            activity!!.applicationInfo.targetSdkVersion
        } else {
            fragment!!.context!!.applicationInfo.targetSdkVersion
        }
        for (permission in permissions) {
            if (permission in allSpecialPermissions) {
                specialPermissionSet.add(permission)
            } else {
                normalPermissionSet.add(permission)
            }
        }
        if (RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION in specialPermissionSet) {
            if (osVersion == Build.VERSION_CODES.Q ||
                (osVersion == Build.VERSION_CODES.R && targetSdkVersion < Build.VERSION_CODES.R)) {
                // If we request ACCESS_BACKGROUND_LOCATION on Q or on R but targetSdkVersion below R,
                // We don't need to request specially, just request as normal permission.
                specialPermissionSet.remove(RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION)
                normalPermissionSet.add(RequestBackgroundLocationPermission.ACCESS_BACKGROUND_LOCATION)
            }
        }
        return PermissionBuilder(activity, fragment, normalPermissionSet, specialPermissionSet)
    }

    /**
     * All permissions that you want to request.
     *
     * @param permissions A vararg param to pass permissions.
     * @return PermissionBuilder itself.
     */
    fun permissions(vararg permissions: String): PermissionBuilder {
        return permissions(listOf(*permissions))
    }

}