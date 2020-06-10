package com.permissionx.guolindev.request;

import com.permissionx.guolindev.callback.ChainedRequestCallback;

import java.util.ArrayList;
import java.util.List;

abstract class ChainTask implements ChainedRequestCallback {

    protected ChainTask next;

    protected PermissionBuilder pb;

    /**
     * Provide specific scopes for explainReasonCallback for specific functions to call.
     */
    ExplainScope explainReasonScope;

    /**
     * Provide specific scopes for forwardToSettingsCallback for specific functions to call.
     */
    ForwardScope forwardToSettingsScope ;

    ChainTask(PermissionBuilder permissionBuilder) {
        pb = permissionBuilder;
        explainReasonScope = new ExplainScope(pb, this);
        forwardToSettingsScope = new ForwardScope(pb, this);
    }

    @Override
    public ExplainScope getExplainScope() {
        return explainReasonScope;
    }

    @Override
    public ForwardScope getForwardScope() {
        return forwardToSettingsScope;
    }

    @Override
    public void onResult() {
        if (next != null) {
            next.onRequest();
        } else {
            List<String> deniedList = new ArrayList<>();
            deniedList.addAll(pb.deniedPermissions);
            deniedList.addAll(pb.permanentDeniedPermissions);
            deniedList.addAll(pb.permissionsWontRequest);
            if (pb.requestCallback != null) {
                pb.requestCallback.onResult(deniedList.isEmpty(), new ArrayList<>(pb.grantedPermissions), deniedList);
            }
        }
    }

    abstract void onRequest();

}
