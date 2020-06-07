package com.permissionx.guolindev;

import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * An internal class to provide specific scope for passing permissions param.
 */
public class PermissionCollection {

    private FragmentActivity activity;

    PermissionCollection(FragmentActivity activity) {
        this.activity = activity;
    }

    /**
     * All permissions that you want to request.
     * @param permissions A vararg param to pass permissions.
     */
    public PermissionBuilder permissions(String... permissions)  {
        return new PermissionBuilder(activity, new ArrayList<>(Arrays.asList(permissions)));
    }

}
