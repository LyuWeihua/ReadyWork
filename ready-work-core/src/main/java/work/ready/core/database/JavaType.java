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

import java.util.HashMap;
import java.util.Map;

public class JavaType {

	@SuppressWarnings("serial")
	private Map<String, Class<?>> strToType = new HashMap<String, Class<?>>(32) {{

		put("java.lang.String", String.class);

		put("java.lang.Integer", Integer.class);

		put("java.lang.Long", Long.class);

		put("java.sql.Date", java.sql.Date.class);

		put("java.lang.Double", Double.class);

		put("java.lang.Float", Float.class);

		put("java.lang.Boolean", Boolean.class);

		put("java.sql.Time", java.sql.Time.class);

		put("java.sql.Timestamp", java.sql.Timestamp.class);

		put("java.math.BigDecimal", java.math.BigDecimal.class);

		put("java.math.BigInteger", java.math.BigInteger.class);

		put("[B", byte[].class);

		put("java.lang.Short", Short.class);
		put("java.lang.Byte", Byte.class);
	}};

	public Class<?> getType(String typeString) {
		return strToType.get(typeString);
	}
}

