/**
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
 */
package work.ready.core.database.jdbc.hikari.util;

import java.util.HashMap;
import java.util.Map;

class ProxyConnectionCodeBlock {
    static final Map<String, String> CodeBlock = new HashMap<>();
    static {
        String code;
        code =  "SQLException e = null;\n" +
                "long start = System.nanoTime();\n" +
                "try {\n" +
                "    #performer.close();\n" +
                "} catch (SQLException sqle) {\n" +
                "    e = sqle;\n" +
                "    throw checkException(e);\n" +
                "} finally {\n" +
                "    connectionInformation.setTimeToCloseConnectionNs(System.nanoTime() - start);\n" +
                "    jdbcEventListener.onAfterConnectionClose(connectionInformation, e);\n" +
                "}\n";
        CodeBlock.put("close()", code);

        code =  "SQLException e = null;\n" +
                "long start = System.nanoTime();\n" +
                "try {\n" +
                "    jdbcEventListener.onBeforeCommit(connectionInformation);\n" +
                "    #performer.commit();\n" +
                "} catch (SQLException sqle) {\n" +
                "    e = sqle;\n" +
                "    throw checkException(e);\n" +
                "} finally {\n" +
                "    jdbcEventListener.onAfterCommit(connectionInformation, System.nanoTime() - start, e);\n" +
                "}\n";
        CodeBlock.put("commit()", code);

        code =  "SQLException e = null;\n" +
                "long start = System.nanoTime();\n" +
                "try {\n" +
                "    jdbcEventListener.onBeforeRollback(connectionInformation);\n" +
                "    #performer.rollback();\n" +
                "} catch (SQLException sqle) {\n" +
                "    e = sqle;\n" +
                "    throw checkException(e);\n" +
                "} finally {\n" +
                "    jdbcEventListener.onAfterRollback(connectionInformation, System.nanoTime() - start, e);\n" +
                "}\n";
        CodeBlock.put("rollback()", code);

        code =  "SQLException e = null;\n" +
                "long start = System.nanoTime();\n" +
                "try {\n" +
                "    jdbcEventListener.onBeforeRollback(connectionInformation);\n" +
                "    #performer.rollback($1N); //$2N\n" +
                "} catch (SQLException sqle) {\n" +
                "    e = sqle;\n" +
                "    throw checkException(e);\n" +
                "} finally {\n" +
                "    jdbcEventListener.onAfterRollback(connectionInformation, System.nanoTime() - start, e);\n" +
                "}\n";
        CodeBlock.put("rollback(Savepoint)", code);

        code =  "SQLException e = null;\n" +
                "boolean oldAutoCommit = delegate.getAutoCommit();\n" +
                "try {\n" +
                "    jdbcEventListener.onBeforeSetAutoCommit(connectionInformation, $2N, oldAutoCommit);\n" +
                "    #performer.setAutoCommit($1N);\n" +
                "} catch (SQLException sqle){\n" +
                "    e = sqle;\n" +
                "    throw checkException(e);\n" +
                "} finally {\n" +
                "    jdbcEventListener.onAfterSetAutoCommit(connectionInformation, $2N, oldAutoCommit, e);\n" +
                "}\n";
        CodeBlock.put("setAutoCommit(boolean)", code);
    }
}
