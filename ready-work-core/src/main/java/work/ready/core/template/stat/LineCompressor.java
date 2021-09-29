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

public class LineCompressor extends Compressor {

	public LineCompressor() {}

	public LineCompressor(char separator) {
		if (separator > ' ') {
			throw new IllegalArgumentException("The parameter separator must be a separator character");
		}
		this.separator = separator;
	}

	public StringBuilder compress(StringBuilder content) {
		int len = content.length();

		if (len == 1) {

			if (content.charAt(0) == '\n') {
				content.setCharAt(0, separator);
			} else if (content.charAt(0) < ' ') {
				content.setCharAt(0, ' ');
			}
			return content;
		}

		int begin = 0;
		int forward = 0;
		int lineType = 1;		
		StringBuilder result = null;
		while (forward < len) {
			if (content.charAt(forward) == '\n') {
				if (result == null) {
					result = new StringBuilder(len);		
				}
				compressLine(content, begin, forward - 1, lineType, result);

				begin = forward + 1;
				forward = begin;
				lineType = 2;	
			} else {
				forward++;
			}
		}

		if (lineType == 1) {	
			return content;
		}

		lineType = 3;			
		compressLine(content, begin, forward - 1, lineType, result);

		return result;
	}

	protected void compressLine(StringBuilder content, int start, int end, int lineType, StringBuilder result) {

		if (lineType != 1) {
			while (start <= end && content.charAt(start) <= ' ') {
				start++;
			}
		}

		if (lineType != 3) {
			while (end >= start && content.charAt(end) <= ' ') {
				end--;
			}
		}

		if (start <= end) {
			for (int i = start; i <= end; i++) {
				result.append(content.charAt(i));
			}

			if (lineType != 3) {
				result.append(separator);
			}
		}

		else {
			if (lineType == 1) {

				result.append(separator);
			}
		}
	}
}

