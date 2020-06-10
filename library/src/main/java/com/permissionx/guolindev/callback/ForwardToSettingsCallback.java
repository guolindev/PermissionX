package com.permissionx.guolindev.callback;

import com.permissionx.guolindev.request.ForwardScope;
import com.permissionx.guolindev.request.PermissionBuilder;

import java.util.List;

/**
 * Callback for {@link PermissionBuilder#onForwardToSettings(ForwardToSettingsCallback)} method.
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