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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CachedConsumer<T> implements Consumer<T>, Supplier<List<T>> {

	private final ConcurrentLinkedDeque<T> elements;

	private final long count;

	public CachedConsumer(long count) {
		if (count <= 0) {
			throw new IllegalArgumentException(String.format("Count '%d' must be positive", count));
		}
		this.count = count;
		this.elements = new ConcurrentLinkedDeque<>();
	}

	@Override
	public void accept(T element) {
		if (this.elements.size() >= this.count) {
			this.elements.removeFirst();
		}
		this.elements.addLast(element);
	}

	@Override
	public List<T> get() {
		return Collections.unmodifiableList(new ArrayList<>(this.elements));
	}

}
