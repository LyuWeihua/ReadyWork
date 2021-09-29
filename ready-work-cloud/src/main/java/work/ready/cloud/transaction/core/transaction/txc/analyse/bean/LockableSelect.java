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
package work.ready.cloud.transaction.core.transaction.txc.analyse.bean;

import net.sf.jsqlparser.statement.select.Select;
import work.ready.cloud.transaction.common.lock.DtxLocks;
import work.ready.cloud.transaction.core.transaction.txc.analyse.util.SqlUtils;
import work.ready.core.tools.StrUtil;

public class LockableSelect {

    private Select select;

    public LockableSelect(Select select) {
        this.select = select;
    }

    public Select statement() {
        return select;
    }

    public boolean isxLock() {
        return StrUtil.endsWithIgnoreCase(StrUtil.trimAll(this.select.toString()),
                StrUtil.trimAll(SqlUtils.FOR_UPDATE));
    }

    public boolean issLock() {
        return StrUtil.endsWithIgnoreCase(StrUtil.trimAll(this.select.toString()),
                StrUtil.trimAll(SqlUtils.LOCK_IN_SHARE_MODE));
    }

    public int shouldLock() {
        if(isxLock()) {
            return DtxLocks.X_LOCK;
        }
        if(issLock()) {
            return DtxLocks.S_LOCK;
        }
        return 0;
    }
}
