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

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.tools.StrUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HttpTransportStateWatcherConsumer implements StateWatcherConsumer {

	private static final Log logger = LogFactory.getLog(HttpTransportStateWatcherConsumer.class);

	private static final Pattern TRANSPORT_PATTERN = Pattern.compile(
			"(?i).*\\.AbstractHttpServerTransport(.*)publish_address \\{(.+?):(\\d+)\\}.*");

	private static final Pattern SECURITY_LISTENER_PATTERN = Pattern.compile(
			"(?i).*\\.SecurityStatusChangeListener.*?Active license.*?Security is (\\w+).*");

	private final boolean ssl;

	private volatile String nodeName;

	private volatile Integer port;

	private volatile Integer sslPort;

	private volatile InetAddress address;

	HttpTransportStateWatcherConsumer(boolean ssl) {
		this.ssl = ssl;
	}

	@Override
	public void accept(String line) {
		Matcher transportMatcher = TRANSPORT_PATTERN.matcher(line);
		Matcher securityMatcher = SECURITY_LISTENER_PATTERN.matcher(line);
		if (transportMatcher.matches()) {
			this.nodeName = StrUtil.replace(transportMatcher.group(1), new char[] {'[',']'}, ' ').trim();
			this.address = getAddress(transportMatcher.group(2));
			this.port = Integer.parseInt(transportMatcher.group(3));
		}
		if (securityMatcher.matches()) {
			if(securityMatcher.group(1).equals("disabled")){
				this.sslPort = null;
			} else {
				this.sslPort = this.port;
			}
		}
	}

	@Override
	public boolean isReady() {
		if (this.ssl && this.sslPort == null) {
			return false;
		}
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

	int getSslPort() {
		Integer port = this.sslPort;
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
