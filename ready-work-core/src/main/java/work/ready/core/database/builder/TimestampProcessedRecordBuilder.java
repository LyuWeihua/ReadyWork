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

import work.ready.core.database.*;
import work.ready.core.database.Record;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class TimestampProcessedRecordBuilder extends RecordBuilder {

	@Override
	public List<Record> build(Config config, ResultSet rs) throws SQLException {
		return build(config, rs, null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Record> build(Config config, ResultSet rs, Function<Record, Boolean> func) throws SQLException {
		List<Record> result = new ArrayList<Record>();
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		String[] labelNames = new String[columnCount + 1];
		int[] types = new int[columnCount + 1];
		buildLabelNamesAndTypes(rsmd, labelNames, types);
		while (rs.next()) {
			Record record = new Record(config.getContainerFactory().getColumnsMap());
			Map<String, Object> columns = record.getColumns();
			for (int i=1; i<=columnCount; i++) {
				Object value;
				if (types[i] < Types.DATE) {
					value = rs.getObject(i);
				} else {
					if (types[i] == Types.TIMESTAMP) {
						value = rs.getTimestamp(i);
					} else if (types[i] == Types.DATE) {
						value = rs.getDate(i);
					} else if (types[i] == Types.CLOB) {
						value = getModelBuilder().handleClob(rs.getClob(i));
					} else if (types[i] == Types.NCLOB) {
						value = getModelBuilder().handleClob(rs.getNClob(i));
					} else if (types[i] == Types.BLOB) {
						value = getModelBuilder().handleBlob(rs.getBlob(i));
					} else {
						value = rs.getObject(i);
					}
				}

				columns.put(labelNames[i], value);
			}

			if (func == null) {
				result.add(record);
			} else {
				if ( ! func.apply(record) ) {
					break ;
				}
			}
		}
		return result;
	}
}

