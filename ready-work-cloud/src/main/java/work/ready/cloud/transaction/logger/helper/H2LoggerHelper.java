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
package work.ready.cloud.transaction.logger.helper;

import work.ready.cloud.ReadyCloud;
import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.logger.db.LogDbHelper;
import work.ready.cloud.transaction.logger.db.TxLog;
import work.ready.cloud.transaction.logger.exception.TxLoggerException;
import work.ready.cloud.transaction.logger.model.*;
import work.ready.core.database.handlers.BeanListHandler;
import work.ready.core.database.handlers.ScalarHandler;
import work.ready.core.database.query.BasicRowProcessor;
import work.ready.core.database.query.GenerousBeanProcessor;
import work.ready.core.database.query.RowProcessor;
import work.ready.core.server.Ready;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class H2LoggerHelper implements TxLoggerHelper {

    private final LogDbHelper dbHelper;

    public H2LoggerHelper() {
        this.dbHelper = Cloud.getTransactionManager().getLogDbHelper();
    }

    private final RowProcessor processor = new BasicRowProcessor(new GenerousBeanProcessor());

    @Override
    public void init() {
        String sql = "CREATE TABLE IF NOT EXISTS READY_TX_LOGGER (\n" +
                "  ID BIGINT NOT NULL,\n" +
                "  GROUP_ID VARCHAR(64) NOT NULL,\n" +
                "  UNIT_ID VARCHAR(32) NOT NULL,\n" +
                "  TAG VARCHAR(50) NOT NULL,\n" +
                "  CONTENT VARCHAR(1024) NOT NULL,\n" +
                "  CREATE_TIME VARCHAR(30) NOT NULL,\n" +
                "  APP_NAME VARCHAR(128) NOT NULL,\n" +
                "  PRIMARY KEY (ID) \n" +
                ") ";
        if(ReadyCloud.getConfig().getTransaction().getTxLogger().isIgnitePersistence()) {
            sql += " WITH \"mode=REPLICATED_PERSISTENCE,cache_name=READY_TX_LOGGER\";";
        }
        dbHelper.update(sql);
    }

    @Override
    public int insert(TxLog txLoggerInfo) {
        String sql = "INSERT INTO READY_TX_LOGGER(ID,GROUP_ID,UNIT_ID,TAG,CONTENT,CREATE_TIME,APP_NAME) VALUES(?,?,?,?,?,?)";
        return dbHelper.update(sql, Ready.getId(), txLoggerInfo.getGroupId(), txLoggerInfo.getUnitId(), txLoggerInfo.getTag(),
                format(txLoggerInfo.getContent(), Map.of("xid", txLoggerInfo.getGroupId(),
                        "uid", txLoggerInfo.getUnitId()), txLoggerInfo.getArgs()),
                txLoggerInfo.getCreateTime(), txLoggerInfo.getAppName());
    }

    public static String format(String input, Map<String, Object> params, Object... args) {
        StringBuilder varString = new StringBuilder();
        StringBuilder finalString = new StringBuilder();
        int varMaxLen = 5;
        int curVarLen = 0;
        boolean wait = false;
        int argIndex = -1;
        char startChar = '%';
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '{' || c == '%') {
                wait = true;
                startChar = c;
                continue;
            }
            if (wait) {
                if (c == '}' || (startChar == '%' && (c == 's' || c == 'd'))) {
                    if (varString.length() > 0) {
                        finalString.append(Optional.ofNullable(params.get(varString.toString())).orElse(startChar + c));
                        curVarLen = 0;
                        varString.delete(0, varString.length());
                    } else if (++argIndex >= args.length) {
                        finalString.append(startChar).append(c);
                    } else {
                        finalString.append(args[argIndex]);
                    }
                } else if (curVarLen < varMaxLen) {
                    varString.append(c);
                    curVarLen++;
                    continue;
                } else {
                    finalString.append(startChar).append(varString);
                    curVarLen = 0;
                    varString.delete(0, varString.length());
                }
                wait = false;
                continue;
            }
            finalString.append(c);
        }
        return finalString.toString();
    }

    private long total(String where, Object... params) {
        return dbHelper.query("SELECT COUNT(*) FROM READY_TX_LOGGER WHERE " + where, new ScalarHandler<>(), params);
    }

    private String timeOrderSql(int timeOrder) {
        return "ORDER BY CREATE_TIME " + (timeOrder == 1 ? "ASC" : "DESC");
    }

    @Override
    public void deleteByFields(List<Field> fields) throws TxLoggerException {
        StringBuilder sql = new StringBuilder("DELETE FROM READY_TX_LOGGER WHERE 1=1 AND ");
        List<String> values = whereSqlAppender(sql, fields);
        dbHelper.update(sql.toString(), values.toArray(new Object[0]));
    }

    private List<String> whereSqlAppender(StringBuilder sql, List<Field> fields) {
        List<String> values = new ArrayList<>(fields.size());
        fields.forEach(field -> {
            if (field instanceof GroupId) {
                sql.append("GROUP_ID=? AND ");
                values.add(((GroupId) field).getGroupId());
            } else if (field instanceof Tag) {
                sql.append("TAG=? AND ");
                values.add(((Tag) field).getTag());
            } else if (field instanceof StartTime) {
                sql.append("CREATE_TIME > ? AND ");
                values.add(((StartTime) field).getStartTime());
            } else if (field instanceof StopTime) {
                sql.append("CREATE_TIME < ? AND ");
                values.add(((StopTime) field).getStopTime());
            }
        });
        sql.delete(sql.length() - 4, sql.length());
        return values;
    }

    @Override
    public LogList findByLimitAndFields(int page, int limit, int timeOrder, List<Field> list) throws TxLoggerException {
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM READY_TX_LOGGER WHERE 1=1 AND ");
        StringBuilder sql = new StringBuilder("SELECT * FROM READY_TX_LOGGER WHERE 1=1 AND ");
        List<String> values = whereSqlAppender(sql, list);
        whereSqlAppender(countSql, list);
        Object[] params = values.toArray(new Object[0]);
        long total = dbHelper.query(countSql.toString(), new ScalarHandler<>(), params);
        if (total < (page - 1) * limit) {
            page = 1;
        }
        sql.append(timeOrderSql(timeOrder)).append(" LIMIT ").append((page - 1) * limit).append(", ").append(limit);
        List<TxLog> txLogs = dbHelper.query(sql.toString(), new BeanListHandler<>(TxLog.class, processor), params);

        LogList logList = new LogList();
        logList.setTotal(total);
        logList.setTxLogs(txLogs);
        return logList;
    }

}
