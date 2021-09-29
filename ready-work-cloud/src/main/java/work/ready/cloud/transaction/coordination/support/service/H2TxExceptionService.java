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
package work.ready.cloud.transaction.coordination.support.service;

import work.ready.cloud.ReadyCloud;
import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.cluster.common.MessageBody;
import work.ready.cloud.cluster.common.MessageException;
import work.ready.cloud.transaction.common.exception.TransactionStateException;
import work.ready.cloud.transaction.common.exception.TxCoordinationException;
import work.ready.cloud.transaction.common.message.CoordinatorClient;
import work.ready.cloud.transaction.coordination.message.MessageCreator;
import work.ready.cloud.transaction.coordination.support.dto.ExceptionInfo;
import work.ready.cloud.transaction.coordination.support.dto.ExceptionList;
import work.ready.cloud.transaction.coordination.support.dto.TxException;
import work.ready.cloud.transaction.coordination.support.dto.WriteTxExceptionDTO;
import work.ready.cloud.transaction.logger.db.LogDbHelper;
import work.ready.core.database.annotation.Transactional;
import work.ready.core.database.handlers.BeanHandler;
import work.ready.core.database.handlers.ScalarHandler;
import work.ready.core.database.query.AdvancedQueryRunner;
import work.ready.core.database.query.BasicRowProcessor;
import work.ready.core.database.query.GenerousBeanProcessor;
import work.ready.core.database.query.RowProcessor;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class H2TxExceptionService implements TxExceptionService {
    private static final Log logger = LogFactory.getLog(H2TxExceptionService.class);

    private final LogDbHelper dbHelper;
    private final RowProcessor processor = new BasicRowProcessor(new GenerousBeanProcessor());

    private final CoordinatorClient messageClient;
    private final TxExceptionListener txExceptionListener;
    public static final String txExceptionTable = "READY_TX_EXCEPTION";

    public H2TxExceptionService() {
        this.dbHelper = Cloud.getTransactionManager().getLogDbHelper();
        this.messageClient = Cloud.getTransactionManager().getCoordinatorClient();
        this.txExceptionListener = Ready.beanManager().get(TxExceptionListener.class, AsyncTxExceptionListener.class);
    }

    @Override
    public void init() {
        String sql = "CREATE TABLE IF NOT EXISTS READY_TX_EXCEPTION (\n" +
                "  ID BIGINT NOT NULL,\n" +
                "  GROUP_ID VARCHAR(64) NULL,\n" +
                "  UNIT_ID VARCHAR(32) NULL,\n" +
                "  NODE_NAME VARCHAR(128) NULL,\n" +
                "  TRANSACTION_STATE TINYINT NULL,\n" +
                "  REGISTRAR TINYINT NULL,\n" +
                "  EX_STATE TINYINT NULL COMMENT '0 待处理 1已处理',\n" +
                "  REMARK VARCHAR(10240) NULL COMMENT '备注',\n" +
                "  CREATE_TIME VARCHAR(30) NULL,\n" +
                "  PRIMARY KEY (ID) \n" +
                ") ";
        if(ReadyCloud.getConfig().getTransaction().getTxLogger().isIgnitePersistence()) {
            sql += " WITH \"mode=REPLICATED_PERSISTENCE,cache_name=TX_EXCEPTION\";";
        }
        dbHelper.update(sql);
        logger.info("TxException log table prepared");
    }

    @Override
    public void writeTxException(WriteTxExceptionDTO writeTxException) {
        logger.info("write tx_exception. %s", writeTxException);
        TxException txException = new TxException();
        txException.setCreatedTime(Ready.now());
        txException.setGroupId(writeTxException.getGroupId());
        txException.setTransactionState(writeTxException.getTransactionState());
        txException.setUnitId(writeTxException.getUnitId());
        txException.setRegistrar(writeTxException.getRegistrar());
        txException.setNodeName(writeTxException.getNodeName());
        txException.setExState((short) 0);
        txException.setRemark(writeTxException.getRemark());
        String sql = "INSERT INTO READY_TX_EXCEPTION(ID,GROUP_ID,UNIT_ID,NODE_NAME,TRANSACTION_STATE,REGISTRAR,EX_STATE,REMARK,CREATE_TIME) values(?,?,?,?,?,?,?,?)";
        dbHelper.update(sql, Ready.getId(), txException.getGroupId(), txException.getUnitId(), txException.getNodeName(),
                txException.getTransactionState(), txException.getRegistrar(), txException.getExState(), txException.getRemark(),
                txException.getCreatedTime());
        txExceptionListener.onException(txException);
    }

    @Override
    public int transactionState(String groupId) {
        logger.debug("transactionState > groupId: %s", groupId);
        Integer result = dbHelper.query("SELECT TRANSACTION_STATE FROM READY_TX_EXCEPTION WHERE GROUP_ID = ?", new ScalarHandler<>(), groupId);
        if(result == null) {
            return -1;
        }
        return result;
    }

    @Override
    @Transactional
    public ExceptionList exceptionList(Integer page, Integer limit, Integer exState, String keyword, Integer registrar) {
        if (Objects.isNull(page) || page <= 0) {
            page = 1;
        }
        if (Objects.isNull(limit) || limit < 1) {
            limit = 10;
        }
        AdvancedQueryRunner dbRunner = new AdvancedQueryRunner(dbHelper.getQueryRunner());
        List<TxException> pageTxExceptions;
        long total;
        try {
            if (exState != null && exState != -2 && registrar != null && registrar != -2) {
                total = dbRunner.count("SELECT COUNT(*) FROM READY_TX_EXCEPTION WHERE EX_STATE = ? AND REGISTRAR = ?", exState, registrar);
                pageTxExceptions = dbRunner.findPage(TxException.class, "SELECT * FROM READY_TX_EXCEPTION WHERE EX_STATE = ? AND REGISTRAR = ?", page, limit, exState, registrar);
            } else if (exState != null && exState != -2) {
                total = dbRunner.count("SELECT COUNT(*) FROM READY_TX_EXCEPTION WHERE EX_STATE = ?", exState);
                pageTxExceptions = dbRunner.findPage(TxException.class, "SELECT * FROM READY_TX_EXCEPTION WHERE EX_STATE = ?", page, limit, exState);
            } else if (registrar != null && registrar != -2) {
                total = dbRunner.count("SELECT COUNT(*) FROM READY_TX_EXCEPTION WHERE REGISTRAR = ?", registrar);
                pageTxExceptions = dbRunner.findPage(TxException.class, "SELECT * FROM READY_TX_EXCEPTION WHERE REGISTRAR = ?", page, limit, registrar);
            } else {
                total = dbRunner.count("SELECT COUNT(*) FROM READY_TX_EXCEPTION");
                pageTxExceptions = dbRunner.findPage(TxException.class, "SELECT * FROM READY_TX_EXCEPTION", page, limit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("query exception list exception: " , e);
        }
        List<ExceptionInfo> exceptionInfoList = new ArrayList<>(pageTxExceptions.size());
        for (TxException txException : pageTxExceptions) {
            ExceptionInfo exceptionInfo = new ExceptionInfo();
            exceptionInfo.setId(txException.getId());
            exceptionInfo.setGroupId(txException.getGroupId());
            exceptionInfo.setUnitId(txException.getUnitId());
            exceptionInfo.setNodeName(txException.getNodeName());
            exceptionInfo.setRegistrar(txException.getRegistrar());
            exceptionInfo.setExState(txException.getExState());
            exceptionInfo.setCreateTime(txException.getCreatedTime());
            exceptionInfo.setRemark(txException.getRemark());

            if (txException.getExState() != 1) {
                try {
                    MessageBody transactionInfo = getTransactionInfo(exceptionInfo.getGroupId(), exceptionInfo.getUnitId());
                    exceptionInfo.setTransactionInfo(transactionInfo);
                } catch (TransactionStateException e) {
                    if (e.getCode() == TransactionStateException.NON_ASPECT) {
                        
                        try {
                            dbRunner.update("UPDATE READY_TX_EXCEPTION SET EX_STATE = ? WHERE ID = ?", new Object[]{(short) 1, txException.getId()});
                        } catch (SQLException e1) {
                            throw new RuntimeException("update EX_STATE exception: " , e1);
                        }
                        exceptionInfo.setExState((short) 1);
                    }
                }
            }
            exceptionInfoList.add(exceptionInfo);
        }
        ExceptionList exceptionList = new ExceptionList();
        exceptionList.setTotal(total);
        exceptionList.setExceptions(exceptionInfoList);
        return exceptionList;
    }

    @Override
    public MessageBody getTransactionInfo(String groupId, String unitId) throws TransactionStateException {
        TxException exception = dbHelper.query("SELECT * FROM READY_TX_EXCEPTION WHERE GROUP_ID = ? AND UNIT_ID = ? LIMIT 1", new BeanHandler<>(TxException.class), groupId, unitId);
        if (Objects.isNull(exception)) {
            throw new TransactionStateException("non exists aspect log", TransactionStateException.NON_ASPECT);
        }
        UUID nodeId = messageClient.getNodeId(exception.getNodeName());
        if (nodeId == null) {
            throw new TransactionStateException("non mod found", TransactionStateException.NON_MOD);
        }
        try {
            MessageBody messageBody = messageClient.request(nodeId, MessageCreator.getAspectLog(groupId, unitId), 5000);
            if (messageBody.isStateOk()) {
                return messageBody;
            }
            throw new TransactionStateException("non exists aspect log", TransactionStateException.NON_ASPECT);
        } catch (MessageException e) {
            throw new TransactionStateException(e, TransactionStateException.RPC_ERR);
        }
    }

    @Override
    public void deleteExceptions(List<Long> ids) {
        for (Long id : ids) {
            dbHelper.update("DELETE FROM READY_TX_EXCEPTION WHERE ID = ?", id);
        }
    }

    @Override
    public void deleteTransactionInfo(String groupId, String unitId, String nodeName) throws TxCoordinationException {
        UUID nodeId = messageClient.getNodeId(nodeName);
        if (nodeId == null) {
            throw new TxCoordinationException("node " + nodeName + " doesn't exist");
        }
        try {
            MessageBody messageBody = messageClient.request(nodeId, MessageCreator.deleteAspectLog(groupId, unitId), 5000);
            if (!messageBody.isStateOk()) {
                
            }
        } catch (MessageException e) {
            throw new TxCoordinationException(e.getMessage());
        }
    }
}
