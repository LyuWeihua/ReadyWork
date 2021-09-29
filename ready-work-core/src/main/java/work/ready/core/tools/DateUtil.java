/**
 *
 * Original work Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
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

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.tools.define.SyncWriteMap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.chrono.ChronoLocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DateUtil {
	private static final Log logger = LogFactory.getLog(DateUtil.class);

	private static Map<Integer, Set<String>> formats = new SyncWriteMap<>();
	private static Map<String, DateTimeFormatter> fastDateFormats = new SyncWriteMap<>();
	private static DateTimeFormatter httpHeader = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz");

	public static final String DAY_PATTERN = "yyyy-MM-dd";
	public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
	public static final String DATETIME_MILLS_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

	static {
		String[] strings = { DAY_PATTERN, DATETIME_PATTERN, DATETIME_MILLS_PATTERN, "yyyyMMdd", "yyyy年MM月dd日", "yyyyMMddHHmmss" };
		for(String string : strings) {
			addFormat(string);
		}
	}

	public static DateTimeFormatter addFormat(String format) {
		if(StrUtil.notBlank(format)) {
			try {
				DateTimeFormatter fastDateFormat = DateTimeFormatter.ofPattern(format);
				Integer length = format.length();
				Set<String> set = formats.computeIfAbsent(length, k -> new HashSet<>());
				if(!set.contains(format)) {
					set.add(format);
					fastDateFormats.put(format, fastDateFormat);
				}
				return fastDateFormat;
			} catch(Exception e) {
				logger.warn("fail to add format: %s, ex: %s", format, e.getMessage());
			}
		}
		return null;
	}

	public static Date parse(String time) {
		if(StrUtil.isBlank(time)) {
			return null;
		}
		Set<String> set = formats.get(time.length());
		if(set != null && set.size()>0) {
			for(String format : set) {
				try {
					DateTimeFormatter fastDateFormat = fastDateFormats.get(format);
					return Date.from(LocalDateTime.parse(time, fastDateFormat).atZone(ZoneId.systemDefault()).toInstant());
				} catch(Exception e) {
					
				}
			}
		}
		int length = time.length();
		boolean isLongTime = (length==10 || length==13) && time.matches("\\d+");
		if(isLongTime) {
			return new Date(length==13?Long.parseLong(time):Long.parseLong(time)*1000);
		}
		int httpHeaderLength = 29;
		if(length == httpHeaderLength) {
			try {
				return Date.from(LocalDateTime.parse(time, httpHeader).atZone(ZoneId.systemDefault()).toInstant());
			}catch(Exception e) {
				
			}
		}
		logger.info("fail to parse time: %s", time);
		return null;
	}

	public static String format(Date date, String format) {
		if(date==null || StrUtil.isBlank(format)) {
			return null;
		}
		DateTimeFormatter fastDateFormat = fastDateFormats.get(format);
		if(fastDateFormat == null) {
			fastDateFormat = addFormat(format);
		}
		return fastDateFormat == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).format(fastDateFormat);
	}

	public static String format(Date date) {
		return format(date, "yyyy-MM-dd HH:mm:ss");
	}

	public static DateTimeFormatter getFormatter(String pattern) {
		DateTimeFormatter fastDateFormat = fastDateFormats.get(pattern);
		if(fastDateFormat == null) {
			fastDateFormat = addFormat(pattern);
		}
		return fastDateFormat;
	}

	private static final ThreadLocal<HashMap<String, SimpleDateFormat>> TL = ThreadLocal.withInitial(() -> new HashMap<>());

	public static SimpleDateFormat getSimpleDateFormat(String pattern) {
		SimpleDateFormat ret = TL.get().get(pattern);
		if (ret == null) {
			ret = new SimpleDateFormat(pattern);
			TL.get().put(pattern, ret);
		}
		return ret;
	}

	public static String now(String pattern) {
		return LocalDateTime.now().format(getFormatter(pattern));
	}

	public static String format(LocalDateTime localDateTime, String pattern) {
		return localDateTime.format(getFormatter(pattern));
	}

	public static String format(LocalDate localDate, String pattern) {
		return localDate.format(getFormatter(pattern));
	}

	public static String format(LocalTime localTime, String pattern) {
		return localTime.format(getFormatter(pattern));
	}

	public static Date parse(String dateString, String pattern) {
		try {
			return getSimpleDateFormat(pattern).parse(dateString);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	public static LocalDateTime parseLocalDateTime(String localDateTimeString, String pattern) {
		return LocalDateTime.parse(localDateTimeString, getFormatter(pattern));
	}

	public static LocalDate parseLocalDate(String localDateString, String pattern) {
		return LocalDate.parse(localDateString, getFormatter(pattern));
	}

	public static LocalTime parseLocalTime(String localTimeString, String pattern) {
		return LocalTime.parse(localTimeString, getFormatter(pattern));
	}

	public static boolean isAfter(ChronoLocalDateTime<?> self, ChronoLocalDateTime<?> other) {
		return self.isAfter(other);
	}

	public static boolean isBefore(ChronoLocalDateTime<?> self, ChronoLocalDateTime<?> other) {
		return self.isBefore(other);
	}

	public static boolean isEqual(ChronoLocalDateTime<?> self, ChronoLocalDateTime<?> other) {
		return self.isEqual(other);
	}

	public static LocalDateTime toLocalDateTime(Date date) {
		
		if (date instanceof java.sql.Date) {
			date = new Date(date.getTime());
		}

		Instant instant = date.toInstant();
		ZoneId zone = ZoneId.systemDefault();
		return LocalDateTime.ofInstant(instant, zone);
	}

	public static LocalDate toLocalDate(Date date) {
		
		if (date instanceof java.sql.Date) {
			date = new Date(date.getTime());
		}

		Instant instant = date.toInstant();
		ZoneId zone = ZoneId.systemDefault();
		LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, zone);
		return localDateTime.toLocalDate();
	}

	public static LocalTime toLocalTime(Date date) {
		
		if (date instanceof java.sql.Date) {
			date = new Date(date.getTime());
		}

		Instant instant = date.toInstant();
		ZoneId zone = ZoneId.systemDefault();
		LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, zone);
		return localDateTime.toLocalTime();
	}

	public static Date toDate(LocalDateTime localDateTime) {
		ZoneId zone = ZoneId.systemDefault();
		Instant instant = localDateTime.atZone(zone).toInstant();
		return Date.from(instant);
	}

	public static Date toDate(LocalDate localDate) {
		ZoneId zone = ZoneId.systemDefault();
		Instant instant = localDate.atStartOfDay().atZone(zone).toInstant();
		return Date.from(instant);
	}

	public static Date toDate(LocalTime localTime) {
		LocalDate localDate = LocalDate.now();
		LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
		ZoneId zone = ZoneId.systemDefault();
		Instant instant = localDateTime.atZone(zone).toInstant();
		return Date.from(instant);
	}

	public static Date toDate(LocalDate localDate, LocalTime localTime) {
		LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
		ZoneId zone = ZoneId.systemDefault();
		Instant instant = localDateTime.atZone(zone).toInstant();
		return Date.from(instant);
	}
}
