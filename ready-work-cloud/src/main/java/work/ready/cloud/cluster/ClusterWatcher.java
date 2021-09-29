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

import org.apache.ignite.cluster.BaselineNode;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.EventType;
import org.apache.ignite.internal.processors.timeout.GridTimeoutObjectAdapter;

import java.util.Set;
import java.util.stream.Collectors;

public class ClusterWatcher {

    private final Cloud cloud;

    private final long bltChangeDelayMillis;

    public ClusterWatcher(Cloud cloud, long bltChangeDelayMillis) {
        this.cloud = cloud;
        this.bltChangeDelayMillis = bltChangeDelayMillis;
    }

    public void start() {
        cloud.events().localListen(event -> {
            DiscoveryEvent e = (DiscoveryEvent)event;

            Set<Object> aliveSrvNodes = e.topologyNodes().stream()
                    .filter(n -> !n.isClient())
                    .map(ClusterNode::consistentId)
                    .collect(Collectors.toSet());

            Set<Object> baseline = cloud.cluster().currentBaselineTopology().stream()
                    .map(BaselineNode::consistentId)
                    .collect(Collectors.toSet());

            final long topVer = e.topologyVersion();

            if (!aliveSrvNodes.equals(baseline))
                cloud.context().timeout().addTimeoutObject(new GridTimeoutObjectAdapter(bltChangeDelayMillis) {
                    @Override public void onTimeout() {
                        if (cloud.cluster().topologyVersion() == topVer)
                            cloud.cluster().setBaselineTopology(topVer);
                    }
                });

            return true;
        }, EventType.EVT_NODE_FAILED, EventType.EVT_NODE_LEFT, EventType.EVT_NODE_JOINED);
    }
}
