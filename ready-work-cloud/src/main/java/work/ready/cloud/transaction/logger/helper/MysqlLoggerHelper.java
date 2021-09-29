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

import java.util.*;

public class MysqlLoggerHelper implements TxLoggerHelper {

    private final LogDbHelper dbHelper;

    public MysqlLoggerHelper() {
        this.dbHelper = Cloud.getTransactionManager().getLogDbHelper();
    }

    private final RowProcessor processor = new BasicRowProcessor(new GenerousBeanProcessor());

    @Override
    public void init() {
        String sql = "CREATE TABLE IF NOT EXISTS `ready_tx_logger`  (\n" +
                "  `id` bigint(20) NOT NULL AUTO_INCREMENT,\n" +
                "  `group_id` varchar(64)  NOT NULL ,\n" +
                "  `unit_id` varchar(32)  NOT NULL ,\n" +
                "  `tag` varchar(50)  NOT NULL ,\n" +
                "  `content` varchar(1024)  NOT NULL ,\n" +
                "  `create_time` varchar(30) NOT NULL,\n" +
                "  `app_name` varchar(128) NOT NULL,\n" +
                "  PRIMARY KEY (`id`) USING BTREE\n" +
                ") ";
        dbHelper.update(sql);
    }

    @Override
    public int insert(TxLog txLoggerInfo) {
        String sql = "insert into ready_tx_logger(group_id,unit_id,tag,content,create_time,app_name) values(?,?,?,?,?,?)";
        return dbHelper.update(sql, txLoggerInfo.getGroupId(), txLoggerInfo.getUnitId(), txLoggerInfo.getTag(),
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
        return dbHelper.query("select count(*) from ready_tx_logger where " + where, new ScalarHandler<>(), params);
    }

    private String timeOrderSql(int timeOrder) {
        return "order by create_time " + (timeOrder == 1 ? "asc" : "desc");
    }

    @Override
    public void deleteByFields(List<Field> fields) throws TxLoggerException {
        StringBuilder sql = new StringBuilder("delete from ready_tx_logger where 1=1 and ");
        List<String> values = whereSqlAppender(sql, fields);
        dbHelper.update(sql.toString(), values.toArray(new Object[0]));
    }

    private List<String> whereSqlAppender(StringBuilder sql, List<Field> fields) {
        List<String> values = new ArrayList<>(fields.size());
        fields.forEach(field -> {
            if (field instanceof GroupId) {
                sql.append("group_id=? and ");
                values.add(((GroupId) field).getGroupId());
            } else if (field instanceof Tag) {
                sql.append("tag=? and ");
                values.add(((Tag) field).getTag());
            } else if (field instanceof StartTime) {
                sql.append("create_time > ? and ");
                values.add(((StartTime) field).getStartTime());
            } else if (field instanceof StopTime) {
                sql.append("create_time < ? and ");
                values.add(((StopTime) field).getStopTime());
            }
        });
        sql.delete(sql.length() - 4, sql.length());
        return values;
    }

    @Override
    public LogList findByLimitAndFields(int page, int limit, int timeOrder, List<Field> list) throws TxLoggerException {
        StringBuilder countSql = new StringBuilder("select count(*) from ready_tx_logger where 1=1 and ");
        StringBuilder sql = new StringBuilder("select * from ready_tx_logger where 1=1 and ");
        List<String> values = whereSqlAppender(sql, list);
        whereSqlAppender(countSql, list);
        Object[] params = values.toArray(new Object[0]);
        long total = dbHelper.query(countSql.toString(), new ScalarHandler<>(), params);
        if (total < (page - 1) * limit) {
            page = 1;
        }
        sql.append(timeOrderSql(timeOrder)).append(" limit ").append((page - 1) * limit).append(", ").append(limit);
        List<TxLog> txLogs = dbHelper.query(sql.toString(), new BeanListHandler<>(TxLog.class, processor), params);

        LogList logList = new LogList();
        logList.setTotal(total);
        logList.setTxLogs(txLogs);
        return logList;
    }

}
