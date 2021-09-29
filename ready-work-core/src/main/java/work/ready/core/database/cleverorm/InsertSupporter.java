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

import work.ready.core.database.ModelService;
import work.ready.core.tools.ClassUtil;

import java.util.regex.Matcher;

import static work.ready.core.tools.ClassUtil.getMethodSignature;

public class InsertSupporter extends CommonSupporter {

    @Override
    public String syntaxCheck(AutoCodeGenerator holder, String sql){
        if(holder.tables == null || holder.tables.size() == 0) throw new RuntimeException("CleverORM: Couldn't find table name");

        if(holder.fields == null || holder.fields.size() == 0 || (holder.values != null && holder.fields.size() != holder.values.size())){
            throw new RuntimeException("CleverORM: found INSERT query with wrong field or value pairs: " + sql + ", on " + getMethodSignature(holder.method));
        }

        Matcher matcher = ModelService.sqlPlaceHolderPattern.matcher(sql);
        boolean isValidParameters = true;
        int placeHolderCount = 0;
        for(int i = 0; i < holder.parameters.length; i++){
            boolean found = matcher.find();
            if(found) placeHolderCount ++;
            if(!ClassUtil.isSimpleType(holder.parameters[i].getType())){
                isValidParameters = false;
                break;
            }
        }
        if(!isValidParameters)
            throw new RuntimeException("CleverORM: Found INSERT query with incompatible method parameters: " + sql + ", on " + getMethodSignature(holder.method));
        if(placeHolderCount != holder.parameters.length || matcher.find())
            throw new RuntimeException("CleverORM: found INSERT query with wrong value pairs which provided by method parameters: " + sql + ", on " + getMethodSignature(holder.method));

        return sql.replaceAll("(?i)\\s+_table_(\\s+|$|\\(|\\))", " " + holder.dao._getTableName() + "$1");
    }

    @Override
    public String generate(AutoCodeGenerator holder) {
        String code = super.generate(holder);
        if(code != null) return code;
        throw new RuntimeException("CleverORM: Unsupported return type for INSERT command: " + holder.genericReturnType.getTypeName() + " of " + getMethodSignature(holder.method));
    }

}
