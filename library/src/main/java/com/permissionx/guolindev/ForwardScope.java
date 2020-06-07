package com.permissionx.guolindev;

import java.util.List;

/**
 * Provide specific scopes for {@link com.permissionx.guolindev.callback.ForwardToSettingsCallback} to give it specific functions to call.
 * @author guolin
 * @since 2020/3/18
 */
public class ForwardScope {

    private PermissionBuilder pb;

    ForwardScope(PermissionBuilder pb) {
        this.pb = pb;
    }

    /**
     * Show a rationale dialog to tell user to allow these permissions in settings.
     * @param permissions
     *          Permissions that to request.
     * @param message
     *          Message that show to user.
     * @param positiveText
     *          Text on the positive button. When user click, PermissionX will forward to settings page of your app.
     * @param negativeText
     *          Text on the negative button. When user click, PermissionX will finish request.
     */
    public void showForwardToSettingsDialog(List<String> permissions, String message, String positiveText, String negativeText) {
        pb.showHandlePermissionDialog(false, permissions, message, positiveText, negativeText);
    }

    /**
     * Show a rationale dialog to tell user to allow these permissions in settings.
     * @param permissions
     *          Permissions that to request.
     * @param message
     *          Message that show to user.
     * @param positiveText
     *          Text on the positive button. When user click, PermissionX will forward to settings page of your app.
     */
    public void showForwardToSettingsDialog(List<String> permissions, String message, String positiveText) {
        pb.showHandlePermissionDialog(false, permissions, message, positiveText, null);
    }

}
