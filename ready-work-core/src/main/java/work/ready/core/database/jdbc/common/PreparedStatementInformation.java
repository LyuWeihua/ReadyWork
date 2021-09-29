/**
 *
 * Original work Copyright (c) 2002 P6Spy
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package work.ready.core.database.jdbc.common;

import work.ready.core.server.Ready;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreparedStatementInformation extends StatementInformation implements Loggable {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private final List<Map<Integer, Value>> parameterValues = new ArrayList<>();

    public PreparedStatementInformation(final ConnectionInformation connectionInformation, String query) {
        super(connectionInformation);
        setStatementQuery(query);
    }

    @Override
    public String getSqlWithValues() {
        final String statementQuery = getStatementQuery();
        if(Ready.getBootstrapConfig().isDevMode()) {
            int count = countSqlPlaceholders(statementQuery);
            for(var map : parameterValues) {
                if (count != map.size()) {
                    throw new RuntimeException("placeholders of the sql query doesn't match the number of parameters. sql: " + statementQuery);
                }
            }
        }
        if(parameterValues.size() > 0) {
            return replaceSqlPlaceholders(statementQuery, parameterValues);
        } else {
            return statementQuery;
        }
    }

    private static final Pattern doubleQuote = Pattern.compile("\\\\\"|\"(?:\\\\\"|[^\"])*\"|(\\+)");
    private static final Pattern singleQuote = Pattern.compile("\\\\'|'(?:\\\\'|[^'])*'|(\\+)");
    private int countSqlPlaceholders(String sql) {
        String strWithoutQuotes = sql;
        if(sql.indexOf('"')>0){
            strWithoutQuotes = doubleQuote.matcher(strWithoutQuotes).replaceAll("");
        }
        if(sql.indexOf('\'')>0){
            strWithoutQuotes = singleQuote.matcher(strWithoutQuotes).replaceAll("");
        }
        int count = 0;
        for (int i = 0; i < strWithoutQuotes.length(); i++) {
            if ('?' == strWithoutQuotes.charAt(i)) {
                count++;
            }
        }
        return count;
    }

    private String replaceSqlPlaceholders(String sql, List<Map<Integer, Value>> values) {
        Map<String, String> replacementMap = new HashMap<>();
        String strWithoutQuotes = sql;
        int i = 0;
        if(sql.indexOf('"')>0) {
            StringBuffer sb = new StringBuffer();
            Matcher m = doubleQuote.matcher(strWithoutQuotes);
            while (m.find()) {
                replacementMap.put("#" + i + "#", m.group());
                m.appendReplacement(sb, "#" + i + "#");
                i++;
            }
            strWithoutQuotes = m.appendTail(sb).toString();
        }
        if(sql.indexOf('\'')>0) {
            StringBuffer sb = new StringBuffer();
            Matcher m = singleQuote.matcher(strWithoutQuotes);
            while (m.find()) {
                replacementMap.put("#" + i + "#", m.group());
                m.appendReplacement(sb, "#" + i + "#");
                i++;
            }
            strWithoutQuotes = m.appendTail(sb).toString();
        }

        final StringBuilder sb = new StringBuilder();
        for (var valueMap : values) {
            int currentParameter = 0;
            if (sb.length() > 0) {
                sb.append(";").append(LINE_SEPARATOR);
            }
            for (int pos = 0; pos < strWithoutQuotes.length(); pos++) {
                char character = strWithoutQuotes.charAt(pos);
                if (character == '?' && currentParameter <= valueMap.size()) {
                    
                    Value value = valueMap.get(currentParameter);
                    sb.append(value != null ? value.toString() : new Value().toString());
                    currentParameter++;
                } else {
                    sb.append(character);
                }
            }
        }
        sql = sb.toString();
        if(replacementMap.size() > 0) {
            for (var entry : replacementMap.entrySet()) {
                sql = sql.replace(entry.getKey(), entry.getValue());
            }
        }
        return sql;
    }

    public void setParameterValue(final int position, final Object value) {
        Map<Integer, Value> valueMap;
        if(position == 1) {
            valueMap = new HashMap<>();
            parameterValues.add(valueMap);
        } else {
            valueMap = parameterValues.get(parameterValues.size() - 1);
        }
        valueMap.put(position - 1, new Value(value));
    }

    public List<Map<Integer, Value>> getParameterValues() {
        return parameterValues;
    }

    public void clearParameters() {
        parameterValues.clear();
    }

    public int countParameters() {
        int paramCount = 0;
        for(var map : parameterValues) {
            paramCount = paramCount + map.size();
        }
        return paramCount;
    }

    public int countParameterRows() {
        return parameterValues.size();
    }

}
