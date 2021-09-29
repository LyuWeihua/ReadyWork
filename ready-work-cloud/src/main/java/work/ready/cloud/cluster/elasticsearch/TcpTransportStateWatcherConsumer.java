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
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.tools.StrUtil;

class TcpTransportStateWatcherConsumer implements StateWatcherConsumer {

	private static final Log logger = LogFactory.getLog(TcpTransportStateWatcherConsumer.class);

	private static final Pattern TRANSPORT_START_PATTERN = Pattern.compile(
			"(?i).*\\.TransportService(.*)publish_address \\{(.+?):(\\d+)\\}.*");

	private volatile String nodeName;

	private volatile Integer port;

	private volatile InetAddress address;

	TcpTransportStateWatcherConsumer() {
	}

	@Override
	public void accept(String line) {
		Matcher matcher = TRANSPORT_START_PATTERN.matcher(line);
		if (matcher.matches()) {
			this.nodeName = StrUtil.replace(matcher.group(1), new char[] {'[',']'}, ' ').trim();
			this.address = getAddress(matcher.group(2));
			this.port = Integer.parseInt(matcher.group(3));
		}
	}

	@Override
	public boolean isReady() {
		return this.port != null;
	}

	public String getNodeName() {
		return nodeName;
	}

	InetAddress getAddress() {
		return this.address;
	}

	int getPort() {
		Integer port = this.port;
		return (port != null) ? port : -1;
	}

	private static InetAddress getAddress(String address) {
		try {
			return InetAddress.getByName(address);
		}
		catch (UnknownHostException ex) {
			logger.error(ex, "Address '%s' cannot be parsed", address);
			return null;
		}
	}

}
