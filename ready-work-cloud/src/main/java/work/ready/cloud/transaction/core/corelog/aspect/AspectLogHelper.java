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
package work.ready.cloud.transaction.core.corelog.aspect;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.core.corelog.H2DbHelper;
import work.ready.cloud.transaction.core.corelog.CoreLogHelper;
import work.ready.core.database.handlers.ScalarHandler;
import work.ready.core.database.query.ResultSetHandler;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AspectLogHelper implements CoreLogHelper {
    private static final Log logger = LogFactory.getLog(AspectLogHelper.class);
    private final H2DbHelper h2DbHelper;

    public AspectLogHelper() {
        this.h2DbHelper = Cloud.getTransactionManager().getH2DbHelper();
    }

    @Override
    public void init() {
        h2DbHelper.update("CREATE TABLE IF NOT EXISTS TXLCN_LOG " +
                "(" +
                "ID BIGINT NOT NULL, " +
                "UNIT_ID VARCHAR(32) NOT NULL," +
                "GROUP_ID VARCHAR(64) NOT NULL," +
                "METHOD_STR VARCHAR(512) NOT NULL ," +
                "BYTES BLOB NOT NULL," +
                "GROUP_ID_HASH BIGINT NOT NULL," +
                "UNIT_ID_HASH BIGINT NOT NULL," +
                "TIME BIGINT NOT NULL, " +
                "PRIMARY KEY(ID) )");
        logger.info("Aspect log table finished (H2 DATABASE)");
    }

    public boolean save(AspectLog txLog) {
        String insertSql = "INSERT INTO TXLCN_LOG(ID, UNIT_ID, GROUP_ID, BYTES, METHOD_STR, GROUP_ID_HASH, UNIT_ID_HASH, TIME) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        return h2DbHelper.update(insertSql, Ready.getId(), txLog.getUnitId(), txLog.getGroupId(), txLog.getBytes(), txLog.getMethodStr(), txLog.getGroupId().hashCode(), txLog.getUnitId().hashCode(), txLog.getTime()) > 0;
    }

    public boolean deleteAll() {
        String sql = "DELETE FROM TXLCN_LOG";
        return h2DbHelper.update(sql) > 0;
    }

    public void truncate() {
        String sql = "TRUNCATE TABLE TXLCN_LOG";
        h2DbHelper.update(sql);
    }

    public boolean delete(long id) {
        String sql = "DELETE FROM TXLCN_LOG WHERE ID = ?";
        return h2DbHelper.update(sql, id) > 0;
    }

    public boolean delete(long groupIdHash, long unitIdHash) {
        String sql = "DELETE FROM TXLCN_LOG WHERE GROUP_ID_HASH = ? and UNIT_ID_HASH = ?";
        return h2DbHelper.update(sql, groupIdHash, unitIdHash) > 0;
    }

    public boolean delete(String groupId) {
        String sql = "DELETE FROM TXLCN_LOG WHERE GROUP_ID = ?";
        return h2DbHelper.update(sql, groupId) > 0;
    }

    public List<AspectLog> findAll() {
        String sql = "SELECT * FROM TXLCN_LOG";
        return h2DbHelper.query(sql, resultSet -> {
            List<AspectLog> list = new ArrayList<>();
            while (resultSet.next()) {
                list.add(fill(resultSet));
            }
            return list;
        });
    }

    public long count() {
        String sql = "SELECT count(*) FROM TXLCN_LOG";
        return h2DbHelper.query(sql, new ScalarHandler<Long>());
    }

    public AspectLog getTxLog(String groupId, String unitId) {
        String sql = "SELECT * FROM TXLCN_LOG WHERE GROUP_ID = ? and UNIT_ID = ?";
        return h2DbHelper.query(sql, resultSetHandler, groupId, unitId);
    }

    public AspectLog getTxLog(long id) {
        String sql = "SELECT * FROM TXLCN_LOG WHERE ID = ?";
        return h2DbHelper.query(sql, resultSetHandler, id);
    }

    private final ResultSetHandler<AspectLog> resultSetHandler = resultSet -> {
        if (resultSet.next()) {
            return fill(resultSet);
        }
        return null;
    };

    private AspectLog fill(ResultSet resultSet) throws SQLException {
        AspectLog txLog = new AspectLog();
        txLog.setBytes(resultSet.getBytes("BYTES"));
        txLog.setGroupId(resultSet.getString("GROUP_ID"));
        txLog.setMethodStr(resultSet.getString("METHOD_STR"));
        txLog.setTime(resultSet.getLong("TIME"));
        txLog.setUnitId(resultSet.getString("UNIT_ID"));
        txLog.setGroupIdHash(resultSet.getLong("GROUP_ID_HASH"));
        txLog.setUnitIdHash(resultSet.getLong("UNIT_ID_HASH"));
        txLog.setId(resultSet.getLong("ID"));
        return txLog;
    }

}
