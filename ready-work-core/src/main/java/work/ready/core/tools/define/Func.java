/**
 * Copyright (c) 2011-2021, James Zhan 詹波 (jfinal@126.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package work.ready.core.tools.define;

public interface Func {

	@FunctionalInterface
	public interface F00 {
		void call();
	}

	@FunctionalInterface
	public interface F10<T> {
		void call(T t);
	}

	@FunctionalInterface
	public interface F20<T, U> {
		void call(T t, U u);
	}

	@FunctionalInterface
	public interface F30<T, U, V> {
		void call(T t, U u, V v);
	}

	@FunctionalInterface
	public interface F40<T, U, V, W> {
		void call(T t, U u, V v, W w);
	}

	@FunctionalInterface
	public interface F50<T, U, V, W, X> {
		void call(T t, U u, V v, W w, X x);
	}

	@FunctionalInterface
	public interface F60<T, U, V, W, X, Y> {
		void call(T t, U u, V v, W w, X x, Y y);
	}

	@FunctionalInterface
	public interface F70<T, U, V, W, X, Y, Z> {
		void call(T t, U u, V v, W w, X x, Y y, Z z);
	}

	@FunctionalInterface
	public interface F01<R> {
		R call();
	}

	@FunctionalInterface
	public interface F11<T, R> {
		R call(T t);
	}

	@FunctionalInterface
	public interface F21<T, U, R> {
		R call(T t, U u);
	}

	@FunctionalInterface
	public interface F31<T, U, V, R> {
		R call(T t, U u, V v);
	}

	@FunctionalInterface
	public interface F41<T, U, V, W, R> {
		R call(T t, U u, V v, W w);
	}

	@FunctionalInterface
	public interface F51<T, U, V, W, X, R> {
		R call(T t, U u, V v, W w, X x);
	}

	@FunctionalInterface
	public interface F61<T, U, V, W, X, Y, R> {
		R call(T t, U u, V v, W w, X x, Y y);
	}

	@FunctionalInterface
	public interface F71<T, U, V, W, X, Y, Z, R> {
		R call(T t, U u, V v, W w, X x, Y y, Z z);
	}
}

