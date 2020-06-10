package com.permissionx.guolindev.callback;

import com.permissionx.guolindev.request.ExplainScope;
import com.permissionx.guolindev.request.PermissionBuilder;

import java.util.List;

/**
 * Callback for {@link PermissionBuilder#onExplainRequestReason(ExplainReasonCallback)} method.
 */
public interface ExplainReasonCallback {

    /**
     * Called when you should explain why you need these permissions.
     * @param scope
     *          Scope to show rationale dialog.
     * @param deniedList
     *          Permissions that you should explain.
     */
    void onExplainReason(ExplainScope scope, List<String> deniedList);

}
