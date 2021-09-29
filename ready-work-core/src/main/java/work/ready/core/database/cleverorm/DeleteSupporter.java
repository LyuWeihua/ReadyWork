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

import java.util.List;
import java.util.regex.Matcher;

public class DeleteSupporter extends CommonSupporter {

    @Override
    public String syntaxCheck(AutoCodeGenerator holder, String sql){
        if(holder.tables == null || holder.tables.size() == 0) throw new RuntimeException("CleverORM: Couldn't find table name");

        if(!holder.isMagicMethod){
            Matcher matcher = ModelService.sqlPlaceHolderPattern.matcher(sql);
            boolean isValidParameters = true;
            int placeHolderCount = 0;
            for(int i = 0; i < holder.parameters.length; i++){
                boolean found = matcher.find();
                if(found) placeHolderCount ++;
                if(!ClassUtil.isSimpleType(holder.parameters[i].getType())){
                    if(holder.parameters[i].getType().getComponentType() != null || holder.parameters[i].getType().equals(List.class)){
                        Class<?> parameterType;
                        if(holder.parameters[i].getType().equals(List.class)){
                            parameterType = ClassUtil.getGenericType(holder.parameters[i].getParameterizedType());
                        } else {
                            parameterType = holder.parameters[i].getType().getComponentType();
                        }
                        if(!ClassUtil.isSimpleType(parameterType) || !found || !matcher.group().startsWith("(") || !matcher.group().endsWith(")")){
                            isValidParameters = false;
                            break;
                        }
                    } else {
                        isValidParameters = false;
                        break;
                    }
                }
            }
            if(!isValidParameters)
                throw new RuntimeException("CleverORM: Found DELETE query with incompatible method parameters: " + sql + ", on " + ClassUtil.getMethodSignature(holder.method));
            if(placeHolderCount != holder.parameters.length || matcher.find())
                throw new RuntimeException("CleverORM: Found DELETE query with wrong value pairs which provided by method parameters: " + sql + ", on " + ClassUtil.getMethodSignature(holder.method));
        }

        if(holder.where == null)
            throw new RuntimeException("CleverORM: found DELETE query without WHERE conditions, it is dangerous and not allowed: " + sql + ", on " + ClassUtil.getMethodSignature(holder.method));

        return sql.replaceAll("(?i)\\s+_table_(\\s+|$|\\(|\\))", " " + holder.dao._getTableName() + "$1");
    }

    @Override
    public String generate(AutoCodeGenerator holder){
        String code = super.generate(holder);

        if(code != null) return code;
        throw new RuntimeException("CleverORM: Unsupported return type for DELETE command: " + holder.genericReturnType.getTypeName() + " of " + ClassUtil.getMethodSignature(holder.method));
    }

}
