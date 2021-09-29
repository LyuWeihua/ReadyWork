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

package work.ready.cloud.transaction.core.transaction;

import work.ready.cloud.transaction.core.controller.DtxLocalController;
import work.ready.cloud.transaction.core.propagation.PropagationState;
import work.ready.cloud.transaction.core.message.CmdExecuteService;
import work.ready.cloud.transaction.common.exception.TransactionTypeException;
import work.ready.cloud.transaction.common.message.CmdType;

import java.lang.reflect.Method;

public interface TransactionType {

    String getName();

    void init();

    boolean verifyDeclaration(Method method) throws TransactionTypeException;

    void setBusinessController(PropagationState propagationState, DtxLocalController controller);

    DtxLocalController getBusinessController(PropagationState propagationState);

    void setCmdExecuteService(CmdType cmdType, CmdExecuteService service);

    CmdExecuteService getCmdExecuteService(CmdType cmdType);

    TransactionClearanceService getClearanceService();

    TransactionResourceHandler getResourceHandler();

}
