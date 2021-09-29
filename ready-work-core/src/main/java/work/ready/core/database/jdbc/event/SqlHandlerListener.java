/**
 *
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
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
package work.ready.core.database.jdbc.event;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.update.Update;
import work.ready.core.database.DatabaseManager;
import work.ready.core.database.DbAuditManager;
import work.ready.core.database.DbChangeType;
import work.ready.core.database.SqlDebugger;
import work.ready.core.database.jdbc.common.*;
import work.ready.core.database.jdbc.hikari.ReadyDataSource;
import work.ready.core.database.jdbc.hikari.pool.*;
import work.ready.core.security.data.SqlAuditTuple;
import work.ready.core.server.Ready;
import work.ready.core.tools.StrUtil;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

public class SqlHandlerListener extends AnyExecuteListener {
    private static final String SqlParsed_Attachment = "SqlParsed_Attachment";
    private final DatabaseManager manager;
    private final DbAuditManager auditManager;
    private final SqlDebugger debugger;
    private static final Pattern createIndexIfNotExists = Pattern.compile("(?i)CREATE\\s+INDEX\\s+IF\\s+NOT\\s+EXISTS\\s+");

    public SqlHandlerListener() {
        this.manager = Ready.dbManager();
        this.auditManager = manager.getAuditManager();
        this.debugger = manager.getSqlDebugger();
    }

    @Override
    public String onBeforeAnyExecute(final StatementInformation statementInformation) throws SQLException {
        String sql = statementInformation.getSql();
        if(sql == null) {
            System.err.println("Sql is null");
            return sql;
        }
        List<Map<Integer, Value>> parameters = Collections.emptyList();
        Map<String, Value> namedParameters = Collections.emptyMap();
        String datasource = ((ReadyDataSource)statementInformation.getConnectionInformation().getDataSource()).getPoolName();
        if(statementInformation instanceof CallableStatementInformation) {
            namedParameters = ((CallableStatementInformation) statementInformation).getNamedParameterValues();
        }
        if(statementInformation instanceof PreparedStatementInformation) {
            parameters = ((PreparedStatementInformation) statementInformation).getParameterValues();
        }
        SqlAuditTuple tuple;
        debugger.beforeAudit(datasource, sql);
        String sqlWithValues = null;
        String[] sqlSegments = StrUtil.split(sql, ";", true, true, true);
        if(sqlSegments.length > 1) {

            if(parameters.size() > 0 || namedParameters.size() > 0) {
                throw new SQLException("Complex sql statement is not supported yet. Sql: " + sql);
            }
            ArrayList<Statement> statements = new ArrayList<>();
            boolean isChanged = false;
            for(int i = 0; i < sqlSegments.length; i++) {
                String eachSql = sqlSegments[i];
                Statement statement = sqlParser(eachSql);
                
                tuple = new SqlAuditTuple(datasource, statement, parameters, namedParameters);
                for(SqlExecuteHandler handler : manager.getSqlExecuteHandlers(datasource)) {
                    handler.beforeAudit(tuple);
                }
                auditManager.performAudit(tuple);
                if(tuple.isChanged()) {
                    isChanged = true;
                    eachSql = tuple.getStatement().toString();
                    sqlSegments[i] = eachSql;
                    statementInformation.setStatementQuery(StrUtil.join(sqlSegments, ";"));
                }
                statements.add(statement);

                for (SqlExecuteHandler handler : manager.getSqlExecuteHandlers(datasource)) {
                    handler.beforeSqlExecute(statementInformation, statement, eachSql);
                }
            }
            
            if(isChanged && statementInformation instanceof PreparedStatementInformation) {
                doChange(statementInformation, null);
            }
            statementInformation.setAttachment(SqlParsed_Attachment, statements);
            
            sql = sqlWithValues = statementInformation.getSql();
        } else {
            Statement statement = sqlParser(sql);
            
            tuple = new SqlAuditTuple(datasource, statement, parameters, namedParameters);
            for(SqlExecuteHandler handler : manager.getSqlExecuteHandlers(datasource)) {
                handler.beforeAudit(tuple);
            }
            auditManager.performAudit(tuple);
            if(tuple.isChanged()) {
                sql = tuple.getStatement().toString();
                statementInformation.setStatementQuery(sql);
                
                if(statementInformation instanceof PreparedStatementInformation) {
                    doChange(statementInformation, tuple);
                }
            }
            statementInformation.setAttachment(SqlParsed_Attachment, statement);
            sqlWithValues = statementInformation.getSqlWithValues();
            for(SqlExecuteHandler handler : manager.getSqlExecuteHandlers(datasource)) {
                handler.beforeSqlExecute(statementInformation, statement, sqlWithValues);
            }
        }
        debugger.beforeExecute(datasource, sqlWithValues);
        return sql;
    }

    @Override
    public void onAfterAnyExecute(final StatementInformation statementInformation, long timeElapsedNanos, SQLException e)
    {
        long start = System.nanoTime();
        auditManager.afterSqlExecute(statementInformation, timeElapsedNanos, e);
        for(SqlExecuteHandler handler : manager.getSqlExecuteHandlers(((ReadyDataSource)statementInformation.getConnectionInformation().getDataSource()).getPoolName())) {
            handler.afterSqlExecute(statementInformation, timeElapsedNanos, e);
        }
        dbChangeEventSupport(statementInformation, e);
        debugger.afterExecute(statementInformation.getTotalTimeElapsed() + (System.nanoTime() - start), e);
    }

    private void doChange(StatementInformation statementInformation, SqlAuditTuple tuple) throws SQLException {
        CallableStatement callableStatement = null;
        if(statementInformation instanceof CallableStatementInformation) {
            ((CallableStatement)statementInformation.getStatement()).clearParameters();
            callableStatement = statementInformation.getConnectionInformation().getConnection().prepareCall(statementInformation.getSql()).unwrap(CallableStatement.class);
            HikariProxyCallableStatement proxiedStatement = (HikariProxyCallableStatement)statementInformation.getStatement();
            if(tuple != null) {
                for (var entry : tuple.getNamedParameters().entrySet()) {
                    callableStatement.setObject(entry.getKey(), entry.getValue().getValue());
                }
            }
            proxiedStatement.replaceStatement(callableStatement);
        }
        if(statementInformation instanceof PreparedStatementInformation) {
            if(callableStatement == null) {
                ((PreparedStatement) statementInformation.getStatement()).clearParameters();
                var newStatement = statementInformation.getConnectionInformation().getConnection().prepareStatement(statementInformation.getSql()).unwrap(PreparedStatement.class);
                HikariProxyPreparedStatement proxiedStatement = (HikariProxyPreparedStatement)statementInformation.getStatement();
                if(tuple != null) {
                    for (var valueMap : tuple.getParameters()) {
                        for (var entry : valueMap.entrySet()) {
                            newStatement.setObject(entry.getKey() + 1, entry.getValue().getValue());
                        }
                    }
                }
                proxiedStatement.replaceStatement(newStatement);
            } else {
                if(tuple != null) {
                    for (var valueMap : tuple.getParameters()) {
                        for (var entry : valueMap.entrySet()) {
                            callableStatement.setObject(entry.getKey() + 1, entry.getValue().getValue());
                        }
                    }
                }
            }
        }
    }

    private Statement sqlParser(String sql) throws SQLException {
        try {
            return manager.sqlParser(sql, true);
        } catch (JSQLParserException e) {
            
            if(createIndexIfNotExists.matcher(sql).find()) {
                try {
                    return manager.sqlParser(createIndexIfNotExists.matcher(sql).replaceAll("CREATE INDEX "), true);
                } catch (JSQLParserException e1) {
                    throw new SQLException("sql parser exception: " + sql, e);
                }
            } else {
                throw new SQLException("sql parser exception: " + sql, e);
            }
        }
    }

    private void dbChangeEventSupport(final StatementInformation statementInformation, SQLException e) {
        if(e != null) {
            return;
        }
        Object object = statementInformation.getAttachment(SqlParsed_Attachment);
        String datasource = ((ReadyDataSource) statementInformation.getConnectionInformation().getDataSource()).getPoolName();
        if(object instanceof Statement) {
            if (object instanceof Insert) {
                manager.dbChangeNotify(datasource, ((Insert)object).getTable().getName(), DbChangeType.INSERTED);
            } else
            if (object instanceof Update) {
                manager.dbChangeNotify(datasource, ((Update)object).getTable().getName(), DbChangeType.UPDATED);
            } else
            if (object instanceof Delete) {
                ((Delete) object).getTables().forEach(
                        table -> manager.dbChangeNotify(datasource, ((Delete)object).getTable().getName(), DbChangeType.DELETED)
                );
            } else
            if (object instanceof Replace) {
                manager.dbChangeNotify(datasource, ((Replace)object).getTable().getName(), DbChangeType.REPLACED);
            } else
            if (object instanceof Merge) {
                manager.dbChangeNotify(datasource, ((Merge)object).getTable().getName(), DbChangeType.MERGED);
            }
        } else if(object instanceof List) {
            List<Statement> statements = (List<Statement>)object;
            Map<DbChangeType, Set<String>> tableChanged = new HashMap<>();
            for(Statement statement : statements) {
                if (statement instanceof Insert) {
                    tableChanged.computeIfAbsent(DbChangeType.INSERTED, type->new HashSet<>()).
                            add(((Insert) statement).getTable().getName());
                } else
                if (statement instanceof Update) {
                    tableChanged.computeIfAbsent(DbChangeType.UPDATED, type->new HashSet<>()).
                            add(((Update) statement).getTable().getName());
                } else
                if (statement instanceof Delete) {
                    var set = tableChanged.computeIfAbsent(DbChangeType.DELETED, type->new HashSet<>());
                    ((Delete) statement).getTables().forEach(table -> set.add(table.getName()));
                } else
                if (statement instanceof Replace) {
                    tableChanged.computeIfAbsent(DbChangeType.REPLACED, type->new HashSet<>()).
                            add(((Replace) statement).getTable().getName());
                } else
                if (statement instanceof Merge) {
                    tableChanged.computeIfAbsent(DbChangeType.MERGED, type->new HashSet<>()).
                            add(((Merge) statement).getTable().getName());
                }
            }
            for(var entry : tableChanged.entrySet()) {
                entry.getValue().forEach(
                        tableName->manager.dbChangeNotify(datasource, tableName, entry.getKey())
                );
            }
        }
    }
}
