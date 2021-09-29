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

package work.ready.core.security.data;

import work.ready.core.database.Model;
import work.ready.core.database.Record;

import java.util.Map;

public interface DataSecurityInspector {

    String auditClassKey = "__audit_class__";
    String auditMethodKey = "__audit_method__";
    String skipAuditKey = "__skip_audit__";

    Record outputExamine(String className, String methodName, String datasource, String table, Record output);
    void outputAudit(String className, String methodName, String datasource, String table, Record output);

    <T extends Model> T outputExamine(String className, String methodName, T output);
    <T extends Model> void outputAudit(String className, String methodName, T output);

    Map<String, Object> outputExamine(String className, String methodName, String datasource, String table, Map<String, Object> outputMap);
    void outputAudit(String className, String methodName, String datasource, String table, Map<String, Object> outputMap);

    <T extends Model> T inputExamine(String className, String methodName, T input);
    <T extends Model> void inputAudit(String className, String methodName, T input);

    Map<String, Object> inputExamine(String className, String methodName, String datasource, String table, Map<String, Object> inputMap);
    void inputAudit(String className, String methodName, String datasource, String table, Map<String, Object> inputMap);

    void sqlExamine(String className, String methodName, SqlAuditTuple tuple);
    void sqlAudit(String className, String methodName, SqlAuditTuple tuple);
}
