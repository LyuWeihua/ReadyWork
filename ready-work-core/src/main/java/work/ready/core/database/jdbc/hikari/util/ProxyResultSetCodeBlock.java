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

class ProxyResultSetCodeBlock {
    static final Map<String, String> GetMethodMap = new HashMap<>();
    static {
        GetMethodMap.put("getString", "String value = #performer.getString($1N);");
        GetMethodMap.put("getBoolean", "boolean value = #performer.getBoolean($1N);");
        GetMethodMap.put("getByte", "byte value = #performer.getByte($1N);");
        GetMethodMap.put("getShort", "short value = #performer.getShort($1N);");
        GetMethodMap.put("getInt", "int value = #performer.getInt($1N);");
        GetMethodMap.put("getLong", "long value = #performer.getLong($1N);");
        GetMethodMap.put("getFloat", "float value = #performer.getFloat($1N);");
        GetMethodMap.put("getDouble", "double value = #performer.getDouble($1N);");
        GetMethodMap.put("getBigDecimal", "BigDecimal value = #performer.getBigDecimal($1N);");
        GetMethodMap.put("getBytes", "byte[] value = #performer.getBytes($1N);");
        GetMethodMap.put("getDate", "Date value = #performer.getDate($1N);");
        GetMethodMap.put("getTime", "Time value = #performer.getTime($1N);");
        GetMethodMap.put("getTimestamp", "Timestamp value = #performer.getTimestamp($1N);");
        GetMethodMap.put("getAsciiStream", "InputStream value = #performer.getAsciiStream($1N);");
        GetMethodMap.put("getUnicodeStream", "InputStream value = #performer.getUnicodeStream($1N);");
        GetMethodMap.put("getBinaryStream", "InputStream value = #performer.getBinaryStream($1N);");
        GetMethodMap.put("getCharacterStream", "Reader value = #performer.getCharacterStream($1N);");
        GetMethodMap.put("getObject", "Object value = #performer.getObject($1N);");
        GetMethodMap.put("getObject(int, Class<T>)", "T value = #performer.getObject($1N);");
        GetMethodMap.put("getObject(String, Class<T>)", "T value = #performer.getObject($1N);");

        GetMethodMap.put("getRef", "Ref value = #performer.getRef($1N);");
        GetMethodMap.put("getBlob", "Blob value = #performer.getBlob($1N);");
        GetMethodMap.put("getClob", "Clob value = #performer.getClob($1N);");
        GetMethodMap.put("getArray", "Array value = #performer.getArray($1N);");
        GetMethodMap.put("getURL", "URL value = #performer.getURL($1N);");
        GetMethodMap.put("getRowId", "RowId value = #performer.getRowId($1N);");
        GetMethodMap.put("getNClob", "NClob value = #performer.getNClob($1N);");
        GetMethodMap.put("getSQLXML", "SQLXML value = #performer.getSQLXML($1N);");
        GetMethodMap.put("getNString", "String value = #performer.getNString($1N);");
        GetMethodMap.put("getNCharacterStream", "Reader value = #performer.getNCharacterStream($1N);");
    }
    static final Map<String, String> CodeBlock = new HashMap<>();
    static final List<String> ignore = new ArrayList<>(Arrays.asList(
            "getType()",
            "getMetaData()",
            "getWarnings()",
            "getHoldability()",
            "getFetchDirection()",
            "getFetchSize()",
            "getCursorName()",
            "getRow()",
            "getConcurrency()"
    ));

    static {
        String code;
        code =  "SQLException e = null;\n" +
                "long start = System.nanoTime();\n" +
                "boolean next = false;\n" +
                "try {\n" +
                "  jdbcEventListener.onBeforeResultSetNext(resultSetInformation);\n" +
                "  next = #performer.next();\n" +
                "  return next;\n" +
                "} catch (SQLException sqle) {\n" +
                "  e = sqle;\n" +
                "  throw checkException(e);\n" +
                "} finally {\n" +
                "  jdbcEventListener.onAfterResultSetNext(resultSetInformation, System.nanoTime() - start, next, e);\n" +
                "}\n";
        CodeBlock.put("next()", code);

        code =  "SQLException e = null;\n" +
                "try {\n" +
                "  #performer.close();\n" +
                "} catch (SQLException sqle) {\n" +
                "  e = sqle;\n" +
                "  throw checkException(e);\n" +
                "} finally {\n" +
                "  jdbcEventListener.onAfterResultSetClose(resultSetInformation, e);\n" +
                "}\n";
        CodeBlock.put("close()", code);

        code =  "SQLException e = null;\n" +
                "try {\n" +
                "  #performer_line\n" +
                "  jdbcEventListener.onAfterResultSetGet(resultSetInformation, $2N, value, null);\n" +
                "  return value;\n" +
                "} catch (SQLException sqle) {\n" +
                "  e = sqle;\n" +
                "  jdbcEventListener.onAfterResultSetGet(resultSetInformation, $2N, null, e);\n" +
                "  throw checkException(e);\n" +
                "}\n";
        CodeBlock.put("getObject(int)", code);
        CodeBlock.put("getString(int)", code);
        CodeBlock.put("getBoolean(int)", code);
        CodeBlock.put("getByte(int)", code);
        CodeBlock.put("getShort(int)", code);
        CodeBlock.put("getInt(int)", code);
        CodeBlock.put("getLong(int)", code);
        CodeBlock.put("getFloat(int)", code);
        CodeBlock.put("getDouble(int)", code);
        CodeBlock.put("getBigDecimal(int)", code);
        CodeBlock.put("getBigDecimal(int, int)", code);
        CodeBlock.put("getBytes(int)", code);
        CodeBlock.put("getDate(int)", code);
        CodeBlock.put("getTime(int)", code);
        CodeBlock.put("getTimestamp(int)", code);
        CodeBlock.put("getAsciiStream(int)", code);
        CodeBlock.put("getUnicodeStream(int)", code);
        CodeBlock.put("getBinaryStream(int)", code);
        CodeBlock.put("getCharacterStream(int)", code);

        CodeBlock.put("getObject(String)", code);
        CodeBlock.put("getString(String)", code);
        CodeBlock.put("getBoolean(String)", code);
        CodeBlock.put("getByte(String)", code);
        CodeBlock.put("getShort(String)", code);
        CodeBlock.put("getInt(String)", code);
        CodeBlock.put("getLong(String)", code);
        CodeBlock.put("getFloat(String)", code);
        CodeBlock.put("getDouble(String)", code);
        CodeBlock.put("getBigDecimal(String)", code);
        CodeBlock.put("getBigDecimal(String, int)", code);
        CodeBlock.put("getBytes(String)", code);
        CodeBlock.put("getDate(String)", code);
        CodeBlock.put("getTime(String)", code);
        CodeBlock.put("getTimestamp(String)", code);
        CodeBlock.put("getAsciiStream(String)", code);
        CodeBlock.put("getUnicodeStream(String)", code);
        CodeBlock.put("getBinaryStream(String)", code);
        CodeBlock.put("getCharacterStream(String)", code);

        CodeBlock.put("getObject(int, Map<String, Class<?>>)", code);
        CodeBlock.put("getObject(String, Map<String, Class<?>>)", code);
        CodeBlock.put("getRef(int)", code);
        CodeBlock.put("getRef(String)", code);
        CodeBlock.put("getBlob(int)", code);
        CodeBlock.put("getBlob(String)", code);
        CodeBlock.put("getClob(int)", code);
        CodeBlock.put("getClob(String)", code);
        CodeBlock.put("getArray(int)", code);
        CodeBlock.put("getArray(String)", code);

        CodeBlock.put("getDate(int, Calendar)", code);
        CodeBlock.put("getDate(String, Calendar)", code);
        CodeBlock.put("getTime(int, Calendar)", code);
        CodeBlock.put("getTime(String, Calendar)", code);
        CodeBlock.put("getTimestamp(int, Calendar)", code);
        CodeBlock.put("getTimestamp(String, Calendar)", code);
        CodeBlock.put("getURL(int)", code);
        CodeBlock.put("getURL(String)", code);
        CodeBlock.put("getRowId(int)", code);
        CodeBlock.put("getRowId(String)", code);
        CodeBlock.put("getNClob(int)", code);
        CodeBlock.put("getNClob(String)", code);
        CodeBlock.put("getSQLXML(int)", code);
        CodeBlock.put("getSQLXML(String)", code);
        CodeBlock.put("getNString(int)", code);
        CodeBlock.put("getNString(String)", code);
        CodeBlock.put("getNCharacterStream(int)", code);
        CodeBlock.put("getNCharacterStream(String)", code);
        CodeBlock.put("getObject(int, Class<T>)", code);
        CodeBlock.put("getObject(String, Class<T>)", code);
    }
}
