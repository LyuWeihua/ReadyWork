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

package work.ready.cloud.transaction;

import work.ready.cloud.ReadyCloud;
import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.registry.base.URLParam;
import work.ready.cloud.transaction.coordination.support.service.H2TxExceptionService;
import work.ready.cloud.transaction.core.controller.*;
import work.ready.cloud.transaction.loadbalance.DtxOptimizedLoadBalancer;
import work.ready.cloud.transaction.core.transaction.TransactionClearanceService;
import work.ready.cloud.transaction.core.check.DefaultTransactionChecker;
import work.ready.cloud.transaction.core.check.TransactionChecker;
import work.ready.cloud.transaction.core.context.DtxNodeContext;
import work.ready.cloud.transaction.core.context.DefaultNodeContext;
import work.ready.cloud.transaction.core.propagation.PropagationState;
import work.ready.cloud.transaction.core.transaction.TransactionType;
import work.ready.cloud.transaction.core.transaction.lcn.LcnTransactionType;
import work.ready.cloud.transaction.core.transaction.tcc.TccTransactionType;
import work.ready.cloud.transaction.core.transaction.txc.TxcTransactionType;
import work.ready.cloud.transaction.core.corelog.H2DbHelper;
import work.ready.cloud.transaction.core.corelog.aspect.AspectLogHelper;
import work.ready.cloud.transaction.core.corelog.aspect.CoreLogger;
import work.ready.cloud.transaction.core.corelog.aspect.AsyncCoreLogger;
import work.ready.cloud.transaction.core.interceptor.DbConnectionListener;
import work.ready.cloud.transaction.core.interceptor.TransactionInterceptor;
import work.ready.cloud.transaction.core.message.service.DeleteAspectLogService;
import work.ready.cloud.transaction.core.message.service.GetAspectLogService;
import work.ready.cloud.transaction.core.message.*;
import work.ready.cloud.transaction.core.transaction.TransactionResourceHandler;
import work.ready.cloud.transaction.common.exception.TransactionTypeException;
import work.ready.cloud.transaction.coordination.support.service.TxExceptionService;
import work.ready.cloud.transaction.coordination.support.service.MysqlTxExceptionService;
import work.ready.cloud.transaction.logger.db.LogDbHelper;
import work.ready.cloud.transaction.logger.helper.H2LoggerHelper;
import work.ready.cloud.transaction.logger.helper.MysqlLoggerHelper;
import work.ready.cloud.transaction.logger.helper.TxLoggerHelper;
import work.ready.cloud.transaction.coordination.message.CoordinationCmdService;
import work.ready.cloud.transaction.coordination.message.Coordinator;
import work.ready.cloud.transaction.coordination.message.transaction.*;
import work.ready.cloud.transaction.tracing.TracingContext;
import work.ready.cloud.transaction.tracing.TracingHelper;
import work.ready.cloud.transaction.common.message.CmdType;
import work.ready.cloud.transaction.common.message.CommunicatorClient;
import work.ready.cloud.transaction.common.message.CoordinatorClient;
import work.ready.core.aop.AopComponent;
import work.ready.core.database.annotation.Transactional;
import work.ready.core.database.datasource.DataSourceConfig;
import work.ready.core.database.jdbc.event.JdbcEventListenerManager;
import work.ready.core.database.transaction.TransactionManager;
import work.ready.core.event.cloud.Event;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Constant;
import work.ready.core.server.Ready;
import work.ready.core.server.WebServer;
import work.ready.core.tools.define.Kv;
import work.ready.core.tools.validator.Assert;

import java.lang.reflect.Method;
import java.util.*;

public class DistributedTransactionManager implements TransactionManager {
    private static final Log logger = LogFactory.getLog(DistributedTransactionManager.class);
    public static final String SERVICE_ID = "DistributedTransaction";
    public static final String DTX_CORE_LOGGER_DATA_SOURCE = "DTX_CORE_LOGGER_DATA_SOURCE";
    public static final String DTX_TX_LOGGER_DATA_SOURCE = "DTX_TX_LOGGER_DATA_SOURCE";
    public static final String LOCAL_STORAGE_PATH = "dtx"; 

    private static final Map<String, TransactionType> TRANSACTION_TYPE = new HashMap<>();
    private final Map<CmdType, CmdExecuteService> cmdExecuteServices = new HashMap<>();
    private final Map<CmdType, CoordinationCmdService> coordinationCmdServices = new HashMap<>();

    private final TransactionConfig config;
    private Coordinator coordinator;
    private Communicator communicator;

    private DtxNodeContext nodeContext;

    private CommunicatorClient communicatorClient;
    private CoordinatorClient coordinatorClient;

    private TransactionClearancer clearancer;
    private TransactionController controller;
    private TransactionChecker checker;
    private ExceptionReporter exceptionReporter;
    private ReliableMessenger messenger;

    private H2DbHelper h2DbHelper;
    private AspectLogHelper aspectLogHelper;
    private CoreLogger coreLogger;

    private LogDbHelper logDbHelper;
    private TxLoggerHelper txLoggerHelper;
    
    private TxExceptionService txExceptionService;

    public DistributedTransactionManager() {
        config = ReadyCloud.getConfig().getTransaction();
    }

    @Override
    public boolean inLocalTransaction() {
        return false;
    }

    public void registerTransactionType(TransactionType transactionType) {
        Assert.notNull(transactionType, "TransactionType cannot be null");
        if(TRANSACTION_TYPE.containsKey(transactionType.getName())) {
            logger.warn("Transaction Type [%s] has been redefined.", transactionType.getName());
        }
        TRANSACTION_TYPE.put(transactionType.getName(), transactionType);
        transactionType.init();
    }

    void distributedTransactionIntegration() {
        JdbcEventListenerManager.addListener(new DbConnectionListener());

        Ready.interceptorManager().addAopComponent(
                new AopComponent()
                        .setAnnotation(Transactional.class)
                        .setInterceptorClass(TransactionInterceptor.class)
        );

        reliableMessageInit();
        Cloud.getCloudClient().addInterceptor((rb-> TracingHelper.transmit(rb::header)));
        Ready.eventManager().addListener((setter -> setter.addName(Event.WEB_SERVER_AFTER_HANDLER_INIT)), (event)->{
            WebServer server = event.getSender();
            server.getRequestHandler().addInterceptor(exchange -> TracingHelper.apply(header->exchange.getRequestHeaders().getFirst(header)));
        });
        if(config.isOptimizeLoadBalancer()) {
            ReadyCloud.getConfig().addLoadBalancer(DtxOptimizedLoadBalancer.name, DtxOptimizedLoadBalancer.class);
            Cloud.setLoadBalancer(Ready.beanManager().get(DtxOptimizedLoadBalancer.class));
        }

        Cloud.getRegistry().register(ReadyCloud.getNodeType().getType(), SERVICE_ID, null, Constant.PROTOCOL_DEFAULT, 0, Kv.by(URLParam.healthCheck.getName(), "false"));
    }

    void distributedTransactionSupport() throws Exception {
        loggerInit();

        nodeContext = Ready.beanManager().get(DtxNodeContext.class, DefaultNodeContext.class);
        messenger = Ready.beanManager().get(ReliableMessenger.class);
        exceptionReporter = Ready.beanManager().get(ExceptionReporter.class);
        checker = Ready.beanManager().get(TransactionChecker.class, DefaultTransactionChecker.class);
        clearancer = Ready.beanManager().get(TransactionClearancer.class, DefaultTransactionClearancer.class);
        checker.setTransactionClearancer(clearancer);
        controller = Ready.beanManager().get(TransactionController.class, DefaultTransactionController.class);

        registerDefaultTransactionType();
        registerDefaultCmdExecuteService();
        registerDefaultCoordinationCmdService();
    }

    private void reliableMessageInit() {
        communicatorClient = Ready.beanManager().get(CommunicatorClient.class);
        coordinatorClient = Ready.beanManager().get(CoordinatorClient.class);
        coordinator = Ready.beanManager().get(Coordinator.class);
        coordinator.listen();
        communicator = Ready.beanManager().get(Communicator.class);
        communicator.listen();
        messenger = Ready.beanManager().get(ReliableMessenger.class, DefaultMessenger.class);
    }

    private void loggerInit() throws Exception {
        
        h2DbHelper = Ready.beanManager().get(H2DbHelper.class);
        aspectLogHelper = Ready.beanManager().get(AspectLogHelper.class);
        aspectLogHelper.init();
        coreLogger = Ready.beanManager().get(CoreLogger.class, AsyncCoreLogger.class);

        String txLoggerDatasource = config.getTxLogger().getDataSource();
        String dbType = DataSourceConfig.TYPE_H2;
        boolean createH2 = true;
        if (txLoggerDatasource == null || Ready.dbManager().getConfig(txLoggerDatasource) == null) {
            if(txLoggerDatasource != null) { logger.warn("Init TxLogger error, dataSource %s is invalid. Trying to use H2 instead.", txLoggerDatasource); }
            txLoggerDatasource = DTX_TX_LOGGER_DATA_SOURCE;
        } else {
            String type = Ready.dbManager().getConfig(txLoggerDatasource).getType();
            if(List.of(DataSourceConfig.TYPE_MYSQL, DataSourceConfig.TYPE_H2, DataSourceConfig.TYPE_IGNITE).contains(type)){
                dbType = type;
                createH2 = false;
            } else {
                logger.warn("Init TxLogger error, %s type of dataSource %s is not supported. Trying to use H2 instead.", type, txLoggerDatasource);
                txLoggerDatasource = DTX_TX_LOGGER_DATA_SOURCE;
            }
        }
        logDbHelper = Ready.beanManager().get(LogDbHelper.class);
        logDbHelper.init(txLoggerDatasource, createH2);
        if(DataSourceConfig.TYPE_MYSQL.equals(dbType)) {
            
            txLoggerHelper = Ready.beanManager().get(TxLoggerHelper.class, MysqlLoggerHelper.class);
            txLoggerHelper.init();

            txExceptionService = Ready.beanManager().get(TxExceptionService.class, MysqlTxExceptionService.class);
            txExceptionService.init();
        } else if(DataSourceConfig.TYPE_IGNITE.equals(dbType) || DataSourceConfig.TYPE_H2.equals(dbType)) {
            
            txLoggerHelper = Ready.beanManager().get(TxLoggerHelper.class, H2LoggerHelper.class);
            txLoggerHelper.init();

            txExceptionService = Ready.beanManager().get(TxExceptionService.class, H2TxExceptionService.class);
            txExceptionService.init();
        }
    }

    public TransactionType getTransactionType(String type) {
        return TRANSACTION_TYPE.get(type);
    }

    private void registerDefaultTransactionType() {
        registerTransactionType(Ready.beanManager().get(LcnTransactionType.class));
        registerTransactionType(Ready.beanManager().get(TccTransactionType.class));
        registerTransactionType(Ready.beanManager().get(TxcTransactionType.class));
    }

    private void registerDefaultCmdExecuteService() {
        setCmdExecuteService(CmdType.getAspectLog, Ready.beanManager().get(GetAspectLogService.class));
        setCmdExecuteService(CmdType.deleteAspectLog, Ready.beanManager().get(DeleteAspectLogService.class));
    }

    private void registerDefaultCoordinationCmdService() {
        setCoordinationCmdService(CmdType.acquireDtxLock, Ready.beanManager().get(AcquireDtxLockService.class));
        setCoordinationCmdService(CmdType.askTransactionState, Ready.beanManager().get(AskTransactionStateService.class));
        setCoordinationCmdService(CmdType.createGroup, Ready.beanManager().get(CreateGroupService.class));
        setCoordinationCmdService(CmdType.joinGroup, Ready.beanManager().get(JoinGroupService.class));
        setCoordinationCmdService(CmdType.notifyGroup, Ready.beanManager().get(NotifyGroupService.class));
        setCoordinationCmdService(CmdType.releaseDtxLock, Ready.beanManager().get(ReleaseDtxLockService.class));
        setCoordinationCmdService(CmdType.writeCompensation, Ready.beanManager().get(WriteTxExceptionService.class));
    }

    public List<String> getSupportedTransactionType() {
        return new ArrayList<>(TRANSACTION_TYPE.keySet());
    }

    public boolean isSupportedTransactionType(String transactionType) {
        return TRANSACTION_TYPE.containsKey(transactionType);
    }

    public boolean verifyTransactionType(String transactionType, Method method) throws TransactionTypeException {
        if(TRANSACTION_TYPE.containsKey(transactionType)) {
            return TRANSACTION_TYPE.get(transactionType).verifyDeclaration(method);
        }
        return false;
    }

    public void setCmdExecuteService(CmdType cmdType, CmdExecuteService service) {
        Assert.notNull(cmdType, "CmdType can not be null");
        Assert.notNull(service, "service can not be null");
        cmdExecuteServices.put(cmdType, service);
    }

    public void setCoordinationCmdService(CmdType cmdType, CoordinationCmdService service) {
        Assert.notNull(cmdType, "CmdType can not be null");
        Assert.notNull(service, "service can not be null");
        coordinationCmdServices.put(cmdType, service);
    }

    public void rollbackGroup(String groupId) {
        nodeContext.setRollbackOnly(groupId);
    }

    public void rollbackCurrentGroup() {
        rollbackGroup(TracingContext.tracing().groupId());
    }

    public TransactionConfig getConfig() {
        return config;
    }

    public DtxNodeContext getNodeContext() {
        return nodeContext;
    }

    public CommunicatorClient getCommunicatorClient() {
        return communicatorClient;
    }

    public CoordinatorClient getCoordinatorClient() {
        return coordinatorClient;
    }

    public Coordinator getCoordinator() {
        return coordinator;
    }

    public Communicator getCommunicator() {
        return communicator;
    }

    public TransactionClearancer getClearancer() {
        return clearancer;
    }

    public TransactionController getController() {
        return controller;
    }

    public TransactionChecker getChecker() {
        return checker;
    }

    public ExceptionReporter getExceptionReporter() {
        return exceptionReporter;
    }

    public ReliableMessenger getMessenger() {
        return messenger;
    }

    public CoordinationCmdService getCoordinationCmdService(CmdType cmdType) {
        return coordinationCmdServices.get(cmdType);
    }

    public CmdExecuteService getCmdExecuteService(CmdType cmdType) {
        return cmdExecuteServices.get(cmdType);
    }

    public CmdExecuteService getCmdExecuteService(String transactionType, CmdType cmdType) {
        return transactionType != null ?
                TRANSACTION_TYPE.get(transactionType).getCmdExecuteService(cmdType) :
                cmdExecuteServices.get(cmdType);
    }

    public DtxLocalController getBusinessController(String transactionType, PropagationState propagationState) {
        return TRANSACTION_TYPE.get(transactionType).getBusinessController(propagationState);
    }

    public TransactionClearanceService getTransactionClearanceService(String transactionType) {
        return TRANSACTION_TYPE.get(transactionType).getClearanceService();
    }

    public TransactionResourceHandler getTransactionResourceHandler(String transactionType) {
        
        return TRANSACTION_TYPE.get(transactionType).getResourceHandler();
    }

    public H2DbHelper getH2DbHelper() {
        return h2DbHelper;
    }
    public AspectLogHelper getAspectLogHelper() {
        return aspectLogHelper;
    }

    public CoreLogger getCoreLogger() {
        return coreLogger;
    }

    public LogDbHelper getLogDbHelper() {
        return logDbHelper;
    }

    public TxLoggerHelper getTxLoggerHelper() {
        return txLoggerHelper;
    }

    public TxExceptionService getTxExceptionService() {
        return txExceptionService;
    }
}
