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

import java.util.*;

class ProxyStatementCodeBlock {
    static final Map<String, String> CodeBlock = new HashMap<>();
    static final List<String> ignore = new ArrayList<>(Arrays.asList(
            "executeLargeBatch()",
            "executeLargeUpdate(String, String[])",
            "executeLargeUpdate(String, int[])",
            "executeLargeUpdate(String, int)",
            "executeLargeUpdate(String)"
    ));
    static final List<String> listenerWithReturn = new ArrayList<>(Arrays.asList(
            "addBatch(String)",
            "executeQuery(String)",
            "execute(String)",
            "execute(String, int)",
            "execute(String, int[])",
            "execute(String, String[])",
            "executeUpdate(String)",
            "executeUpdate(String, int)",
            "executeUpdate(String, int[])",
            "executeUpdate(String, String[])"
    ));

    static {
        String code;
        code =  "SQLException e = null;\n" +
                "long start = System.nanoTime();\n" +
                "try {\n" +
                "  return #performer.getResultSet();\n" +
                "} catch (SQLException sqle) {\n" +
                "  e = sqle;\n" +
                "  throw checkException(e);\n" +
                "} finally {\n" +
                "  jdbcEventListener.onAfterGetResultSet(statementInformation, System.nanoTime() - start, e);\n" +
                "}\n";
        CodeBlock.put("getResultSet()", code);

        code =  "if (statementInformation.getStatementQuery() == null) {\n" +
                "   statementInformation.setStatementQuery($2N);\n" +
                "} else {\n" +
                "   String lineSeparator = LINE_SEPARATOR;\n" +
                "   if(!statementInformation.getStatementQuery().endsWith(\";\")) {\n" +
                "       lineSeparator = \";\" + lineSeparator;\n" +
                "   }" +
                "   statementInformation.setStatementQuery(statementInformation.getStatementQuery() + lineSeparator + $2N);\n" +
                "}\n" +
                "SQLException e = null;\n" +
                "long start = System.nanoTime();\n" +
                "try {\n" +
                "  #performer.addBatch(jdbcEventListener.onBeforeAddBatch(statementInformation, $2N)$1N);\n" +
                "} catch (SQLException sqle) {\n" +
                "  e = sqle;\n" +
                "  throw checkException(e);\n" +
                "} finally {\n" +
                "  jdbcEventListener.onAfterAddBatch(statementInformation, System.nanoTime() - start, $2N, e);\n" +
                "}\n";
        CodeBlock.put("addBatch(String)", code);

        code =  "SQLException e = null;\n" +
                "long start = System.nanoTime();\n" +
                "int[] updateCounts = null;\n" +
                "try {\n" +
                "  jdbcEventListener.onBeforeExecuteBatch(statementInformation);\n" +
                "  updateCounts = #performer.executeBatch();\n" +
                "  return updateCounts;\n" +
                "} catch (SQLException sqle) {\n" +
                "  e = sqle;\n" +
                "  throw checkException(e);\n" +
                "} finally {\n" +
                "  jdbcEventListener.onAfterExecuteBatch(statementInformation, System.nanoTime() - start, updateCounts, e);\n" +
                "}\n";
        CodeBlock.put("executeBatch()", code);

        code =  "statementInformation.setStatementQuery($2N);\n" +
                "SQLException e = null;\n" +
                "long start = System.nanoTime();\n" +
                "try {\n" +
                "  return #performer.executeQuery(jdbcEventListener.onBeforeExecuteQuery(statementInformation, $2N)$1N);\n" +
                "} catch (SQLException sqle) {\n" +
                "  e = sqle;\n" +
                "  throw checkException(e);\n" +
                "} finally {\n" +
                "  jdbcEventListener.onAfterExecuteQuery(statementInformation, System.nanoTime() - start, $2N, e);\n" +
                "}\n";
        CodeBlock.put("executeQuery(String)", code);

        code =  "statementInformation.setStatementQuery($2N);\n" +
                "SQLException e = null;\n" +
                "long start = System.nanoTime();\n" +
                "try {\n" +
                "  return #performer.execute(jdbcEventListener.onBeforeExecute(statementInformation, $2N)$1N);\n" +
                "} catch (SQLException sqle) {\n" +
                "  e = sqle;\n" +
                "  throw checkException(e);\n" +
                "} finally {\n" +
                "  jdbcEventListener.onAfterExecute(statementInformation, System.nanoTime() - start, $2N, e);\n" +
                "}\n";
        CodeBlock.put("execute(String)", code);
        CodeBlock.put("execute(String, int)", code);
        CodeBlock.put("execute(String, int[])", code);
        CodeBlock.put("execute(String, String[])", code);

        code =  "statementInformation.setStatementQuery($2N);\n" +
                "SQLException e = null;\n" +
                "long start = System.nanoTime();\n" +
                "int rowCount = 0;\n" +
                "try {\n" +
                "  rowCount = #performer.executeUpdate(jdbcEventListener.onBeforeExecuteUpdate(statementInformation, $2N)$1N);\n" +
                "  return rowCount;\n" +
                "} catch (SQLException sqle) {\n" +
                "  e = sqle;\n" +
                "  throw checkException(e);\n" +
                "} finally {\n" +
                "  jdbcEventListener.onAfterExecuteUpdate(statementInformation, System.nanoTime() - start, $2N, rowCount, e);\n" +
                "}\n";
        CodeBlock.put("executeUpdate(String)", code);
        CodeBlock.put("executeUpdate(String, int)", code);
        CodeBlock.put("executeUpdate(String, int[])", code);
        CodeBlock.put("executeUpdate(String, String[])", code);

        code =  "SQLException e = null;\n" +
                "try {\n" +
                "  #performer.close();\n" +
                "} catch (SQLException sqle) {\n" +
                "  e = sqle;\n" +
                "  throw checkException(e);\n" +
                "} finally {\n" +
                "  jdbcEventListener.onAfterStatementClose(statementInformation, e);\n" +
                "}\n";
        CodeBlock.put("close()", code);
    }
}
