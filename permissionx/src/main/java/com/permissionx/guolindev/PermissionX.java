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

package com.permissionx.guolindev;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.permissionx.guolindev.patch.PermissionDelegateHolder;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * An open source Android library that makes handling runtime permissions extremely easy.
 *
 * The following snippet shows the simple usage:
 * <pre>
 *   PermissionX.init(activity)
 *      .permissions(Manifest.permission.READ_CONTACTS, Manifest.permission.CAMERA)
 *      .request { allGranted, grantedList, deniedList ->
 *          // handling the logic
 *      }
 *</pre>
 *
 * if you are using component activity, you can use the following snippet:
 * <pre>
 * PermissionX.init(this, permissionMediator -> {
 *     permissionMediator
 *          .permissions(Manifest.permission.READ_CONTACTS, Manifest.permission.CAMERA)
 *          .request { allGranted, grantedList, deniedList ->
 *              // handling the logic
 *          }
 *     return null;
 * });
 * </pre>
 *
 * @author guolin
 * @since 2019/11/2
 */
public class PermissionX {

    /**
     * Init PermissionX to make everything prepare to work.
     *
     * @param activity An instance of FragmentActivity
     * @return PermissionCollection instance.
     */
    public static PermissionMediator init(@NonNull FragmentActivity activity) {
        return new PermissionMediator(activity);
    }

    /**
     * Init PermissionX to make everything prepare to work.
     *
     * @param fragment An instance of Fragment
     * @return PermissionCollection instance.
     */
    public static PermissionMediator init(@NonNull Fragment fragment) {
        return new PermissionMediator(fragment);
    }

    /**
     * Init PermissionX to make everything prepare to work.
     * request permissions in a component activity.
     *
     * @param context the context which can launch a new activity
     * @param block the request permissions block to be executed
     */
    public static void init(@NonNull Context context, @NonNull Function1<PermissionMediator, Unit> block) {
        PermissionDelegateHolder.delegate(context, fragmentActivity -> {
            PermissionMediator permissionMediator = new PermissionMediator(fragmentActivity);
            block.invoke(permissionMediator);
            return null;
        });
    }

    /**
     *  A helper function to check a permission is granted or not.
     *
     *  @param context Any context, will not be retained.
     *  @param permission Specific permission name to check. e.g. [android.Manifest.permission.CAMERA].
     *  @return True if this permission is granted, False otherwise.
     */
    public static boolean isGranted(@NonNull Context context, @NonNull String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * A helper function to check are notifications are enabled for current app.
     * @param context
     *          Any context, will not be retained.
     * @return Note that if Android version is lower than N, the return value will always be true.
     */
    public static boolean areNotificationsEnabled(@NonNull Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static final class permission {
        /**
         * Define the const to compat with system lower than T.
         */
        public static final String POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";
    }
}
