/**
 *
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package work.ready.cloud.cluster;

import org.apache.ignite.Ignite;
import org.apache.ignite.lifecycle.LifecycleBean;
import org.apache.ignite.lifecycle.LifecycleEventType;
import org.apache.ignite.resources.IgniteInstanceResource;

public class NodeLifecycleBean implements LifecycleBean {
    @IgniteInstanceResource
    public Ignite ignite;

    @Override
    public void onLifecycleEvent(LifecycleEventType evt) {
        if (evt == LifecycleEventType.BEFORE_NODE_START) {
            System.out.format("before the node (consistentId = %s) starts.\n", ignite.cluster().node().consistentId());
        }
        if (evt == LifecycleEventType.AFTER_NODE_START) {
            System.out.format("After the node (consistentId = %s) starts.\n", ignite.cluster().node().consistentId());
        }
        if (evt == LifecycleEventType.BEFORE_NODE_STOP) {
            System.out.format("before the node (consistentId = %s) stops.\n", ignite.cluster().node().consistentId());
        }
        if (evt == LifecycleEventType.AFTER_NODE_STOP) {
            System.out.format("After the node (consistentId = %s) stops.\n", ignite.cluster().node().consistentId());
        }
    }
}
