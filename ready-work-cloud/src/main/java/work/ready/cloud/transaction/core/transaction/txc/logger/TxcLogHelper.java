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
package work.ready.cloud.transaction.core.transaction.txc.logger;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.core.corelog.H2DbHelper;
import work.ready.cloud.transaction.core.corelog.CoreLogHelper;
import work.ready.cloud.transaction.core.transaction.txc.analyse.bean.UndoLogDO;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TxcLogHelper implements CoreLogHelper {
    private static final Log logger = LogFactory.getLog(TxcLogHelper.class);
    private final H2DbHelper h2DbHelper;

    public TxcLogHelper() {
        this.h2DbHelper = Cloud.getTransactionManager().getH2DbHelper();
    }

    public TxcLogHelper(H2DbHelper h2DbHelper) {
        this.h2DbHelper = h2DbHelper;
    }

    @Override
    public void init() {
        h2DbHelper.update("CREATE TABLE IF NOT EXISTS TXC_UNDO_LOG (" +
                "ID BIGINT NOT NULL, " +
                "DATASOURCE VARCHAR(64) NOT NULL," +
                "UNIT_ID VARCHAR(32) NOT NULL," +
                "GROUP_ID VARCHAR(64) NOT NULL," +
                "SQL_TYPE INT NOT NULL," +
                "ROLLBACK_INFO BLOB NOT NULL," +
                "CREATE_TIME CHAR(23) NOT NULL, " +
                "PRIMARY KEY(ID) )");
        logger.info("Txc log table prepared (H2 DATABASE)");
    }

    public void saveUndoLog(UndoLogDO undoLogDO) throws SQLException {
        String sql = "INSERT INTO TXC_UNDO_LOG (ID, DATASOURCE, UNIT_ID, GROUP_ID, SQL_TYPE, ROLLBACK_INFO, CREATE_TIME) VALUES(?, ?, ?, ?, ?, ?, ?)";
        h2DbHelper.queryRunner().update(sql, Ready.getId(), undoLogDO.getDatasource(), undoLogDO.getUnitId(), undoLogDO.getGroupId(), undoLogDO.getSqlType(),
                undoLogDO.getRollbackInfo(), undoLogDO.getCreateTime());
    }

    public List<UndoLogDO> getUndoLogByGroupAndUnitId(String groupId, String unitId) throws SQLException {
        String sql = "SELECT * FROM TXC_UNDO_LOG WHERE GROUP_ID = ? and UNIT_ID = ?";
        return h2DbHelper.queryRunner().query(sql, rs -> {
            List<UndoLogDO> undoLogDOList = new ArrayList<>();
            while (rs.next()) {
                UndoLogDO undoLogDO = new UndoLogDO();
                undoLogDO.setDatasource(rs.getString("DATASOURCE"));
                undoLogDO.setSqlType(rs.getInt("SQL_TYPE"));
                undoLogDO.setRollbackInfo(rs.getBytes("ROLLBACK_INFO"));
                undoLogDO.setUnitId(rs.getString("UNIT_ID"));
                undoLogDO.setGroupId("GROUP_ID");
                undoLogDO.setCreateTime(rs.getString("CREATE_TIME"));
                undoLogDOList.add(undoLogDO);
            }
            return undoLogDOList;
        }, groupId, unitId);
    }

    public void deleteUndoLog(String groupId, String unitId) throws SQLException {
        String sql = "DELETE FROM TXC_UNDO_LOG WHERE GROUP_ID=? AND UNIT_ID=?";
        h2DbHelper.queryRunner().update(sql, groupId, unitId);
    }
}
