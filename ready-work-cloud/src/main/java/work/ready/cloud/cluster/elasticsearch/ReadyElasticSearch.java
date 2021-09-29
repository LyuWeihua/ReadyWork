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

import java.net.InetAddress;
import java.util.StringJoiner;

class ReadyElasticSearch implements ElasticSearch {

	private static final Log logger = LogFactory.getLog(ReadyElasticSearch.class);

	private final String name;

	private final Version version;

	private final ElasticSearchInstance instance;

	private volatile boolean started = false;

	private volatile boolean running = false;

	ReadyElasticSearch(String name, Version version, ElasticSearchInstance instance) {
		this.name = name;
		this.version = version;
		this.instance = instance;
	}

	@Override
	public synchronized void start() {
		if (this.started) {
			return;
		}
		try {
			this.started = true;
			logger.info("Starts %s", toString());
			doStart();
			this.running = true;
			logger.info("%s has been started and ready for connections!", toString());
		}
		catch (ElasticSearchException ex) {
			try {
				doStop();
				this.started = false;
			}
			catch (ElasticSearchException swallow) {
				ex.addSuppressed(swallow);
			}
			throw ex;
		}
	}

	@Override
	public synchronized void stop() {
		if (!this.started) {
			return;
		}
		logger.info("Stops %s", toString());
		doStop();
		logger.info("%s has been stopped", toString());
		this.started = false;
		this.running = false;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Version getVersion() {
		return this.version;
	}

	@Override
	public InetAddress getAddress() {
		if (this.running) {
			return this.instance.getAddress();
		}
		return null;
	}

	@Override
	public int getPort() {
		if (this.running) {
			return this.instance.getPort();
		}
		return -1;
	}

	@Override
	public int getSslPort() {
		if (this.running) {
			return this.instance.getSslPort();
		}
		return -1;
	}

	@Override
	public int getTcpPort() {
		if (this.running) {
			return this.instance.getTcpPort();
		}
		return -1;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", ReadyElasticSearch.class.getSimpleName() + "[", "]")
				.add("name='" + this.name + "'")
				.add("version='" + this.version + "'")
				.toString();
	}

	private void doStart() {
		try {
			this.instance.start();
		}
		catch (InterruptedException ex) {
			throw new ElasticSearchInterruptedException("ElasticSearch start interrupted", ex);
		}
		catch (Exception ex) {
			throw new ElasticSearchException("Unable to start " + toString(), ex);
		}
	}

	private void doStop() {
		try {
			this.instance.stop();
		}
		catch (InterruptedException ex) {
			throw new ElasticSearchInterruptedException("ElasticSearch stop interrupted", ex);
		}
		catch (Exception ex) {
			throw new ElasticSearchException("Unable to stop " + toString(), ex);
		}
	}

}
