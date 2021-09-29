/**
 *
 * Original work Copyright 2017-2019 CodingApi
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
package work.ready.cloud.transaction.core.transaction.txc.analyse;

import work.ready.cloud.transaction.core.transaction.txc.analyse.bean.FieldCluster;
import work.ready.cloud.transaction.core.transaction.txc.analyse.bean.InvolvedRecord;
import work.ready.cloud.transaction.core.transaction.txc.analyse.bean.FieldValue;
import work.ready.core.database.query.ResultSetHandler;
import work.ready.core.tools.StrUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TxcModifiedRecordListHandler implements ResultSetHandler<List<InvolvedRecord>> {

    private final List<String> columns;
    private final List<String> primaryKeys;

    public TxcModifiedRecordListHandler(List<String> primaryKeys, List<String> columns) {
        this.columns = columns;
        this.primaryKeys = primaryKeys;
    }

    @Override
    public List<InvolvedRecord> handle(ResultSet rs) throws SQLException {
        List<InvolvedRecord> involvedRecords = new ArrayList<>();
        while (rs.next()) {
            InvolvedRecord record = recordByColumns(rs, columns);

            for (String primaryKey : primaryKeys) {
                String[] pkName = StrUtil.split(primaryKey, '.');
                FieldValue fieldValue = new FieldValue();
                fieldValue.setTableName(pkName[0]);
                fieldValue.setFieldName(primaryKey);
                fieldValue.setValue(rs.getObject(pkName[1]));
                fieldValue.setValueType(fieldValue.getValue().getClass());
                record.getFieldClusters().get(fieldValue.getTableName()).getPrimaryKeys().add(fieldValue);
            }
            involvedRecords.add(record);
        }
        return involvedRecords;
    }

    public static InvolvedRecord recordByColumns(ResultSet rs, List<String> columns) throws SQLException {
        InvolvedRecord record = new InvolvedRecord();
        for (String column : columns) {
            String[] columnName = StrUtil.split(column, '.');
            FieldValue fieldValue = new FieldValue();
            fieldValue.setFieldName(column);
            fieldValue.setTableName(columnName[0]);
            fieldValue.setValue(rs.getObject(columnName[1]));
            fieldValue.setValueType(Objects.isNull(fieldValue.getValue()) ? Void.class : fieldValue.getValue().getClass());
            if (record.getFieldClusters().get(fieldValue.getTableName()) != null) {
                record.getFieldClusters().get(fieldValue.getTableName()).getFields().add(fieldValue);
            } else {
                FieldCluster fieldCluster = new FieldCluster();
                fieldCluster.getFields().add(fieldValue);
                record.getFieldClusters().put(fieldValue.getTableName(), fieldCluster);
            }
        }
        return record;
    }

}
