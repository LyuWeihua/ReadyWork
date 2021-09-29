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

import work.ready.core.template.TemplateException;
import work.ready.core.template.stat.Location;

import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;

public class ForIteratorStatus {

	private Object outer;
	private int index;
	private int size;
	private Iterator<?> iterator;
	private Location location;

	public ForIteratorStatus(Object outer, Object target, Location location) {
		this.outer = outer;
		this.index = 0;
		this.location = location;
		init(target);
	}

	@SuppressWarnings("unchecked")
	private void init(Object target) {
		if (target instanceof Collection) {
			size = ((Collection<?>)target).size();
			iterator = ((Collection<?>)target).iterator();
			return ;
		}
		if (target instanceof Map<?, ?>) {
			size = ((Map<?, ?>)target).size();
			iterator = new MapIterator(((Map<Object, Object>)target).entrySet().iterator());
			return ;
		}
		if (target == null) {	
			size = 0;
			iterator = NullIterator.me;
			return ;
		}
		if (target.getClass().isArray()) {
			size = Array.getLength(target);
			iterator = new ArrayIterator(target, size);
			return ;
		}
		if (target instanceof Iterator) {
			size = -1;
			iterator = (Iterator<?>)target;
			return ;
		}
		if (target instanceof Iterable) {
			size = -1;
			iterator = ((Iterable<?>)target).iterator();
			return ;
		}
		if (target instanceof Enumeration) {
			ArrayList<?> list = Collections.list((Enumeration<?>)target);
			size = list.size();
			iterator = list.iterator();
			return ;
		}

		size = 1;
		iterator = new SingleObjectIterator(target);
	}

	Iterator<?> getIterator() {
		return iterator;
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

	public int getSize() {
		if (size >= 0) {
			return size;
		}
		throw new TemplateException("No such method getSize() of the iterator", location);
	}

	public boolean getFirst() {
		return index == 0;
	}

	public boolean getLast() {
		return !iterator.hasNext();
	}

	public boolean getOdd() {
		return index % 2 == 0;
	}

	public boolean getEven() {
		return index % 2 != 0;
	}
}

class MapIterator implements Iterator<Entry<Object, Object>> {

	private Iterator<Entry<Object, Object>> iterator;
	private ForEntry forEntry = new ForEntry();

	public MapIterator(Iterator<Entry<Object, Object>> iterator) {
		this.iterator = iterator;
	}

	public boolean hasNext() {
		return iterator.hasNext();
	}

	public Entry<Object, Object> next() {
		forEntry.init(iterator.next());
		return forEntry;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}

class ArrayIterator implements Iterator<Object> {

	private Object array;
	private int size;
	private int index;

	ArrayIterator(Object array, int size) {
		this.array = array;
		this.size = size;
		this.index = 0;
	}

	public boolean hasNext() {
		return index < size;
	}

	public Object next() {
		return Array.get(array, index++);
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}

class SingleObjectIterator implements Iterator<Object> {

	private Object target;
	private boolean hasNext = true;

	public SingleObjectIterator(Object target) {
		this.target = target;
	}

	public boolean hasNext() {
		return hasNext;
	}

	public Object next() {
		if (hasNext) {
			hasNext = false;
			return target;
		}
		throw new NoSuchElementException();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}

class NullIterator implements Iterator<Object> {

    static final Iterator<?> me = new NullIterator();

    private NullIterator() {
    }

    public boolean hasNext() {
        return false;
    }

    public Object next() {
        throw new NoSuchElementException();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}

