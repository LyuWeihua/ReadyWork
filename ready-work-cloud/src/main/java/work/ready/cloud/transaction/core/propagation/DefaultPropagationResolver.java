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
package work.ready.cloud.transaction.core.propagation;

import work.ready.cloud.transaction.core.context.DtxThreadContext;
import work.ready.cloud.transaction.core.interceptor.DtxTransactionInfo;
import work.ready.cloud.transaction.common.exception.TransactionException;
import work.ready.core.database.transaction.Propagation;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

public class DefaultPropagationResolver implements PropagationResolver {

    private static final Log logger = LogFactory.getLog(DefaultPropagationResolver.class);

    @Override
    public PropagationState resolvePropagationState(DtxTransactionInfo dtxTransactionInfo) throws TransactionException {
        
        if (DtxThreadContext.current().isInGroup()) {
            
            if(Propagation.NEVER.equals(dtxTransactionInfo.getTransactionInfo().getTransactionPropagation())) {
                throw new RuntimeException("transactional logic with NEVER propagation.");
            }
            
            logger.info("JOIN LOCAL NODE'S TRANSACTION!");
            return PropagationState.JOIN_LOCAL_NODE;
        }

        if (dtxTransactionInfo.isStarter()) {

            if (Propagation.SUPPORTS.equals(dtxTransactionInfo.getTransactionInfo().getTransactionPropagation())
                    || Propagation.NEVER.equals(dtxTransactionInfo.getTransactionInfo().getTransactionPropagation())) {
                logger.info("NONE TRANSACTION!");
                return PropagationState.NONE;
            }
            
            if(Propagation.MANDATORY.equals(dtxTransactionInfo.getTransactionInfo().getTransactionPropagation())) {
                throw new RuntimeException("transactional logic with MANDATORY propagation, but there is no existing transaction.");
            }
            
            logger.info("CREATE GROUP, START A TRANSACTION!");
            return PropagationState.CREATE;
        }

        if(Propagation.NEVER.equals(dtxTransactionInfo.getTransactionInfo().getTransactionPropagation())) {
            throw new RuntimeException("transactional logic with NEVER propagation.");
        }

        logger.info("JOIN OTHER NODE'S TRANSACTION!");
        return PropagationState.JOIN_OTHER_NODE;
    }
}
