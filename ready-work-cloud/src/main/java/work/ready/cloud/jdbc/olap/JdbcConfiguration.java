/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.olap;

import work.ready.cloud.jdbc.debug.JdbcDebugConfig;
import work.ready.cloud.jdbc.olap.client.ClientVersion;
import work.ready.cloud.jdbc.olap.client.ConnectionConfiguration;
import work.ready.core.tools.StrUtil;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.DriverPropertyInfo;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static work.ready.cloud.jdbc.ReadyJdbcDriver.OLAP_URL_PREFIX;
import static work.ready.core.tools.HttpUtil.parseURI;
import static work.ready.core.tools.HttpUtil.removeQuery;

public class JdbcConfiguration extends ConnectionConfiguration implements JdbcDebugConfig {
    public static URI DEFAULT_URI = URI.create("http://localhost:9200/");

    static final String DEBUG = "debug";
    static final String DEBUG_DEFAULT = "false";

    static final String DEBUG_OUTPUT = "debug.output";
    
    static final String DEBUG_OUTPUT_DEFAULT = "err";

    static final String DEBUG_FLUSH_ALWAYS = "debug.flushAlways";
    
    static final String DEBUG_FLUSH_ALWAYS_DEFAULT = "true";

    public static final String TIME_ZONE = "timezone";

    static final String TIME_ZONE_DEFAULT = TimeZone.getDefault().getID();

    static final String FIELD_MULTI_VALUE_LENIENCY = "field.multi.value.leniency";
    static final String FIELD_MULTI_VALUE_LENIENCY_DEFAULT = "true";

    static final String INDEX_INCLUDE_FROZEN = "index.include.frozen";
    static final String INDEX_INCLUDE_FROZEN_DEFAULT = "false";

    private static final Set<String> OPTION_NAMES = new LinkedHashSet<>(
            Arrays.asList(TIME_ZONE, FIELD_MULTI_VALUE_LENIENCY, INDEX_INCLUDE_FROZEN, DEBUG, DEBUG_OUTPUT, DEBUG_FLUSH_ALWAYS));

    static {

        ClientVersion.CURRENT.toString();
    }

    private final boolean debug;
    private final String debugOut;
    private final boolean flushAlways;

    private ZoneId zoneId;
    private boolean fieldMultiValueLeniency;
    private boolean includeFrozen;

    public static JdbcConfiguration create(String u, Properties props, int loginTimeoutSeconds) throws JdbcSQLException {
        URI uri = parseUrl(u);
        Properties urlProps = parseProperties(uri, u);
        uri = removeQuery(uri, u, DEFAULT_URI);

        if (props != null) {
            urlProps.putAll(props);
        }

        if (loginTimeoutSeconds > 0) {
            urlProps.setProperty(CONNECT_TIMEOUT, Long.toString(TimeUnit.SECONDS.toMillis(loginTimeoutSeconds)));
        }

        try {
            return new JdbcConfiguration(uri, u, urlProps);
        } catch (JdbcSQLException e) {
            throw e;
        } catch (Exception ex) {
            throw new JdbcSQLException(ex, ex.getMessage());
        }
    }

    private static URI parseUrl(String u) throws JdbcSQLException {
        if (!canAccept(u)) {
            throw new JdbcSQLException("Expected [" + OLAP_URL_PREFIX + "] url, received [" + u + "]");
        }

        try {
            return parseURI(removeJdbcPrefix(u), DEFAULT_URI);
        } catch (IllegalArgumentException ex) {
            final String format = "jdbc:ready:olap://[[http|https]://]?[host[:port]]?/[prefix]?[\\?[option=value]&]*";
            throw new JdbcSQLException(ex, "Invalid URL: " + ex.getMessage() + "; format should be [" + format + "]");
        }
    }

    private static String removeJdbcPrefix(String connectionString) throws JdbcSQLException {
        if (connectionString.startsWith(OLAP_URL_PREFIX)) {
            return connectionString.substring(OLAP_URL_PREFIX.length());
        } else {
            throw new JdbcSQLException("Expected [" + OLAP_URL_PREFIX + "] url, received [" + connectionString + "]");
        }
    }

    private static Properties parseProperties(URI uri, String u) throws JdbcSQLException {
        Properties props = new Properties();
        try {
            if (uri.getRawQuery() != null) {
                
                String[] prms = StrUtil.split(uri.getRawQuery(), "&");
                for (String param : prms) {
                    String[] args = StrUtil.split(param, "=");
                    if (args.length != 2) {
                        throw new JdbcSQLException("Invalid parameter [" + param + "], format needs to be key=value");
                    }
                    final String key = URLDecoder.decode(args[0], StandardCharsets.UTF_8).trim();
                    final String val = URLDecoder.decode(args[1], StandardCharsets.UTF_8);
                    
                    props.setProperty(key, val);
                }
            }
        } catch (JdbcSQLException e) {
            throw e;
        } catch (Exception e) {
            
            throw new IllegalArgumentException("Failed to parse acceptable jdbc url [" + u + "]", e);
        }
        return props;
    }

    private JdbcConfiguration(URI baseURI, String u, Properties props) throws JdbcSQLException {
        super(baseURI, u, props);

        this.debug = parseValue(DEBUG, props.getProperty(DEBUG, DEBUG_DEFAULT), Boolean::parseBoolean);
        this.debugOut = props.getProperty(DEBUG_OUTPUT, DEBUG_OUTPUT_DEFAULT);
        this.flushAlways = parseValue(DEBUG_FLUSH_ALWAYS, props.getProperty(DEBUG_FLUSH_ALWAYS, DEBUG_FLUSH_ALWAYS_DEFAULT),
                Boolean::parseBoolean);

        this.zoneId = parseValue(TIME_ZONE, props.getProperty(TIME_ZONE, TIME_ZONE_DEFAULT),
                s -> TimeZone.getTimeZone(s).toZoneId().normalized());
        this.fieldMultiValueLeniency = parseValue(FIELD_MULTI_VALUE_LENIENCY,
                props.getProperty(FIELD_MULTI_VALUE_LENIENCY, FIELD_MULTI_VALUE_LENIENCY_DEFAULT), Boolean::parseBoolean);
        this.includeFrozen = parseValue(INDEX_INCLUDE_FROZEN, props.getProperty(INDEX_INCLUDE_FROZEN, INDEX_INCLUDE_FROZEN_DEFAULT),
                Boolean::parseBoolean);
    }

    @Override
    protected Collection<String> extraOptions() {
        return OPTION_NAMES;
    }

    ZoneId zoneId() {
        return zoneId;
    }

    @Override
    public boolean debug() {
        return debug;
    }

    @Override
    public String debugOut() {
        return debugOut;
    }

    @Override
    public boolean flushAlways() {
        return flushAlways;
    }

    public TimeZone timeZone() {
        return zoneId != null ? TimeZone.getTimeZone(zoneId) : null;
    }

    public boolean fieldMultiValueLeniency() {
        return fieldMultiValueLeniency;
    }

    public boolean indexIncludeFrozen() {
        return includeFrozen;
    }

    public static boolean canAccept(String url) {
        String u = url.trim();
        return (StrUtil.notBlank(u) &&
            (u.startsWith(OLAP_URL_PREFIX)));
    }

    public DriverPropertyInfo[] driverPropertyInfo() {
        List<DriverPropertyInfo> info = new ArrayList<>();
        for (String option : optionNames()) {
            DriverPropertyInfo prop = new DriverPropertyInfo(option, null);
            info.add(prop);
        }

        return info.toArray(new DriverPropertyInfo[info.size()]);
    }
}
