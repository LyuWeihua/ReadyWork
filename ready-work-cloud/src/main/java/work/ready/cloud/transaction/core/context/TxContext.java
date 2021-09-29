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
package work.ready.cloud.transaction.core.context;

import work.ready.core.server.Ready;

import java.util.*;

public class TxContext {

    private Object lock = new Object();

    public Object getLock() {
        return lock;
    }

    public void setLock(Object lock) {
        this.lock = lock;
    }

    private String groupId;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    private boolean isStarter;

    public boolean isStarter() {
        return isStarter;
    }

    public void setStarter(boolean starter) {
        this.isStarter = starter;
    }

    private long createTime = Ready.currentTimeMillis();

    public long getCreateTime() {
        return createTime;
    }

    private HashMap<String, List<String>> transactionTypes = new HashMap<>(16);

    public List<String> getUnitIdByType(String type) {
        return transactionTypes.computeIfAbsent(type, (b)->new ArrayList<>());
    }

    public void addTransactionTypes(String type, String unitId) {
        getUnitIdByType(type).add(unitId);
    }
}
