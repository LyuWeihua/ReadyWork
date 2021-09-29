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

class ProxyPreparedStatementCodeBlock {
    static final Map<String, String> CodeBlock = new HashMap<>();
    static final List<String> ignore = new ArrayList<>(Arrays.asList(
            "setMaxFieldSize(int)",
            "setMaxRows(int)",
            "setEscapeProcessing(boolean)",
            "setQueryTimeout(int)",
            "setCursorName(String)",
            "setFetchDirection(int)",
            "setFetchSize(int)",
            "setPoolable(boolean)",
            "setLargeMaxRows(long)"
    ));

    static {
        String code;
        code =  "SQLException e = null;\n" +
                "long start = System.nanoTime();\n" +
                "try {\n" +
                "  jdbcEventListener.onBeforeExecuteQuery(statementInformation);\n" +
                "  return #performer.executeQuery();\n" +
                "} catch (SQLException sqle) {\n" +
                "  e = sqle;\n" +
                "  throw checkException(e);\n" +
                "} finally {\n" +
                "  jdbcEventListener.onAfterExecuteQuery(statementInformation, System.nanoTime() - start, e);\n" +
                "}\n";
        CodeBlock.put("executeQuery()", code);

        code =  "SQLException e = null;\n" +
                "long start = System.nanoTime();\n" +
                "int rowCount = 0;\n" +
                "try {\n" +
                "  jdbcEventListener.onBeforeExecuteUpdate(statementInformation);\n" +
                "  rowCount = #performer.executeUpdate();\n" +
                "  return rowCount;\n" +
                "} catch (SQLException sqle) {\n" +
                "  e = sqle;\n" +
                "  throw checkException(e);\n" +
                "} finally {\n" +
                "  jdbcEventListener.onAfterExecuteUpdate(statementInformation, System.nanoTime() - start, rowCount, e);\n" +
                "}\n";
        CodeBlock.put("executeUpdate()", code);

        code =  "SQLException e = null;\n" +
                "long start = System.nanoTime();\n" +
                "try {\n" +
                "  jdbcEventListener.onBeforeExecute(statementInformation);\n" +
                "  return #performer.execute();\n" +
                "} catch (SQLException sqle) {\n" +
                "  e = sqle;\n" +
                "  throw checkException(e);\n" +
                "} finally {\n" +
                "  jdbcEventListener.onAfterExecute(statementInformation, System.nanoTime() - start, e);\n" +
                "}\n";
        CodeBlock.put("execute()", code);

        code =  "SQLException e = null;\n" +
                "long start = System.nanoTime();\n" +
                "try {\n" +
                "  jdbcEventListener.onBeforeAddBatch(statementInformation);\n" +
                "  #performer.addBatch();\n" +
                "} catch (SQLException sqle) {\n" +
                "  e = sqle;\n" +
                "  throw checkException(e);\n" +
                "} finally {\n" +
                "  jdbcEventListener.onAfterAddBatch(statementInformation, System.nanoTime() - start, e);\n" +
                "}\n";
        CodeBlock.put("addBatch()", code);

        code =  "SQLException e = null;\n" +
                "try {\n" +
                "  #performer.setNull($1N); //$3N\n" +
                "} catch (SQLException sqle) {\n" +
                "  e = sqle;\n" +
                "  throw checkException(e);\n" +
                "} finally {\n" +
                "  jdbcEventListener.onAfterPreparedStatementSet(statementInformation, $2N, null, e);\n" +
                "}\n";
        CodeBlock.put("setNull(int, int)", code);
        CodeBlock.put("setNull(int, int, String)", code);

        code =  "SQLException e = null;\n" +
                "try {\n" +
                "  #performer.#method($1N);\n" +
                "} catch (SQLException sqle) {\n" +
                "  e = sqle;\n" +
                "  throw checkException(e);\n" +
                "} finally {\n" +
                "  jdbcEventListener.onAfterPreparedStatementSet(statementInformation, $2N, $3N, e);\n" +
                "}\n";
        CodeBlock.put("setBoolean(int, boolean)", code);
        CodeBlock.put("setByte(int, byte)", code);
        CodeBlock.put("setShort(int, short)", code);
        CodeBlock.put("setInt(int, int)", code);
        CodeBlock.put("setLong(int, long)", code);
        CodeBlock.put("setFloat(int, float)", code);
        CodeBlock.put("setDouble(int, double)", code);
        CodeBlock.put("setBigDecimal(int, BigDecimal)", code);
        CodeBlock.put("setString(int, String)", code);
        CodeBlock.put("setBytes(int, byte[])", code);
        CodeBlock.put("setDate(int, Date)", code);
        CodeBlock.put("setTime(int, Time)", code);
        CodeBlock.put("setTimestamp(int, Timestamp)", code);
        CodeBlock.put("setAsciiStream(int, InputStream, int)", code);
        CodeBlock.put("setUnicodeStream(int, InputStream, int)", code);
        CodeBlock.put("setBinaryStream(int, InputStream, int)", code);

        CodeBlock.put("setObject(int, Object, SQLType)", code);
        CodeBlock.put("setObject(int, Object, SQLType, int)", code);
        CodeBlock.put("setObject(int, Object, int)", code);
        CodeBlock.put("setObject(int, Object)", code);
        CodeBlock.put("setObject(int, Object, int, int)", code);
        CodeBlock.put("setCharacterStream(int, Reader, int)", code);
        CodeBlock.put("setRef(int, Ref)", code);
        CodeBlock.put("setBlob(int, Blob)", code);
        CodeBlock.put("setClob(int, Clob)", code);
        CodeBlock.put("setArray(int, Array)", code);
        CodeBlock.put("setDate(int, Date, Calendar)", code);
        CodeBlock.put("setTime(int, Time, Calendar)", code);
        CodeBlock.put("setTimestamp(int, Timestamp, Calendar)", code);
        CodeBlock.put("setURL(int, URL)", code);
        CodeBlock.put("setRowId(int, RowId)", code);
        CodeBlock.put("setNString(int, String)", code);
        CodeBlock.put("setNCharacterStream(int, Reader, long)", code);
        CodeBlock.put("setNClob(int, NClob)", code);
        CodeBlock.put("setClob(int, Reader, long)", code);
        CodeBlock.put("setBlob(int, InputStream, long)", code);
        CodeBlock.put("setNClob(int, Reader, long)", code);
        CodeBlock.put("setSQLXML(int, SQLXML)", code);
        CodeBlock.put("setAsciiStream(int, InputStream, long)", code);
        CodeBlock.put("setBinaryStream(int, InputStream, long)", code);
        CodeBlock.put("setCharacterStream(int, Reader, long)", code);
        CodeBlock.put("setAsciiStream(int, InputStream)", code);
        CodeBlock.put("setBinaryStream(int, InputStream)", code);
        CodeBlock.put("setCharacterStream(int, Reader)", code);
        CodeBlock.put("setNCharacterStream(int, Reader)", code);
        CodeBlock.put("setClob(int, Reader)", code);
        CodeBlock.put("setBlob(int, InputStream)", code);
        CodeBlock.put("setNClob(int, Reader)", code);
    }
}
