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

package work.ready.cloud.transaction.core.transaction.tcc;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.common.exception.TransactionException;
import work.ready.cloud.transaction.core.context.DtxNodeContext;
import work.ready.cloud.transaction.core.controller.DtxLocalController;
import work.ready.cloud.transaction.core.transaction.TransactionClearanceService;
import work.ready.cloud.transaction.core.propagation.PropagationState;
import work.ready.cloud.transaction.core.transaction.TransactionType;
import work.ready.cloud.transaction.core.transaction.tcc.controller.*;
import work.ready.cloud.transaction.core.transaction.tcc.resource.TccTransactionResourceHandler;
import work.ready.cloud.transaction.core.message.CmdExecuteService;
import work.ready.cloud.transaction.core.transaction.TransactionResourceHandler;
import work.ready.cloud.transaction.core.transaction.tcc.controller.TccNotifiedUnitService;
import work.ready.cloud.transaction.common.exception.TransactionTypeException;
import work.ready.cloud.transaction.common.message.CmdType;
import work.ready.core.database.annotation.Transactional;
import work.ready.core.server.Ready;
import work.ready.core.tools.StrUtil;
import work.ready.core.tools.define.CheckedSupplier;
import work.ready.core.tools.validator.Assert;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TccTransactionType implements TransactionType {

    public static final String name = "tcc";
    private final DtxNodeContext nodeContext;
    private final Map<PropagationState, DtxLocalController> businessControllers = new HashMap<>();
    private final Map<CmdType, CmdExecuteService> cmdExecuteServices = new HashMap<>();

    public TccTransactionType() {
        this.nodeContext = Cloud.getTransactionManager().getNodeContext();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void init() {
        setBusinessController(PropagationState.CREATE, Ready.beanManager().get(TccStartNewTransaction.class));
        setBusinessController(PropagationState.JOIN_OTHER_NODE, Ready.beanManager().get(TccJoinOtherNodeTransaction.class));
        setBusinessController(PropagationState.JOIN_LOCAL_NODE, Ready.beanManager().get(TccJoinLocalNodeTransaction.class));
        setCmdExecuteService(CmdType.notifyUnit, Ready.beanManager().get(TccNotifiedUnitService.class));
    }

    @Override
    public boolean verifyDeclaration(Method method) throws TransactionTypeException {
        Transactional transactional = method.getAnnotation(Transactional.class);
        if(transactional == null) {
            transactional = method.getDeclaringClass().getAnnotation(Transactional.class);
        }
        if((StrUtil.isBlank(transactional.type()) && name.equals(Cloud.getTransactionManager().getConfig().getDefaultType())) || name.equalsIgnoreCase(transactional.type())) {
            String cancelMethod = transactional.cancelMethod();
            String confirmMethod = transactional.confirmMethod();
            Class<?> executeClass = transactional.executeClass();
            if (StrUtil.isEmpty(transactional.cancelMethod())) {
                cancelMethod = "cancel" + StrUtil.firstCharToUpperCase(method.getName());
            }
            if (StrUtil.isEmpty(transactional.confirmMethod())) {
                confirmMethod = "confirm" + StrUtil.firstCharToUpperCase(method.getName());
            }
            if (Void.class.isAssignableFrom(executeClass)) {
                executeClass = method.getDeclaringClass();
            }
            try {
                executeClass.getDeclaredMethod(confirmMethod, method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                try {
                    executeClass.getDeclaredMethod(confirmMethod);
                } catch (NoSuchMethodException e1) {
                    throw new TransactionTypeException("confirm method '" + confirmMethod + "' for TCC transaction doesn't exist in class: " + executeClass.getCanonicalName());
                }
            }
            try {
                executeClass.getDeclaredMethod(cancelMethod, method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                try {
                    executeClass.getDeclaredMethod(cancelMethod);
                } catch (NoSuchMethodException e1) {
                    throw new TransactionTypeException("cancel method '" + cancelMethod + "' for TCC transaction doesn't exist in class: " + executeClass.getCanonicalName());
                }
            }
        } else {
            throw new TransactionTypeException("invalid TCC type of transaction declaration: " + transactional.type());
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
        return Ready.beanManager().get(TccTransactionClearanceService.class);
    }

    @Override
    public TransactionResourceHandler getResourceHandler() {
        return Ready.beanManager().get(TccTransactionResourceHandler.class);
    }

    public TccTransactionInfo tccTransactionInfo(String unitId, CheckedSupplier<TccTransactionInfo, TransactionException> supplier)
            throws TransactionException {
        String unitTransactionInfoKey = unitId + ".tcc.transaction";
        if (Objects.isNull(supplier)) {
            return nodeContext.attachment(unitTransactionInfoKey);
        }

        if (nodeContext.containsKey(unitTransactionInfoKey)) {
            return nodeContext.attachment(unitTransactionInfoKey);
        }

        TccTransactionInfo tccTransactionInfo = supplier.get();
        nodeContext.attach(unitTransactionInfoKey, tccTransactionInfo);
        return tccTransactionInfo;
    }
}
