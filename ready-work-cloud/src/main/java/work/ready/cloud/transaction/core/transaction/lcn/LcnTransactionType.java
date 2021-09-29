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

package work.ready.cloud.transaction.core.transaction.lcn;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.common.exception.DtxNodeContextException;
import work.ready.cloud.transaction.core.context.DtxNodeContext;
import work.ready.cloud.transaction.core.controller.DtxLocalController;
import work.ready.cloud.transaction.core.transaction.TransactionClearanceService;
import work.ready.cloud.transaction.core.propagation.PropagationState;
import work.ready.cloud.transaction.core.transaction.TransactionType;
import work.ready.cloud.transaction.core.transaction.lcn.controller.*;
import work.ready.cloud.transaction.core.transaction.lcn.resource.LcnConnectionProxy;
import work.ready.cloud.transaction.core.transaction.lcn.resource.LcnTransactionResourceHandler;
import work.ready.cloud.transaction.core.message.CmdExecuteService;
import work.ready.cloud.transaction.core.transaction.TransactionResourceHandler;
import work.ready.cloud.transaction.core.transaction.lcn.controller.LcnNotifiedUnitService;
import work.ready.cloud.transaction.common.exception.TransactionTypeException;
import work.ready.cloud.transaction.common.message.CmdType;
import work.ready.core.database.annotation.Transactional;
import work.ready.core.server.Ready;
import work.ready.core.tools.StrUtil;
import work.ready.core.tools.validator.Assert;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LcnTransactionType implements TransactionType {

    public static final String name = "lcn";
    private final DtxNodeContext nodeContext;
    private final Map<PropagationState, DtxLocalController> businessControllers = new HashMap<>();
    private final Map<CmdType, CmdExecuteService> cmdExecuteServices = new HashMap<>();

    public LcnTransactionType() {
        this.nodeContext = Cloud.getTransactionManager().getNodeContext();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void init() {
        setBusinessController(PropagationState.CREATE, Ready.beanManager().get(LcnStartNewTransaction.class));
        setBusinessController(PropagationState.JOIN_OTHER_NODE, Ready.beanManager().get(LcnJoinOtherNodeTransaction.class));
        setBusinessController(PropagationState.JOIN_LOCAL_NODE, Ready.beanManager().get(LcnJoinLocalNodeTransaction.class));
        setCmdExecuteService(CmdType.notifyUnit, Ready.beanManager().get(LcnNotifiedUnitService.class));
    }

    @Override
    public boolean verifyDeclaration(Method method) throws TransactionTypeException {
        Transactional transactional = method.getAnnotation(Transactional.class);
        if(transactional == null) {
            transactional = method.getDeclaringClass().getAnnotation(Transactional.class);
        }
        if((StrUtil.isBlank(transactional.type()) && name.equals(Cloud.getTransactionManager().getConfig().getDefaultType())) || name.equalsIgnoreCase(transactional.type())) {
        } else {
            throw new TransactionTypeException("invalid LCN type of transaction declaration: " + transactional.type());
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
        return Ready.beanManager().get(LcnTransactionClearanceService.class);
    }

    @Override
    public TransactionResourceHandler getResourceHandler() {
        return Ready.beanManager().get(LcnTransactionResourceHandler.class);
    }

    public void setLcnConnection(String groupId, String dataSource, LcnConnectionProxy connectionProxy) {
        Map<String, LcnConnectionProxy> connectionMap;
        if (nodeContext.containsKey(groupId, LcnConnectionProxy.class.getName())) {
            connectionMap = nodeContext.attachment(groupId, LcnConnectionProxy.class.getName());
        } else {
            connectionMap = new HashMap<>();
            nodeContext.attach(groupId,  LcnConnectionProxy.class.getName(), connectionMap);
        }
        connectionMap.put(dataSource, connectionProxy);
    }

    public Map<String, LcnConnectionProxy> getLcnConnections(String groupId) throws DtxNodeContextException {
        if (nodeContext.containsKey(groupId, LcnConnectionProxy.class.getName())) {
            Map<String, LcnConnectionProxy> connectionMap = nodeContext.attachment(groupId, LcnConnectionProxy.class.getName());
            return Collections.unmodifiableMap(connectionMap);
        }
        return null;
    }

    public LcnConnectionProxy getLcnConnection(String groupId, String dataSource) throws DtxNodeContextException {
        if (nodeContext.containsKey(groupId, LcnConnectionProxy.class.getName())) {
            Map<String, LcnConnectionProxy> connectionMap = nodeContext.attachment(groupId, LcnConnectionProxy.class.getName());
            if(connectionMap.containsKey(dataSource)) {
                return connectionMap.get(dataSource);
            }
        }
        throw new DtxNodeContextException("no lcn connection exists");
    }
}
