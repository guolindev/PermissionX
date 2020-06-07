package com.permissionx.guolindev.callback;

import com.permissionx.guolindev.ForwardScope;

import java.util.List;

/**
 * Callback for {@link com.permissionx.guolindev.PermissionBuilder#onForwardToSettings(ForwardToSettingsCallback)} method.
 */
public interface ForwardToSettingsCallback {

    /**
     * Called when you should tell user to allow these permissions in settings.
     * @param scope
     *          Scope to show rationale dialog.
     * @param deniedList
     *          Permissions that should allow in settings.
     */
    void onForwardToSettings(ForwardScope scope, List<String> deniedList);

}