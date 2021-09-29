/**
 * Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
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

package work.ready.core.template.stat.ast;

public class ForLoopStatus {

	private Object outer;
	private int index;

	public ForLoopStatus(Object outer) {
		this.outer = outer;
		this.index = 0;
	}

	void nextState() {
		index++;
	}

	public Object getOuter() {
		return outer;
	}

	public int getIndex() {
		return index;
	}

	public int getCount() {
		return index + 1;
	}

	public boolean getFirst() {
		return index == 0;
	}

	public boolean getOdd() {
		return index % 2 == 0;
	}

	public boolean getEven() {
		return index % 2 != 0;
	}
}

