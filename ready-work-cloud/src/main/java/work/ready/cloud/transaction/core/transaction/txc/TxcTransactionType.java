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

package work.ready.cloud.transaction.core.transaction.txc;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.common.exception.DtxNodeContextException;
import work.ready.cloud.transaction.core.context.DtxNodeContext;
import work.ready.cloud.transaction.core.controller.DtxLocalController;
import work.ready.cloud.transaction.core.transaction.TransactionClearanceService;
import work.ready.cloud.transaction.core.propagation.PropagationState;
import work.ready.cloud.transaction.core.transaction.TransactionType;
import work.ready.cloud.transaction.core.transaction.txc.analyse.PrimaryKeysProvider;
import work.ready.cloud.transaction.core.transaction.txc.analyse.bean.TableStruct;
import work.ready.cloud.transaction.core.transaction.txc.controller.*;
import work.ready.cloud.transaction.core.transaction.txc.resource.TxcJdbcEventListener;
import work.ready.cloud.transaction.core.transaction.txc.resource.TxcTransactionResourceHandler;
import work.ready.cloud.transaction.core.transaction.txc.logger.TxcLogHelper;
import work.ready.cloud.transaction.core.message.CmdExecuteService;
import work.ready.cloud.transaction.core.transaction.TransactionResourceHandler;
import work.ready.cloud.transaction.core.transaction.txc.controller.TxcNotifiedUnitService;
import work.ready.cloud.transaction.common.exception.TransactionTypeException;
import work.ready.cloud.transaction.common.message.CmdType;
import work.ready.core.database.annotation.Transactional;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.StrUtil;
import work.ready.core.tools.define.CheckedSupplier;
import work.ready.core.tools.validator.Assert;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class TxcTransactionType implements TransactionType {
    private static final Log logger = LogFactory.getLog(TxcTransactionType.class);
    public static final String name = "txc";
    private final DtxNodeContext nodeContext;
    private final Map<PropagationState, DtxLocalController> businessControllers = new HashMap<>();
    private final Map<CmdType, CmdExecuteService> cmdExecuteServices = new HashMap<>();
    private final List<PrimaryKeysProvider> primaryKeysProviders = new ArrayList<>();
    private final TxcLogHelper txcLogHelper;
    private int tryRowLock = 3;

    public TxcTransactionType() {
        nodeContext = Cloud.getTransactionManager().getNodeContext();
        
        txcLogHelper = Ready.beanManager().get(TxcLogHelper.class);
        txcLogHelper.init();
    }

    public int getTryRowLock() {
        return tryRowLock;
    }

    public void setTryRowLock(int tryRowLock) {
        this.tryRowLock = tryRowLock;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void init() {
        setBusinessController(PropagationState.CREATE, Ready.beanManager().get(TxcStartNewTransaction.class));
        setBusinessController(PropagationState.JOIN_OTHER_NODE, Ready.beanManager().get(TxcJoinOtherNodeTransaction.class));
        setBusinessController(PropagationState.JOIN_LOCAL_NODE, Ready.beanManager().get(TxcJoinLocalNodeTransaction.class));
        setCmdExecuteService(CmdType.notifyUnit, Ready.beanManager().get(TxcNotifiedUnitService.class));
        Ready.dbManager().addSqlExecuteHandlers(Ready.beanManager().get(TxcJdbcEventListener.class));
    }

    @Override
    public boolean verifyDeclaration(Method method) throws TransactionTypeException {
        Transactional transactional = method.getAnnotation(Transactional.class);
        if(transactional == null) {
            transactional = method.getDeclaringClass().getAnnotation(Transactional.class);
        }
        if((StrUtil.isBlank(transactional.type()) && name.equals(Cloud.getTransactionManager().getConfig().getDefaultType())) || name.equalsIgnoreCase(transactional.type())) {
        } else {
            throw new TransactionTypeException("invalid TXC type of transaction declaration: " + transactional.type());
        }
        return true;
    }

    @Override
    public void setBusinessController(PropagationState propagationState, DtxLocalController controller) {
        Assert.notNull(propagationState, "PropagationState can not be null");
        Assert.notNull(propagationState, "business controller can not be null");
        businessControllers.put(propagationState, controller);
    }

    @Override
    public DtxLocalController getBusinessController(PropagationState propagationState) {
        return businessControllers.get(propagationState);
    }

    @Override
    public void setCmdExecuteService(CmdType cmdType, CmdExecuteService service) {
        Assert.notNull(cmdType, "CmdType can not be null");
        Assert.notNull(service, "service can not be null");
        cmdExecuteServices.put(cmdType, service);
    }

    @Override
    public CmdExecuteService getCmdExecuteService(CmdType cmdType) {
        return cmdExecuteServices.get(cmdType);
    }

    @Override
    public TransactionClearanceService getClearanceService() {
        return Ready.beanManager().get(TxcTransactionClearanceService.class);
    }

    @Override
    public TransactionResourceHandler getResourceHandler() {
        return Ready.beanManager().get(TxcTransactionResourceHandler.class);
    }

    public TxcLogHelper getTxcLogHelper() {
        return txcLogHelper;
    }

    public void addPrimaryKeysProvider(PrimaryKeysProvider primaryKeysProvider) {
        primaryKeysProviders.add(primaryKeysProvider);
    }

    public void addPrimaryKeys(String table, String... primaryKey) {
        addPrimaryKeys(table, Arrays.asList(primaryKey));
    }

    public void addPrimaryKeys(String table, List<String> primaryKeys) {
        primaryKeysProviders.forEach(provider -> {
            List<String> keys = provider.provide().computeIfAbsent(table, tn->new ArrayList<>());
            keys.addAll(primaryKeys);
        });
    }

    @SuppressWarnings("unchecked")
    public void addTxcLockId(String groupId, String unitId, Map<String, Set<String>> lockMap) {
        String lockKey = unitId + ".txc.lock";
        if (nodeContext.containsKey(groupId, lockKey)) {
            lockMap.forEach((key,val)->{
                ((Map<String, Set<String>>) nodeContext.attachment(groupId, lockKey)).computeIfAbsent(key, k-> new HashSet<>()).addAll(val);
            });
            return;
        }
        Map<String, Set<String>> lockList = new HashMap<>(lockMap);
        nodeContext.attach(groupId, lockKey, lockList);
    }

    public Map<String, Set<String>> findTxcLockSet(String groupId, String unitId) throws DtxNodeContextException {
        String lockKey = unitId + ".txc.lock";
        if (nodeContext.containsKey(groupId, lockKey)) {
            return nodeContext.attachment(groupId, lockKey);
        }
        throw new DtxNodeContextException("non exists lock id.");
    }

    public TableStruct tableStruct(String table, CheckedSupplier<TableStruct, SQLException> structSupplier) throws SQLException {
        String tableStructKey = table + ".struct";
        if (nodeContext.containsKey(tableStructKey)) {
            logger.debug("cache hit! table %s's struct.", table);
            return nodeContext.attachment(tableStructKey);
        }
        TableStruct tableStruct = structSupplier.get();
        if (!primaryKeysProviders.isEmpty()) {
            primaryKeysProviders.forEach(primaryKeysProvider -> {
                List<String> list = primaryKeysProvider.provide().get(table);
                if (Objects.nonNull(list)) {
                    List<String> primaryKes = tableStruct.getPrimaryKeys();
                    primaryKes.addAll(list.stream()
                            .map(String::toUpperCase)
                            .filter(key -> !primaryKes.contains(key))
                            .filter(key -> tableStruct.getColumns().keySet().contains(key)).collect(Collectors.toList()));
                    tableStruct.setPrimaryKeys(primaryKes);
                }
            });
        }
        if(tableStruct.getPrimaryKeys().isEmpty()) {
            throw new SQLException("table '" + table + "' doesn't have a primary key, a table must have a primary key or provide it manually");
        }
        nodeContext.attach(tableStructKey, tableStruct);
        return tableStruct;
    }
}
