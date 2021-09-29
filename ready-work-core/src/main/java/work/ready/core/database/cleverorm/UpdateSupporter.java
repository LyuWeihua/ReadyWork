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
import work.ready.core.tools.StrUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class UpdateSupporter extends CommonSupporter {

    @Override
    public String syntaxCheck(AutoCodeGenerator holder, String sql){
        if(holder.tables == null || holder.tables.size() == 0) throw new RuntimeException("CleverORM: Couldn't find table name");

        if(holder.fields == null || holder.values == null || holder.fields.size() == 0 || holder.fields.size() != holder.values.size())
            throw new RuntimeException("CleverORM: found UPDATE query with wrong field or value pairs: " + sql + ", on " + ClassUtil.getMethodSignature(holder.method));

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
                throw new RuntimeException("CleverORM: Found UPDATE query with incompatible method parameters: " + sql + ", on " + ClassUtil.getMethodSignature(holder.method));
            if(placeHolderCount != holder.parameters.length || matcher.find())
                throw new RuntimeException("CleverORM: Found UPDATE query with wrong value pairs which provided by method parameters: " + sql + ", on " + ClassUtil.getMethodSignature(holder.method));
        }

        if(holder.where == null)
            throw new RuntimeException("CleverORM: found UPDATE query without WHERE conditions, it is dangerous and not allowed: " + sql + ", on " + ClassUtil.getMethodSignature(holder.method));

        return sql.replaceAll("(?i)\\s+_table_(\\s+|$|\\(|\\))", " " + holder.dao._getTableName() + "$1");
    }

    @Override
    public String generate(AutoCodeGenerator holder) {
        String code = null;
        if(holder.isMagicMethod){
            holder.addClassImport(LinkedList.class);
            holder.addClassImport(List.class);
            holder.addClassImport(Map.class);
            if(holder.returnType.equals(boolean.class) || holder.returnType.equals(Boolean.class)){
                code = returnBoolean(holder);
            } else
            if(holder.returnType.equals(int.class) || holder.returnType.equals(Integer.class)){
                code = returnInteger(holder);
            } else
            if(holder.returnType.equals(long.class) || holder.returnType.equals(Long.class)){
                code = returnLong(holder);
            }
        } else {
            code = super.generate(holder);
        }
        if(code != null) return code;
        throw new RuntimeException("CleverORM: Unsupported return type for UPDATE command: " + holder.genericReturnType.getTypeName() + " of " + ClassUtil.getMethodSignature(holder.method));
    }

    private String assembleSql(AutoCodeGenerator holder){
        String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
        String audit = ".audit(\"" + holder.modelServiceClass.getName() + "\",\"" + ClassUtil.getMethodSignature(holder.method, false) + "\")";
        String code = "int result = 0;" +
                "if(p1 != null && p1.size() > 0) {" +
                "List<Object> params = new LinkedList<>();" +
                "String data = \"\";" +
                "for (String key : p1.keySet()) {" +
                "    if(!data.isEmpty()) data += \" , \";" +
                "    data += key + \" = ? \";" +
                "    params.add(p1.get(key));" +
                "}";
        if(ClassUtil.isSimpleType(holder.parameters[0].getType())) {
            code += "String where = \" WHERE " + holder.updateStatement.getWhere() + "\"; params.add(p0);";
        } else if(holder.parameters[0].getType().getComponentType() != null) {
            code += "String placeHolder = \"\";\n" +
                    "for(int j = 0; j < p0.length; j++){\n";
            if(!ClassUtil.isBasicType(holder.parameters[0].getType().getComponentType())) {
                code += "   if(p0[j] == null) continue;\n";
            }
            code +=
                    "   params.add(p0[j]);\n" +
                    "   placeHolder += (placeHolder.isEmpty()) ? \"?\" : \", ?\";\n" +
                    "}\n";
            String[] where = StrUtil.split(holder.updateStatement.getWhere().toString(), '?'); 
            code += "String where = \" WHERE " + where[0] + "\" + placeHolder + \"" + where[1] + "\";";
        } else if(holder.parameters[0].getType().equals(List.class)) {
            code += "String placeHolder = \"\";\n" +
                    "for(int j = 0; j < p0.size(); j++){\n" +
                    "   if(p0.get(j) == null) continue;\n" +
                    "   params.add(p0.get(j));\n" +
                    "   placeHolder += (placeHolder.isEmpty()) ? \"?\" : \", ?\";\n" +
                    "}\n";
            String[] where = StrUtil.split(holder.updateStatement.getWhere().toString(), '?'); 
            code += "String where = \" WHERE " + where[0] + "\" + placeHolder + \"" + where[1] + "\";";
        }
        code += "String sql = \"UPDATE " + holder.dao._getTableName() + " set \" + data + where;\n";
        code += "result = " + dbHolder + audit + ".update(sql, params.toArray());}\n";
        return code;
    }

    private String returnBoolean(AutoCodeGenerator holder){
        String code = assembleSql(holder);
        code += "returnObject = result > 0 ? true : false;\n";
        return code;
    }

    private String returnInteger(AutoCodeGenerator holder){
        String code = assembleSql(holder);
        code += "returnObject = result;\n";
        return code;
    }

    private String returnLong(AutoCodeGenerator holder){
        String code = assembleSql(holder);
        code += "returnObject = java.lang.Long.valueOf(result);\n";
        return code;
    }
}
