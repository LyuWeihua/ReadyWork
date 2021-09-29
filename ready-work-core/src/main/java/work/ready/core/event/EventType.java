/**
 *
 * Original work Copyright jfinal-event L.cm
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

import java.util.Objects;

public class EventType {
	private final Class<?> eventClass;
	private final Class<?> sourceClass;

	public EventType(Class<?> eventClass, Class<?> sourceClass) {
		this.eventClass = eventClass;
		this.sourceClass = sourceClass;
	}

	public Class<?> getEventClass() {
		return eventClass;
	}

	public Class<?> getSourceClass() {
		return sourceClass;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		EventType eventType = (EventType) o;
		return Objects.equals(eventClass, eventType.eventClass) &&
			Objects.equals(sourceClass, eventType.sourceClass);
	}

	@Override
	public int hashCode() {
		return Objects.hash(eventClass, sourceClass);
	}
}
