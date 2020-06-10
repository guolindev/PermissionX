package com.permissionx.guolindev.request;

import com.permissionx.guolindev.PermissionX;

import java.util.ArrayList;
import java.util.List;

public class RequestNormalPermissions extends ChainTask {

    RequestNormalPermissions(PermissionBuilder permissionBuilder) {
        super(permissionBuilder);
    }

    @Override
    public void onRequest() {
        List<String> requestList = new ArrayList<>();
        for (String permission : pb.normalPermissions) {
            if (PermissionX.isGranted(pb.activity, permission)) {
                pb.grantedPermissions.add(permission); // already granted
            } else {
                requestList.add(permission); // still need to request
            }
        }
        if (requestList.isEmpty()) { // all permissions are granted
            onResult();
            return;
        }
        if (pb.explainReasonBeforeRequest && (pb.explainReasonCallback != null || pb.explainReasonCallbackWithBeforeParam != null)) {
            pb.explainReasonBeforeRequest = false;
            pb.deniedPermissions.addAll(requestList);
            if (pb.explainReasonCallbackWithBeforeParam != null) {
                // callback ExplainReasonCallbackWithBeforeParam prior to ExplainReasonCallback
                pb.explainReasonCallbackWithBeforeParam.onExplainReason(explainReasonScope, requestList, true);
            } else {
                pb.explainReasonCallback.onExplainReason(explainReasonScope, requestList);
            }
        } else {
            // Do the request at once. Always request all permissions no matter they are already granted or not, in case user turn them off in Settings.
            pb.requestNow(pb.normalPermissions, this);
        }
    }

}
