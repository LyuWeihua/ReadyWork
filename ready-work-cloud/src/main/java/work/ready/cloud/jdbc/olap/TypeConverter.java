/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.olap;

import work.ready.cloud.jdbc.olap.proto.StringUtils;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.ERA;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;
import static work.ready.cloud.jdbc.olap.EsType.DATE;
import static work.ready.cloud.jdbc.olap.EsType.DATETIME;
import static work.ready.cloud.jdbc.olap.EsType.TIME;
import static work.ready.cloud.jdbc.olap.JdbcDateUtils.asDateTimeField;
import static work.ready.cloud.jdbc.olap.JdbcDateUtils.timeAsTime;

final class TypeConverter {

    private TypeConverter() {}

    static Date convertDate(Long millis, Calendar cal) {
        return dateTimeConvert(millis, cal, c -> {
            c.set(HOUR_OF_DAY, 0);
            c.set(MINUTE, 0);
            c.set(SECOND, 0);
            c.set(MILLISECOND, 0);
            return new Date(c.getTimeInMillis());
        });
    }

    static Time convertTime(Long millis, Calendar cal) {
        return dateTimeConvert(millis, cal, c -> {
            c.set(ERA, GregorianCalendar.AD);
            c.set(YEAR, 1970);
            c.set(MONTH, 0);
            c.set(DAY_OF_MONTH, 1);
            return new Time(c.getTimeInMillis());
        });
    }

    static Timestamp convertTimestamp(Long millis, Calendar cal) {
        return dateTimeConvert(millis, cal, c -> new Timestamp(c.getTimeInMillis()));
    }

    private static <T> T dateTimeConvert(Long millis, Calendar c, Function<Calendar, T> creator) {
        if (millis == null) {
            return null;
        }
        long initial = c.getTimeInMillis();
        try {
            c.setTimeInMillis(millis);
            return creator.apply(c);
        } finally {
            c.setTimeInMillis(initial);
        }
    }

    static long convertFromCalendarToUTC(long value, Calendar cal) {
        if (cal == null) {
            return value;
        }
        Calendar c = (Calendar) cal.clone();
        c.setTimeInMillis(value);

        ZonedDateTime convertedDateTime = ZonedDateTime
                .ofInstant(c.toInstant(), c.getTimeZone().toZoneId())
                .withZoneSameLocal(ZoneOffset.UTC);

        return convertedDateTime.toInstant().toEpochMilli();
    }

    @SuppressWarnings("unchecked")
    static <T> T convert(Object val, EsType columnType, Class<T> type, String typeString) throws SQLException {
        if (type == null) {
            return (T) convert(val, columnType, typeString);
        }

        if (type.isInstance(val) && TypeUtils.classOf(columnType) == type) {
            try {
                return type.cast(val);
            } catch (ClassCastException cce) {
                failConversion(val, columnType, typeString, type, cce);
            }
        }

        if (type == String.class) {
            return (T) asString(convert(val, columnType, typeString));
        }
        if (type == Boolean.class) {
            return (T) asBoolean(val, columnType, typeString);
        }
        if (type == Byte.class) {
            return (T) asByte(val, columnType, typeString);
        }
        if (type == Short.class) {
            return (T) asShort(val, columnType, typeString);
        }
        if (type == Integer.class) {
            return (T) asInteger(val, columnType, typeString);
        }
        if (type == Long.class) {
            return (T) asLong(val, columnType, typeString);
        }
        if (type == Float.class) {
            return (T) asFloat(val, columnType, typeString);
        }
        if (type == Double.class) {
            return (T) asDouble(val, columnType, typeString);
        }
        if (type == Date.class) {
            return (T) asDate(val, columnType, typeString);
        }
        if (type == Time.class) {
            return (T) asTime(val, columnType, typeString);
        }
        if (type == Timestamp.class) {
            return (T) asTimestamp(val, columnType, typeString);
        }
        if (type == byte[].class) {
            return (T) asByteArray(val, columnType, typeString);
        }
        if (type == BigDecimal.class) {
            return (T) asBigDecimal(val, columnType, typeString);
        }

        if (type == LocalDate.class) {
            return (T) asLocalDate(val, columnType, typeString);
        }
        if (type == LocalTime.class) {
            return (T) asLocalTime(val, columnType, typeString);
        }
        if (type == LocalDateTime.class) {
            return (T) asLocalDateTime(val, columnType, typeString);
        }
        if (type == OffsetTime.class) {
            return (T) asOffsetTime(val, columnType, typeString);
        }
        if (type == OffsetDateTime.class) {
            return (T) asOffsetDateTime(val, columnType, typeString);
        }

        return failConversion(val, columnType, typeString, type);
    }

    static Object convert(Object v, EsType columnType, String typeString) throws SQLException {
        switch (columnType) {
            case NULL:
                return null;
            case BOOLEAN:
            case TEXT:
            case KEYWORD:
            case CONSTANT_KEYWORD:
                return v; 
            case BYTE:
                return ((Number) v).byteValue(); 
            case SHORT:
                return ((Number) v).shortValue(); 
            case INTEGER:
                return ((Number) v).intValue();
            case LONG:
                return ((Number) v).longValue();
            case HALF_FLOAT:
            case SCALED_FLOAT:
            case DOUBLE:
                return doubleValue(v); 
            case FLOAT:
                return floatValue(v); 
            case DATE:
                return asDateTimeField(v, JdbcDateUtils::asDate, Date::new);
            case TIME:
                return timeAsTime(v.toString());
            case DATETIME:
                return asDateTimeField(v, JdbcDateUtils::asTimestamp, Timestamp::new);
            case INTERVAL_YEAR:
            case INTERVAL_MONTH:
            case INTERVAL_YEAR_TO_MONTH:
                return Period.parse(v.toString());
            case INTERVAL_DAY:
            case INTERVAL_HOUR:
            case INTERVAL_MINUTE:
            case INTERVAL_SECOND:
            case INTERVAL_DAY_TO_HOUR:
            case INTERVAL_DAY_TO_MINUTE:
            case INTERVAL_DAY_TO_SECOND:
            case INTERVAL_HOUR_TO_MINUTE:
            case INTERVAL_HOUR_TO_SECOND:
            case INTERVAL_MINUTE_TO_SECOND:
                return Duration.parse(v.toString());
            case GEO_POINT:
            case GEO_SHAPE:
            case SHAPE:
                throw new SQLException("Column type geo_shape is not supported yet");
            case IP:
                return v.toString();
            default:
                throw new SQLException("Unexpected column type [" + typeString + "]");
        }
    }

    private static Double doubleValue(Object v) {
        if (v instanceof String) {
            switch ((String) v) {
                case "NaN":
                    return Double.NaN;
                case "Infinity":
                    return Double.POSITIVE_INFINITY;
                case "-Infinity":
                    return Double.NEGATIVE_INFINITY;
                default:
                    return Double.parseDouble((String) v);
            }
        }
        return ((Number) v).doubleValue();
    }

    private static Float floatValue(Object v) {
        if (v instanceof String) {
            switch ((String) v) {
                case "NaN":
                    return Float.NaN;
                case "Infinity":
                    return Float.POSITIVE_INFINITY;
                case "-Infinity":
                    return Float.NEGATIVE_INFINITY;
                default:
                    return Float.parseFloat((String) v);
            }
        }
        return ((Number) v).floatValue();
    }

    private static String asString(Object nativeValue) {
        return nativeValue == null ? null : StringUtils.toString(nativeValue);
    }

    private static <T> T failConversion(Object value, EsType columnType, String typeString, Class<T> target) throws SQLException {
        return failConversion(value, columnType, typeString, target, null);
    }

    private static <T> T failConversion(Object value, EsType columnType, String typeString, Class<T> target, Exception e)
            throws SQLException {
        String message = format(Locale.ROOT, "Unable to convert value [%.128s] of type [%s] to [%s]", value, columnType,
                typeString);
        throw e != null ? new SQLException(message, e) : new SQLException(message);
    }

    private static Boolean asBoolean(Object val, EsType columnType, String typeString) throws SQLException {
        switch (columnType) {
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLOAT:
            case HALF_FLOAT:
            case SCALED_FLOAT:
            case DOUBLE:
                return Boolean.valueOf(Integer.signum(((Number) val).intValue()) != 0);
            case KEYWORD:
            case TEXT:
            case CONSTANT_KEYWORD:
                return Boolean.valueOf((String) val);
            default:
                return failConversion(val, columnType, typeString, Boolean.class);
        }
    }

    private static Byte asByte(Object val, EsType columnType, String typeString) throws SQLException {
        switch (columnType) {
            case BOOLEAN:
                return Byte.valueOf(((Boolean) val).booleanValue() ? (byte) 1 : (byte) 0);
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
                return safeToByte(((Number) val).longValue());
            case FLOAT:
            case HALF_FLOAT:
            case SCALED_FLOAT:
            case DOUBLE:
                return safeToByte(safeToLong(((Number) val).doubleValue()));
            case KEYWORD:
            case TEXT:
            case CONSTANT_KEYWORD:
                try {
                    return Byte.valueOf((String) val);
                } catch (NumberFormatException e) {
                    return failConversion(val, columnType, typeString, Byte.class, e);
                }
            default:
        }

        return failConversion(val, columnType, typeString, Byte.class);
    }

    private static Short asShort(Object val, EsType columnType, String typeString) throws SQLException {
        switch (columnType) {
            case BOOLEAN:
                return Short.valueOf(((Boolean) val).booleanValue() ? (short) 1 : (short) 0);
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
                return safeToShort(((Number) val).longValue());
            case FLOAT:
            case HALF_FLOAT:
            case SCALED_FLOAT:
            case DOUBLE:
                return safeToShort(safeToLong(((Number) val).doubleValue()));
            case KEYWORD:
            case TEXT:
            case CONSTANT_KEYWORD:
                try {
                    return Short.valueOf((String) val);
                } catch (NumberFormatException e) {
                    return failConversion(val, columnType, typeString, Short.class, e);
                }
            default:
        }
        return failConversion(val, columnType, typeString, Short.class);
    }

    private static Integer asInteger(Object val, EsType columnType, String typeString) throws SQLException {
        switch (columnType) {
            case BOOLEAN:
                return Integer.valueOf(((Boolean) val).booleanValue() ? 1 : 0);
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
                return safeToInt(((Number) val).longValue());
            case FLOAT:
            case HALF_FLOAT:
            case SCALED_FLOAT:
            case DOUBLE:
                return safeToInt(safeToLong(((Number) val).doubleValue()));
            case KEYWORD:
            case TEXT:
            case CONSTANT_KEYWORD:
                try {
                    return Integer.valueOf((String) val);
                } catch (NumberFormatException e) {
                    return failConversion(val, columnType, typeString, Integer.class, e);
                }
            default:
        }
        return failConversion(val, columnType, typeString, Integer.class);
    }

    private static Long asLong(Object val, EsType columnType, String typeString) throws SQLException {
        switch (columnType) {
            case BOOLEAN:
                return Long.valueOf(((Boolean) val).booleanValue() ? 1 : 0);
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
                return Long.valueOf(((Number) val).longValue());
            case FLOAT:
            case HALF_FLOAT:
            case SCALED_FLOAT:
            case DOUBLE:
                return safeToLong(((Number) val).doubleValue());

            case KEYWORD:
            case TEXT:
            case CONSTANT_KEYWORD:
                try {
                    return Long.valueOf((String) val);
                } catch (NumberFormatException e) {
                    return failConversion(val, columnType, typeString, Long.class, e);
                }
            default:
        }

        return failConversion(val, columnType, typeString, Long.class);
    }

    private static Float asFloat(Object val, EsType columnType, String typeString) throws SQLException {
        switch (columnType) {
            case BOOLEAN:
                return Float.valueOf(((Boolean) val).booleanValue() ? 1 : 0);
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
                return Float.valueOf(((Number) val).longValue());
            case FLOAT:
            case HALF_FLOAT:
            case SCALED_FLOAT:
            case DOUBLE:
                return Float.valueOf(((Number) val).floatValue());
            case KEYWORD:
            case TEXT:
            case CONSTANT_KEYWORD:
                try {
                    return Float.valueOf((String) val);
                } catch (NumberFormatException e) {
                    return failConversion(val, columnType, typeString, Float.class, e);
                }
            default:
        }
        return failConversion(val, columnType, typeString, Float.class);
    }

    private static Double asDouble(Object val, EsType columnType, String typeString) throws SQLException {
        switch (columnType) {
            case BOOLEAN:
                return Double.valueOf(((Boolean) val).booleanValue() ? 1 : 0);
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
                return Double.valueOf(((Number) val).longValue());
            case FLOAT:
            case HALF_FLOAT:
            case SCALED_FLOAT:
            case DOUBLE:
                return Double.valueOf(((Number) val).doubleValue());
            case KEYWORD:
            case TEXT:
            case CONSTANT_KEYWORD:
                try {
                    return Double.valueOf((String) val);
                } catch (NumberFormatException e) {
                    return failConversion(val, columnType, typeString, Double.class, e);
                }
            default:
        }
        return failConversion(val, columnType, typeString, Double.class);
    }

    private static Date asDate(Object val, EsType columnType, String typeString) throws SQLException {
        if (columnType == DATETIME || columnType == DATE) {
            return asDateTimeField(val, JdbcDateUtils::asDate, Date::new);
        }
        if (columnType == TIME) {
            return new Date(0L);
        }
        return failConversion(val, columnType, typeString, Date.class);
    }

    private static Time asTime(Object val, EsType columnType, String typeString) throws SQLException {
        if (columnType == DATETIME) {
            return asDateTimeField(val, JdbcDateUtils::asTime, Time::new);
        }
        if (columnType == TIME) {
            return asDateTimeField(val, JdbcDateUtils::timeAsTime, Time::new);
        }
        if (columnType == DATE) {
            return new Time(0L);
        }
        return failConversion(val, columnType, typeString, Time.class);
    }

    private static Timestamp asTimestamp(Object val, EsType columnType, String typeString) throws SQLException {
        if (columnType == DATETIME || columnType == DATE) {
            return asDateTimeField(val, JdbcDateUtils::asTimestamp, Timestamp::new);
        }
        if (columnType == TIME) {
            return asDateTimeField(val, JdbcDateUtils::timeAsTimestamp, Timestamp::new);
        }
        return failConversion(val, columnType, typeString, Timestamp.class);
    }

    private static byte[] asByteArray(Object val, EsType columnType, String typeString) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    private static BigDecimal asBigDecimal(Object val, EsType columnType, String typeString) throws SQLException {
        switch (columnType) {
            case BOOLEAN:
                return (Boolean) val ? BigDecimal.ONE : BigDecimal.ZERO;
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
                return BigDecimal.valueOf(((Number) val).longValue());
            case FLOAT:
            case HALF_FLOAT:
                
                return new BigDecimal(String.valueOf(((Number) val).floatValue()));
            case DOUBLE:
            case SCALED_FLOAT:
                return BigDecimal.valueOf(((Number) val).doubleValue());
            case KEYWORD:
            case TEXT:
            case CONSTANT_KEYWORD:
                try {
                    return new BigDecimal((String) val);
                } catch (NumberFormatException nfe) {
                    return failConversion(val, columnType, typeString, BigDecimal.class, nfe);
                }

        }
        return failConversion(val, columnType, typeString, BigDecimal.class);
    }

    private static LocalDate asLocalDate(Object val, EsType columnType, String typeString) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    private static LocalTime asLocalTime(Object val, EsType columnType, String typeString) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    private static LocalDateTime asLocalDateTime(Object val, EsType columnType, String typeString) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    private static OffsetTime asOffsetTime(Object val, EsType columnType, String typeString) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    private static OffsetDateTime asOffsetDateTime(Object val, EsType columnType, String typeString) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    private static byte safeToByte(long x) throws SQLException {
        if (x > Byte.MAX_VALUE || x < Byte.MIN_VALUE) {
            throw new SQLException(format(Locale.ROOT, "Numeric %s out of range", Long.toString(x)));
        }
        return (byte) x;
    }

    private static short safeToShort(long x) throws SQLException {
        if (x > Short.MAX_VALUE || x < Short.MIN_VALUE) {
            throw new SQLException(format(Locale.ROOT, "Numeric %s out of range", Long.toString(x)));
        }
        return (short) x;
    }

    private static int safeToInt(long x) throws SQLException {
        if (x > Integer.MAX_VALUE || x < Integer.MIN_VALUE) {
            throw new SQLException(format(Locale.ROOT, "Numeric %s out of range", Long.toString(x)));
        }
        return (int) x;
    }

    private static long safeToLong(double x) throws SQLException {
        if (x > Long.MAX_VALUE || x < Long.MIN_VALUE) {
            throw new SQLException(format(Locale.ROOT, "Numeric %s out of range", Double.toString(x)));
        }
        return Math.round(x);
    }
}
