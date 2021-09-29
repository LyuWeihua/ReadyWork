/**
 *
 * Original work Copyright Spring
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
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
package work.ready.core.event;

import work.ready.core.server.Ready;

public abstract class BaseEvent<T> implements java.io.Serializable {
	private static final long serialVersionUID = 7099057708183571937L;
	protected final T source;
	private final long timestamp;

	public BaseEvent(T source) {
		if (source == null) {
			throw new IllegalArgumentException("null source");
		}
		this.source = source;
		this.timestamp = Ready.currentTimeMillis();
	}

	public T getSource() {
		return source;
	}

	public Class<?> getSourceClass() {
		return this.source.getClass();
	}

	@Override
	public String toString() {
		return getClass().getName() + "[source=" + source + "]";
	}

	public final long getTimestamp() {
		return this.timestamp;
	}

}
