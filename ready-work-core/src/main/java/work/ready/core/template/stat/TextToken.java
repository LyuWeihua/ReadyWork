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

package work.ready.core.template.stat;

class TextToken extends Token {

	private StringBuilder text;

	public TextToken(StringBuilder value, int row) {
		super(Symbol.TEXT, row);
		this.text = value;
	}

	public void append(StringBuilder content) {
		if (content != null) {
			text.append(content);	
		}
	}

	public boolean deleteBlankTails() {
		for (int i = text.length() - 1; i >= 0; i--) {
			if (CharTable.isBlank(text.charAt(i))) {
				continue ;
			}

			if (text.charAt(i) == '\n') {
				text.delete(i+1, text.length());
				return true;
			} else {
				return false;
			}
		}

		text.setLength(0);
		return true;		
	}

	public String value() {
		return text.toString();
	}

	public StringBuilder getContent() {
		return text;
	}

	public String toString() {
		return text.toString();
	}

	public void print() {
		System.out.print("[");
		System.out.print(row);
		System.out.print(", TEXT, ");
		System.out.print(text.toString());
		System.out.println("]");
	}
}

