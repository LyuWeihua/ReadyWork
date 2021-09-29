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

import work.ready.core.tools.validator.Assert;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;

public class BiTuple<V1, V2> implements Map<V1, V2>, Map.Entry<V1, V2>,
        Iterable<Object>, Externalizable, Cloneable {

    private static ThreadLocal<StringBuilder> threadSB = new ThreadLocal<StringBuilder>() {
        @Override protected StringBuilder initialValue() {
            StringBuilder sb = new StringBuilder(256);
            return sb;
        }
    };

    private static final long serialVersionUID = 0L;

    private V1 val1;

    private V2 val2;

    public BiTuple() {
        
    }

    public BiTuple(V1 val1, V2 val2) {
        this.val1 = val1;
        this.val2 = val2;
    }

    public BiTuple<V2, V1> swap() {
        return new BiTuple<>(val2, val1);
    }

    public V1 get1() {
        return val1;
    }

    public V2 get2() {
        return val2;
    }

    public void set1( V1 val1) {
        this.val1 = val1;
    }

    public void set2(V2 val2) {
        this.val2 = val2;
    }

    public void set(V1 val1, V2 val2) {
        set1(val1);
        set2(val2);
    }

    @Override public V1 getKey() {
        return val1;
    }

    @Override public V2 getValue() {
        return val2;
    }

    @Override public V2 setValue(V2 val) {
        V2 old = val2;

        set2(val);

        return old;
    }

    @Override public Iterator<Object> iterator() {
        return new Iterator<Object>() {
            
            private int nextIdx = 1;

            @Override public boolean hasNext() {
                return nextIdx < 3;
            }

            @Override public Object next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                Object res = null;

                if (nextIdx == 1)
                    res = get1();
                else if (nextIdx == 2)
                    res = get2();

                nextIdx++;

                return res;
            }

            @Override public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override public int size() {
        return val1 == null && val2 == null ? 0 : 1;
    }

    @Override public boolean isEmpty() {
        return size() == 0;
    }

    public static boolean eq(Object o1, Object o2) {
        return o1 == null ? o2 == null : o2 != null && (o1 == o2 || o1.equals(o2));
    }

    @Override public boolean containsKey(Object key) {
        return eq(val1, key);
    }

    @Override public boolean containsValue(Object val) {
        return eq(val2, val);
    }

    @Override public V2 get(Object key) {
        return containsKey(key) ? val2 : null;
    }

    @Override
    public V2 put(V1 key, V2 val) {
        V2 old = containsKey(key) ? val2 : null;

        set(key, val);

        return old;
    }

    @Override public V2 remove(Object key) {
        if (containsKey(key)) {
            V2 v2 = val2;

            val1 = null;
            val2 = null;

            return v2;
        }

        return null;
    }

    @Override public void putAll(Map<? extends V1, ? extends V2> m) {
        Assert.notNull(m,"m is required");
        Assert.isTrue(m.size() <= 1,"m.size() <= 1");

        for (Map.Entry<? extends V1, ? extends V2> e : m.entrySet())
            put(e.getKey(), e.getValue());
    }

    @Override public void clear() {
        val1 = null;
        val2 = null;
    }

    @Override public Set<V1> keySet() {
        return Collections.singleton(val1);
    }

    @Override public Collection<V2> values() {
        return Collections.singleton(val2);
    }

    @Override public Set<Map.Entry<V1, V2>> entrySet() {
        return isEmpty() ?
                Collections.<Entry<V1,V2>>emptySet() :
                Collections.<Entry<V1, V2>>singleton(this);
    }

    @Override public Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException ignore) {
            throw new InternalError();
        }
    }

    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(val1);
        out.writeObject(val2);
    }

    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        val1 = (V1)in.readObject();
        val2 = (V2)in.readObject();
    }

    @Override public int hashCode() {
        return val1 == null ? 0 : val1.hashCode() * 31 + (val2 == null ? 0 : val2.hashCode());
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof BiTuple))
            return false;

        BiTuple<?, ?> t = (BiTuple<?, ?>)o;

        return eq(val1, t.val1) && eq(val2, t.val2);
    }

    @Override public String toString() {
        return super.toString();
    }
}

