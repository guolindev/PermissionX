package com.permissionx.guolindev;

import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        return new PermissionBuilder(activity, distinctPermissions(permissions));
    }

    /**
     * Distinct the permissions that passed in. Duplicate permissions can lead to the wrong answer.
     * @param permissions
     *          Permissions that app want to request.
     * @return A filtered permission list with no duplicate value.
     */
    private List<String> distinctPermissions(String... permissions) {
        List<String> uniquePermissions = new ArrayList<>();
        Set<String> set = new HashSet<>();
        if (permissions != null) {
            for (String permission : permissions) {
                if (!set.contains(permission)) {
                    set.add(permission);
                    uniquePermissions.add(permission);
                }
            }
        }
        return uniquePermissions;
    }

}
