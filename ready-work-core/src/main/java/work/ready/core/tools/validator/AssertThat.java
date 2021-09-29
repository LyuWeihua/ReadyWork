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
import work.ready.core.tools.StrUtil;

import java.util.Collection;
import java.util.Map;

public class AssertThat<T> {

    private T value;

    AssertThat(T value) {
        this.value = value;
    }

    public AssertThat <T> and(T value) {
        this.value = value;
        return this;
    }

    public AssertThat isTrue(String code, Object... args) {
        Assert.isTrue(value, code, args);
        return this;
    }

    public AssertThat notTrue(String code, Object... args) {
        Assert.notTrue(value, code, args);
        return this;
    }

    public AssertThat isNull(String code, Object... args) {
        Assert.isNull(value, code, args);
        return this;
    }

    public AssertThat notNull(String code, Object... args) {
        Assert.notNull(value, code, args);
        return this;
    }

    public AssertThat hasLength(String code, Object... args) {
        Assert.hasLength(value, code, args);
        return this;
    }

    public AssertThat hasText(String code, Object... args) {
        Assert.hasText(value, code, args);
        return this;
    }

    public AssertThat doesNotContain(Object item, String code, Object... args) {
        if (null == value || item == null) {
            return this;
        }
        if (value instanceof String && item instanceof String) {
            Assert.doesNotContain((String) value, (String) item, code, args);
        } else if (value instanceof Collection) {
            Assert.doesNotContain((Collection) value, item, code, args);
        } else if (value instanceof Map) {
            Assert.doesNotContain((Map) value, item, code, args);
        } else if (value instanceof Object[]) {
            Assert.doesNotContain((Object[]) value, item, code, args);
        }
        return this;
    }

    public AssertThat contains(Object item, String code, Object... args) {
        if (null == value || item == null) {
            return this;
        }
        if (value instanceof String && item instanceof String) {
            Assert.contains((String) value, (String) item, code, args);
        } else if (value instanceof Collection) {
            Assert.contains((Collection) value, item, code, args);
        } else if (value instanceof Map) {
            Assert.contains((Map) value, item, code, args);
        } else if (value instanceof Object[]) {
            Assert.contains((Object[]) value, item, code, args);
        }
        return this;
    }

    public AssertThat noNullElements(String code, Object... args) {
        if (null == value) {
            return this;
        }
        if (value instanceof Object[]) {
            Assert.noNullElements((Object[]) value, code, args);
        } else if(value instanceof Collection) {
            Assert.noNullElements((Collection) value, code, args);
        } else if(value instanceof Map) {
            Assert.noNullElements((Map) value, code, args);
        }
        return this;
    }

    public AssertThat notEmpty(String code, Object... args) {
        Assert.notNull(value, code, args);
        if (value instanceof String) {
            Assert.notEmpty((String) value, code, args);
        } else if (value instanceof Number) {
            Assert.notNull((Number) value, code, args);
        } else if (value instanceof Collection) {
            Assert.notEmpty((Collection) value, code, args);
        } else if (value instanceof Map) {
            Assert.notEmpty((Map) value, code, args);
        } else if (value instanceof Object[]) {
            Assert.notEmpty((Object[]) value, code, args);
        }
        return this;
    }

    public AssertThat instanceOf(Class<?> type, String code, Object... args) {
        Assert.instanceOf(value, type, code, args);
        return this;
    }

    public AssertThat assignable(Class<?> superType, String code, Object... args) {
        Assert.assignable(value == null ? null : value.getClass(), superType, code, args);
        return this;
    }

    public AssertThat matches(String regex, String code, Object... args) {
        Assert.matches(value, regex, code, args);
        return this;
    }

    public AssertThat equals(Object compare, String code, Object... args) {
        Assert.equals(value, compare, code, args);
        return this;
    }

    public AssertThat notEqual(Object compare, String code, Object... args) {
        Assert.notEqual(value, compare, code, args);
        return this;
    }

    public AssertThat lessThan(Number max, String code, Object... args) {
        Assert.notNull(value, code, args);
        if (!(value instanceof Number)) {
            return this;
        }
        if (value instanceof Integer) {
            Assert.lessThan((Integer) value, max.intValue(), code, args);
        } else if (value instanceof Long) {
            Assert.lessThan((Long) value, max.longValue(), code, args);
        } else if (value instanceof Double) {
            Assert.lessThan((Double) value, max.doubleValue(), code, args);
        } else if (value instanceof Float) {
            Assert.lessThan((Float) value, max.floatValue(), code, args);
        } else if (value instanceof Short) {
            Assert.lessThan((Short) value, max.shortValue(), code, args);
        } else if (value instanceof Byte) {
            Assert.lessThan((Byte) value, max.byteValue(), code, args);
        }
        return this;
    }

    public AssertThat greaterThan(Number min, String code, Object... args) {
        Assert.notNull(value, code, args);
        if(!(value instanceof Number)) {
            return this;
        }
        if (value instanceof Integer) {
            Assert.greaterThan((Integer) value, min.intValue(), code, args);
        } else if (value instanceof Long) {
            Assert.greaterThan((Long) value, min.longValue(), code, args);
        } else if (value instanceof Double) {
            Assert.greaterThan((Double) value, min.doubleValue(), code, args);
        } else if (value instanceof Float) {
            Assert.greaterThan((Float) value, min.floatValue(), code, args);
        } else if (value instanceof Short) {
            Assert.greaterThan((Short) value, min.shortValue(), code, args);
        } else if (value instanceof Byte) {
            Assert.greaterThan((Byte) value, min.byteValue(), code, args);
        }
        return this;
    }

    public AssertThat maxLength(int max, String code, Object... args) {
        if(value == null) return this;
        if (value instanceof String) {
            Assert.maxLength((String) value, max, code, args);
        } else if (value instanceof Collection) {
            Assert.maxLength((Collection) value, max, code, args);
        } else if (value instanceof Map) {
            Assert.maxLength((Map) value, max, code, args);
        } else if (value instanceof Object[]) {
            Assert.maxLength((Object[]) value, max, code, args);
        }
        return this;
    }

    public AssertThat minLength(int min, String code, Object... args) {
        Assert.notNull(value, code, args);
        if (value instanceof String) {
            Assert.minLength((String)value, min, code, args);
        } else if (value instanceof Collection) {
            Assert.minLength((Collection)value, min, code, args);
        } else if (value instanceof Map) {
            Assert.minLength((Map)value, min, code, args);
        } else if (value instanceof Object[]) {
            Assert.minLength((Object[])value, min, code, args);
        }
        return this;
    }

    public AssertThat isChinese(String code, Object... args) {
        Assert.isChinese(value, code, args);
        return this;
    }

    public AssertThat isEnglish(String code, Object... args) {
        Assert.isEnglish(value, code, args);
        return this;
    }

    public AssertThat isMobile(String code, Object... args) {
        Assert.isMobile(value, code, args);
        return this;
    }

    public AssertThat isEmail(String code, Object... args) {
        Assert.isEmail(value, code, args);
        return this;
    }

    public AssertThat isDate(String format, String code, Object... args) {
        Assert.isDate(value, format, code, args);
        return this;
    }

    public AssertThat isIdCard(String code, Object... args) {
        Assert.isIdCard(value, code, args);
        return this;
    }

    public AssertThat isIp(String code, Object... args) {
        Assert.isIp(value, code, args);
        return this;
    }
}
