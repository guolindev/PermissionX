/*
 * Copyright (C)  guolin, PermissionX Open Source Project
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

/**
 * Provide specific scopes for [ExplainReasonCallback] to give it specific functions to call.
 * @author guolin
 * @since 2020/3/18
 */
class ExplainReasonScope(private val permissionBuilder: PermissionBuilder) {

    fun showRequestReasonDialog(
        permissions: List<String>,
        message: String,
        positiveText: String,
        negativeText: String? = null
    ) {
        permissionBuilder.showHandlePermissionDialog(
            true,
            permissions,
            message,
            positiveText,
            negativeText
        )
    }

}

/**
 * Provide specific scopes for [ForwardToSettingsCallback] to give it specific functions to call.
 * @author guolin
 * @since 2020/3/18
 */
class ForwardToSettingsScope(private val permissionBuilder: PermissionBuilder) {

    fun showForwardToSettingsDialog(
        permissions: List<String>,
        message: String,
        positiveText: String,
        negativeText: String? = null
    ) {
        permissionBuilder.showHandlePermissionDialog(
            false,
            permissions,
            message,
            positiveText,
            negativeText
        )
    }

}