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

import work.ready.core.tools.DateUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Converters {

	private static final String timeStampPattern = "yyyy-MM-dd HH:mm:ss";
	private static final String datePattern = "yyyy-MM-dd";
	private static final int dateLen = datePattern.length();
	private static final int timeStampWithoutSecPatternLen = "yyyy-MM-dd HH:mm".length();
	private static final int timePatternLen = "hh:mm:ss".length();
	private static final int timeWithoutSecPatternLen = "hh:mm".length();

	private Converters() {}

	private static SimpleDateFormat getFormat(String pattern) {
		return DateUtil.getSimpleDateFormat(pattern);
	}

	public static class IntegerConverter implements IConverter<Integer> {
		
		@Override
		public Integer convert(String s) {
			return Integer.parseInt(s);
		}
	}

	public static class ShortConverter implements IConverter<Short> {
		@Override
		public Short convert(String s) {
			return Short.parseShort(s);
		}
	}

	public static class ByteConverter implements IConverter<Byte> {
		@Override
		public Byte convert(String s) {
			return Byte.parseByte(s);
		}
	}

	public static class LongConverter implements IConverter<Long> {
		
		@Override
		public Long convert(String s) {
			return Long.parseLong(s);
		}
	}

	public static class FloatConverter implements IConverter<Float> {
		
		@Override
		public Float convert(String s) {
			return Float.parseFloat(s);
		}
	}

	public static class DoubleConverter implements IConverter<Double> {
		
		@Override
		public Double convert(String s) {
			return Double.parseDouble(s);
		}
	}

	public static class ByteArrayConverter implements IConverter<byte[]> {
		
		@Override
		public byte[] convert(String s) {
			return s.getBytes();
		}
	}

	public static class BigIntegerConverter implements IConverter<java.math.BigInteger> {
		
		@Override
		public java.math.BigInteger convert(String s) {
			return new java.math.BigInteger(s);
		}
	}

	public static class BigDecimalConverter implements IConverter<java.math.BigDecimal> {
		
		@Override
		public java.math.BigDecimal convert(String s) {
			return new java.math.BigDecimal(s);
		}
	}

	public static class BooleanConverter implements IConverter<Boolean> {
		
		@Override
		public Boolean convert(String s) {
			String value = s.toLowerCase();
			if ("true".equals(value) || "1".equals(value) ) {
				return Boolean.TRUE;
			}
			else if ("false".equals(value) || "0".equals(value) ) {
				return Boolean.FALSE;
			}
			else {
				throw new RuntimeException("Can not parse to boolean type of value: " + s);
			}
		}
	}

	public static class DateConverter implements IConverter<java.util.Date> {

		@Override
		public java.util.Date convert(String s) throws ParseException {
			s = supportHtml5DateTimePattern(s);

			if (timeStampWithoutSecPatternLen == s.length()) {
				s = s + ":00";
			}
			if (s.length() > dateLen) {

				return getFormat(timeStampPattern).parse(s);
			}
			else {
				
				return getFormat(datePattern).parse(s);
			}
		}
	}

	public static class SqlDateConverter implements IConverter<java.sql.Date> {
		
		@Override
		public java.sql.Date convert(String s) throws ParseException {
			s = supportHtml5DateTimePattern(s);

			if (timeStampWithoutSecPatternLen == s.length()) {
				s = s + ":00";
			}
			if (s.length() > dateLen) {	
				
				return new java.sql.Date(getFormat(timeStampPattern).parse(s).getTime());
			}
			else {
				
				return new java.sql.Date(getFormat(datePattern).parse(s).getTime());
			}
		}
	}

	public static class TimeConverter implements IConverter<java.sql.Time> {
		
		@Override
		public java.sql.Time convert(String s) throws ParseException {
			int len = s.length();
			if (len == timeWithoutSecPatternLen) {
				s = s + ":00";
			}
			if (len > timePatternLen) {
				s = s.substring(0, timePatternLen);
			}
			return java.sql.Time.valueOf(s);
		}
	}

	public static class TimestampConverter implements IConverter<java.sql.Timestamp> {
		
		@Override
		public java.sql.Timestamp convert(String s) throws ParseException {
			s = supportHtml5DateTimePattern(s);

			if (timeStampWithoutSecPatternLen == s.length()) {
				s = s + ":00";
			}
			if (s.length() > dateLen) {
				return java.sql.Timestamp.valueOf(s);
			}
			else {
				return new java.sql.Timestamp(getFormat(datePattern).parse(s).getTime());
			}
		}
	}

	public static String supportHtml5DateTimePattern(String s) {
		if (s.indexOf(' ') == -1 && s.indexOf('T') != -1 && s.indexOf('-') != -1 && s.indexOf(':') != -1) {
			return s.replace("T", " ");
		} else {
			return s;
		}
	}

	public static class LocalDateTimeConverter implements IConverter<LocalDateTime> {

		private static final DateConverter dateConverter = new DateConverter();

		@Override
		public LocalDateTime convert(String s) throws ParseException {
			java.util.Date ret = dateConverter.convert(s);
			return DateUtil.toLocalDateTime(ret);
		}
	}

	public static class LocalDateConverter implements IConverter<LocalDate> {

		private static final DateConverter dateConverter = new DateConverter();

		@Override
		public LocalDate convert(String s) throws ParseException {
			java.util.Date ret = dateConverter.convert(s);
			return DateUtil.toLocalDate(ret);
		}
	}
}
