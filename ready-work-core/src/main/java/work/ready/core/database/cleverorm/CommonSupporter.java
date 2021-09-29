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

package work.ready.core.database.cleverorm;

import work.ready.core.tools.StrUtil;

import java.util.Map;

abstract class CommonSupporter implements SqlSupporter {

    @Override
    public String generate(AutoCodeGenerator holder) {

        String code = null;
        if(holder.returnType.equals(boolean.class) || holder.returnType.equals(Boolean.class)){
            code = returnBoolean(holder);
        } else
        if(holder.returnType.equals(int.class) || holder.returnType.equals(Integer.class)){
            code = returnInteger(holder);
        } else
        if(holder.returnType.equals(long.class) || holder.returnType.equals(Long.class)){
            code = returnLong(holder);
        }

        return code;
   }

    private String returnBoolean(AutoCodeGenerator holder){
        String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
        Map<String, String> result = sqlParameterProcess(holder);
        return result.get("code") + "int result = " + dbHolder + result.get("audit") + ".update(sql, paramArray);\n" +
                "returnObject = result > 0 ? true : false;\n";
    }

    private String returnInteger(AutoCodeGenerator holder){
        String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
        Map<String, String> result = sqlParameterProcess(holder);
        return result.get("code") + "int result = " + dbHolder + result.get("audit") + ".update(sql, paramArray);\n" +
                "returnObject = result;\n";
    }

    private String returnLong(AutoCodeGenerator holder){
        String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
        Map<String, String> result = sqlParameterProcess(holder);
        return result.get("code") + "int result = " + dbHolder + result.get("audit") + ".update(sql, paramArray);\n" +
                "returnObject = java.lang.Long.valueOf(result);\n";
    }
}
