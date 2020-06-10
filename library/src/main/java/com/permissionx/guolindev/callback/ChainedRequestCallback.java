package com.permissionx.guolindev.callback;

import com.permissionx.guolindev.request.ExplainScope;
import com.permissionx.guolindev.request.ForwardScope;

public interface ChainedRequestCallback {

    ExplainScope getExplainScope();

    ForwardScope getForwardScope();

    void onResult();

}