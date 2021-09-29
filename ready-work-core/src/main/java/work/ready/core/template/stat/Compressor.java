/**
 * Copyright (c) 2011-2021, James Zhan 詹波 (jfinal@126.com).
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

public class Compressor {

	protected char separator = '\n';

	public Compressor() {}

	public Compressor(char separator) {
		if (separator > ' ') {
			throw new IllegalArgumentException("The parameter separator must be a separator character");
		}
		this.separator = separator;
	}

	public StringBuilder compress(StringBuilder content) {
		int len = content.length();
		StringBuilder ret = new StringBuilder(len);

		char ch;
		boolean hasLineFeed;
		int begin = 0;
		int forward = 0;

		while (forward < len) {

			hasLineFeed = false;
			while (forward < len) {
				ch = content.charAt(forward);
				if (ch <= ' ') {			
					if (ch == '\n') {		
						hasLineFeed = true;
					}
					forward++;
				} else {					
					break ;
				}
			}

			if (begin != forward) {
				if (hasLineFeed) {
					ret.append(separator);
				} else {
					ret.append(' ');
				}
			}

			while (forward < len) {
				ch = content.charAt(forward);
				if (ch > ' ') {
					ret.append(ch);
					forward++;
				} else {
					break ;
				}
			}

			begin = forward;
		}

		return ret;
	}
}

