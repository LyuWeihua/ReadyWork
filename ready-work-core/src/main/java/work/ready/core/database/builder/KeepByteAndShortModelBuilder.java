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

import work.ready.core.database.CPI;
import work.ready.core.database.Model;
import work.ready.core.database.ModelBuilder;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class KeepByteAndShortModelBuilder extends ModelBuilder {

	@Override
	@SuppressWarnings({"rawtypes"})
	public <T> List<T> build(ResultSet rs, Class<? extends Model> modelClass) throws SQLException, ReflectiveOperationException {
		return build(rs, modelClass, null);
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public <T> List<T> build(ResultSet rs, Class<? extends Model> modelClass, Function<T, Boolean> func) throws SQLException, ReflectiveOperationException {
		List<T> result = new ArrayList<T>();
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		String[] labelNames = new String[columnCount + 1];
		int[] types = new int[columnCount + 1];
		buildLabelNamesAndTypes(rsmd, labelNames, types);
		while (rs.next()) {
			Model<?> ar = modelClass.getDeclaredConstructor().newInstance();
			Map<String, Object> attrs = CPI.getAttrs(ar);
			for (int i=1; i<=columnCount; i++) {
				Object value;
				int t = types[i];
				if (t < Types.DATE) {
					if (t == Types.TINYINT) {
						value = BuilderKit.getByte(rs, i);
					} else if (t == Types.SMALLINT) {
						value = BuilderKit.getShort(rs, i);
					} else {
						value = rs.getObject(i);
					}
				} else {
					if (t == Types.TIMESTAMP) {
						value = rs.getTimestamp(i);
					} else if (t == Types.DATE) {
						value = rs.getDate(i);
					} else if (t == Types.CLOB) {
						value = handleClob(rs.getClob(i));
					} else if (t == Types.NCLOB) {
						value = handleClob(rs.getNClob(i));
					} else if (t == Types.BLOB) {
						value = handleBlob(rs.getBlob(i));
					} else {
						value = rs.getObject(i);
					}
				}

				attrs.put(labelNames[i], value);
			}

			if (func == null) {
				result.add((T)ar);
			} else {
				if ( ! func.apply((T)ar) ) {
					break ;
				}
			}
		}
		return result;
	}
}

