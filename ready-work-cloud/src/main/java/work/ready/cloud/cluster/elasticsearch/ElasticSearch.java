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

package work.ready.cloud.cluster.elasticsearch;

import java.net.InetAddress;

public interface ElasticSearch {

	void start() throws ElasticSearchException, ElasticSearchInterruptedException;

	void stop() throws ElasticSearchException, ElasticSearchInterruptedException;

	String getName();

	Version getVersion();

	default InetAddress getAddress() {
		return null;
	}

	default int getPort() {
		return -1;
	}

	default int getSslPort() {
		return -1;
	}

	default int getTcpPort() {
		return -1;
	}

}

