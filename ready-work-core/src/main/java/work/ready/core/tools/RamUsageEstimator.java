/**
 *
 * Original work Copyright lucene
 * Modified Copyright (c) 2020 WeiHua Lyu [ready.work]
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

import work.ready.core.server.Constant;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public final class RamUsageEstimator {

    public static final long ONE_KB = 1024;

    public static final long ONE_MB = ONE_KB * ONE_KB;

    public static final long ONE_GB = ONE_KB * ONE_MB;

    private RamUsageEstimator() {}

    public final static boolean COMPRESSED_REFS_ENABLED;

    public final static int NUM_BYTES_OBJECT_REF;

    public final static int NUM_BYTES_OBJECT_HEADER;

    public final static int NUM_BYTES_ARRAY_HEADER;

    public final static int NUM_BYTES_OBJECT_ALIGNMENT;

    public static final int QUERY_DEFAULT_RAM_BYTES_USED = 1024;

    public static final int UNKNOWN_DEFAULT_RAM_BYTES_USED = 256;

    public static final Map<Class<?>,Integer> primitiveSizes;

    static {
        Map<Class<?>, Integer> primitiveSizesMap = new IdentityHashMap<>();
        primitiveSizesMap.put(boolean.class, 1);
        primitiveSizesMap.put(byte.class, 1);
        primitiveSizesMap.put(char.class, Integer.valueOf(Character.BYTES));
        primitiveSizesMap.put(short.class, Integer.valueOf(Short.BYTES));
        primitiveSizesMap.put(int.class, Integer.valueOf(Integer.BYTES));
        primitiveSizesMap.put(float.class, Integer.valueOf(Float.BYTES));
        primitiveSizesMap.put(double.class, Integer.valueOf(Double.BYTES));
        primitiveSizesMap.put(long.class, Integer.valueOf(Long.BYTES));

        primitiveSizes = Collections.unmodifiableMap(primitiveSizesMap);
    }

    static final int LONG_SIZE, STRING_SIZE;

    static final boolean JVM_IS_HOTSPOT_64BIT;

    static final String MANAGEMENT_FACTORY_CLASS = "java.lang.management.ManagementFactory";
    static final String HOTSPOT_BEAN_CLASS = "com.sun.management.HotSpotDiagnosticMXBean";

    static {
        if (Constant.JRE_IS_64BIT) {

            boolean compressedOops = false;
            int objectAlignment = 8;
            boolean isHotspot = false;
            try {
                final Class<?> beanClazz = Class.forName(HOTSPOT_BEAN_CLASS);

                final Object hotSpotBean = Class.forName(MANAGEMENT_FACTORY_CLASS)
                        .getMethod("getPlatformMXBean", Class.class)
                        .invoke(null, beanClazz);
                if (hotSpotBean != null) {
                    isHotspot = true;
                    final Method getVMOptionMethod = beanClazz.getMethod("getVMOption", String.class);
                    try {
                        final Object vmOption = getVMOptionMethod.invoke(hotSpotBean, "UseCompressedOops");
                        compressedOops = Boolean.parseBoolean(
                                vmOption.getClass().getMethod("getValue").invoke(vmOption).toString()
                        );
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        isHotspot = false;
                    }
                    try {
                        final Object vmOption = getVMOptionMethod.invoke(hotSpotBean, "ObjectAlignmentInBytes");
                        objectAlignment = Integer.parseInt(
                                vmOption.getClass().getMethod("getValue").invoke(vmOption).toString()
                        );
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        isHotspot = false;
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                isHotspot = false;
            }
            JVM_IS_HOTSPOT_64BIT = isHotspot;
            COMPRESSED_REFS_ENABLED = compressedOops;
            NUM_BYTES_OBJECT_ALIGNMENT = objectAlignment;
            
            NUM_BYTES_OBJECT_REF = COMPRESSED_REFS_ENABLED ? 4 : 8;
            
            NUM_BYTES_OBJECT_HEADER = 8 + NUM_BYTES_OBJECT_REF;
            
            NUM_BYTES_ARRAY_HEADER = (int) alignObjectSize(NUM_BYTES_OBJECT_HEADER + Integer.BYTES);
        } else {
            JVM_IS_HOTSPOT_64BIT = false;
            COMPRESSED_REFS_ENABLED = false;
            NUM_BYTES_OBJECT_ALIGNMENT = 8;
            NUM_BYTES_OBJECT_REF = 4;
            NUM_BYTES_OBJECT_HEADER = 8;
            
            NUM_BYTES_ARRAY_HEADER = NUM_BYTES_OBJECT_HEADER + Integer.BYTES;
        }

        LONG_SIZE = (int) shallowSizeOfInstance(Long.class);
        STRING_SIZE = (int) shallowSizeOfInstance(String.class);
    }

    public static final long HASHTABLE_RAM_BYTES_PER_ENTRY =
            2L * NUM_BYTES_OBJECT_REF 
                    * 2;

    public static final long LINKED_HASHTABLE_RAM_BYTES_PER_ENTRY =
            HASHTABLE_RAM_BYTES_PER_ENTRY
                    + 2 * NUM_BYTES_OBJECT_REF;

    public static long alignObjectSize(long size) {
        size += (long) NUM_BYTES_OBJECT_ALIGNMENT - 1L;
        return size - (size % NUM_BYTES_OBJECT_ALIGNMENT);
    }

    public static long sizeOf(Long value) {
        return LONG_SIZE;
    }

    public static long sizeOf(byte[] arr) {
        return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + arr.length);
    }

    public static long sizeOf(boolean[] arr) {
        return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + arr.length);
    }

    public static long sizeOf(char[] arr) {
        return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) Character.BYTES * arr.length);
    }

    public static long sizeOf(short[] arr) {
        return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) Short.BYTES * arr.length);
    }

    public static long sizeOf(int[] arr) {
        return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) Integer.BYTES * arr.length);
    }

    public static long sizeOf(float[] arr) {
        return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) Float.BYTES * arr.length);
    }

    public static long sizeOf(long[] arr) {
        return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) Long.BYTES * arr.length);
    }

    public static long sizeOf(double[] arr) {
        return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) Double.BYTES * arr.length);
    }

    public static long sizeOf(String[] arr) {
        long size = shallowSizeOf(arr);
        for (String s : arr) {
            if (s == null) {
                continue;
            }
            size += sizeOf(s);
        }
        return size;
    }

    public static final int MAX_DEPTH = 1;

    public static long sizeOfMap(Map<?, ?> map) {
        return sizeOfMap(map, 0, UNKNOWN_DEFAULT_RAM_BYTES_USED);
    }

    public static long sizeOfMap(Map<?, ?> map, long defSize) {
        return sizeOfMap(map, 0, defSize);
    }

    private static long sizeOfMap(Map<?, ?> map, int depth, long defSize) {
        if (map == null) {
            return 0;
        }
        long size = shallowSizeOf(map);
        if (depth > MAX_DEPTH) {
            return size;
        }
        long sizeOfEntry = -1;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (sizeOfEntry == -1) {
                sizeOfEntry = shallowSizeOf(entry);
            }
            size += sizeOfEntry;
            size += sizeOfObject(entry.getKey(), depth, defSize);
            size += sizeOfObject(entry.getValue(), depth, defSize);
        }
        return alignObjectSize(size);
    }

    public static long sizeOfCollection(Collection<?> collection) {
        return sizeOfCollection(collection, 0, UNKNOWN_DEFAULT_RAM_BYTES_USED);
    }

    public static long sizeOfCollection(Collection<?> collection, long defSize) {
        return sizeOfCollection(collection, 0, defSize);
    }

    private static long sizeOfCollection(Collection<?> collection, int depth, long defSize) {
        if (collection == null) {
            return 0;
        }
        long size = shallowSizeOf(collection);
        if (depth > MAX_DEPTH) {
            return size;
        }
        
        size += NUM_BYTES_ARRAY_HEADER + collection.size() * NUM_BYTES_OBJECT_REF;
        for (Object o : collection) {
            size += sizeOfObject(o, depth, defSize);
        }
        return alignObjectSize(size);
    }

    public static long sizeOfObject(Object o) {
        return sizeOfObject(o, 0, UNKNOWN_DEFAULT_RAM_BYTES_USED);
    }

    public static long sizeOfObject(Object o, long defSize) {
        return sizeOfObject(o, 0, defSize);
    }

    private static long sizeOfObject(Object o, int depth, long defSize) {
        if (o == null) {
            return 0;
        }
        long size;
        if (o instanceof String) {
            size = sizeOf((String)o);
        } else if (o instanceof boolean[]) {
            size = sizeOf((boolean[])o);
        } else if (o instanceof byte[]) {
            size = sizeOf((byte[])o);
        } else if (o instanceof char[]) {
            size = sizeOf((char[])o);
        } else if (o instanceof double[]) {
            size = sizeOf((double[])o);
        } else if (o instanceof float[]) {
            size = sizeOf((float[])o);
        } else if (o instanceof int[]) {
            size = sizeOf((int[])o);
        } else if (o instanceof Long) {
            size = sizeOf((Long)o);
        } else if (o instanceof long[]) {
            size = sizeOf((long[])o);
        } else if (o instanceof short[]) {
            size = sizeOf((short[])o);
        } else if (o instanceof String[]) {
            size = sizeOf((String[]) o);
        } else if (o instanceof Map) {
            size = sizeOfMap((Map) o, ++depth, defSize);
        } else if (o instanceof Collection) {
            size = sizeOfCollection((Collection)o, ++depth, defSize);
        } else {
            if (defSize > 0) {
                size = defSize;
            } else {
                size = shallowSizeOf(o);
            }
        }
        return size;
    }

    public static long sizeOf(String s) {
        if (s == null) {
            return 0;
        }

        long size = STRING_SIZE + (long)NUM_BYTES_ARRAY_HEADER + (long)Character.BYTES * s.length();
        return alignObjectSize(size);
    }

    public static long shallowSizeOf(Object[] arr) {
        return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) NUM_BYTES_OBJECT_REF * arr.length);
    }

    public static long shallowSizeOf(Object obj) {
        if (obj == null) return 0;
        final Class<?> clz = obj.getClass();
        if (clz.isArray()) {
            return shallowSizeOfArray(obj);
        } else {
            return shallowSizeOfInstance(clz);
        }
    }

    public static long shallowSizeOfInstance(Class<?> clazz) {
        if (clazz.isArray())
            throw new IllegalArgumentException("This method does not work with array classes.");
        if (clazz.isPrimitive())
            return primitiveSizes.get(clazz);

        long size = NUM_BYTES_OBJECT_HEADER;

        for (;clazz != null; clazz = clazz.getSuperclass()) {
            final Class<?> target = clazz;
            final Field[] fields = AccessController.doPrivileged(new PrivilegedAction<Field[]>() {
                @Override
                public Field[] run() {
                    return target.getDeclaredFields();
                }
            });
            for (Field f : fields) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    size = adjustForField(size, f);
                }
            }
        }
        return alignObjectSize(size);
    }

    private static long shallowSizeOfArray(Object array) {
        long size = NUM_BYTES_ARRAY_HEADER;
        final int len = Array.getLength(array);
        if (len > 0) {
            Class<?> arrayElementClazz = array.getClass().getComponentType();
            if (arrayElementClazz.isPrimitive()) {
                size += (long) len * primitiveSizes.get(arrayElementClazz);
            } else {
                size += (long) NUM_BYTES_OBJECT_REF * len;
            }
        }
        return alignObjectSize(size);
    }

    static long adjustForField(long sizeSoFar, final Field f) {
        final Class<?> type = f.getType();
        final int fsize = type.isPrimitive() ? primitiveSizes.get(type) : NUM_BYTES_OBJECT_REF;
        
        return sizeSoFar + fsize;
    }

}
