/*
 * Copyright (C)  guolin, PermissionX Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.permissionx.guolindev.request;

/**
 * Maintain the task chain of permission request process.
 *
 * @author guolin
 * @since 2020/6/10
 */
public class RequestChain {

    /**
     * Holds the first task of request process. Permissions request begins here.
     */
    private BaseTask headTask;

    /**
     * Holds the last task of request process. Permissions request ends here.
     */
    private BaseTask tailTask;

    /**
     * Add a task into task chain.
     *
     * @param task task to add.
     */
    public void addTaskToChain(BaseTask task) {
        if (headTask == null) {
            headTask = task;
        }
        // add task to the tail
        if (tailTask != null) {
            tailTask.next = task;
        }
        tailTask = task;
    }

    /**
     * Run this task chain from the first task.
     */
    public void runTask() {
        headTask.request();
    }

}
