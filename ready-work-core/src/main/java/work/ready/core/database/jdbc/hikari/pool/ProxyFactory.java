package work.ready.core.database.jdbc.hikari.pool;

import java.lang.String;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import work.ready.core.database.jdbc.common.CallableStatementInformation;
import work.ready.core.database.jdbc.common.PreparedStatementInformation;
import work.ready.core.database.jdbc.common.ResultSetInformation;
import work.ready.core.database.jdbc.common.StatementInformation;
import work.ready.core.database.jdbc.hikari.util.FastList;

class ProxyFactory {
    static ProxyConnection getProxyConnection(final PoolEntry poolEntry,
            final Connection connection, final FastList<Statement> openStatements,
            final ProxyLeakTask leakTask, final long now, final boolean isReadOnly,
            final boolean isAutoCommit) {
        return new HikariProxyConnection(poolEntry, connection, openStatements, leakTask, now, isReadOnly, isAutoCommit);
    }

    static Statement getProxyStatement(final ProxyConnection connection,
            final Statement statement) {
        HikariProxyConnection hikariConnection = (HikariProxyConnection)connection;
        var si = new StatementInformation(hikariConnection.getConnectionInformation());
        
        Statement proxied = new HikariProxyStatement(connection, statement).advancedFeatureSupport(si, hikariConnection.getJdbcEventListener());
        si.setStatement(proxied);
        return proxied;
    }

    static PreparedStatement getProxyPreparedStatement(final ProxyConnection connection,
            final String sql, final PreparedStatement statement) {
        HikariProxyConnection hikariConnection = (HikariProxyConnection)connection;
        var si = new PreparedStatementInformation(hikariConnection.getConnectionInformation(), sql);
        
        PreparedStatement proxied = new HikariProxyPreparedStatement(connection, statement).advancedFeatureSupport(si, hikariConnection.getJdbcEventListener());
        si.setStatement(proxied);
        return proxied;
    }

    static CallableStatement getProxyCallableStatement(final ProxyConnection connection,
            final String sql, final CallableStatement statement) {
        HikariProxyConnection hikariConnection = (HikariProxyConnection)connection;
        var si = new CallableStatementInformation(hikariConnection.getConnectionInformation(), sql);
        
        CallableStatement proxied = new HikariProxyCallableStatement(connection, statement).advancedFeatureSupport(si, hikariConnection.getJdbcEventListener());
        si.setStatement(proxied);
        return proxied;
    }

    static ResultSet getProxyResultSet(final ProxyConnection connection,
            final ProxyStatement statement, final ResultSet resultSet) {
        StatementInformation statementInformation;
        if(statement instanceof HikariProxyPreparedStatement) {
           statementInformation = ((HikariProxyPreparedStatement)statement).getStatementInformation();
        } else if(statement instanceof HikariProxyCallableStatement) {
           statementInformation = ((HikariProxyCallableStatement)statement).getStatementInformation();
        } else if(statement != null) {
           statementInformation = ((HikariProxyStatement)statement).getStatementInformation();
        } else {
           statementInformation = new StatementInformation(((HikariProxyConnection)connection).getConnectionInformation());
        }
        var ri = new ResultSetInformation(statementInformation);
        ri.setResultSet(resultSet);
        return new HikariProxyResultSet(connection, statement, resultSet).advancedFeatureSupport(ri, ((HikariProxyConnection)connection).getJdbcEventListener());
    }

    static DatabaseMetaData getProxyDatabaseMetaData(final ProxyConnection connection,
            final DatabaseMetaData metaData) {
        return new HikariProxyDatabaseMetaData(connection, metaData);
    }
}
