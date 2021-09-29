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

import java.sql.SQLException;

public interface SqlDebugger {

    default void addListener(SqlDebugger listener){
        throw new RuntimeException("SqlDebugger.addListener not implemented yet");
    }

    default void removeListener(SqlDebugger listener){
        throw new RuntimeException("SqlDebugger.removeListener not implemented yet");
    }

    void beforeAudit(String datasource, String sql);

    void beforeExecute(String datasource, String sql);

    void afterExecute(long timeElapsedNanos, SQLException e);
}
