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

package work.ready.core.database.generator;

import java.util.HashMap;
import java.util.Map;

public class TypeMapping {

	@SuppressWarnings("serial")
	protected Map<String, String> map = new HashMap<String, String>(32) {{

		put("java.sql.Date", "java.util.Date");

		put("java.sql.Time", "java.sql.Time");

		put("java.sql.Timestamp", "java.util.Date");

		put("[B", "byte[]");

		put("java.lang.String", "java.lang.String");

		put("java.lang.Integer", "java.lang.Integer");

		put("java.lang.Long", "java.lang.Long");

		put("java.lang.Double", "java.lang.Double");

		put("java.lang.Float", "java.lang.Float");

		put("java.lang.Boolean", "java.lang.Boolean");

		put("java.math.BigDecimal", "java.math.BigDecimal");

		put("java.math.BigInteger", "java.math.BigInteger");

		put("java.lang.Short", "java.lang.Short");

		put("java.lang.Byte", "java.lang.Byte");

		put("java.time.LocalDateTime", "java.time.LocalDateTime");
		put("java.time.LocalDate", "java.time.LocalDate");
		put("java.time.LocalTime", "java.time.LocalTime");
	}};

	public void addMapping(Class<?> from, Class<?> to) {
		map.put(from.getName(), to.getName());
	}

	public void addMapping(String from, String to) {
		map.put(from, to);
	}

	public String getType(String typeString) {
		return map.get(typeString);
	}
}
