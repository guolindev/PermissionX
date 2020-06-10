package com.permissionx.guolindev.request;

import android.Manifest;
import android.os.Build;

import androidx.fragment.app.FragmentActivity;

import com.permissionx.guolindev.request.PermissionBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An internal class to provide specific scope for passing permissions param.
 */
public class PermissionCollection {

    private FragmentActivity activity;

    public PermissionCollection(FragmentActivity activity) {
        this.activity = activity;
    }

    /**
     * All permissions that you want to request.
     * @param permissions A vararg param to pass permissions.
     */
    public PermissionBuilder permissions(String... permissions)  {
        return permissions(new ArrayList<>(Arrays.asList(permissions)));
    }

    /**
     * All permissions that you want to request.
     * @param permissions A vararg param to pass permissions.
     */
    public PermissionBuilder permissions(List<String> permissions)  {
        Set<String> permissionSet = new HashSet<>(permissions);
        boolean requireBackgroundLocationPermission = false;
        Set<String> permissionsWontRequest = new HashSet<>();
        if (permissionSet.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            int osVersion = Build.VERSION.SDK_INT;
            int targetSdkVersion = activity.getApplicationInfo().targetSdkVersion;
            if (osVersion >= 29 && targetSdkVersion == 10000) {
                requireBackgroundLocationPermission = true;
                permissionSet.remove(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            } else if (osVersion < 29) {
                permissionSet.remove("android.permission.ACCESS_BACKGROUND_LOCATION");
                permissionsWontRequest.add("android.permission.ACCESS_BACKGROUND_LOCATION");
            }
        }
        return new PermissionBuilder(activity, permissionSet, requireBackgroundLocationPermission, permissionsWontRequest);
    }

}
