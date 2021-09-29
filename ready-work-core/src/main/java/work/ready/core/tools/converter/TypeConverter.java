/**
 * Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com) / 玛雅牛 (myaniu AT gmail dot com).
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

package work.ready.core.tools.converter;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import work.ready.core.server.Ready;
import work.ready.core.tools.converter.Converters.BigDecimalConverter;
import work.ready.core.tools.converter.Converters.BigIntegerConverter;
import work.ready.core.tools.converter.Converters.BooleanConverter;
import work.ready.core.tools.converter.Converters.ByteConverter;
import work.ready.core.tools.converter.Converters.ByteArrayConverter;
import work.ready.core.tools.converter.Converters.DateConverter;
import work.ready.core.tools.converter.Converters.DoubleConverter;
import work.ready.core.tools.converter.Converters.FloatConverter;
import work.ready.core.tools.converter.Converters.IntegerConverter;
import work.ready.core.tools.converter.Converters.LongConverter;
import work.ready.core.tools.converter.Converters.SqlDateConverter;
import work.ready.core.tools.converter.Converters.ShortConverter;
import work.ready.core.tools.converter.Converters.TimeConverter;
import work.ready.core.tools.converter.Converters.TimestampConverter;
import work.ready.core.tools.converter.Converters.LocalDateTimeConverter;
import work.ready.core.tools.converter.Converters.LocalDateConverter;

public class TypeConverter {

	private final Map<Class<?>, IConverter<?>> converterMap = new HashMap<Class<?>, IConverter<?>>(64);

	private static class LazyHolder{
		static final TypeConverter instance = new TypeConverter();
	}

	public static TypeConverter getInstance(){
		return LazyHolder.instance;
	}

	private TypeConverter() {
		register(Integer.class, new IntegerConverter());
		register(int.class, new IntegerConverter());
		register(Long.class, new LongConverter());
		register(long.class, new LongConverter());
		register(Double.class, new DoubleConverter());
		register(double.class, new DoubleConverter());
		register(Float.class, new FloatConverter());
		register(float.class, new FloatConverter());
		register(Boolean.class, new BooleanConverter());
		register(boolean.class, new BooleanConverter());
		register(java.util.Date.class, new DateConverter());
		register(java.sql.Date.class, new SqlDateConverter());
		register(java.sql.Time.class, new TimeConverter());
		register(java.sql.Timestamp.class, new TimestampConverter());
		register(java.math.BigDecimal.class, new BigDecimalConverter());
		register(java.math.BigInteger.class, new BigIntegerConverter());
		register(byte[].class, new ByteArrayConverter());

		register(Short.class, new ShortConverter());
		register(short.class, new ShortConverter());
		register(Byte.class, new ByteConverter());
		register(byte.class, new ByteConverter());

		register(java.time.LocalDateTime.class, new LocalDateTimeConverter());
		register(java.time.LocalDate.class, new LocalDateConverter());
	}

	public <T> void register(Class<T> type, IConverter<T> converter) {
		converterMap.put(type, converter);
	}

	public final Object convert(Class<?> type, String s) throws ParseException {
		if (s == null) {
			return null;
		}

		if (type == String.class) {
			return ("".equals(s) ? null : s);	
		}
		s = s.trim();
		if ("".equals(s)) {	
			return null;
		}

		IConverter<?> converter = converterMap.get(type);
		if (converter != null) {
			return converter.convert(s);
		}
		if (Ready.getBootstrapConfig().isDevMode()) {
			throw new RuntimeException("Please add code in " + TypeConverter.class  + ". The type can't be converted: " + type.getName());
		} else {
			throw new RuntimeException(type.getName() + " can not be converted, please use other type of attributes in your model!");
		}
	}
}

