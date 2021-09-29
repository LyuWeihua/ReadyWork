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
package work.ready.cloud.transaction.core.transaction.txc.analyse.util;

import work.ready.cloud.transaction.common.serializer.SerializerContext;

public class SqlUtils {

    public static final String SQL_COMMA_SEPARATOR = ", ";

    public static final String DOT = ".";

    public static final String AND = " and ";

    public static final String OR = " or ";

    public static final String UPDATE = "UPDATE ";

    public static final String INSERT = "INSERT INTO ";

    public static final String DELETE = "DELETE ";

    public static final String SELECT = "SELECT ";

    public static final String FROM = " FROM ";

    public static final String WHERE = " WHERE ";

    public static final String SET = " SET ";

    public static final int MYSQL_TABLE_NOT_EXISTS_CODE = 1146;

    public static final String FOR_UPDATE = "FOR UPDATE";

    public static final String LOCK_IN_SHARE_MODE = "LOCK IN SHARE MODE";

    public static final int SQL_TYPE_INSERT = 1;
    public static final int SQL_TYPE_DELETE = 2;
    public static final int SQL_TYPE_UPDATE = 3;
    public static final int SQL_TYPE_SELECT = 4;

    public static String tableName(String fieldFullyQualifiedName) {
        if (fieldFullyQualifiedName.contains(".")) {
            return fieldFullyQualifiedName.substring(0, fieldFullyQualifiedName.indexOf("."));
        }
        return null;
    }

    public static void cutSuffix(String suffix, StringBuilder stringBuilder) {
        if (stringBuilder.substring(stringBuilder.length() - suffix.length()).equals(suffix)) {
            stringBuilder.delete(stringBuilder.length() - suffix.length(), stringBuilder.length());
        }
    }

    public static byte[] objectToBlob(Object o) {
        return SerializerContext.getInstance().serialize(o);
    }

    public static <T> T blobToObject(byte[] blob) {
        return (T)SerializerContext.getInstance().deserialize(blob);
    }
}
