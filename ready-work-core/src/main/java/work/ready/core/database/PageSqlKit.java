/**
 * Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
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

package work.ready.core.database;

public class PageSqlKit {

	private static final int start = "select ".length();

	private static final char NULL = 0;
	private static final char SIZE = 128;
	private static final char[] charTable = buildCharTable();

	private static char[] buildCharTable() {
		char[] ret = new char[SIZE];
		for (char i=0; i<SIZE; i++) {
			ret[i] = NULL;
		}

		ret['('] = '(';
		ret[')'] = ')';

		ret['f'] = 'f';
		ret['F'] = 'f';
		ret['r'] = 'r';
		ret['R'] = 'r';
		ret['o'] = 'o';
		ret['O'] = 'o';
		ret['m'] = 'm';
		ret['M'] = 'm';

		ret[' '] = ' ';
		ret['\r'] = ' ';
		ret['\n'] =  ' ';
		ret['\t'] =  ' ';
		return ret;
	}

	private static int getIndexOfFrom(String sql) {
		int parenDepth = 0;
		char c;
		for (int i = start, end = sql.length() - 5; i < end; i++) {
			c = sql.charAt(i);
			if (c >= SIZE) {
				continue ;
			}

			c = charTable[c];
			if (c == NULL) {
				continue ;
			}

			if (c == '(') {
				parenDepth++;
				continue ;
			}

			if (c == ')') {
				if (parenDepth == 0) {
					throw new RuntimeException("Can not match left paren '(' for right paren ')': " + sql);
				}
				parenDepth--;
				continue ;
			}
			if (parenDepth > 0) {
				continue ;
			}

			if (c == 'f'
				&& charTable[sql.charAt(i + 1)] == 'r'
				&& charTable[sql.charAt(i + 2)] == 'o'
				&& charTable[sql.charAt(i + 3)] == 'm') {
				c = sql.charAt(i + 4);

				if (charTable[c] == ' ' || c == '(') {		
					c = sql.charAt(i - 1);
					if (charTable[c] == ' ' || c == ')') {	
						return i;
					}
				}
			}
		}
		return -1;
	}

	public static String[] parsePageSql(String sql) {
		int index = getIndexOfFrom(sql);
		if (index == -1) {
			return null;
		}

		String[] ret = new String[2];
		ret[0] = sql.substring(0, index);
		ret[1] = sql.substring(index);
		return ret;
	}
}

