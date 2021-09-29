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
package work.ready.core.tools.define;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class CompositeConsumer<T> implements Consumer<T> {

	private final List<Consumer<? super T>> consumers = new CopyOnWriteArrayList<>();

	public boolean add(Consumer<? super T> consumer) {
		Objects.requireNonNull(consumer, "'consumer' must not be null");
		if (!this.consumers.contains(consumer)) {
			return this.consumers.add(consumer);
		}
		return false;
	}

	public boolean remove(Consumer<? super T> consumer) {
		Objects.requireNonNull(consumer, "'consumer' must not be null");
		return this.consumers.remove(consumer);
	}

	@Override
	public void accept(T element) {
		this.consumers.forEach(consumer -> consumer.accept(element));
	}

}
