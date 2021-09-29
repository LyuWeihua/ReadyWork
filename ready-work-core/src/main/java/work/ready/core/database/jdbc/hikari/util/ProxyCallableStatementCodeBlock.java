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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ProxyCallableStatementCodeBlock {
    static final Map<String, String> CodeBlock = new HashMap<>();
    static final List<String> ignore = new ArrayList<>(ProxyPreparedStatementCodeBlock.ignore);
    static {
        String code;
        code =  "SQLException e = null;\n" +
                "try {\n" +
                "  #performer.setNull($1N); //$3N\n" +
                "} catch (SQLException sqle) {\n" +
                "  e = sqle;\n" +
                "  throw checkException(e);\n" +
                "} finally {\n" +
                "  jdbcEventListener.onAfterCallableStatementSet(statementInformation,  $2N, null, e);\n" +
                "}\n";
        CodeBlock.put("setNull(String, int)", code);
        CodeBlock.put("setNull(String, int, String)", code);

        code =  "SQLException e = null;\n" +
                "try {\n" +
                "  #performer.#method($1N);\n" +
                "} catch (SQLException sqle) {\n" +
                "  e = sqle;\n" +
                "  throw checkException(e);\n" +
                "} finally {\n" +
                "  jdbcEventListener.onAfterCallableStatementSet(statementInformation, $2N, $3N, e);\n" +
                "}\n";
        CodeBlock.put("setBoolean(String, boolean)", code);
        CodeBlock.put("setByte(String, byte)", code);
        CodeBlock.put("setShort(String, short)", code);
        CodeBlock.put("setInt(String, int)", code);
        CodeBlock.put("setLong(String, long)", code);
        CodeBlock.put("setFloat(String, float)", code);
        CodeBlock.put("setDouble(String, double)", code);
        CodeBlock.put("setBigDecimal(String, BigDecimal)", code);
        CodeBlock.put("setString(String, String)", code);
        CodeBlock.put("setBytes(String, byte[])", code);
        CodeBlock.put("setDate(String, Date)", code);
        CodeBlock.put("setTime(String, Time)", code);
        CodeBlock.put("setTimestamp(String, Timestamp)", code);
        CodeBlock.put("setAsciiStream(String, InputStream, int)", code);
        CodeBlock.put("setBinaryStream(String, InputStream, int)", code);

        CodeBlock.put("setObject(String, Object, int, int)", code);
        CodeBlock.put("setObject(String, Object, int)", code);
        CodeBlock.put("setObject(String, Object)", code);
        CodeBlock.put("setObject(String, Object, SQLType)", code);
        CodeBlock.put("setObject(String, Object, SQLType, int)", code);
        CodeBlock.put("setCharacterStream(String, Reader, int)", code);
        CodeBlock.put("setBlob(String, Blob)", code);
        CodeBlock.put("setClob(String, Clob)", code);
        CodeBlock.put("setDate(String, Date, Calendar)", code);
        CodeBlock.put("setTime(String, Time, Calendar)", code);
        CodeBlock.put("setTimestamp(String, Timestamp, Calendar)", code);
        CodeBlock.put("setURL(String, URL)", code);
        CodeBlock.put("setRowId(String, RowId)", code);
        CodeBlock.put("setNString(String, String)", code);
        CodeBlock.put("setNCharacterStream(String, Reader, long)", code);
        CodeBlock.put("setNClob(String, NClob)", code);
        CodeBlock.put("setClob(String, Reader, long)", code);
        CodeBlock.put("setBlob(String, InputStream, long)", code);
        CodeBlock.put("setNClob(String, Reader, long)", code);
        CodeBlock.put("setSQLXML(String, SQLXML)", code);

        CodeBlock.put("setAsciiStream(String, InputStream, long)", code);
        CodeBlock.put("setBinaryStream(String, InputStream, long)", code);
        CodeBlock.put("setCharacterStream(String, Reader, long)", code);
        CodeBlock.put("setAsciiStream(String, InputStream)", code);
        CodeBlock.put("setBinaryStream(String, InputStream)", code);
        CodeBlock.put("setCharacterStream(String, Reader)", code);
        CodeBlock.put("setNCharacterStream(String, Reader)", code);
        CodeBlock.put("setClob(String, Reader)", code);
        CodeBlock.put("setBlob(String, InputStream)", code);
        CodeBlock.put("setNClob(String, Reader)", code);
    }
}
