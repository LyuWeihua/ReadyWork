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

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.tools.validator.Assert;

import java.util.HashMap;
import java.util.Map;

public class DtxThreadContext {

    private static final Log logger = LogFactory.getLog(DtxThreadContext.class);
    private final static ThreadLocal<DtxThreadContext> currentLocal = new InheritableThreadLocal<>();

    private String transactionType;

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    private String groupId;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    private String unitId;

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    private boolean inGroup;

    public boolean isInGroup() {
        return inGroup;
    }

    public void setInGroup(boolean inGroup) {
        this.inGroup = inGroup;
    }

    private Map<String, Object> attachment;

    public void setAttachment(String name, Object object) {
        if(attachment == null) {
            attachment = new HashMap<>();
        }
        attachment.put(name, object);
    }

    public Object getAttachment(String name) {
        return attachment == null ? null : attachment.get(name);
    }

    private int sysTransactionState = 1;

    public int getSysTransactionState() {
        return sysTransactionState;
    }

    public void setSysTransactionState(int sysTransactionState) {
        this.sysTransactionState = sysTransactionState;
    }

    private int userTransactionState = -1;

    private boolean activate;

    public boolean isActivate() {
        return activate;
    }

    public static DtxThreadContext current() {
        return currentLocal.get();
    }

    public static DtxThreadContext getOrNew() {
        if (currentLocal.get() == null) {
            currentLocal.set(new DtxThreadContext());
        }
        return currentLocal.get();
    }

    private boolean previous;

    public static void activate() {
        if (currentLocal.get() != null) {
            current().previous = current().activate;
            current().activate = true;
        }
    }

    public static void inActivate() {
        if (currentLocal.get() != null) {
            current().previous = current().activate;
            current().activate = false;
        }
    }

    public static void rollbackActivate() {
        if (currentLocal.get() != null) {
            current().activate = current().previous;
        }
    }

    public static void close() {
        logger.debug("clean thread local[%s]: %S", DtxThreadContext.class.getSimpleName(), current());
        currentLocal.remove();
    }

    public static int transactionState(int userDtxState) {
        DtxThreadContext dtxThreadContext = Assert.notNull(currentLocal.get(), "DTX can't be null.");
        return userDtxState == 1 ? dtxThreadContext.sysTransactionState : userDtxState;
    }
}
