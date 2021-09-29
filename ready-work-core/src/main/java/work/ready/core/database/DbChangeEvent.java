/**
 *
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
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

package work.ready.core.database;

import work.ready.core.tools.StrUtil;

import java.io.Serializable;
import java.math.BigInteger;

public class DbChangeEvent implements Serializable {

    private String dbSource;

    private String table;

    private DbChangeType type;

    private Object[] id;

    private boolean internal = true;

    private boolean skip = false;

    public DbChangeEvent(String dbSource, String table, DbChangeType type, Object... id) {
        this.dbSource = dbSource;
        this.table = table;
        this.type = type;
        this.id = id;
    }

    public String getDbSource() {
        return dbSource;
    }

    public void setDbSource(String dbSource) {
        this.dbSource = dbSource;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public DbChangeType getType() {
        return type;
    }

    public void setType(DbChangeType type) {
        this.type = type;
    }

    public Object[] getId() {
        return id;
    }

    public void setId(Object... id) {
        this.id = id;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public String toMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(dbSource != null ? dbSource : "");
        sb.append("|");
        sb.append(table != null ? table : "");
        sb.append("|");
        sb.append(type != null ? type.name() : "");
        sb.append("|");
        if(id != null && id.length > 0) {
            for(int i = 0; i < id.length; i++) {
                if(i > 0) sb.append(";");
                
                if (id[i] instanceof Integer) {
                    sb.append("Integer,");
                } else if (id[i] instanceof Long) {
                    sb.append("Long,");
                } else if (id[i] instanceof BigInteger) {
                    sb.append("BigInteger,");
                } else if (id[i] instanceof String){
                    sb.append("String,");

                } else {
                    throw new RuntimeException("DbChangeEvent doesn't support this primary key: " + id[i]);
                }
                sb.append(id[i]);
            }
        }
        return sb.toString();
    }

    public static DbChangeEvent fromMessage(String str){
        String[] fields = StrUtil.split(str,'|');
        if(fields.length != 4) throw new RuntimeException("Bad DbChangeEvent String: " + str);
        Object[] id = null;
        if(!fields[3].isEmpty()) {
            String[] idTypes = StrUtil.split(fields[3], ';');
            id = new Object[idTypes.length];
            for (int i = 0; i < idTypes.length; i++) {
                String[] idType = StrUtil.split(idTypes[i], ',');
                if("Integer".equals(idType[0])){
                    id[i] = Integer.parseInt(idType[1]);
                } else if("Long".equals(idType[0])){
                    id[i] = Long.parseLong(idType[1]);
                } else if("BigInteger".equals(idType[0])){
                    id[i] = new BigInteger(idType[1]);
                } else {
                    id[i] = idType[1];
                }
            }
        }
        return new DbChangeEvent(fields[0], fields[1], DbChangeType.valueOf(fields[2]), id);
    }
}
