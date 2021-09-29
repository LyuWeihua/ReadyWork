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
package work.ready.cloud.transaction.core.interceptor;

import work.ready.cloud.transaction.common.BusinessCallback;

public class DtxTransactionInfo {

    private boolean isStarter;

    public boolean isStarter() {
        return isStarter;
    }

    void setStarter(boolean starter) {
        this.isStarter = starter;
    }

    private String groupId;

    public String getGroupId() {
        return groupId;
    }

    void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    private BusinessCallback businessCallback;

    public BusinessCallback getBusinessCallback() {
        return businessCallback;
    }

    void setBusinessCallback(BusinessCallback businessCallback) {
        this.businessCallback = businessCallback;
    }

    private Object[] arguments;

    public Object[] getArguments() {
        return arguments;
    }

    void setArguments(Object[] arguments) {
        this.arguments = arguments;
    }

    private TransactionInfo transactionInfo;

    public TransactionInfo getTransactionInfo() {
        return transactionInfo;
    }

    void setTransactionInfo(TransactionInfo transactionInfo) {
        this.transactionInfo = transactionInfo;
    }

}

