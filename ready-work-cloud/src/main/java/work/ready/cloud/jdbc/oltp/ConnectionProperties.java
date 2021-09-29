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

package work.ready.cloud.jdbc.oltp;

import java.io.Serializable;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.internal.jdbc.thin.ConnectionPropertiesImpl;
import org.apache.ignite.internal.jdbc.thin.JdbcThinUtils;
import org.apache.ignite.internal.processors.odbc.SqlStateCode;
import org.apache.ignite.internal.processors.odbc.jdbc.JdbcThinFeature;
import org.apache.ignite.internal.processors.query.NestedTxMode;
import org.apache.ignite.internal.util.HostAndPortRange;
import org.apache.ignite.internal.util.typedef.F;
import work.ready.cloud.jdbc.ReadyJdbcDriver;
import work.ready.cloud.jdbc.debug.JdbcDebugConfig;

public class ConnectionProperties extends ConnectionPropertiesImpl implements JdbcDebugConfig {

    private static final long serialVersionUID = 0L;

    public static final String PROP_PREFIX = "ignite.jdbc.";

    private static final int DFLT_SOCK_BUFFER_SIZE = 64 * 1024;

    private static final String PROP_SCHEMA = "schema";

    private String url;

    private HostAndPortRange[] addrs;

    private BooleanProperty debug = new BooleanProperty(
            "debug", "Enable jdbc debug", false, false);
    
    private StringProperty debugOut = new StringProperty(
            "debug.output", "jdbc debug output", "out", null, false, null);
    private BooleanProperty flushAlways = new BooleanProperty(
            "debug.flushAlways", "Enable jdbc debug output flush immediately", true, false);

    private StringProperty schema = new StringProperty(PROP_SCHEMA,
        "Schema name of the connection", "PUBLIC", null, false, null);

    private BooleanProperty distributedJoins = new BooleanProperty(
        "distributedJoins", "Enable distributed joins", false, false);

    private BooleanProperty enforceJoinOrder = new BooleanProperty(
        "enforceJoinOrder", "Enable enforce join order", false, false);

    private BooleanProperty collocated = new BooleanProperty(
        "collocated", "Enable collocated query", false, false);

    private BooleanProperty replicatedOnly = new BooleanProperty(
        "replicatedOnly", "Specify if the all queries contain only replicated tables", false, false);

    private BooleanProperty autoCloseServerCursor = new BooleanProperty(
        "autoCloseServerCursor", "Enable auto close server cursors when last piece of result set is retrieved. " +
        "If the server-side cursor is already closed, you may get an exception when trying to call " +
        "`ResultSet.getMetadata()` method.", false, false);

    private BooleanProperty tcpNoDelay = new BooleanProperty(
        "tcpNoDelay", "TCP no delay flag", true, false);

    private BooleanProperty lazy = new BooleanProperty(
        "lazy", "Enable lazy query execution", false, false);

    private IntegerProperty socketSendBuffer = new IntegerProperty(
        "socketSendBuffer", "Socket send buffer size",
        DFLT_SOCK_BUFFER_SIZE, false, 0, Integer.MAX_VALUE);

    private IntegerProperty socketReceiveBuffer = new IntegerProperty(
        "socketReceiveBuffer", "Socket send buffer size",
        DFLT_SOCK_BUFFER_SIZE, false, 0, Integer.MAX_VALUE);

    private BooleanProperty skipReducerOnUpdate = new BooleanProperty(
        "skipReducerOnUpdate", "Enable execution update queries on ignite server nodes", false, false);

    private StringProperty nestedTxMode = new StringProperty(
        "nestedTransactionsMode", "Way to handle nested transactions", NestedTxMode.ERROR.name(),
        new String[] { NestedTxMode.COMMIT.name(), NestedTxMode.ERROR.name(), NestedTxMode.IGNORE.name() },
        false, new PropertyValidator() {
        private static final long serialVersionUID = 0L;

        @Override public void validate(String mode) throws SQLException {
            if (!F.isEmpty(mode)) {
                try {
                    NestedTxMode.valueOf(mode.toUpperCase());
                }
                catch (IllegalArgumentException e) {
                    throw new SQLException("Invalid nested transactions handling mode, allowed values: " +
                        Arrays.toString(nestedTxMode.choices), SqlStateCode.CLIENT_CONNECTION_FAILED);
                }
            }
        }
    });

    private StringProperty sslMode = new StringProperty("sslMode",
        "The SSL mode of the connection", SSL_MODE_DISABLE,
        new String[] {SSL_MODE_DISABLE, SSL_MODE_REQUIRE}, false, null);

    private StringProperty sslProtocol = new StringProperty("sslProtocol",
        "SSL protocol name", null, null, false, null);

    private StringProperty sslCipherSuites = new StringProperty("sslCipherSuites",
        "Supported SSL ciphers", null,
        null, false, null);

    private StringProperty sslKeyAlgorithm = new StringProperty("sslKeyAlgorithm",
        "SSL key algorithm name", "SunX509", null, false, null);

    private StringProperty sslClientCertificateKeyStoreUrl =
        new StringProperty("sslClientCertificateKeyStoreUrl",
            "Client certificate key store URL",
            null, null, false, null);

    private StringProperty sslClientCertificateKeyStorePassword =
        new StringProperty("sslClientCertificateKeyStorePassword",
            "Client certificate key store password",
            null, null, false, null);

    private StringProperty sslClientCertificateKeyStoreType =
        new StringProperty("sslClientCertificateKeyStoreType",
            "Client certificate key store type",
            null, null, false, null);

    private StringProperty sslTrustCertificateKeyStoreUrl =
        new StringProperty("sslTrustCertificateKeyStoreUrl",
            "Trusted certificate key store URL", null, null, false, null);

    private StringProperty sslTrustCertificateKeyStorePassword =
        new StringProperty("sslTrustCertificateKeyStorePassword",
            "Trusted certificate key store password", null, null, false, null);

    private StringProperty sslTrustCertificateKeyStoreType =
        new StringProperty("sslTrustCertificateKeyStoreType",
            "Trusted certificate key store type",
            null, null, false, null);

    private BooleanProperty sslTrustAll = new BooleanProperty("sslTrustAll",
        "Trust all certificates", false, false);

    private StringProperty sslFactory = new StringProperty("sslFactory",
        "Custom class name that implements Factory<SSLSocketFactory>", null, null, false, null);

    private StringProperty userAttrsFactory = new StringProperty("userAttributesFactory",
        "Custom class name that implements Factory<Map<String, String>> (user attributes)", null, null, false, null);

    private StringProperty user = new StringProperty(
        "user", "User name to authenticate the client on the server side", null, null, false, null);

    private StringProperty passwd = new StringProperty(
        "password", "User's password", null, null, false, null);

    private BooleanProperty dataPageScanEnabled = new BooleanProperty("dataPageScanEnabled",
        "Whether data page scan for queries is allowed. If not specified, server defines the default behaviour.",
        null, false);

    private BooleanProperty partitionAwareness = new BooleanProperty(
        "partitionAwareness",
        "Whether jdbc thin partition awareness is enabled.",
        false, false);

    private IntegerProperty updateBatchSize = new IntegerProperty("updateBatchSize",
        "Update bach size (the size of internal batches are used for INSERT/UPDATE/DELETE operation). " +
            "Set to 1 to prevent deadlock on update where keys sequence are different " +
            "in several concurrent updates.", null, false, 1, Integer.MAX_VALUE);

    private IntegerProperty partitionAwarenessSQLCacheSize = new IntegerProperty("partitionAwarenessSQLCacheSize",
        "The size of sql cache that is used within partition awareness optimization.",
        1_000, false, 1, Integer.MAX_VALUE);

    private IntegerProperty partitionAwarenessPartDistributionsCacheSize = new IntegerProperty(
        "partitionAwarenessPartitionDistributionsCacheSize",
        "The size of partition distributions cache that is used within partition awareness optimization.",
        1_000, false, 1, Integer.MAX_VALUE);

    private IntegerProperty qryTimeout = new IntegerProperty("queryTimeout",
        "Sets the number of seconds the driver will wait for a <code>Statement</code> object to execute." +
            " Zero means there is no limits.",
        0, false, 0, Integer.MAX_VALUE);

    private IntegerProperty connTimeout = new IntegerProperty("connectionTimeout",
        "Sets the number of milliseconds JDBC client will waits for server to response." +
            " Zero means there is no limits.",
        0, false, 0, Integer.MAX_VALUE);

    private StringProperty disabledFeatures = new StringProperty("disabledFeatures",
        "Sets enumeration of features to force disable its.", null, null, false, new PropertyValidator() {
        @Override public void validate(String val) throws SQLException {
            if (val == null)
                return;

            String[] features = val.split("\\W+");

            for (String f : features) {
                try {
                    JdbcThinFeature.valueOf(f.toUpperCase());
                }
                catch (IllegalArgumentException e) {
                    throw new SQLException("Unknown feature: " + f);
                }
            }
        }
    });

    private BooleanProperty keepBinary = new BooleanProperty("keepBinary",
        "Whether to keep binary objects in binary form.", false, false);

    private final ConnectionProperty[] propsArray = {
            debug, debugOut, flushAlways,
        distributedJoins, enforceJoinOrder, collocated, replicatedOnly, autoCloseServerCursor,
        tcpNoDelay, lazy, socketSendBuffer, socketReceiveBuffer, skipReducerOnUpdate, nestedTxMode,
        sslMode, sslCipherSuites, sslProtocol, sslKeyAlgorithm,
        sslClientCertificateKeyStoreUrl, sslClientCertificateKeyStorePassword, sslClientCertificateKeyStoreType,
        sslTrustCertificateKeyStoreUrl, sslTrustCertificateKeyStorePassword, sslTrustCertificateKeyStoreType,
        sslTrustAll, sslFactory,
        userAttrsFactory,
        user, passwd,
        dataPageScanEnabled,
            partitionAwareness,
        updateBatchSize,
            partitionAwarenessSQLCacheSize,
            partitionAwarenessPartDistributionsCacheSize,
        qryTimeout,
        connTimeout,
        disabledFeatures,
        keepBinary
    };

    @Override
    public boolean debug() {
        return debug.value();
    }

    @Override
    public String debugOut() {
        return debugOut.value();
    }

    @Override
    public boolean flushAlways() {
        return flushAlways.value();
    }

    @Override public String getSchema() {
        return schema.value();
    }

    @Override public void setSchema(String schema) {
        this.schema.setValue(schema);
    }

    @Override public String getUrl() {
        if (url != null)
            return url;
        else {
            if (F.isEmpty(getAddresses()))
                return null;

            StringBuilder sbUrl = new StringBuilder(ReadyJdbcDriver.OLTP_URL_PREFIX);

            HostAndPortRange[] addrs = getAddresses();

            for (int i = 0; i < addrs.length; i++) {
                if (i > 0)
                    sbUrl.append(',');

                sbUrl.append(addrs[i].toString());
            }

            if (!F.isEmpty(getSchema()))
                sbUrl.append('/').append(getSchema());

            return sbUrl.toString();
        }
    }

    @Override public void setUrl(String url) throws SQLException {
        this.url = url;

        init(url, new Properties());
    }

    @Override public HostAndPortRange[] getAddresses() {
        return addrs;
    }

    @Override public void setAddresses(HostAndPortRange[] addrs) {
        this.addrs = addrs;
    }

    @Override public boolean isDistributedJoins() {
        return distributedJoins.value();
    }

    @Override public void setDistributedJoins(boolean val) {
        distributedJoins.setValue(val);
    }

    @Override public boolean isEnforceJoinOrder() {
        return enforceJoinOrder.value();
    }

    @Override public void setEnforceJoinOrder(boolean val) {
        enforceJoinOrder.setValue(val);
    }

    @Override public boolean isCollocated() {
        return collocated.value();
    }

    @Override public void setCollocated(boolean val) {
        collocated.setValue(val);
    }

    @Override public boolean isReplicatedOnly() {
        return replicatedOnly.value();
    }

    @Override public void setReplicatedOnly(boolean val) {
        replicatedOnly.setValue(val);
    }

    @Override public boolean isAutoCloseServerCursor() {
        return autoCloseServerCursor.value();
    }

    @Override public void setAutoCloseServerCursor(boolean val) {
        autoCloseServerCursor.setValue(val);
    }

    @Override public int getSocketSendBuffer() {
        return socketSendBuffer.value();
    }

    @Override public void setSocketSendBuffer(int size) throws SQLException {
        socketSendBuffer.setValue(size);
    }

    @Override public int getSocketReceiveBuffer() {
        return socketReceiveBuffer.value();
    }

    @Override public void setSocketReceiveBuffer(int size) throws SQLException {
        socketReceiveBuffer.setValue(size);
    }

    @Override public boolean isTcpNoDelay() {
        return tcpNoDelay.value();
    }

    @Override public void setTcpNoDelay(boolean val) {
        tcpNoDelay.setValue(val);
    }

    @Override public boolean isLazy() {
        return lazy.value();
    }

    @Override public void setLazy(boolean val) {
        lazy.setValue(val);
    }

    @Override public boolean isSkipReducerOnUpdate() {
        return skipReducerOnUpdate.value();
    }

    @Override public void setSkipReducerOnUpdate(boolean val) {
        skipReducerOnUpdate.setValue(val);
    }

    @Override public String getSslMode() {
        return sslMode.value();
    }

    @Override public void setSslMode(String mode) {
        sslMode.setValue(mode);
    }

    @Override public String getSslProtocol() {
        return sslProtocol.value();
    }

    @Override public void setSslProtocol(String sslProtocol) {
        this.sslProtocol.setValue(sslProtocol);
    }

    @Override public String getSslCipherSuites() {
        return sslCipherSuites.value();
    }

    @Override public void setSslCipherSuites(String sslCipherSuites) {
        this.sslCipherSuites.setValue(sslCipherSuites);
    }

    @Override public String getSslKeyAlgorithm() {
        return sslKeyAlgorithm.value();
    }

    @Override public void setSslKeyAlgorithm(String keyAlgorithm) {
        sslKeyAlgorithm.setValue(keyAlgorithm);
    }

    @Override public String getSslClientCertificateKeyStoreUrl() {
        return sslClientCertificateKeyStoreUrl.value();
    }

    @Override public void setSslClientCertificateKeyStoreUrl(String url) {
        sslClientCertificateKeyStoreUrl.setValue(url);
    }

    @Override public String getSslClientCertificateKeyStorePassword() {
        return sslClientCertificateKeyStorePassword.value();
    }

    @Override public void setSslClientCertificateKeyStorePassword(String passwd) {
        sslClientCertificateKeyStorePassword.setValue(passwd);
    }

    @Override public String getSslClientCertificateKeyStoreType() {
        return sslClientCertificateKeyStoreType.value();
    }

    @Override public void setSslClientCertificateKeyStoreType(String ksType) {
        sslClientCertificateKeyStoreType.setValue(ksType);
    }

    @Override public String getSslTrustCertificateKeyStoreUrl() {
        return sslTrustCertificateKeyStoreUrl.value();
    }

    @Override public void setSslTrustCertificateKeyStoreUrl(String url) {
        sslTrustCertificateKeyStoreUrl.setValue(url);
    }

    @Override public String getSslTrustCertificateKeyStorePassword() {
        return sslTrustCertificateKeyStorePassword.value();
    }

    @Override public void setSslTrustCertificateKeyStorePassword(String passwd) {
        sslTrustCertificateKeyStorePassword.setValue(passwd);
    }

    @Override public String getSslTrustCertificateKeyStoreType() {
        return sslTrustCertificateKeyStoreType.value();
    }

    @Override public void setSslTrustCertificateKeyStoreType(String ksType) {
        sslTrustCertificateKeyStoreType.setValue(ksType);
    }

    @Override public boolean isSslTrustAll() {
        return sslTrustAll.value();
    }

    @Override public void setSslTrustAll(boolean trustAll) {
        this.sslTrustAll.setValue(trustAll);
    }

    @Override public String getSslFactory() {
        return sslFactory.value();
    }

    @Override public void setSslFactory(String sslFactory) {
        this.sslFactory.setValue(sslFactory);
    }

    @Override public String nestedTxMode() {
        return nestedTxMode.value();
    }

    @Override public void nestedTxMode(String val) {
        nestedTxMode.setValue(val);
    }

    @Override public void setUsername(String name) {
        user.setValue(name);
    }

    @Override public String getUsername() {
        return user.value();
    }

    @Override public void setPassword(String passwd) {
        this.passwd.setValue(passwd);
    }

    @Override public String getPassword() {
        return passwd.value();
    }

    @Override public Boolean isDataPageScanEnabled() {
        return dataPageScanEnabled.value();
    }

    @Override public void setDataPageScanEnabled(Boolean dataPageScanEnabled) {
        this.dataPageScanEnabled.setValue(dataPageScanEnabled);
    }

    @Override public boolean isPartitionAwareness() {
        return partitionAwareness.value();
    }

    @Override public void setPartitionAwareness(boolean partitionAwareness) {
        this.partitionAwareness.setValue(partitionAwareness);
    }

    @Override public Integer getUpdateBatchSize() {
        return updateBatchSize.value();
    }

    @Override public void setUpdateBatchSize(Integer updateBatchSize) throws SQLException {
        this.updateBatchSize.setValue(updateBatchSize);
    }

    @Override public int getPartitionAwarenessSqlCacheSize() {
        return partitionAwarenessSQLCacheSize.value();
    }

    @Override public void setPartitionAwarenessSqlCacheSize(int partitionAwarenessSqlCacheSize)
        throws SQLException {
        this.partitionAwarenessSQLCacheSize.setValue(partitionAwarenessSqlCacheSize);
    }

    @Override public int getPartitionAwarenessPartitionDistributionsCacheSize() {
        return partitionAwarenessPartDistributionsCacheSize.value();
    }

    @Override public void setPartitionAwarenessPartitionDistributionsCacheSize(
        int partitionAwarenessPartDistributionsCacheSize) throws SQLException {
        this.partitionAwarenessPartDistributionsCacheSize.setValue(
                partitionAwarenessPartDistributionsCacheSize);
    }

    @Override public Integer getQueryTimeout() {
        return qryTimeout.value();
    }

    @Override public void setQueryTimeout(Integer timeout) throws SQLException {
        qryTimeout.setValue(timeout);
    }

    @Override public int getConnectionTimeout() {
        return connTimeout.value();
    }

    @Override public void setConnectionTimeout(Integer timeout) throws SQLException {
        connTimeout.setValue(timeout);
    }

    @Override public String getUserAttributesFactory() {
        return userAttrsFactory.value();
    }

    @Override public void setUserAttributesFactory(String cls) {
        userAttrsFactory.setValue(cls);
    }

    @Override public String disabledFeatures() {
        return disabledFeatures.value();
    }

    @Override public void disabledFeatures(String features) {
        disabledFeatures.setValue(features);
    }

    @Override public boolean isKeepBinary() {
        return keepBinary.value();
    }

    @Override public void setKeepBinary(boolean keepBinary) {
        this.keepBinary.setValue(keepBinary);
    }

    public void init(String url, Properties props) throws SQLException {
        Properties props0 = (Properties)props.clone();

        if (!F.isEmpty(url))
            parseUrl(url, props0);

        for (ConnectionProperty aPropsArray : propsArray)
            aPropsArray.init(props0);

        if (!F.isEmpty(props.getProperty("user"))) {
            setUsername(props.getProperty("user"));
            setPassword(props.getProperty("password"));
        }
    }

    private void parseUrl(String url, Properties props) throws SQLException {
        if (F.isEmpty(url))
            throw new SQLException("URL cannot be null or empty.");

        if (!url.startsWith(ReadyJdbcDriver.OLTP_URL_PREFIX))
            throw new SQLException("URL must start with \"" + ReadyJdbcDriver.OLTP_URL_PREFIX + "\"");

        String nakedUrl = url.substring(ReadyJdbcDriver.OLTP_URL_PREFIX.length()).trim();

        parseUrl0(nakedUrl, props);
    }

    private void parseUrl0(String url, Properties props) throws SQLException {
        
        int semicolonPos = url.indexOf(";");
        int slashPos = url.indexOf("/");
        int queryPos = url.indexOf("?");

        boolean semicolonMode;

        if (semicolonPos == -1 && slashPos == -1 && queryPos == -1)
            
            semicolonMode = true;
        else {
            if (semicolonPos != -1) {
                
                semicolonMode =
                    (slashPos == -1 || semicolonPos < slashPos) && (queryPos == -1 || semicolonPos < queryPos);
            }
            else
                
                semicolonMode = false;
        }

        if (semicolonMode)
            parseUrlWithSemicolon(url, props);
        else
            parseUrlWithQuery(url, props);
    }

    private void parseUrlWithSemicolon(String url, Properties props) throws SQLException {
        int pathPartEndPos = url.indexOf(';');

        if (pathPartEndPos == -1)
            pathPartEndPos = url.length();

        String pathPart = url.substring(0, pathPartEndPos);

        String paramPart = null;

        if (pathPartEndPos > 0 && pathPartEndPos < url.length())
            paramPart = url.substring(pathPartEndPos + 1, url.length());

        parseEndpoints(pathPart);

        if (!F.isEmpty(paramPart))
            parseParameters(paramPart, props, ";");
    }

    private void parseUrlWithQuery(String url, Properties props) throws SQLException {
        int pathPartEndPos = url.indexOf('?');

        if (pathPartEndPos == -1)
            pathPartEndPos = url.length();

        String pathPart = url.substring(0, pathPartEndPos);

        String paramPart = null;

        if (pathPartEndPos > 0 && pathPartEndPos < url.length())
            paramPart = url.substring(pathPartEndPos + 1, url.length());

        String[] pathParts = pathPart.split("/");

        parseEndpoints(pathParts[0]);

        if (pathParts.length > 2) {
            throw new SQLException("Invalid URL format (only schema name is allowed in URL path parameter " +
                "'host:port[/schemaName]'): " + this.url, SqlStateCode.CLIENT_CONNECTION_FAILED);
        }

        setSchema(pathParts.length == 2 ? pathParts[1] : null);

        if (!F.isEmpty(paramPart))
            parseParameters(paramPart, props, "&");
    }

    private void parseEndpoints(String endpointStr) throws SQLException {
        String[] endpoints = endpointStr.split(",");

        if (endpoints.length > 0)
            addrs = new HostAndPortRange[endpoints.length];

        for (int i = 0; i < endpoints.length; ++i ) {
            try {
                addrs[i] = HostAndPortRange.parse(endpoints[i],
                    ClientConnectorConfiguration.DFLT_PORT, ClientConnectorConfiguration.DFLT_PORT,
                    "Invalid endpoint format (should be \"host[:portRangeFrom[..portRangeTo]]\")");
            }
            catch (IgniteCheckedException e) {
                throw new SQLException(e.getMessage(), SqlStateCode.CLIENT_CONNECTION_FAILED, e);
            }
        }

        if (F.isEmpty(addrs) || F.isEmpty(addrs[0].host()))
            throw new SQLException("Host name is empty", SqlStateCode.CLIENT_CONNECTION_FAILED);
    }

    private void parseParameters(String paramStr, Properties props, String delimChar) throws SQLException {
        StringTokenizer st = new StringTokenizer(paramStr, delimChar);

        boolean insideBrace = false;

        String key = null;
        String val = null;

        while (st.hasMoreTokens()) {
            String token = st.nextToken();

            if (!insideBrace) {
                int eqSymPos = token.indexOf('=');

                if (eqSymPos < 0) {
                    throw new SQLException("Invalid parameter format (should be \"key1=val1" + delimChar +
                        "key2=val2" + delimChar + "...\"): " + token);
                }

                if (eqSymPos == token.length())
                    throw new SQLException("Invalid parameter format (key and value cannot be empty): " + token);

                key = token.substring(0, eqSymPos);
                val = token.substring(eqSymPos + 1, token.length());

                if (val.startsWith("{")) {
                    val = val.substring(1);

                    insideBrace = true;
                }
            }
            else
                val += delimChar + token;

            if (val.endsWith("}")) {
                insideBrace = false;

                val = val.substring(0, val.length() - 1);
            }

            if (val.contains("{") || val.contains("}")) {
                throw new SQLException("Braces cannot be escaped in the value. " +
                    "Please use the connection Properties for such values. [property=" + key + ']');
            }

            if (!insideBrace) {
                if (key.isEmpty() || val.isEmpty())
                    throw new SQLException("Invalid parameter format (key and value cannot be empty): " + token);

                if (PROP_SCHEMA.equalsIgnoreCase(key))
                    setSchema(val);
                else
                    props.setProperty(PROP_PREFIX + key, val);
            }
        }
    }

    public DriverPropertyInfo[] getDriverPropertyInfo() {
        DriverPropertyInfo[] infos = new DriverPropertyInfo[propsArray.length];

        for (int i = 0; i < propsArray.length; ++i)
            infos[i] = propsArray[i].getDriverPropertyInfo();

        return infos;
    }

    public Properties storeToProperties() {
        Properties props = new Properties();

        for (ConnectionProperty prop : propsArray) {
            if (prop.valueObject() != null)
                props.setProperty(PROP_PREFIX + prop.getName(), prop.valueObject());
        }

        return props;
    }

    private interface PropertyValidator extends Serializable {
        
        void validate(String val) throws SQLException;
    }

    private abstract static class ConnectionProperty implements Serializable {
        
        private static final long serialVersionUID = 0L;

        protected String name;

        protected String desc;

        protected Object dfltVal;

        protected String[] choices;

        protected boolean required;

        protected PropertyValidator validator;

        ConnectionProperty(String name, String desc, Object dfltVal, String[] choices, boolean required) {
            this.name = name;
            this.desc = desc;
            this.dfltVal = dfltVal;
            this.choices = choices;
            this.required = required;
        }

        ConnectionProperty(String name, String desc, Object dfltVal, String[] choices, boolean required,
            PropertyValidator validator) {
            this.name = name;
            this.desc = desc;
            this.dfltVal = dfltVal;
            this.choices = choices;
            this.required = required;
            this.validator = validator;
        }

        Object getDfltVal() {
            return dfltVal;
        }

        String getName() {
            return name;
        }

        String[] choices() {
            return choices;
        }

        void init(Properties props) throws SQLException {
            String strVal = props.getProperty(PROP_PREFIX + name);

            if (required && strVal == null) {
                throw new SQLException("Property '" + name + "' is required but not defined",
                    SqlStateCode.CLIENT_CONNECTION_FAILED);
            }

            if (validator != null)
                validator.validate(strVal);

            checkChoices(strVal);

            props.remove(name);

            init(strVal);
        }

        protected void checkChoices(String strVal) throws SQLException {
            if (strVal == null)
                return;

            if (choices != null) {
                for (String ch : choices) {
                    if (ch.equalsIgnoreCase(strVal))
                        return;
                }

                throw new SQLException("Invalid property value. [name=" + name + ", val=" + strVal
                    + ", choices=" + Arrays.toString(choices) + ']', SqlStateCode.CLIENT_CONNECTION_FAILED);
            }
        }

        abstract void init(String str) throws SQLException;

        abstract String valueObject();

        DriverPropertyInfo getDriverPropertyInfo() {
            DriverPropertyInfo dpi = new DriverPropertyInfo(name, valueObject());

            dpi.choices = choices();
            dpi.required = required;
            dpi.description = desc;

            return dpi;
        }
    }

    private static class BooleanProperty extends ConnectionProperty {
        
        private static final long serialVersionUID = 0L;

        private static final String[] boolChoices = new String[] {Boolean.TRUE.toString(), Boolean.FALSE.toString()};

        private Boolean val;

        BooleanProperty(String name, String desc, Boolean dfltVal, boolean required) {
            super(name, desc, dfltVal, boolChoices, required);

            val = dfltVal;
        }

        Boolean value() {
            return val;
        }

        @Override void init(String str) throws SQLException {
            if (str == null)
                val = (Boolean)dfltVal;
            else {
                if (Boolean.TRUE.toString().equalsIgnoreCase(str))
                    val = true;
                else if (Boolean.FALSE.toString().equalsIgnoreCase(str))
                    val = false;
                else
                    throw new SQLException("Failed to parse boolean property [name=" + name +
                        ", value=" + str + ']', SqlStateCode.CLIENT_CONNECTION_FAILED);
            }
        }

        @Override String valueObject() {
            if (val == null)
                return null;

            return Boolean.toString(val);
        }

        void setValue(Boolean val) {
            this.val = val;
        }
    }

    private abstract static class NumberProperty extends ConnectionProperty {
        
        private static final long serialVersionUID = 0L;

        protected Number val;

        private Number[] range;

        NumberProperty(String name, String desc, Number dfltVal, boolean required, Number min, Number max) {
            super(name, desc, dfltVal, null, required);

            val = dfltVal;

            range = new Number[] {min, max};
        }

        @Override void init(String str) throws SQLException {
            if (str == null)
                val = dfltVal != null ? (Number)dfltVal : null;
            else {
                try {
                    setValue(parse(str));
                }
                catch (NumberFormatException e) {
                    throw new SQLException("Failed to parse int property [name=" + name +
                        ", value=" + str + ']', SqlStateCode.CLIENT_CONNECTION_FAILED);
                }
            }
        }

        protected abstract Number parse(String str) throws NumberFormatException;

        @Override String valueObject() {
            return val != null ? String.valueOf(val) : null;
        }

        void setValue(Number val) throws SQLException {
            if (range != null) {
                if (val.doubleValue() < range[0].doubleValue()) {
                    throw new SQLException("Property cannot be lower than " + range[0].toString() + " [name=" + name +
                        ", value=" + val.toString() + ']', SqlStateCode.CLIENT_CONNECTION_FAILED);
                }

                if (val.doubleValue() > range[1].doubleValue()) {
                    throw new SQLException("Property cannot be upper than " + range[1].toString() + " [name=" + name +
                        ", value=" + val.toString() + ']', SqlStateCode.CLIENT_CONNECTION_FAILED);
                }
            }

            this.val = val;
        }
    }

    private static class IntegerProperty extends NumberProperty {
        
        private static final long serialVersionUID = 0L;

        IntegerProperty(String name, String desc, Number dfltVal, boolean required, int min, int max) {
            super(name, desc, dfltVal, required, min, max);
        }

        @Override protected Number parse(String str) throws NumberFormatException {
            return Integer.parseInt(str);
        }

        Integer value() {
            return val != null ? val.intValue() : null;
        }
    }

    private static class StringProperty extends ConnectionProperty {
        
        private static final long serialVersionUID = 0L;

        private String val;

        StringProperty(String name, String desc, String dfltVal, String[] choices, boolean required,
            PropertyValidator validator) {
            super(name, desc, dfltVal, choices, required, validator);

            val = dfltVal;
        }

        void setValue(String val) {
            this.val = val;
        }

        String value() {
            return val;
        }

        @Override void init(String str) throws SQLException {
            if (validator != null)
                validator.validate(str);

            if (str == null)
                val = (String)dfltVal;
            else
                val = str;
        }

        @Override String valueObject() {
            return val;
        }
    }
}
