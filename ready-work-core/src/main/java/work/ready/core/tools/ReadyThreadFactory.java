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

package work.ready.core.tools;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ReadyThreadFactory implements ThreadFactory {
	private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
	private final ThreadGroup group;
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String namePrefix;
	private final int priority;
	private boolean daemon;
	private ThreadUncaughtExceptionHandler exceptionHandler;

	public ReadyThreadFactory(String namePrefix) {
		this(namePrefix, Thread.NORM_PRIORITY);
	}

	public ReadyThreadFactory(String namePrefix, int priority) {
		this(namePrefix, Thread.NORM_PRIORITY, false);
	}

	public ReadyThreadFactory(String namePrefix, int priority, boolean daemon) {
		SecurityManager s = System.getSecurityManager();
		this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
		this.namePrefix = namePrefix + "-pool-" + POOL_NUMBER.getAndIncrement() + "-thread-";
		this.priority = priority;
		this.daemon = daemon;
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
		t.setDaemon(daemon);
		if (t.getPriority() != priority) {
			t.setPriority(priority);
		}
		t.setUncaughtExceptionHandler(exceptionHandler == null ? new ThreadUncaughtExceptionHandler() : exceptionHandler);
		return t;
	}

	static class ThreadUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
		private static final Log logger = LogFactory.getLog(ThreadUncaughtExceptionHandler.class);

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			logger.error(e, "Uncaught exception");
		}
	}

}
