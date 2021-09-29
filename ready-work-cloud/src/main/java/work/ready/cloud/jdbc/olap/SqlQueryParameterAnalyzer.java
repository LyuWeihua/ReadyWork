/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.olap;

import java.sql.SQLException;

final class SqlQueryParameterAnalyzer {

    private SqlQueryParameterAnalyzer() {

    }

    public static int parametersCount(String sql) throws SQLException {

        int l = sql.length();
        int params = 0;
        for (int i = 0; i < l; i++) {
            char c = sql.charAt(i);

            switch (c) {
                case '{':
                    i = skipJdbcEscape(i, sql);
                    break;
                case '\'':
                    i = skipString(i, sql, c);
                    break;
                case '"':
                    i = skipString(i, sql, c);
                    break;
                case '?':
                    params ++;
                    break;
                case '-':
                    if (i + 1 < l && sql.charAt(i + 1) == '-') {
                        i = skipLineComment(i, sql);
                    }
                    break;
                case '/':
                    if (i + 1 < l && sql.charAt(i + 1) == '*') {
                        i = skipMultiLineComment(i, sql);
                    }
                    break;
            }
        }
        return params;
    }

    private static int skipJdbcEscape(int i, String sql) throws SQLException {

        throw new SQLException("Jdbc escape sequences are not supported yet");
    }

    private static int skipLineComment(int i, String sql) {
        for (; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\n' || c == '\r') {
                return i;
            }
        }
        return i;
    }

    private static int skipMultiLineComment(int i, String sql) throws SQLException {
        int block = 0;

        for (; i < sql.length() - 1; i++) {
            char c = sql.charAt(i);
            if (c == '/' && sql.charAt(i + 1) == '*') {
                i++;
                block++;
            } else if (c == '*' && sql.charAt(i + 1) == '/') {
                i++;
                block--;
            }
            if (block == 0) {
                return i;
            }
        }
        throw new SQLException("Cannot parse given sql; unclosed /* comment");
    }

    /**
     * Skips a string starting at the current position i, returns the length of the string
     */
    private static int skipString(int i, String sql, char q) throws SQLException {
        for (i = i + 1; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == q) {
                
                if (i + 1 < sql.length() && sql.charAt(i + 1) == q) {
                    i++;
                } else {
                    return i;
                }
            }
        }
        throw new SQLException("Cannot parse given sql; unclosed string");
    }
}
