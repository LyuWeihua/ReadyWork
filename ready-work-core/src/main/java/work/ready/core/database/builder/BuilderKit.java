/**
 *
 * Original work Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
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
 *
 */
package work.ready.core.database.builder;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BuilderKit {

	public static Byte getByte(ResultSet rs, int i) throws SQLException {
		Object value = rs.getObject(i);
		if (value != null) {
			value = Byte.parseByte(value + "");
			return (Byte)value;
		} else {
			return null;
		}
	}

	public static Short getShort(ResultSet rs, int i) throws SQLException {
		Object value = rs.getObject(i);
		if (value != null) {
			value = Short.parseShort(value + "");
			return (Short)value;
		} else {
			return null;
		}
	}
}

