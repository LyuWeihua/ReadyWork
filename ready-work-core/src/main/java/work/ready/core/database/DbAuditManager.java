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

package work.ready.core.database;

import work.ready.core.database.jdbc.common.*;
import work.ready.core.security.data.CallerInspector;
import work.ready.core.security.data.SqlAuditTuple;
import work.ready.core.server.Ready;

import java.sql.SQLException;
import java.util.*;

import static work.ready.core.security.data.DataSecurityInspector.*;

public class DbAuditManager {
    private static final ThreadLocal<HashMap<String, String>> auditKeyMap = ThreadLocal.withInitial(HashMap::new);
    private final DatabaseManager manager;

    public DbAuditManager audit(String className, String methodName){
        auditKeyMap.get().put(auditClassKey, className);
        auditKeyMap.get().put(auditMethodKey, methodName);
        return this;
    }

    public DbAuditManager skipAudit(){
        auditKeyMap.get().put(skipAuditKey, "true");
        return this;
    }

    public DbAuditManager() {
        this.manager = Ready.dbManager();
    }

    public void performAudit(SqlAuditTuple tuple) throws SQLException {
        if(manager.getDataSecurityInspector() != null && auditKeyMap.get().remove(skipAuditKey) == null) {
            if(auditKeyMap.get().get(auditClassKey) != null && auditKeyMap.get().get(auditMethodKey) != null) {
                manager.getDataSecurityInspector().sqlExamine(auditKeyMap.get().remove(auditClassKey), auditKeyMap.get().remove(auditMethodKey), tuple);
            } else {
                CallerInspector caller = new CallerInspector();
                manager.getDataSecurityInspector().sqlExamine(caller.getCallerClassName(), caller.getCallerMethodName(), tuple);
            }
        }
    }

    public void afterSqlExecute(final StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {

    }
}
