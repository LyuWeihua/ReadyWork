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
package work.ready.cloud.transaction.core.transaction.txc.resource;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import work.ready.cloud.transaction.core.context.DtxThreadContext;
import work.ready.cloud.transaction.core.transaction.txc.analyse.BusinessSqlInterceptor;
import work.ready.cloud.transaction.core.transaction.txc.analyse.TxcBusinessSqlInterceptor;
import work.ready.cloud.transaction.core.transaction.txc.analyse.bean.LockableSelect;
import work.ready.cloud.transaction.common.Transaction;
import work.ready.core.database.DatabaseManager;
import work.ready.core.database.jdbc.common.SqlExecuteHandler;
import work.ready.core.database.jdbc.common.StatementInformation;
import work.ready.core.database.jdbc.hikari.ReadyDataSource;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TxcJdbcEventListener implements SqlExecuteHandler {
    private static final Log logger = LogFactory.getLog(TxcJdbcEventListener.class);
    public static final String Attachment_ForTXC = "Attachment_ForTXC";
    private final DatabaseManager manager;
    private final BusinessSqlInterceptor businessSqlInterceptor;

    public TxcJdbcEventListener() {
        this.manager = Ready.dbManager();
        this.businessSqlInterceptor = Ready.beanManager().get(BusinessSqlInterceptor.class, TxcBusinessSqlInterceptor.class);
    }

    @Override
    public void beforeSqlExecute(StatementInformation statementInformation, Statement parsed, String sqlWithValues) throws SQLException {
        if(DtxThreadContext.current() == null || !DtxThreadContext.current().isActivate() ||
                !Transaction.TXC.equals(DtxThreadContext.current().getTransactionType()) ||
                !((ReadyDataSource)statementInformation.getConnectionInformation().getDataSource()).isEnabledTransaction()) {
            return;
        }
        
        if (parsed instanceof Update) {
            Statement statement = parseStatement(statementInformation, sqlWithValues);
            businessSqlInterceptor.beforeUpdate(statementInformation.getConnectionInformation().getConnection(), (Update) statement);
        } else if (parsed instanceof Delete) {
            Statement statement = parseStatement(statementInformation, sqlWithValues);
            businessSqlInterceptor.beforeDelete(statementInformation.getConnectionInformation().getConnection(), (Delete) statement);
        } else if (parsed instanceof Insert) {
            Statement statement = parseStatement(statementInformation ,sqlWithValues);
            businessSqlInterceptor.beforeInsert(statementInformation.getConnectionInformation().getConnection(), (Insert) statement);
        } else if (parsed instanceof Select) {
            Statement statement = parseStatement(statementInformation, sqlWithValues);
            businessSqlInterceptor.beforeSelect(statementInformation.getConnectionInformation().getConnection(), new LockableSelect((Select) statement));
        }
    }

    private Statement parseStatement(StatementInformation statementInformation, String sqlWithValues) throws SQLException {
        try {
            Statement statement = manager.sqlParser(sqlWithValues, true);
            List<Statement> list;
            if(statementInformation.getAttachment(Attachment_ForTXC) == null) {
                list = new ArrayList<>();
                statementInformation.setAttachment(Attachment_ForTXC, list);
            } else {
                list = (List<Statement>)statementInformation.getAttachment(Attachment_ForTXC);
            }
            list.add(statement);
            logger.debug("statement > %s", statement);
            return statement;
        } catch (JSQLParserException e) {
            throw new SQLException("sql parser exception: " + sqlWithValues, e);
        }
    }

    @Override
    public void afterSqlExecute(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
        if(DtxThreadContext.current() == null || !DtxThreadContext.current().isActivate() ||
                !Transaction.TXC.equals(DtxThreadContext.current().getTransactionType()) ||
                !((ReadyDataSource)statementInformation.getConnectionInformation().getDataSource()).isEnabledTransaction()) {
            return;
        }
        if(statementInformation.getAttachment(Attachment_ForTXC) instanceof List) {
            List<Statement> list = (List<Statement>) statementInformation.getAttachment(Attachment_ForTXC);
            for(Statement statement : list) {
                if (statement instanceof Insert) {
                    try {
                        businessSqlInterceptor.afterInsert(statementInformation.getConnectionInformation().getConnection(),
                                statementInformation.getStatement(),
                                (Insert) statement);
                    } catch (SQLException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            }
        }
    }

}
