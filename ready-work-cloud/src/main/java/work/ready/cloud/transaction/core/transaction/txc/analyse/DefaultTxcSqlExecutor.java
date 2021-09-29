/**
 *
 * Original work Copyright 2017-2019 CodingApi
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
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
package work.ready.cloud.transaction.core.transaction.txc.analyse;

import work.ready.cloud.transaction.common.lock.DtxLocks;
import work.ready.cloud.transaction.core.context.DtxThreadContext;
import work.ready.cloud.transaction.core.transaction.txc.analyse.util.SqlUtils;
import work.ready.cloud.transaction.core.transaction.txc.analyse.bean.*;
import work.ready.core.database.query.QueryRunner;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class DefaultTxcSqlExecutor implements TxcSqlExecutor {
    private static final Log logger = LogFactory.getLog(DefaultTxcSqlExecutor.class);
    private final QueryRunner queryRunner;

    public DefaultTxcSqlExecutor() {
        this.queryRunner = new QueryRunner();
    }

    @Override
    public List<InvolvedRecord> dataAffectedByUpdate(Connection connection, UpdateImageParams updateImageParams)
            throws SQLException {
        
        String beforeSql = SqlUtils.SELECT
                + String.join(SqlUtils.SQL_COMMA_SEPARATOR, updateImageParams.getColumns())
                + SqlUtils.SQL_COMMA_SEPARATOR
                + String.join(SqlUtils.SQL_COMMA_SEPARATOR, updateImageParams.getPrimaryKeys())
                + SqlUtils.FROM
                + String.join(SqlUtils.SQL_COMMA_SEPARATOR, updateImageParams.getTables())
                + SqlUtils.WHERE
                + updateImageParams.getWhereSql();
        DtxThreadContext.inActivate();
        try {
            return queryRunner.query(connection, beforeSql,
                    new TxcModifiedRecordListHandler(updateImageParams.getPrimaryKeys(), updateImageParams.getColumns()));
        } catch (SQLException e) {
            DtxThreadContext.rollbackActivate();
            throw e;
        } finally {
            DtxThreadContext.rollbackActivate();
        }
    }

    @Override
    public List<InvolvedRecord> dataAffectedByDelete(Connection connection, DeleteImageParams deleteImageParams)
            throws SQLException {
        String beforeSql = SqlUtils.SELECT + String.join(SqlUtils.SQL_COMMA_SEPARATOR, deleteImageParams.getColumns()) +
                SqlUtils.FROM +
                String.join(SqlUtils.SQL_COMMA_SEPARATOR, deleteImageParams.getTables()) +
                SqlUtils.WHERE +
                deleteImageParams.getSqlWhere();
        DtxThreadContext.inActivate();
        try {
            return queryRunner.query(connection, beforeSql,
                    new TxcModifiedRecordListHandler(
                            deleteImageParams.getPrimaryKeys(),
                            deleteImageParams.getColumns()));
        } catch (SQLException e) {
            DtxThreadContext.rollbackActivate();
            throw e;
        } finally {
            DtxThreadContext.rollbackActivate();
        }
    }

    private static final Pattern FOR_UPDATE = Pattern.compile("(?i)\\s+FOR\\s+UPDATE");
    private static final Pattern LOCK_IN_SHARE_MODE = Pattern.compile("(?i)\\s+LOCK\\s+IN\\s+SHARE\\s+MODE");
    @Override
    public List<InvolvedRecord> involvedPrimaryKeys(Connection connection, SelectImageParams selectImageParams, int lockType)
            throws SQLException {
        DtxThreadContext.inActivate();
        try {
            
            return queryRunner.query(connection,
                    lockType == DtxLocks.X_LOCK ? FOR_UPDATE.matcher(selectImageParams.getSql()).replaceAll("")
                            : LOCK_IN_SHARE_MODE.matcher(selectImageParams.getSql()).replaceAll(""),
                    new TxcModifiedRecordListHandler(
                            selectImageParams.getPrimaryKeys(),
                            selectImageParams.getPrimaryKeys()));
        } catch (SQLException e) {
            DtxThreadContext.rollbackActivate();
            throw e;
        } finally {
            DtxThreadContext.rollbackActivate();
        }
    }

    @Override
    public void applyUndoLog(Map<String, List<StatementInfo>> statementInfo) throws SQLException {
        Connection connection = null;
        DtxThreadContext.inActivate();
        try {
            for (var entry : statementInfo.entrySet()) {
                try {
                    connection = Ready.dbManager().getConfig(entry.getKey()).getConnection();
                    connection.setAutoCommit(false);
                    for (StatementInfo eachStatement : entry.getValue()) {
                        logger.warn("TXC ROLLBACK, apply undo log sql: %s, params: %s", eachStatement.getSql(), Arrays.asList(eachStatement.getParams()));
                        queryRunner.update(connection, eachStatement.getSql(), eachStatement.getParams());
                    }
                    connection.commit();
                } catch (SQLException e) {
                    if (connection != null) {
                        connection.rollback();
                    }
                    logger.error(e, "txc exception, while applying undo log.");
                    throw e;
                } finally {
                    if (connection != null) {
                        connection.setAutoCommit(true);
                        queryRunner.close(connection);
                    }
                }
            }
        } catch (SQLException e) {
            DtxThreadContext.rollbackActivate();
            throw e;
        } finally {
            DtxThreadContext.rollbackActivate();
        }
    }
}
