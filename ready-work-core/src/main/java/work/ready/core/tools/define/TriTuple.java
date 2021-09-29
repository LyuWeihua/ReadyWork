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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class TriTuple<V1, V2, V3> implements Iterable<Object>, Externalizable, Cloneable {
    
    private static final long serialVersionUID = 0L;

    private V1 val1;

    private V2 val2;

    private V3 val3;

    public TriTuple() {
        
    }

    public TriTuple(V1 val1, V2 val2, V3 val3) {
        this.val1 = val1;
        this.val2 = val2;
        this.val3 = val3;
    }

    public V1 get1() {
        return val1;
    }

    public V2 get2() {
        return val2;
    }

    public V3 get3() {
        return val3;
    }

    public void set1(V1 val1) {
        this.val1 = val1;
    }

    public void set2(V2 val2) {
        this.val2 = val2;
    }

    public void set3(V3 val3) {
        this.val3 = val3;
    }

    public void set(V1 val1, V2 val2, V3 val3) {
        set1(val1);
        set2(val2);
        set3(val3);
    }

    @Override public Iterator<Object> iterator() {
        return new Iterator<Object>() {
            private int nextIdx = 1;

            @Override public boolean hasNext() {
                return nextIdx < 4;
            }

            @Override public Object next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                Object res = null;

                if (nextIdx == 1)
                    res = get1();
                else if (nextIdx == 2)
                    res = get2();
                else if (nextIdx == 3)
                    res = get3();

                nextIdx++;

                return res;
            }

            @Override public void remove() {
                throw new UnsupportedOperationException();
            }
        };
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
        out.writeObject(val3);
    }

    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        val1 = (V1)in.readObject();
        val2 = (V2)in.readObject();
        val3 = (V3)in.readObject();
    }

    public static boolean eq(Object o1, Object o2) {
        return o1 == null ? o2 == null : o2 != null && (o1 == o2 || o1.equals(o2));
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof TriTuple))
            return false;

        TriTuple<?, ?, ?> t = (TriTuple<?, ?, ?>)o;

        return eq(val1, t.val1) && eq(val2, t.val2) && eq(val3, t.val3);
    }

    @Override public int hashCode() {
        int res = val1 != null ? val1.hashCode() : 0;

        res = 17 * res + (val2 != null ? val2.hashCode() : 0);
        res = 31 * res + (val3 != null ? val3.hashCode() : 0);

        return res;
    }

    @Override public String toString() {
        return super.toString();
    }
}
