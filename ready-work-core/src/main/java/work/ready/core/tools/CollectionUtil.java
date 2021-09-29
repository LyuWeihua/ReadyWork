/**
 *
 * Original work Copyright apache, Spring
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
import java.util.*;

public abstract class CollectionUtil {

	public static boolean isEmpty(Collection<?> collection) {
		return (collection == null || collection.isEmpty());
	}

	public static boolean isEmpty(Map<?, ?> map) {
		return (map == null || map.isEmpty());
	}

	public static boolean isEmpty(Object[] array) {
		return (array == null || array.length == 0);
	}

	@SuppressWarnings("rawtypes")
	public static List arrayToList(Object source) {
		return Arrays.asList(ObjectUtil.toObjectArray(source));
	}

	public static String[] sortStringArray(String[] array) {
		if (CollectionUtil.isEmpty(array)) {
			return new String[0];
		}

		Arrays.sort(array);
		return array;
	}

	public static String[] toStringArray(Collection<String> collection) {
		return collection.toArray(new String[0]);
	}

	public static String[] toStringArray(Enumeration<String> enumeration) {
		return toStringArray(Collections.list(enumeration));
	}

	public static String[] trimArrayElements(String[] array) {
		if (CollectionUtil.isEmpty(array)) {
			return array;
		}

		String[] result = new String[array.length];
		for (int i = 0; i < array.length; i++) {
			String element = array[i];
			result[i] = (element != null ? element.trim() : null);
		}
		return result;
	}

	public static String[] removeDuplicateStrings(String[] array) {
		if (CollectionUtil.isEmpty(array)) {
			return array;
		}

		Set<String> set = new LinkedHashSet<>(Arrays.asList(array));
		return toStringArray(set);
	}

	@SuppressWarnings("unchecked")
	public static <E> void mergeArrayIntoCollection(Object array, Collection<E> collection) {
		if (collection == null) {
			throw new IllegalArgumentException("Collection must not be null");
		}
		Object[] arr = ObjectUtil.toObjectArray(array);
		for (Object elem : arr) {
			collection.add((E) elem);
		}
	}

	public static String[] mergeStringArrays(String[] array1, String[] array2) {
		if (CollectionUtil.isEmpty(array1)) {
			return array2;
		}
		if (CollectionUtil.isEmpty(array2)) {
			return array1;
		}

		List<String> result = new ArrayList<>();
		result.addAll(Arrays.asList(array1));
		for (String str : array2) {
			if (!result.contains(str)) {
				result.add(str);
			}
		}
		return toStringArray(result);
	}

	public static String[] concatenateStringArrays(String[] array1, String[] array2) {
		if (CollectionUtil.isEmpty(array1)) {
			return array2;
		}
		if (CollectionUtil.isEmpty(array2)) {
			return array1;
		}

		String[] newArr = new String[array1.length + array2.length];
		System.arraycopy(array1, 0, newArr, 0, array1.length);
		System.arraycopy(array2, 0, newArr, array1.length, array2.length);
		return newArr;
	}

	public static String[] addStringToArray(String[] array, String str) {
		if (CollectionUtil.isEmpty(array)) {
			return new String[] {str};
		}

		String[] newArr = new String[array.length + 1];
		System.arraycopy(array, 0, newArr, 0, array.length);
		newArr[array.length] = str;
		return newArr;
	}

	@SuppressWarnings("unchecked")
	public static <K, V> void mergePropertiesIntoMap(Properties props, Map<K, V> map) {
		if (map == null) {
			throw new IllegalArgumentException("Map must not be null");
		}
		if (props != null) {
			for (Enumeration<?> en = props.propertyNames(); en.hasMoreElements();) {
				String key = (String) en.nextElement();
				Object value = props.get(key);
				if (value == null) {

					value = props.getProperty(key);
				}
				map.put((K) key, (V) value);
			}
		}
	}

	public static boolean contains(Iterator<?> iterator, Object element) {
		if (iterator != null) {
			while (iterator.hasNext()) {
				Object candidate = iterator.next();
				if (ObjectUtil.nullSafeEquals(candidate, element)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean contains(Enumeration<?> enumeration, Object element) {
		if (enumeration != null) {
			while (enumeration.hasMoreElements()) {
				Object candidate = enumeration.nextElement();
				if (ObjectUtil.nullSafeEquals(candidate, element)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean containsInstance(Collection<?> collection, Object element) {
		if (collection != null) {
			for (Object candidate : collection) {
				if (candidate == element) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean containsAny(Collection<?> source, Collection<?> candidates) {
		if (isEmpty(source) || isEmpty(candidates)) {
			return false;
		}
		for (Object candidate : candidates) {
			if (source.contains(candidate)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public static <E> E findFirstMatch(Collection<?> source, Collection<E> candidates) {
		if (isEmpty(source) || isEmpty(candidates)) {
			return null;
		}
		for (Object candidate : candidates) {
			if (source.contains(candidate)) {
				return (E) candidate;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T> T findValueOfType(Collection<?> collection, Class<T> type) {
		if (isEmpty(collection)) {
			return null;
		}
		T value = null;
		for (Object element : collection) {
			if (type == null || type.isInstance(element)) {
				if (value != null) {
					
					return null;
				}
				value = (T) element;
			}
		}
		return value;
	}

	public static Object findValueOfType(Collection<?> collection, Class<?>[] types) {
		if (isEmpty(collection) || ObjectUtil.isEmpty(types)) {
			return null;
		}
		for (Class<?> type : types) {
			Object value = findValueOfType(collection, type);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	public static boolean hasUniqueObject(Collection<?> collection) {
		if (isEmpty(collection)) {
			return false;
		}
		boolean hasCandidate = false;
		Object candidate = null;
		for (Object elem : collection) {
			if (!hasCandidate) {
				hasCandidate = true;
				candidate = elem;
			} else if (candidate != elem) {
				return false;
			}
		}
		return true;
	}

	public static Class<?> findCommonElementType(Collection<?> collection) {
		if (isEmpty(collection)) {
			return null;
		}
		Class<?> candidate = null;
		for (Object val : collection) {
			if (val != null) {
				if (candidate == null) {
					candidate = val.getClass();
				} else if (candidate != val.getClass()) {
					return null;
				}
			}
		}
		return candidate;
	}

	public static <A, E extends A> A[] toArray(Enumeration<E> enumeration, A[] array) {
		ArrayList<A> elements = new ArrayList<A>();
		while (enumeration.hasMoreElements()) {
			elements.add(enumeration.nextElement());
		}
		return elements.toArray(array);
	}

	public static <T> T[] appendArray(T[] src,T... specific) {
		Class<?> type = src.getClass().getComponentType();
		T[] temp = (T[]) Array.newInstance(type,src.length + specific.length);
		System.arraycopy(src, 0, temp, 0, src.length);
		for (int i = 0; i <specific.length ; i++) {
			temp[src.length+i] = specific[i];
		}
		return  temp;
	}

	public static <T> T[] insertArray(T[] src,T... specific) {
		Class<?> type=src.getClass().getComponentType();
		T[] temp = (T[]) Array.newInstance(type,src.length + specific.length);
		System.arraycopy(src, 0, temp, specific.length, src.length);
		for (int i = 0; i < specific.length ; i++) {
			temp[i] = specific[i];
		}
		return  temp;
	}

	public static <T> T[] arrayRemove(T[] content, T specific) {
		int len = content.length;
		for (int i = 0; i < content.length; i++) {
			if (content[i].equals(specific)) {
				System.arraycopy(content, i + 1, content, i, len - 1 - i);
				break;
			}
		}
		return Arrays.copyOf(content, len - 1);
	}

	public static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - RamUsageEstimator.NUM_BYTES_ARRAY_HEADER;
	public static int oversize(int minTargetSize, int bytesPerElement) {

		if (minTargetSize < 0) {
			
			throw new IllegalArgumentException("invalid array size " + minTargetSize);
		}

		if (minTargetSize == 0) {
			
			return 0;
		}

		if (minTargetSize > MAX_ARRAY_LENGTH) {
			throw new IllegalArgumentException("requested array size " + minTargetSize + " exceeds maximum array in java (" + MAX_ARRAY_LENGTH + ")");
		}

		int extra = minTargetSize >> 3;

		if (extra < 3) {

			extra = 3;
		}

		int newSize = minTargetSize + extra;

		if (newSize+7 < 0 || newSize+7 > MAX_ARRAY_LENGTH) {
			
			return MAX_ARRAY_LENGTH;
		}

		if (Constant.JRE_IS_64BIT) {
			
			switch(bytesPerElement) {
				case 4:
					
					return (newSize + 1) & 0x7ffffffe;
				case 2:
					
					return (newSize + 3) & 0x7ffffffc;
				case 1:
					
					return (newSize + 7) & 0x7ffffff8;
				case 8:
					
				default:
					
					return newSize;
			}
		} else {
			
			switch(bytesPerElement) {
				case 2:
					
					return (newSize + 1) & 0x7ffffffe;
				case 1:
					
					return (newSize + 3) & 0x7ffffffc;
				case 4:
				case 8:
					
				default:
					
					return newSize;
			}
		}
	}

	public static byte[] copyOfSubArray(byte[] array, int from, int to) {
		final byte[] copy = new byte[to-from];
		System.arraycopy(array, from, copy, 0, to-from);
		return copy;
	}

	public static char[] copyOfSubArray(char[] array, int from, int to) {
		final char[] copy = new char[to-from];
		System.arraycopy(array, from, copy, 0, to-from);
		return copy;
	}

	public static short[] copyOfSubArray(short[] array, int from, int to) {
		final short[] copy = new short[to-from];
		System.arraycopy(array, from, copy, 0, to-from);
		return copy;
	}

	public static int[] copyOfSubArray(int[] array, int from, int to) {
		final int[] copy = new int[to-from];
		System.arraycopy(array, from, copy, 0, to-from);
		return copy;
	}

	public static long[] copyOfSubArray(long[] array, int from, int to) {
		final long[] copy = new long[to-from];
		System.arraycopy(array, from, copy, 0, to-from);
		return copy;
	}

	public static float[] copyOfSubArray(float[] array, int from, int to) {
		final float[] copy = new float[to-from];
		System.arraycopy(array, from, copy, 0, to-from);
		return copy;
	}

	public static double[] copyOfSubArray(double[] array, int from, int to) {
		final double[] copy = new double[to-from];
		System.arraycopy(array, from, copy, 0, to-from);
		return copy;
	}

	public static <T> T[] copyOfSubArray(T[] array, int from, int to) {
		final int subLength = to - from;
		final Class<? extends Object[]> type = array.getClass();
		@SuppressWarnings("unchecked")
		final T[] copy = (type == Object[].class)
				? (T[]) new Object[subLength]
				: (T[]) Array.newInstance(type.getComponentType(), subLength);
		System.arraycopy(array, from, copy, 0, subLength);
		return copy;
	}

	public static int capacity(int size) {
		int capacity;
		if (size < 3) {
			capacity = size + 1;
		} else {
			capacity = (int) ((float) size / 0.75f + 1);
		}
		return capacity;
	}
}
