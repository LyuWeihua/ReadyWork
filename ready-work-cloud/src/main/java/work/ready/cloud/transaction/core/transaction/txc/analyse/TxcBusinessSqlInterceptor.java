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

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.update.Update;
import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.core.transaction.txc.TxcTransactionType;
import work.ready.cloud.transaction.core.transaction.txc.analyse.bean.*;
import work.ready.cloud.transaction.core.transaction.txc.analyse.util.SqlUtils;
import work.ready.cloud.transaction.core.transaction.txc.exception.TxcLogicException;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class TxcBusinessSqlInterceptor implements BusinessSqlInterceptor {
    private static final Log logger = LogFactory.getLog(TxcBusinessSqlInterceptor.class);
    private final TableStructAnalyser tableStructAnalyser;

    private final TxcService txcService;

    private final TxcTransactionType txcTransaction;

    public TxcBusinessSqlInterceptor() {
        this.tableStructAnalyser = Ready.beanManager().get(TableStructAnalyser.class);
        this.txcService = Ready.beanManager().get(TxcService.class, DefaultTxcService.class);
        this.txcTransaction = (TxcTransactionType)Cloud.getTransactionManager().getTransactionType(TxcTransactionType.name);
    }

    @Override
    public void beforeUpdate(Connection connection, Update update) throws SQLException {
        
        List<String> columns = new ArrayList<>(update.getColumns().size());
        List<String> primaryKeys = new ArrayList<>(3);
        List<String> tables = new ArrayList<>(1);
        update.getColumns().forEach(column -> {
            column.setTable(update.getTable());
            columns.add(column.getFullyQualifiedName().toUpperCase());
        });

        String tableName = update.getTable().getName().toUpperCase();
        tables.add(tableName);
        TableStruct tableStruct = txcTransaction.tableStruct(tableName,
                () -> tableStructAnalyser.analyse(connection, tableName));
        tableStruct.getPrimaryKeys().forEach(key -> primaryKeys.add(tableName + SqlUtils.DOT + key));

        try {
            UpdateImageParams updateImageParams = new UpdateImageParams();
            updateImageParams.setColumns(columns);
            updateImageParams.setPrimaryKeys(primaryKeys);
            updateImageParams.setTables(tables);
            updateImageParams.setWhereSql(update.getWhere() == null ? "1=1" : update.getWhere().toString());
            txcService.resolveUpdateImage(connection, updateImageParams);
        } catch (TxcLogicException e) {
            throw new SQLException(e.getMessage());
        }
    }

    @Override
    public void beforeDelete(Connection connection, Delete delete) throws SQLException {
        
        if (delete.getTables().size() == 0) {
            delete.setTables(Collections.singletonList(delete.getTable()));
        }

        List<String> tables = new ArrayList<>(delete.getTables().size());
        List<String> primaryKeys = new ArrayList<>(3);
        List<String> columns = new ArrayList<>();

        for (Table table : delete.getTables()) {
            String tableName = table.getName().toUpperCase();
            TableStruct tableStruct = txcTransaction.tableStruct(tableName,
                    () -> tableStructAnalyser.analyse(connection, tableName));
            tableStruct.getColumns().forEach((k, v) -> columns.add(tableStruct.getTableName() + SqlUtils.DOT + k));
            tableStruct.getPrimaryKeys().forEach(primaryKey -> primaryKeys.add(tableStruct.getTableName() + SqlUtils.DOT + primaryKey));
            tables.add(tableStruct.getTableName());
        }

        try {
            DeleteImageParams deleteImageParams = new DeleteImageParams();
            deleteImageParams.setColumns(columns);
            deleteImageParams.setPrimaryKeys(primaryKeys);
            deleteImageParams.setTables(tables);
            deleteImageParams.setSqlWhere(delete.getWhere().toString());
            txcService.resolveDeleteImage(connection, deleteImageParams);
        } catch (TxcLogicException e) {
            throw new SQLException(e.getMessage());
        }
    }

    @Override
    public void beforeInsert(Connection connection, Insert insert) {
    }

    @Override
    public void afterInsert(Connection connection, Statement statement, Insert insert) throws SQLException {
        insert.getTable().setName(insert.getTable().getName().toUpperCase());
        TableStruct tableStruct = txcTransaction.tableStruct(insert.getTable().getName(),
                () -> tableStructAnalyser.analyse(connection, insert.getTable().getName()));

        PrimaryKeyListVisitor primaryKeyListVisitor = new PrimaryKeyListVisitor(insert.getTable(),
                insert.getColumns(), tableStruct.getFullyQualifiedPrimaryKeys());
        insert.getItemsList().accept(primaryKeyListVisitor);

        try {
            InsertImageParams insertImageParams = new InsertImageParams();
            insertImageParams.setTableName(tableStruct.getTableName());
            insertImageParams.setStatement(statement);
            insertImageParams.setFullyQualifiedPrimaryKeys(tableStruct.getFullyQualifiedPrimaryKeys());
            insertImageParams.setPrimaryKeyValuesList(primaryKeyListVisitor.getPrimaryKeyValuesList());
            txcService.resolveInsertImage(connection, insertImageParams);
        } catch (TxcLogicException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public void beforeSelect(Connection connection, LockableSelect lockableSelect) throws SQLException {
        
        int lockType = lockableSelect.shouldLock();
        if (lockType == 0) {
            return;
        }

        if (!(lockableSelect.statement().getSelectBody() instanceof PlainSelect)) {
            throw new SQLException("complex query is not supported yet. Query: " + lockableSelect.statement().getSelectBody());
        }
        PlainSelect plainSelect = (PlainSelect) lockableSelect.statement().getSelectBody();

        if (!(plainSelect.getFromItem() instanceof Table)) {
            throw new SQLException("complex query is not supported yet. Query: " + plainSelect);
        }

        List<String> primaryKeys = new ArrayList<>();
        Table leftTable = (Table) plainSelect.getFromItem();
        leftTable.setName(leftTable.getName().toUpperCase());
        List<SelectItem> selectItems = new ArrayList<>();

        TableStruct leftTableStruct = txcTransaction.tableStruct(leftTable.getName(),
                () -> tableStructAnalyser.analyse(connection, leftTable.getName()));
        leftTableStruct.getPrimaryKeys().forEach(primaryKey -> {
            Column column = new Column(leftTable, primaryKey);
            selectItems.add(new SelectExpressionItem(column));
            primaryKeys.add(column.getFullyQualifiedName());
        });

        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                if (join.getRightItem() instanceof Table) {
                    Table rightTable = (Table)join.getRightItem();
                    rightTable.setName(rightTable.getName().toUpperCase());
                    TableStruct rightTableStruct = txcTransaction.tableStruct(rightTable.getName(),
                            () -> tableStructAnalyser.analyse(connection, rightTable.getName()));
                    rightTableStruct.getPrimaryKeys().forEach(primaryKey -> {
                        Column column = new Column(rightTable, primaryKey);
                        selectItems.add(new SelectExpressionItem(column));
                        primaryKeys.add(column.getFullyQualifiedName());
                    });
                }
            }
        }
        plainSelect.setSelectItems(selectItems);

        logger.info("select sql for lock: %s", plainSelect);
        SelectImageParams selectImageParams = new SelectImageParams();
        selectImageParams.setPrimaryKeys(primaryKeys);
        selectImageParams.setSql(plainSelect.toString());

        try {
            txcService.lockSelect(connection, selectImageParams, lockType);
        } catch (TxcLogicException e) {
            throw new SQLException(e.getMessage());
        }
    }

}
