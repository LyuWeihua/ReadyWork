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

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.cluster.common.MessageBody;
import work.ready.cloud.cluster.common.MessageException;
import work.ready.cloud.transaction.common.exception.TransactionStateException;
import work.ready.cloud.transaction.common.exception.TxCoordinationException;
import work.ready.cloud.transaction.common.message.CoordinatorClient;
import work.ready.cloud.transaction.coordination.message.MessageCreator;
import work.ready.cloud.transaction.coordination.support.dto.TxException;
import work.ready.cloud.transaction.coordination.support.dto.WriteTxExceptionDTO;
import work.ready.cloud.transaction.coordination.support.dto.ExceptionInfo;
import work.ready.cloud.transaction.coordination.support.dto.ExceptionList;
import work.ready.cloud.transaction.logger.db.LogDbHelper;
import work.ready.core.database.annotation.Transactional;
import work.ready.core.database.handlers.BeanHandler;
import work.ready.core.database.handlers.ScalarHandler;
import work.ready.core.database.query.BasicRowProcessor;
import work.ready.core.database.query.AdvancedQueryRunner;
import work.ready.core.database.query.GenerousBeanProcessor;
import work.ready.core.database.query.RowProcessor;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.sql.SQLException;
import java.util.*;

public class MysqlTxExceptionService implements TxExceptionService {
    private static final Log logger = LogFactory.getLog(MysqlTxExceptionService.class);

    private final LogDbHelper dbHelper;
    private final RowProcessor processor = new BasicRowProcessor(new GenerousBeanProcessor());

    private final CoordinatorClient messageClient;
    private final TxExceptionListener txExceptionListener;
    public static final String txExceptionTable = "ready_tx_exception";

    public MysqlTxExceptionService() {
        this.dbHelper = Cloud.getTransactionManager().getLogDbHelper();
        this.messageClient = Cloud.getTransactionManager().getCoordinatorClient();
        this.txExceptionListener = Ready.beanManager().get(TxExceptionListener.class, AsyncTxExceptionListener.class);
    }

    @Override
    public void init() {
        String sql = "CREATE TABLE IF NOT EXISTS `ready_tx_exception`  (\n" +
                "  `id` bigint(20) NOT NULL AUTO_INCREMENT,\n" +
                "  `group_id` varchar(64) NULL DEFAULT NULL,\n" +
                "  `unit_id` varchar(32) NULL DEFAULT NULL,\n" +
                "  `node_name` varchar(128) NULL DEFAULT NULL,\n" +
                "  `transaction_state` tinyint(4) NULL DEFAULT NULL,\n" +
                "  `registrar` tinyint(4) NULL DEFAULT NULL,\n" +
                "  `ex_state` tinyint(4) NULL DEFAULT NULL COMMENT '0 待处理 1已处理',\n" +
                "  `remark` varchar(10240) NULL DEFAULT NULL COMMENT '备注',\n" +
                "  `create_time` datetime(0) NULL DEFAULT NULL,\n" +
                "  PRIMARY KEY (`id`) USING BTREE\n" +
                ") ";
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
        String sql = "insert into ready_tx_exception(group_id,unit_id,node_name,transaction_state,registrar,ex_state,remark,create_time) values(?,?,?,?,?,?,?,?)";
        dbHelper.update(sql, txException.getGroupId(), txException.getUnitId(), txException.getNodeName(),
                txException.getTransactionState(), txException.getRegistrar(), txException.getExState(), txException.getRemark(),
                txException.getCreatedTime());
        txExceptionListener.onException(txException);
    }

    @Override
    public int transactionState(String groupId) {
        logger.debug("transactionState > groupId: %s", groupId);
        Integer result = dbHelper.query("select transaction_state from ready_tx_exception where group_id = ?", new ScalarHandler<>(), groupId);
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
                total = dbRunner.count("select count(*) from ready_tx_exception where EX_STATE = ? and REGISTRAR = ?", exState, registrar);
                pageTxExceptions = dbRunner.findPage(TxException.class, "select * from ready_tx_exception where EX_STATE = ? and REGISTRAR = ?", page, limit, exState, registrar);
            } else if (exState != null && exState != -2) {
                total = dbRunner.count("select count(*) from ready_tx_exception where EX_STATE = ?", exState);
                pageTxExceptions = dbRunner.findPage(TxException.class, "select * from ready_tx_exception where EX_STATE = ?", page, limit, exState);
            } else if (registrar != null && registrar != -2) {
                total = dbRunner.count("select count(*) from ready_tx_exception where REGISTRAR = ?", registrar);
                pageTxExceptions = dbRunner.findPage(TxException.class, "select * from ready_tx_exception where REGISTRAR = ?", page, limit, registrar);
            } else {
                total = dbRunner.count("select count(*) from ready_tx_exception");
                pageTxExceptions = dbRunner.findPage(TxException.class, "select * from ready_tx_exception", page, limit);
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
                            dbRunner.update("update ready_tx_exception set EX_STATE = ? where ID = ?", new Object[]{(short) 1, txException.getId()});
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
        TxException exception = dbHelper.query("select * from ready_tx_exception where GROUP_ID = ? and UNIT_ID = ? limit 1", new BeanHandler<>(TxException.class), groupId, unitId);
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
            dbHelper.update("delete from ready_tx_exception where ID = ?", id);
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
