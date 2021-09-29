/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package work.ready.cloud.jdbc.olap;

import work.ready.cloud.jdbc.olap.proto.StringUtils;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.function.Function;

final class JdbcDateUtils {

    private JdbcDateUtils() {}

    private static final LocalDate EPOCH = LocalDate.of(1970, 1, 1);

    private static ZonedDateTime asDateTime(String date) {
        return StringUtils.ISO_DATE_WITH_MILLIS.parse(date, ZonedDateTime::from);
    }

    static long dateTimeAsMillisSinceEpoch(String date) {
        return asDateTime(date).toInstant().toEpochMilli();
    }

    static long timeAsMillisSinceEpoch(String date) {
        return StringUtils.ISO_TIME_WITH_MILLIS.parse(date, OffsetTime::from).atDate(EPOCH).toInstant().toEpochMilli();
    }

    static Date asDate(String date) {
        ZonedDateTime zdt = asDateTime(date);
        return new Date(zdt.toLocalDate().atStartOfDay(zdt.getZone()).toInstant().toEpochMilli());
    }

    static Time asTime(String date) {
        ZonedDateTime zdt = asDateTime(date);
        return new Time(zdt.toLocalTime().atDate(EPOCH).atZone(zdt.getZone()).toInstant().toEpochMilli());
    }

    static Time timeAsTime(String date) {
        OffsetTime ot = StringUtils.ISO_TIME_WITH_MILLIS.parse(date, OffsetTime::from);
        return new Time(ot.atDate(EPOCH).toInstant().toEpochMilli());
    }

    static Timestamp asTimestamp(long millisSinceEpoch) {
        return new Timestamp(millisSinceEpoch);
    }

    static Timestamp asTimestamp(String date) {
        return new Timestamp(dateTimeAsMillisSinceEpoch(date));
    }

    static Timestamp timeAsTimestamp(String date) {
        return new Timestamp(timeAsMillisSinceEpoch(date));
    }
    
    static <R> R asDateTimeField(Object value, Function<String, R> asDateTimeMethod, Function<Long, R> ctor) {
        if (value instanceof String) {
            return asDateTimeMethod.apply((String) value);
        } else {
            return ctor.apply(((Number) value).longValue());
        }
    }
}
