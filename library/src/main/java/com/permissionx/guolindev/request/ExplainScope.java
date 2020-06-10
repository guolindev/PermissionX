package com.permissionx.guolindev.request;

import java.util.List;

/**
 * Provide specific scopes for {@link com.permissionx.guolindev.callback.ExplainReasonCallback} and {@link com.permissionx.guolindev.callback.ExplainReasonCallbackWithBeforeParam} to give it specific functions to call.
 * @author guolin
 * @since 2020/3/18
 */
public class ExplainScope {

    private PermissionBuilder pb;

    private ChainTask chainTask;

    ExplainScope(PermissionBuilder pb, ChainTask chainTask) {
        this.pb = pb;
        this.chainTask = chainTask;
    }

    /**
     * Show a rationale dialog to explain to user why you need these permissions.
     * @param permissions
     *          Permissions that to request.
     * @param message
     *          Message that show to user.
     * @param positiveText
     *          Text on the positive button. When user click, PermissionX will request permissions again.
     * @param negativeText
     *          Text on the negative button. When user click, PermissionX will finish request.
     */
    public void showRequestReasonDialog(List<String> permissions, String message, String positiveText, String negativeText) {
        pb.showHandlePermissionDialog(chainTask, true, permissions, message, positiveText, negativeText);
    }

    /**
     * Show a rationale dialog to explain to user why you need these permissions.
     * @param permissions
     *          Permissions that to request.
     * @param message
     *          Message that show to user.
     * @param positiveText
     *          Text on the positive button. When user click, PermissionX will request permissions again.
     */
    public void showRequestReasonDialog(List<String> permissions, String message, String positiveText) {
        showRequestReasonDialog(permissions, message, positiveText, null);
    }

}