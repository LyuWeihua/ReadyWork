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

package work.ready.core.tools.validator;

import work.ready.core.exception.IllegalArgumentException;
import work.ready.core.tools.*;
import work.ready.core.tools.validator.annotation.*;
import work.ready.core.tools.validator.annotation.Date;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;

public class Assert {

    public static <T> AssertThat that(T value) {
        return new AssertThat<>(value);
    }

    public static AssertThat verify(Object value) throws Exception {
        AssertThat anAssert = new AssertThat<>(value);
        anAssert.notNull("The object to be verified cannot be null");
        Class classType = value.getClass();
        Set<Field> fieldSet = AssertCache.getInstance().getFieldsByClass(classType);
        if (null == fieldSet) {
            synchronized (Assert.class) {
                fieldSet = AssertCache.getInstance().getFieldsByClass(classType);
                if(null == fieldSet) {
                    fieldSet = getFieldsByClass(value.getClass());
                    AssertCache.getInstance().setClassFields(classType, fieldSet);
                }
            }
        }
        if (CollectionUtil.isEmpty(fieldSet)) {
            return anAssert;
        }
        for (Field field : fieldSet) {
            Annotation[] annotations = AssertCache.getInstance().getAnnotationsByField(field);
            if (null == annotations) {
                synchronized (Assert.class) {
                    annotations = AssertCache.getInstance().getAnnotationsByField(field);
                    if(null == annotations) {
                        annotations = field.getAnnotations();
                        AssertCache.getInstance().setFieldAnnotations(field, annotations);
                    }
                }
            }
            if (CollectionUtil.isEmpty(annotations)) {
                return anAssert;
            }
            Object fieldValue = field.get(value);
            for (Annotation annotation : annotations) {
                if (annotation instanceof NotNull) {
                    anAssert.and(fieldValue).notNull(((NotNull) annotation).message());
                } else if (annotation instanceof LessThan) {
                    LessThan max = (LessThan) annotation;
                    anAssert.and(fieldValue).lessThan(max.value(), max.message());
                } else if (annotation instanceof GreaterThan) {
                    GreaterThan min = (GreaterThan) annotation;
                    anAssert.and(fieldValue).greaterThan(min.value(), min.message());
                } else if (annotation instanceof MaxLength) {
                    MaxLength maxLength = (MaxLength) annotation;
                    anAssert.and(fieldValue).maxLength(maxLength.value(), maxLength.message());
                } else if (annotation instanceof MinLength) {
                    MinLength minLength = (MinLength) annotation;
                    anAssert.and(fieldValue).minLength(minLength.value(), minLength.message());
                } else if (annotation instanceof Email) {
                    anAssert.and(fieldValue).isEmail(((Email) annotation).message());
                } else if (annotation instanceof Mobile) {
                    anAssert.and(fieldValue).isMobile(((Mobile) annotation).message());
                } else if (annotation instanceof IdCard) {
                    anAssert.and(fieldValue).isIdCard(((IdCard) annotation).message());
                } else if (annotation instanceof Regex) {
                    Regex regex = (Regex) annotation;
                    anAssert.and(fieldValue).matches(regex.value(), regex.message());
                } else if (annotation instanceof Date) {
                    Date date = (Date) annotation;
                    String format = date.format();
                    anAssert.and(fieldValue).isDate(format, date.message());
                } else if (annotation instanceof Chinese) {
                    anAssert.and(fieldValue).isChinese(((Chinese) annotation).message());
                } else if (annotation instanceof English) {
                    anAssert.and(fieldValue).isEnglish(((English) annotation).message());
                } else if (annotation instanceof IP) {
                    anAssert.and(fieldValue).isIp(((IP) annotation).message());
                }
            }
        }
        return anAssert;
    }

    public static Set<Field> getFieldsByClass(Class cls) {
        Set<Field> fieldSet = new HashSet<>();
        for (Class<?> clazz = cls; clazz != Object.class; clazz = clazz.getSuperclass()) {
            Field[] fields = ClassUtil.getDeclaredFields(clazz);
            if (CollectionUtil.isEmpty(fields)) {
                continue;
            }
            for (Field field : fields) {
                if (!field.getName().equals("class") && !field.getName().equals("serialVersionUID")) {
                    fieldSet.add(field);
                }
            }
        }
        return fieldSet;
    }

    public static <T> T isNull(T value, String message, Object... args) {
        if (null != value) {
            throw new IllegalArgumentException(message, args);
        }
        return value;
    }

    public static <T> T notNull(T value, String message, Object... args) {
        if (null == value) {
            throw new IllegalArgumentException(message, args);
        }
        return value;
    }

    public static <T> T isTrue(T value, String message, Object... args) {
        if(!(value instanceof Boolean) || !(Boolean)value){
            throw new IllegalArgumentException(message, args);
        }
        return value;
    }

    public static <T> T notTrue(T value, String message, Object... args) {
        if((value instanceof Boolean) && (Boolean)value){
            throw new IllegalArgumentException(message, args);
        }
        return value;
    }

    public static Number notEmpty(Number value, String message, Object... args) {
        if (value == null) throw new IllegalArgumentException(message, args);
        return value;
    }

    public static String notEmpty(String value, String message, Object... args) {
        if (!StrUtil.hasLength(value)) throw new IllegalArgumentException(message, args);
        return value;
    }

    public static <T> Collection<T> notEmpty(Collection<T> value, String message, Object... args) {
        if (CollectionUtil.isEmpty(value)) throw new IllegalArgumentException(message, args);
        return value;
    }

    public static <K,V> Map<K ,V> notEmpty(Map<K,V> value, String message, Object... args) {
        if (CollectionUtil.isEmpty(value)) throw new IllegalArgumentException(message, args);
        return value;
    }

    public static <T> T[] notEmpty(T[] value, String message, Object... args) {
        if (CollectionUtil.isEmpty(value)) throw new IllegalArgumentException(message, args);
        return value;
    }

    public static <T> T[] noNullElements(T[] array, String message, Object... args){
        if (array != null) {
            for (T element : array) {
                if (element == null) {
                    throw new IllegalArgumentException(message, args);
                }
            }
        }
        return array;
    }

    public static <T> Collection<T> noNullElements(Collection<T> collection, String message, Object... args){
        if (collection != null) {
            for (T element : collection) {
                if (element == null) {
                    throw new IllegalArgumentException(message, args);
                }
            }
        }
        return collection;
    }

    public static <K,V> Map<K,V> noNullElements(Map<K,V> map, String message, Object... args){
        if (map != null) {
            for (V element : map.values()) {
                if (element == null) {
                    throw new IllegalArgumentException(message, args);
                }
            }
        }
        return map;
    }

    public static String doesNotContain(String textToSearch, String substring, String message, Object... args) {
        if (StrUtil.hasLength(textToSearch) && StrUtil.hasLength(substring) &&
                textToSearch.contains(substring)) {
            throw new IllegalArgumentException(message, args);
        }
        return textToSearch;
    }

    public static <T> Collection<T> doesNotContain(Collection<T> collection, T item, String message, Object... args) {
        if (collection != null && !collection.isEmpty() && collection.contains(item)) {
            throw new IllegalArgumentException(message, args);
        }
        return collection;
    }

    public static <K,V> Map<K,V> doesNotContain(Map<K,V> map, V item, String message, Object... args) {
        if (map != null && !map.isEmpty() && map.values().contains(item)) {
            throw new IllegalArgumentException(message, args);
        }
        return map;
    }

    public static <T> T[] doesNotContain(T[] array, T item, String message, Object... args) {
        if (array != null && array.length != 0 && Arrays.asList(array).contains(item)) {
            throw new IllegalArgumentException(message, args);
        }
        return array;
    }

    public static String contains(String textToSearch, String substring, String message, Object... args) {
        if (StrUtil.hasLength(textToSearch) && StrUtil.hasLength(substring) &&
                textToSearch.contains(substring)) {
        } else {
            throw new IllegalArgumentException(message, args);
        }
        return textToSearch;
    }

    public static <T> Collection<T> contains(Collection<T> collection, T item, String message, Object... args) {
        if (collection != null && !collection.isEmpty() && collection.contains(item)) {
        } else {
            throw new IllegalArgumentException(message, args);
        }
        return collection;
    }

    public static <K,V> Map<K,V> contains(Map<K,V> map, V item, String message, Object... args) {
        if (map != null && !map.isEmpty() && map.values().contains(item)) {
        } else {
            throw new IllegalArgumentException(message, args);
        }
        return map;
    }

    public static <T> T[] contains(T[] array, T item, String message, Object... args) {
        if (array != null && array.length != 0 && Arrays.asList(array).contains(item)) {
        } else {
            throw new IllegalArgumentException(message, args);
        }
        return array;
    }

    public static void matches(Object value, String regex, String message, Object... args) {
        if (value != null && !Pattern.compile(regex).matcher(value.toString()).matches()) {
            throw new IllegalArgumentException(message, args);
        }
    }

    public static void equals(Object value, Object compare, String message, Object... args) {
        if((compare != null && !compare.equals(value)) || (value != null && !value.equals(compare))){
            throw new IllegalArgumentException(message, args);
        }
    }

    public static void notEqual(Object value, Object compare, String message, Object... args) {
        if((compare != null && !compare.equals(value)) || (value != null && !value.equals(compare))){
        } else {
            throw new IllegalArgumentException(message, args);
        }
    }

    public static String maxLength(String value, int max, String message, Object... args) {
        if (value != null) {
            if (value.length() > max)
                throw new IllegalArgumentException(message, args);
        }
        return value;
    }

    public static <T> Collection<T> maxLength(Collection<T> value, int max, String message, Object... args) {
        if (null != value) {
            if (value.size() > max)
                throw new IllegalArgumentException(message, args);
        }
        return value;
    }

    public static <K,V> Map<K,V> maxLength(Map<K,V> value, int max, String message, Object... args) {
        if (null != value) {
            if (value.size() > max)
                throw new IllegalArgumentException(message, args);
        }
        return value;
    }

    public static <T> T[] maxLength(T[] value, int max, String message, Object... args) {
        if (null != value) {
            if (value.length > max)
                throw new IllegalArgumentException(message, args);
        }
        return value;
    }

    public static String minLength(String value, int min, String message, Object... args) {
        if (null != value) {
            if (value.length() < min) throw new IllegalArgumentException(message, args);
        }
        return value;
    }

    public static <T> Collection<T> minLength(Collection<T> value, int min, String message, Object... args) {
        if (null != value) {
            if (value.size() < min)
                throw new IllegalArgumentException(message, args);
        }
        return value;
    }

    public static <K,V> Map<K,V> minLength(Map<K,V> value, int min, String message, Object... args) {
        if (null != value) {
            if (value.size() < min)
                throw new IllegalArgumentException(message, args);
        }
        return value;
    }

    public static <T> T[] minLength(T[] value, int min, String message, Object... args) {
        if (null != value) {
            if (value.length < min)
                throw new IllegalArgumentException(message, args);
        }
        return value;
    }

    public static int lessThan(int value, int max, String message, Object... args) {
        if (value >= max) throw new IllegalArgumentException(message, args);
        return value;
    }

    public static long lessThan(long value, long max, String message, Object... args) {
        if (value >= max) throw new IllegalArgumentException(message, args);
        return value;
    }

    public static float lessThan(float value, float max, String message, Object... args) {
        if (value >= max) throw new IllegalArgumentException(message, args);
        return value;
    }

    public static double lessThan(double value, double max, String message, Object... args) {
        if (value >= max) throw new IllegalArgumentException(message, args);
        return value;
    }

    public static byte lessThan(byte value, byte max, String message, Object... args) {
        if (value >= max) throw new IllegalArgumentException(message, args);
        return value;
    }

    public static short lessThan(short value, short max, String message, Object... args) {
        if (value >= max) throw new IllegalArgumentException(message, args);
        return value;
    }

    public static int greaterThan(int value, int min, String message, Object... args) {
        if (value <= min) throw new IllegalArgumentException(message, args);
        return value;
    }

    public static long greaterThan(long value, long min, String message, Object... args) {
        if (value <= min) throw new IllegalArgumentException(message, args);
        return value;
    }

    public static float greaterThan(float value, float min, String message, Object... args) {
        if (value <= min) throw new IllegalArgumentException(message, args);
        return value;
    }

    public static double greaterThan(double value, double min, String message, Object... args) {
        if (value <= min) throw new IllegalArgumentException(message, args);
        return value;
    }

    public static byte greaterThan(byte value, byte min, String message, Object... args) {
        if (value <= min) throw new IllegalArgumentException(message, args);
        return value;
    }

    public static short greaterThan(short value, short min, String message, Object... args) {
        if (value <= min) throw new IllegalArgumentException(message, args);
        return value;
    }

    public static <T> T isChinese(T value, String message, Object... args) {
        if (value == null || !StrUtil.isChinese(value.toString())) {
            throw new IllegalArgumentException(message, args);
        }
        return value;
    }

    private final static Pattern REGEX_ENGLISH = Pattern.compile("^[a-zA-Z]+$");
    public static <T> T isEnglish(T value, String message, Object... args) {
        if(value == null || !REGEX_ENGLISH.matcher(value.toString()).matches())
            throw new IllegalArgumentException(message, args);
        return value;
    }

    public static <T> T isMobile(T value, String message, Object... args) {
        if(value == null || !StrUtil.isMobile(value.toString()))
            throw new IllegalArgumentException(message, args);
        return value;
    }

    public static <T> T isEmail(T value, String message, Object... args) {
        if(value == null || !StrUtil.isEmail(value.toString()))
            throw new IllegalArgumentException(message, args);
        return value;
    }

    public static <T> T isDate(T value, String format, String message, Object... args) {
        try {
            new java.text.SimpleDateFormat(format).parse(value.toString());
        } catch (ParseException e) {
            throw new IllegalArgumentException(message, args);
        }
        return value;
    }

    public static <T> T isIp(T value, String message, Object... args) {
        if(!NetUtil.isIPv4Address(value.toString()))
            throw new IllegalArgumentException(message, args);
        return value;
    }

    public static <T> T hasText(T text, String message, Object... args){
        if (!(text instanceof String) || StrUtil.isBlank((String) text)) {
            throw new IllegalArgumentException(message, args);
        }
        return text;
    }

    public static <T> T hasLength(T text, String message, Object... args){
        if (!(text instanceof String) || !StrUtil.hasLength((String) text)) {
            throw new IllegalArgumentException(message, args);
        }
        return text;
    }

    public static void instanceOf(Object obj, Class<?> type, String message, Object... args){
        if (type == null || !type.isInstance(obj)) {
            throw new IllegalArgumentException(message, args);
        }
    }

    public static void assignable(Class<?> subType, Class<?> superType, String message, Object... args) {
        if (subType == null || !superType.isAssignableFrom(subType)) {
            throw new IllegalArgumentException(message, args);
        }
    }

    private final static int[] factorArr = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2, 1};
    private final static char[] parityBit = {'1', '0', 'x', '9', '8', '7', '6', '5', '4', '3', '2'};
    public static void isIdCard(Object value, String message, Object... args) {
        if(value == null) throw new IllegalArgumentException(message, args);
        String idCard = value.toString().toLowerCase();
        int length = idCard.length();
        
        if (length != 15 && length != 18) {
            throw new IllegalArgumentException(message, args);
        }
        
        if (!isArea(idCard.substring(0, 2))) {
            throw new IllegalArgumentException(message, args);
        }
        
        if (15 == length && !isDate6(idCard.substring(6, 12))) {
            throw new IllegalArgumentException(message, args);
        }
        if (18 == length && !isDate8(idCard.substring(6, 14))) {
            throw new IllegalArgumentException(message, args);
        }
        
        if (18 == length) {
            char[] idCardArray = idCard.toCharArray();
            int sum = 0;
            for (int i = 0; i < idCardArray.length - 1; i++) {
                if (idCardArray[i] < '0' || idCardArray[i] > '9') {
                    throw new IllegalArgumentException(message, args);
                }
                sum += (idCardArray[i] - '0') * factorArr[i];
            }
            if (idCardArray[idCardArray.length - 1] != parityBit[sum % 11]) {
                throw new IllegalArgumentException(message, args);
            }
        }
    }

    private final static Map<Integer, String> zoneNum = new HashMap<>();
    static {
        zoneNum.put(11, "北京");
        zoneNum.put(12, "天津");
        zoneNum.put(13, "河北");
        zoneNum.put(14, "山西");
        zoneNum.put(15, "内蒙古");
        zoneNum.put(21, "辽宁");
        zoneNum.put(22, "吉林");
        zoneNum.put(23, "黑龙江");
        zoneNum.put(31, "上海");
        zoneNum.put(32, "江苏");
        zoneNum.put(33, "浙江");
        zoneNum.put(34, "安徽");
        zoneNum.put(35, "福建");
        zoneNum.put(36, "江西");
        zoneNum.put(37, "山东");
        zoneNum.put(41, "河南");
        zoneNum.put(42, "湖北");
        zoneNum.put(43, "湖南");
        zoneNum.put(44, "广东");
        zoneNum.put(45, "广西");
        zoneNum.put(46, "海南");
        zoneNum.put(50, "重庆");
        zoneNum.put(51, "四川");
        zoneNum.put(52, "贵州");
        zoneNum.put(53, "云南");
        zoneNum.put(54, "西藏");
        zoneNum.put(61, "陕西");
        zoneNum.put(62, "甘肃");
        zoneNum.put(63, "青海");
        zoneNum.put(64, "新疆");
        zoneNum.put(71, "台湾");
        zoneNum.put(81, "香港");
        zoneNum.put(82, "澳门");
        zoneNum.put(91, "外国");
    }
    private final static Pattern REGEX_AREA = Pattern.compile("^[0-9]{2}$");
    private static boolean isArea(String area) {
        return REGEX_AREA.matcher(area).matches() && zoneNum.containsKey(Integer.valueOf(area));
    }

    private static boolean isDate6(String date) {
        return isDate8("20" + date);
    }

    private final static int MIN_YEAR = 1700;
    private final static int MAX_YEAR = 2500;
    private final static Pattern REGEX_DATE8 = Pattern.compile("^[0-9]{8}$");
    private static boolean isDate8(String date) {
        if (!REGEX_DATE8.matcher(date).matches()) {
            return false;
        }
        int[] iaMonthDays = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        int year = Integer.parseInt(date.substring(0, 4));
        int month = Integer.parseInt(date.substring(4, 6));
        int day = Integer.parseInt(date.substring(6, 8));
        if (((year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0)) iaMonthDays[1] = 29;
        return !(year < MIN_YEAR || year > MAX_YEAR) && !(month < 1 || month > 12) && !(day < 1 || day > iaMonthDays[month - 1]);
    }

}
