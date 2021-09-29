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

package work.ready.cloud.cluster;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class CloudDb {

    static final ArrayList<String> allowedTableMode = new ArrayList<>(Arrays.asList(TableMode.REPLICATED_PERSISTENCE.toString(), TableMode.PARTITIONED_PERSISTENCE.toString(), TableMode.REPLICATED_MEMORY.toString(), TableMode.PARTITIONED_MEMORY.toString()));

    static final Map<Class<?>, String> typeMap = new HashMap<>();
    static
    {
        typeMap.put(boolean.class, "BOOLEAN");
        typeMap.put(Boolean.class, "BOOLEAN");
        typeMap.put(int.class, "INT");
        typeMap.put(Integer.class, "INT");
        typeMap.put(byte.class, "TINYINT");
        typeMap.put(Byte.class, "TINYINT");
        typeMap.put(short.class, "SMALLINT");
        typeMap.put(Short.class, "SMALLINT");
        typeMap.put(long.class, "BIGINT");
        typeMap.put(Long.class, "BIGINT");
        typeMap.put(BigInteger.class, "BIGINT");
        typeMap.put(BigDecimal.class, "DECIMAL");
        typeMap.put(Double.class, "DOUBLE");
        typeMap.put(double.class, "DOUBLE");
        typeMap.put(Float.class, "REAL");
        typeMap.put(float.class, "REAL");
        typeMap.put(java.sql.Time.class, "TIME");
        typeMap.put(java.sql.Date.class, "DATE");
        typeMap.put(java.util.Date.class, "DATE");
        typeMap.put(java.time.LocalDate.class, "DATE");
        typeMap.put(java.time.LocalDateTime.class, "DATE");
        typeMap.put(java.sql.Timestamp.class, "TIMESTAMP");
        typeMap.put(String.class, "VARCHAR");
        typeMap.put(Character.class, "VARCHAR");
        typeMap.put(char.class, "VARCHAR");
        typeMap.put(byte[].class, "BINARY");
        typeMap.put(java.util.UUID.class, "UUID");

    }

    public enum TableMode {
        REPLICATED_PERSISTENCE, PARTITIONED_PERSISTENCE, REPLICATED_MEMORY, PARTITIONED_MEMORY
    }
}
