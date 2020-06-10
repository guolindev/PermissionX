package com.permissionx.guolindev.request;

public class RequestChain {

    private ChainTask headTask;

    private ChainTask tailTask;

    public void addTaskToChain(ChainTask task) {
        if (headTask == null) {
            headTask = task;
        }
        if (tailTask != null) {
            tailTask.next = task;
        }
        tailTask = task;
    }

    public void runTask() {
        headTask.onRequest();
    }

}
