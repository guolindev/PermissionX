package com.permissionx.guolindev.callback;

import java.util.List;

/**
 * Callback for {@link com.permissionx.guolindev.PermissionBuilder#request(RequestCallback)} method.
 */
public interface RequestCallback {

    /**
     * Callback for the request result.
     * @param allGranted
     *          Indicate if all permissions that are granted.
     * @param grantedList
     *          All permissions that granted by user.
     * @param deniedList
     *          All permissions that denied by user.
     */
    void onResult(boolean allGranted, List<String> grantedList, List<String> deniedList);

}
