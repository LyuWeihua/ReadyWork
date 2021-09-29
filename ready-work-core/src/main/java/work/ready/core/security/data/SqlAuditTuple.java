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

import net.sf.jsqlparser.statement.Statement;
import work.ready.core.database.jdbc.common.Value;

import java.util.List;
import java.util.Map;

public class SqlAuditTuple {

    private final String dataSource;
    private final Statement statement;
    private final List<Map<Integer, Value>> parameters;
    private final Map<String, Value> namedParameters;
    private boolean changed = false;

    public SqlAuditTuple(String dataSource, Statement statement, List<Map<Integer, Value>> parameters, Map<String, Value> namedParameters){
        this.dataSource = dataSource;
        this.statement = statement;
        this.parameters = parameters;
        this.namedParameters = namedParameters;
    }

    public String getDataSource() {
        return dataSource;
    }

    public Statement getStatement() {
        return statement;
    }

    public List<Map<Integer, Value>> getParameters() {
        return parameters;
    }

    public Map<String, Value> getNamedParameters() {
        return namedParameters;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }
}
