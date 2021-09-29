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

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.core.context.DtxThreadContext;
import work.ready.cloud.transaction.core.transaction.txc.TxcTransactionType;
import work.ready.cloud.transaction.core.transaction.txc.analyse.bean.*;
import work.ready.cloud.transaction.core.transaction.txc.analyse.util.SqlUtils;
import work.ready.cloud.transaction.core.transaction.txc.logger.TxcLogHelper;
import work.ready.cloud.transaction.core.transaction.txc.analyse.undo.TableRecord;
import work.ready.cloud.transaction.core.transaction.txc.analyse.undo.TableRecordList;
import work.ready.cloud.transaction.core.transaction.txc.analyse.undo.UndoLogAnalyser;
import work.ready.cloud.transaction.core.transaction.txc.exception.TxcLogicException;
import work.ready.cloud.transaction.common.exception.DtxNodeContextException;
import work.ready.cloud.transaction.common.lock.DtxLocks;
import work.ready.cloud.transaction.core.message.ReliableMessenger;
import work.ready.cloud.cluster.common.MessageException;
import work.ready.core.database.jdbc.hikari.pool.HikariProxyConnection;
import work.ready.core.server.Ready;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DefaultTxcService implements TxcService {

    private final TxcSqlExecutor txcSqlExecutor;

    private final TxcLogHelper txcLogHelper;

    private final ReliableMessenger reliableMessenger;

    private final TxcTransactionType txcTransaction;

    public DefaultTxcService() {
        this.txcSqlExecutor = Ready.beanManager().get(TxcSqlExecutor.class, DefaultTxcSqlExecutor.class);
        this.txcLogHelper = ((TxcTransactionType)Cloud.getTransactionManager().getTransactionType(TxcTransactionType.name)).getTxcLogHelper();
        this.reliableMessenger = Cloud.getTransactionManager().getMessenger();
        this.txcTransaction = (TxcTransactionType)Cloud.getTransactionManager().getTransactionType(TxcTransactionType.name);
    }

    private void lockDataRow(String groupId, String unitId, Map<String, Set<String>> lockMap, int lockType) throws TxcLogicException {
        try {
            int tryCounter = 0;
            while (tryCounter < txcTransaction.getTryRowLock()) {
                if(reliableMessenger.acquireLocks(groupId, lockMap, lockType)) {
                    txcTransaction.addTxcLockId(groupId, unitId, lockMap);
                    return;
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) { }
                tryCounter++;
            }
            throw new TxcLogicException("resource is locked! please try again later.");
        } catch (MessageException e) {
            throw new TxcLogicException("exception when contact coordinator for lock info.");
        }
    }

    private void saveUndoLog(String datasource, String groupId, String unitId, int sqlType, TableRecordList recordList) throws TxcLogicException {
        UndoLogDO undoLogDO = new UndoLogDO();
        undoLogDO.setRollbackInfo(SqlUtils.objectToBlob(recordList));
        undoLogDO.setUnitId(unitId);
        undoLogDO.setGroupId(groupId);
        undoLogDO.setDatasource(datasource);
        undoLogDO.setSqlType(sqlType);
        try {
            txcLogHelper.saveUndoLog(undoLogDO);
        } catch (SQLException e) {
            throw new TxcLogicException(e);
        }
    }

    private void resolveModifiedRecords(Connection connection, List<InvolvedRecord> modifiedRecords, int sqlType) throws TxcLogicException {
        String datasource = ((HikariProxyConnection)connection).getPoolName();
        TableRecordList tableRecords = new TableRecordList();
        Map<String, Set<String>> lockMap = new HashMap<>();

        for (InvolvedRecord modifiedRecord : modifiedRecords) {
            for (Map.Entry<String, FieldCluster> entry : modifiedRecord.getFieldClusters().entrySet()) {
                TableRecord tableRecord = new TableRecord();
                tableRecord.setTableName(entry.getKey());
                tableRecord.setFieldCluster(entry.getValue());

                tableRecords.getTableRecords().add(tableRecord);

                StringBuilder lockId = new StringBuilder();
                StringBuilder lockKey = new StringBuilder(datasource).append('.');
                tableRecord.getFieldCluster().getPrimaryKeys().forEach(pk->{
                    if(lockId.length() > 0) {
                        lockId.append(';');
                    } else {
                        lockKey.append(pk.getTableName());
                    }
                    int dot = pk.getFieldName().lastIndexOf(".");
                    lockId.append(dot > 0 ? pk.getFieldName().substring(dot + 1) : pk.getFieldName()).append('=').append(pk.getValue());
                });
                Set<String> lockIdSet = lockMap.computeIfAbsent(lockKey.toString(), k->new HashSet<>());
                lockIdSet.add(lockId.toString());
            }
        }

        if (lockMap.isEmpty()) {
            return;
        }

        String groupId = DtxThreadContext.current().getGroupId();
        String unitId = DtxThreadContext.current().getUnitId();
        
        lockDataRow(groupId, unitId, lockMap, DtxLocks.X_LOCK);
        
        saveUndoLog(datasource, groupId, unitId, sqlType, tableRecords);
    }

    @Override
    public void lockSelect(Connection connection, SelectImageParams selectImageParams, int lockType) throws TxcLogicException {
        try {
            List<InvolvedRecord> involvedRecords = txcSqlExecutor.involvedPrimaryKeys(connection, selectImageParams, lockType);
            String datasource = ((HikariProxyConnection)connection).getPoolName();
            Map<String, Set<String>> lockMap = new HashMap<>();
            for (InvolvedRecord involvedRecord : involvedRecords) {
                for (Map.Entry<String, FieldCluster> entry : involvedRecord.getFieldClusters().entrySet()) {
                    FieldCluster fieldCluster = entry.getValue();

                    StringBuilder lockId = new StringBuilder();
                    StringBuilder lockKey = new StringBuilder(datasource).append('.');
                    fieldCluster.getPrimaryKeys().forEach(pk->{
                        if(lockId.length() > 0) {
                            lockId.append(';');
                        } else {
                            lockKey.append(pk.getTableName());
                        }
                        int dot = pk.getFieldName().lastIndexOf(".");
                        lockId.append(dot > 0 ? pk.getFieldName().substring(dot + 1) : pk.getFieldName()).append('=').append(pk.getValue());
                    });
                    Set<String> lockIdSet = lockMap.computeIfAbsent(lockKey.toString(), k->new HashSet<>());
                    lockIdSet.add(lockId.toString());
                }
            }
            lockDataRow(DtxThreadContext.current().getGroupId(), DtxThreadContext.current().getUnitId(), lockMap, lockType);
        } catch (SQLException e) {
            throw new TxcLogicException(e);
        }
    }

    @Override
    public void resolveUpdateImage(Connection connection, UpdateImageParams updateImageParams) throws TxcLogicException {
        
        List<InvolvedRecord> modifiedRecords;
        try {
            modifiedRecords = txcSqlExecutor.dataAffectedByUpdate(connection, updateImageParams);
        } catch (SQLException e) {
            throw new TxcLogicException(e);
        }

        resolveModifiedRecords(connection, modifiedRecords, SqlUtils.SQL_TYPE_UPDATE);
    }

    @Override
    public void resolveDeleteImage(Connection connection, DeleteImageParams deleteImageParams) throws TxcLogicException {
        
        List<InvolvedRecord> modifiedRecords;
        try {
            modifiedRecords = txcSqlExecutor.dataAffectedByDelete(connection, deleteImageParams);
        } catch (SQLException e) {
            throw new TxcLogicException(e);
        }

        resolveModifiedRecords(connection, modifiedRecords, SqlUtils.SQL_TYPE_DELETE);
    }

    @Override
    public void resolveInsertImage(Connection connection, InsertImageParams insertImageParams) throws TxcLogicException {
        List<FieldValue> primaryKeys = new ArrayList<>();
        FieldCluster fieldCluster = new FieldCluster();
        fieldCluster.setPrimaryKeys(primaryKeys);
        ResultSet resultSet = null;
        try {
            resultSet = insertImageParams.getStatement().getGeneratedKeys();
        } catch (SQLException ignored) {
            
        }
        try {
            for (int i = 0; i < insertImageParams.getPrimaryKeyValuesList().size(); i++) {
                Map<String, Object> pks = insertImageParams.getPrimaryKeyValuesList().get(i);
                for (String key : insertImageParams.getFullyQualifiedPrimaryKeys()) {
                    FieldValue fieldValue = new FieldValue();
                    fieldValue.setFieldName(key);
                    if (pks.containsKey(key)) {
                        fieldValue.setValue(pks.get(key));
                    } else if (Objects.nonNull(resultSet)) {
                        try {
                            resultSet.next();
                            fieldValue.setValue(resultSet.getObject(1));
                        } catch (SQLException ignored) {
                        }
                    }
                    primaryKeys.add(fieldValue);
                }
            }
        } finally {
            try {
                Ready.dbManager().close(resultSet);
            } catch (SQLException ignored) {
            }
        }

        TableRecordList tableRecords = new TableRecordList();
        tableRecords.getTableRecords().add(new TableRecord(insertImageParams.getTableName(), fieldCluster));

        saveUndoLog(((HikariProxyConnection)connection).getPoolName(), DtxThreadContext.current().getGroupId(), DtxThreadContext.current().getUnitId(), SqlUtils.SQL_TYPE_INSERT, tableRecords);
    }

    @Override
    public void cleanTxc(String groupId, String unitId) throws TxcLogicException {
        
        try {
            reliableMessenger.releaseLocks(txcTransaction.findTxcLockSet(groupId, unitId));
        } catch (MessageException e) {
            throw new TxcLogicException(e);
        } catch (DtxNodeContextException e) {
            
        }

        try {
            txcLogHelper.deleteUndoLog(groupId, unitId);
        } catch (SQLException e) {
            throw new TxcLogicException(e);
        }
    }

    @Override
    public void undo(String groupId, String unitId) throws TxcLogicException {
        Map<String, List<StatementInfo>> statementInfo = new HashMap<>();
        try {
            List<UndoLogDO> undoLogDOList = txcLogHelper.getUndoLogByGroupAndUnitId(groupId, unitId);

            Collections.reverse(undoLogDOList);
            for (UndoLogDO undoLogDO : undoLogDOList) {
                TableRecordList tableRecords = SqlUtils.blobToObject(undoLogDO.getRollbackInfo());
                List<StatementInfo> statementInfoList = statementInfo.computeIfAbsent(undoLogDO.getDatasource(), k->new ArrayList<>());
                switch (undoLogDO.getSqlType()) {
                    case SqlUtils.SQL_TYPE_UPDATE:
                        tableRecords.getTableRecords().forEach(tableRecord -> statementInfoList.add(UndoLogAnalyser.update(tableRecord)));
                        break;
                    case SqlUtils.SQL_TYPE_DELETE:
                        tableRecords.getTableRecords().forEach(tableRecord -> statementInfoList.add(UndoLogAnalyser.delete(tableRecord)));
                        break;
                    case SqlUtils.SQL_TYPE_INSERT:
                        tableRecords.getTableRecords().forEach(tableRecord -> statementInfoList.add(UndoLogAnalyser.insert(tableRecord)));
                        break;
                    default:
                        break;
                }
            }
            txcSqlExecutor.applyUndoLog(statementInfo);
        } catch (SQLException e) {
            TxcLogicException exception = new TxcLogicException(e);
            exception.setAttachment(statementInfo);
            throw exception;
        }
    }
}
