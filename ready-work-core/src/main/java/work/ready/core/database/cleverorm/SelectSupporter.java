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

import work.ready.core.database.Model;
import work.ready.core.database.ModelService;
import work.ready.core.database.Page;
import work.ready.core.database.Record;
import work.ready.core.database.annotation.Auto;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.StrUtil;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class SelectSupporter implements SqlSupporter {

    @Override
    public String syntaxCheck(AutoCodeGenerator holder, String sql){
        if(holder.tables == null || holder.tables.size() == 0) throw new RuntimeException("CleverORM: Couldn't find table name");

        List<Class<?>> typeList = new ArrayList<>();
        ClassUtil.getGenericType(holder.genericReturnType, typeList, true);
        boolean withPagination = false;

        if(typeList.size() > 0 && typeList.get(0).equals(Page.class)){
            if(typeList.size() != 2 || (!Model.class.isAssignableFrom(typeList.get(1)) && !Record.class.equals(typeList.get(1))) || (Model.class.equals(typeList.get(1)) && ClassUtil.getGenericType(holder.genericReturnType) != null)){
                throw new RuntimeException("CleverORM: Found select query with incompatible page return type, wrong generic type of page: " + holder.genericReturnType + ", on " + ClassUtil.getMethodSignature(holder.method));
            }
            if(holder.parameters.length < 2 || (!int.class.equals(holder.parameters[0].getType()) && !Integer.class.equals(holder.parameters[0].getType()))
                                            || (!int.class.equals(holder.parameters[1].getType()) && !Integer.class.equals(holder.parameters[1].getType()))){
                throw new RuntimeException("CleverORM: Incompatible method parameters for page type select query, the first two parameters should be int type of page number and page size: " + Arrays.asList(holder.parameters) + ", on " + ClassUtil.getMethodSignature(holder.method));
            }
            withPagination = true;
        }
        if(typeList.size() > 0 && (typeList.get(0).equals(List.class) || typeList.get(0).equals(Set.class) || typeList.get(0).equals(Map.class))){
            if(holder.genericReturnType.toString().contains("<work.ready.core.database.Model>")){
                throw new RuntimeException("CleverORM: Found select query with incompatible Model return type: " + holder.genericReturnType + ", on " + ClassUtil.getMethodSignature(holder.method));
            }
        }

        Matcher matcher = ModelService.sqlPlaceHolderPattern.matcher(sql);
        boolean isValidParameters = true;
        int placeHolderCount = 0;
        for(int i = 0; i < holder.parameters.length; i++){
            if(withPagination && i < 2) { placeHolderCount ++;continue; }  
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
            throw new RuntimeException("CleverORM: Found select query with incompatible method parameters: " + sql + ", on " + ClassUtil.getMethodSignature(holder.method));
        if(placeHolderCount != holder.parameters.length || matcher.find())
            throw new RuntimeException("CleverORM: Found select query with wrong place holder pairs, doesn't match method parameters: " + sql + ", on " + ClassUtil.getMethodSignature(holder.method));

        return sql.replaceAll("(?i)\\s+_table_(\\s+|$|\\(|\\))", " " + holder.dao._getTableName() + "$1");
    }

    @Override
    public String generate(AutoCodeGenerator holder){

        holder.addClassImport(Record.class);
        holder.addClassImport(StrUtil.class);

        String code = null;
        if(Model.class.isAssignableFrom(holder.returnType)){
            code = returnModel(holder);
        } else
        if(holder.returnType.equals(Record.class)){
            code = returnRecord(holder);
        } else
        if(holder.returnType.equals(Page.class)){
            code = returnPage(holder);
        } else
        if(holder.returnType.equals(Map.class)){
            code = returnMap(holder);
        } else
        if(holder.returnType.equals(List.class)){
            code = returnList(holder);
        } else
        if(holder.returnType.equals(Set.class)){
            code = returnSet(holder);
        } else
        if(holder.returnType.equals(byte.class)){
            code = returnPrimitiveByte(holder);
        } else
        if(holder.returnType.equals(char.class)){
            code = returnPrimitiveChar(holder);
        } else
        if(holder.returnType.equals(short.class)){
            code = returnPrimitiveShort(holder);
        } else
        if(holder.returnType.equals(int.class)){
            code = returnPrimitiveInt(holder);
        } else
        if(holder.returnType.equals(float.class)){
            code = returnPrimitiveFloat(holder);
        } else
        if(holder.returnType.equals(double.class)){
            code = returnPrimitiveDouble(holder);
        } else
        if(holder.returnType.equals(long.class)){
            code = returnPrimitiveLong(holder);
        } else
        if(holder.returnType.equals(boolean.class)){
            code = returnPrimitiveBoolean(holder);
        } else
        if(ClassUtil.isWrapped(holder.returnType.getComponentType())
                || holder.returnType.getComponentType() != null && (Model.class.isAssignableFrom(holder.returnType.getComponentType())
                || Record.class.equals(holder.returnType.getComponentType()))){
            code = returnComponentType(holder);
        } else
        if(ClassUtil.isWrapped(holder.returnType)){
            code = returnBasicWrappedType(holder);
        }

        String findFist = (holder.limit == null && holder.top == null && holder.offset == null) ? "findTop1(" : "findFirst("; 
        String find = "find(";
        if(holder.isMagicMethod && holder.topN != null){
            findFist = "findTop1(";
            find = "findTop(" + holder.topN + ", ";
        }

        if(code != null) return code.replaceAll("find\\(", find).replaceAll("findFirst\\(", findFist);
        throw new RuntimeException("CleverORM: Unsupported return type: " + holder.genericReturnType.getTypeName() + " of " + ClassUtil.getMethodSignature(holder.method));
    }

    private String returnPrimitiveByte(AutoCodeGenerator holder){
        String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
        Map<String, String> result = sqlParameterProcess(holder);
        return result.get("code") + "Record result = " + dbHolder + result.get("audit") + ".findFirst(sql, paramArray);\n" +
                "returnObject = result != null ? Byte.parseByte(result.getColumnValues()[0].toString()) : 0;\n";
    }

    private String returnPrimitiveChar(AutoCodeGenerator holder){
        String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
        Map<String, String> result = sqlParameterProcess(holder);
        return result.get("code") + "Record result = " + dbHolder + result.get("audit") + ".findFirst(sql, paramArray);\n" +
                "returnObject = result != null ? Character.valueOf(result.getColumnValues()[0].toString().charAt(0)) : 0;\n";
    }

    private String returnPrimitiveShort(AutoCodeGenerator holder){
        String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
        Map<String, String> result = sqlParameterProcess(holder);
        return result.get("code") + "Record result = " + dbHolder + result.get("audit") + ".findFirst(sql, paramArray);\n" +
                "returnObject = result != null ? Short.parseShort(result.getColumnValues()[0].toString()) : 0;\n";
    }

    private String returnPrimitiveInt(AutoCodeGenerator holder){
        String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
        Map<String, String> result = sqlParameterProcess(holder);
        return result.get("code") + "Record result = " + dbHolder + result.get("audit") + ".findFirst(sql, paramArray);\n" +
                "returnObject = result != null ? Integer.parseInt(result.getColumnValues()[0].toString()) : 0;\n";
    }

    private String returnPrimitiveLong(AutoCodeGenerator holder){
        String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
        Map<String, String> result = sqlParameterProcess(holder);
        return result.get("code") + "Record result = " + dbHolder + result.get("audit") + ".findFirst(sql, paramArray);\n" +
                "returnObject = result != null ? Long.parseLong(result.getColumnValues()[0].toString()) : 0;\n";
    }

    private String returnPrimitiveFloat(AutoCodeGenerator holder){
        String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
        Map<String, String> result = sqlParameterProcess(holder);
        return result.get("code") + "Record result = " + dbHolder + result.get("audit") + ".findFirst(sql, paramArray);\n" +
                "returnObject = result != null ? Float.parseFloat(result.getColumnValues()[0].toString()) : 0;\n";
    }

    private String returnPrimitiveDouble(AutoCodeGenerator holder){
        String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
        Map<String, String> result = sqlParameterProcess(holder);
        return result.get("code") + "Record result = " + dbHolder + result.get("audit") + ".findFirst(sql, paramArray);\n" +
                "returnObject = result != null ? Double.parseDouble(result.getColumnValues()[0].toString()) : 0;\n";
    }

    private String returnPrimitiveBoolean(AutoCodeGenerator holder){ 
        String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
        Map<String, String> result = sqlParameterProcess(holder);
        return result.get("code") + "Record result = " + dbHolder + result.get("audit") + ".findFirst(sql, paramArray);\n" +
                "returnObject = result != null;\n";
    }

    private String returnRecord(AutoCodeGenerator holder) {
        String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
        Map<String, String> result = sqlParameterProcess(holder);
        return result.get("code") + "returnObject = " + dbHolder + result.get("audit") + ".findFirst(sql, paramArray);\n";
    }

    private String returnModel(AutoCodeGenerator holder) {
        String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "use(\""+holder.dataSource+"\")" : "dao";
        Map<String, String> result = sqlParameterProcess(holder);
        return result.get("code") + "returnObject = " + dbHolder + result.get("audit") + ".findFirst(sql, paramArray);\n";
    }

    private String returnPage(AutoCodeGenerator holder) {
        holder.addClassImport(List.class);
        holder.addClassImport(Record.class);
        holder.addClassImport(Model.class);
        boolean isGroupBy =  holder.groupByElement != null && holder.groupByElement.getGroupByExpressions().size() > 0;

        Map<String, String> result = sqlParameterProcess(holder);
        List<Class<?>> typeList = new ArrayList<>();
        ClassUtil.getGenericType(holder.genericReturnType, typeList, true);
        if(Record.class.equals(typeList.get(1))){
            String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
            return result.get("code") + "\nreturnObject = " + dbHolder + result.get("audit") + ".paginateByFullSql(p0, p1, " + isGroupBy + " , countTotalSql, sql, paramArray);\n";
        } else {
            String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "use(\""+holder.dataSource+"\")" : "dao";
            return result.get("code") + "\nreturnObject = (" + holder.genericReturnType.getTypeName() + ")" + dbHolder + result.get("audit") + ".paginateByFullSql(p0, p1, " + isGroupBy + " , countTotalSql, sql, paramArray);\n";
        }
    }

    private String returnBasicWrappedType(AutoCodeGenerator holder) {
        holder.addClassImport(Method.class);
        holder.addClassImport(RuntimeException.class);
        holder.addClassImport(NoSuchMethodException.class);
        holder.addClassImport(SecurityException.class);
        holder.addClassImport(IllegalAccessException.class);
        holder.addClassImport(IllegalArgumentException.class);
        holder.addClassImport(InvocationTargetException.class);
        String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
        Map<String, String> result = sqlParameterProcess(holder);
        String code = result.get("code") + "Record result = " + dbHolder + result.get("audit") + ".findFirst(sql, paramArray);\n" +
                "if(result == null) return null;" +
                "String column = result.getColumnNames()[0];" +
                "Object value = result.get(column);" +
                "if(value == null) return null;";
                if(String.class.equals(holder.returnType)){
                    code += "returnObject = String.valueOf(value);";
                } else {
                    code += "if(!" + holder.returnType.getCanonicalName() + ".class.equals(value.getClass())) {";
                        code += "String methodName = \"" + holder.method.getDeclaringClass().getName() + "." + holder.method.getName() + "\";";
                        code += "try {";
                        code += "Method valueOfMethod = "+ holder.returnType.getCanonicalName() + ".class.getDeclaredMethod(\"valueOf\", String.class);";
                        code += "returnObject = (" + holder.genericReturnType.getTypeName() + ")valueOfMethod.invoke(null, value.toString());";
                        code += "} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {";
                        code += "throw new RuntimeException(\"return type convert failed: \" + methodName + \", is it possible to change the return type from " + holder.returnType.getName() + " to \" + value.getClass().getName() + \" ?\", e);";
                        code += "}";
                    code += "} else {";
                    code += "returnObject = (" + holder.genericReturnType.getTypeName() + ")value;\n";
                    code += "}";
                }
                return code;
    }

    private String returnComponentType(AutoCodeGenerator holder) {
        holder.addClassImport(Array.class);
        holder.addClassImport(List.class);
        holder.addClassImport(Model.class);
        holder.addClassImport(Method.class);
        holder.addClassImport(RuntimeException.class);
        holder.addClassImport(NoSuchMethodException.class);
        holder.addClassImport(SecurityException.class);
        holder.addClassImport(IllegalAccessException.class);
        holder.addClassImport(IllegalArgumentException.class);
        holder.addClassImport(InvocationTargetException.class);
        Class<?> componentType = holder.returnType.getComponentType();
        String code = null;
        if(Model.class.isAssignableFrom(componentType)){
            String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "use(\""+holder.dataSource+"\")" : "dao";
            Map<String, String> result = sqlParameterProcess(holder);
            String modelGenericType = componentType.equals(Model.class) ? "? extends Model" : componentType.getTypeName();
            code = result.get("code") + "List<" + modelGenericType + "> result = " + dbHolder + result.get("audit") + ".find(sql, paramArray);\n" +
            "Object array = Array.newInstance(" + componentType.getCanonicalName() + ".class, result.size());" +
            "if(result.size() > 0) {" +
            "    for (int i = 0; i < result.size(); i++) {" +
            "        Array.set(array, i, result.get(i));" +
            "    }" +
            "}" +
            "returnObject = (" + holder.genericReturnType.getTypeName() + ")array;";
        } else {
            String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
            Map<String, String> result = sqlParameterProcess(holder);
            code = result.get("code") + "List<Record> result = " + dbHolder + result.get("audit") + ".find(sql, paramArray);\n" +
                    "Object array = Array.newInstance(" + componentType.getCanonicalName() + ".class, result.size());" +
                    "if(result.size() > 0) {" +
                        "if(Record.class.isAssignableFrom(" + componentType.getCanonicalName() + ".class)){" +
                        "    for (int i = 0; i < result.size(); i++) {" +
                        "        Array.set(array, i, result.get(i));" +
                        "    }" +
                        "} else {" +
                        "    String column = result.get(0).getColumnNames()[0];" +
                        "    String methodName = \"" + holder.method.getDeclaringClass().getName() + "." + holder.method.getName() + "\";" +
                        "    Object value = null;" +
                        "    if(String.class.isAssignableFrom(" + componentType.getCanonicalName() + ".class)) {" +
                        "        for (int i = 0; i < result.size(); i++) {" +
                        "            value = result.get(i).get(column);" +
                        "            Array.set(array, i, value.toString());" +
                        "        }" +
                        "    } else {" +
                        "        try {" +
                        "            Method valueOfMethod = " + componentType.getCanonicalName() + ".class.getDeclaredMethod(\"valueOf\", String.class);" +
                        "            for (int i = 0; i < result.size(); i++) {" +
                        "                value = result.get(i).get(column);" +
                        "                Array.set(array, i, valueOfMethod.invoke(null, value.toString()));" +
                        "            }" +
                        "        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {" +
                        "           throw new RuntimeException(\"return type convert failed: \" + methodName + \", is it possible to change the return type from " + holder.returnType.getName() + " to \" + value.getClass().getName() + \" ?\", e);" +
                        "        }" +
                        "    }" +
                        "}" +
                    "}" +
                    "returnObject = (" + holder.genericReturnType.getTypeName() + ")array;";
        }
        return code;
    }

    private String returnMap(AutoCodeGenerator holder){

        holder.addClassImport(List.class);
        holder.addClassImport(Map.class);
        holder.addClassImport(HashMap.class);
        holder.addClassImport(LinkedHashMap.class);
        holder.addClassImport(Comparator.class);
        holder.addClassImport(ArrayList.class);

        List<Class<?>> typeList = new ArrayList<>();
        ClassUtil.getGenericType(holder.genericReturnType, typeList, true);
        if(typeList.size() > 1 && !String.class.equals(typeList.get(1))) return null; 
        Class<?> type = typeList.size() > 1 ? typeList.get(2) : null;

        if(type == null || Object.class.equals(type)) { 
            String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
            Map<String, String> result = sqlParameterProcess(holder);
            return result.get("code") + "List<Record> result = " + dbHolder + result.get("audit") + ".find(sql, paramArray);\n" +
                    "returnObject = (" + holder.genericReturnType.getTypeName() + ")result.get(0).getColumns();\n";
        } else if (List.class.equals(type) && typeList.size() > 2) { 
            type = typeList.get(3);
            if(Map.class.equals(type) && typeList.size() == 6 && String.class.equals(typeList.get(4)) && Object.class.equals(typeList.get(5))) { 

                Class<?> sortType = StrUtil.notBlank(holder.sortColumn) ? holder.dao._getTable(true).getColumnTypeMap().get(holder.sortColumn) : null;
                String body = "Map<String, List<Map<String, Object>>> map = new LinkedHashMap<>();";
                if(sortType != null){
                    String reversed = (holder.sortBy.equals(Auto.Order.DESC)) ? ".reversed()" : "";
                    if(String.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(String::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Map<String, Object>> list = (List<Map<String, Object>>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record.getColumns());map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Integer.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Integer::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Map<String, Object>> list = (List<Map<String, Object>>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record.getColumns());map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Long.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Long::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Map<String, Object>> list = (List<Map<String, Object>>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record.getColumns());map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Float.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Float::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Map<String, Object>> list = (List<Map<String, Object>>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record.getColumns());map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Double.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Double::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Map<String, Object>> list = (List<Map<String, Object>>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record.getColumns());map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Boolean.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Boolean::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Map<String, Object>> list = (List<Map<String, Object>>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record.getColumns());map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(BigDecimal.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigDecimal::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Map<String, Object>> list = (List<Map<String, Object>>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record.getColumns());map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(BigInteger.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigInteger::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Map<String, Object>> list = (List<Map<String, Object>>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record.getColumns());map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Short.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Short::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Map<String, Object>> list = (List<Map<String, Object>>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record.getColumns());map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Byte.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Byte::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Map<String, Object>> list = (List<Map<String, Object>>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record.getColumns());map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(java.sql.Date.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Date::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Map<String, Object>> list = (List<Map<String, Object>>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record.getColumns());map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(java.sql.Time.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Time::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Map<String, Object>> list = (List<Map<String, Object>>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record.getColumns());map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(java.sql.Timestamp.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> (java.sql.Timestamp)((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Timestamp::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Map<String, Object>> list = (List<Map<String, Object>>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record.getColumns());map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    }
                } else {
                    body += "for (var record : result) {"+
                            "List<Map<String, Object>> list = (List<Map<String, Object>>)map.get(record.get(groupField, \"null\").toString());" +
                            "if (list == null) list = new ArrayList<>();" +
                            "list.add(record.getColumns());" +
                            "map.put(record.get(groupField, \"null\").toString(), list);" +
                            "}";
                }
                body += "returnObject = (" + holder.genericReturnType.getTypeName() + ")map;";

                String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
                Map<String, String> result = sqlParameterProcess(holder);
                return result.get("code") + "List<Record> result = " + dbHolder + result.get("audit") + ".find(sql, paramArray);\n" +
                        "returnObject = new HashMap<>();\n" +
                        "if (result.size() > 0) {\n" +
                        "String groupField = " + (StrUtil.notBlank(holder.groupColumn) ? "\"" + holder.groupColumn + "\";" : "result.get(0).getColumnNames()[0];") +
                        body +
                        "}";
            } else if (Record.class.equals(type) && typeList.size() == 4) { 

                Class<?> sortType = StrUtil.notBlank(holder.sortColumn) ? holder.dao._getTable(true).getColumnTypeMap().get(holder.sortColumn) : null;
                String body = "Map<String, List<Record>> map = new LinkedHashMap<>();";
                if(sortType != null){
                    String reversed = (holder.sortBy.equals(Auto.Order.DESC)) ? ".reversed()" : "";
                    if(String.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(String::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Record> list = (List<Record>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record);map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Integer.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Integer::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Record> list = (List<Record>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record);map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Long.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Long::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Record> list = (List<Record>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record);map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Float.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Float::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Record> list = (List<Record>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record);map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Double.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Double::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Record> list = (List<Record>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record);map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Boolean.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Boolean::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Record> list = (List<Record>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record);map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(BigDecimal.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigDecimal::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Record> list = (List<Record>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record);map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(BigInteger.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigInteger::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Record> list = (List<Record>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record);map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Short.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Short::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Record> list = (List<Record>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record);map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Byte.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Byte::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Record> list = (List<Record>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record);map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(java.sql.Date.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Date::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Record> list = (List<Record>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record);map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(java.sql.Time.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Time::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Record> list = (List<Record>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record);map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(java.sql.Timestamp.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(record -> (java.sql.Timestamp)((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Timestamp::compareTo))" + reversed + ")" +
                                ".peek(record -> {List<Record> list = (List<Record>)map.get(record.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(record);map.put(record.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    }
                } else {
                    body += "for (var record : result) {"+
                            "List<Record> list = (List<Record>)map.get(record.get(groupField, \"null\").toString());" +
                            "if (list == null) list = new ArrayList<>();" +
                            "list.add(record);" +
                            "map.put(record.get(groupField, \"null\").toString(), list);" +
                            "}";
                }
                body += "returnObject = (" + holder.genericReturnType.getTypeName() + ")map;";

                String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
                Map<String, String> result = sqlParameterProcess(holder);
                return result.get("code") + "List<Record> result = " + dbHolder + result.get("audit") + ".find(sql, paramArray);\n" +
                        "returnObject = new HashMap<>();\n" +
                        "if (result.size() > 0) {\n" +
                        "String groupField = " + (StrUtil.notBlank(holder.groupColumn) ? "\"" + holder.groupColumn + "\";" : "result.get(0).getColumnNames()[0];") +
                        body +
                        "}";
            } else if (Model.class.isAssignableFrom(type) && typeList.size() == 4) { 
                holder.addClassImport(Model.class);
                String modelGenericType = type.equals(Model.class) ? "? extends Model" : type.getTypeName();
                String modelType = type.getTypeName();
                Class<?> sortType = StrUtil.notBlank(holder.sortColumn) ? holder.dao._getTable(true).getColumnTypeMap().get(holder.sortColumn) : null;
                String body = holder.genericReturnType.getTypeName() + " map = new LinkedHashMap<>();";
                if(sortType != null){
                    String reversed = (holder.sortBy.equals(Auto.Order.DESC)) ? ".reversed()" : "";
                    if(String.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(model -> ((Model)model).getStr(\"" + holder.sortColumn + "\"), Comparator.nullsLast(String::compareTo))" + reversed + ")" +
                                ".peek(model -> {List<" + modelType + "> list = (List<" + modelType + ">)map.get(model.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(model);map.put(model.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Integer.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(model -> ((Model)model).getInt(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Integer::compareTo))" + reversed + ")" +
                                ".peek(model -> {List<" + modelType + "> list = (List<" + modelType + ">)map.get(model.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(model);map.put(model.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Long.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(model -> ((Model)model).getLong(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Long::compareTo))" + reversed + ")" +
                                ".peek(model -> {List<" + modelType + "> list = (List<" + modelType + ">)map.get(model.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(model);map.put(model.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Float.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(model -> ((Model)model).getFloat(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Float::compareTo))" + reversed + ")" +
                                ".peek(model -> {List<" + modelType + "> list = (List<" + modelType + ">)map.get(model.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(model);map.put(model.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Double.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(model -> ((Model)model).getDouble(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Double::compareTo))" + reversed + ")" +
                                ".peek(model -> {List<" + modelType + "> list = (List<" + modelType + ">)map.get(model.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(model);map.put(model.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Boolean.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(model -> ((Model)model).getBoolean(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Boolean::compareTo))" + reversed + ")" +
                                ".peek(model -> {List<" + modelType + "> list = (List<" + modelType + ">)map.get(model.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(model);map.put(model.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(BigDecimal.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(model -> ((Model)model).getBigDecimal(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigDecimal::compareTo))" + reversed + ")" +
                                ".peek(model -> {List<" + modelType + "> list = (List<" + modelType + ">)map.get(model.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(model);map.put(model.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(BigInteger.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(model -> ((Model)model).getBigInteger(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigInteger::compareTo))" + reversed + ")" +
                                ".peek(model -> {List<" + modelType + "> list = (List<" + modelType + ">)map.get(model.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(model);map.put(model.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Short.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(model -> ((Model)model).getShort(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Short::compareTo))" + reversed + ")" +
                                ".peek(model -> {List<" + modelType + "> list = (List<" + modelType + ">)map.get(model.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(model);map.put(model.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(Byte.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(model -> ((Model)model).getByte(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Byte::compareTo))" + reversed + ")" +
                                ".peek(model -> {List<" + modelType + "> list = (List<" + modelType + ">)map.get(model.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(model);map.put(model.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(java.sql.Date.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(model -> ((Model)model).getDate(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.util.Date::compareTo))" + reversed + ")" +
                                ".peek(model -> {List<" + modelType + "> list = (List<" + modelType + ">)map.get(model.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(model);map.put(model.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(java.sql.Time.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(model -> ((Model)model).getTime(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Time::compareTo))" + reversed + ")" +
                                ".peek(model -> {List<" + modelType + "> list = (List<" + modelType + ">)map.get(model.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(model);map.put(model.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    } else if(java.sql.Timestamp.class.equals(sortType)){
                        body += "result.stream().sorted(Comparator.comparing(model -> ((Model)model).getTimestamp(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Timestamp::compareTo))" + reversed + ")" +
                                ".peek(model -> {List<" + modelType + "> list = (List<" + modelType + ">)map.get(model.get(groupField, \"null\").toString());if (list == null) list = new ArrayList<>();list.add(model);map.put(model.get(groupField, \"null\").toString(), list);}).collect(Collectors.toList());";
                    }
                } else {
                    body += "for (var model : result) {"+
                            "List<" + modelType + "> list = (List<" + modelType + ">)map.get(model.get(groupField, \"null\").toString());" +
                            "if (list == null) list = new ArrayList<>();" +
                            "list.add(model);" +
                            "map.put(model.get(groupField, \"null\").toString(), list);" +
                            "}";
                }
                body += "returnObject = map;";

                String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "use(\""+holder.dataSource+"\")" : "dao";
                Map<String, String> result = sqlParameterProcess(holder);
                return result.get("code") + "List<"+modelGenericType+"> result = " + dbHolder + result.get("audit") + ".find(sql, paramArray);\n" +
                        "returnObject = new HashMap<>();\n" +
                        "if (result.size() > 0) {\n" +
                        "String groupField = " + (StrUtil.notBlank(holder.groupColumn) ? "\"" + holder.groupColumn + "\";" : "result.get(0)._getAttrNames()[0];") +
                        body +
                        "}";
            }
        }
        return null;
    }

    private String returnList(AutoCodeGenerator holder){
        holder.addClassImport(Comparator.class);
        holder.addClassImport(List.class);
        holder.addClassImport(Map.class);
        holder.addClassImport(LinkedList.class);
        holder.addClassImport(Collections.class);
        holder.addClassImport(Collectors.class);
        holder.addClassImport(Model.class);

        List<Class<?>> typeList = new ArrayList<>();
        ClassUtil.getGenericType(holder.genericReturnType, typeList, true);

        Class<?> type = typeList.size() > 1 ? typeList.get(1) : null;
        if(type == null || ClassUtil.isWrapped(type)) { 
            Class<?> sortType = StrUtil.notBlank(holder.sortColumn) ? holder.dao._getTable(true).getColumnTypeMap().get(holder.sortColumn) : null;
            String body = "List<?> list;";
            if(sortType != null){
                String reversed = (holder.sortBy.equals(Auto.Order.DESC)) ? ".reversed()" : "";
                if(String.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(String::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(LinkedList::new));";
                } else if(Integer.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Integer::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(LinkedList::new));";
                } else if(Long.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Long::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(LinkedList::new));";
                } else if(Float.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Float::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(LinkedList::new));";
                } else if(Double.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Double::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(LinkedList::new));";
                } else if(Boolean.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Boolean::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(LinkedList::new));";
                } else if(BigDecimal.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigDecimal::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(LinkedList::new));";
                } else if(BigInteger.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigInteger::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(LinkedList::new));";
                } else if(Short.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Short::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(LinkedList::new));";
                } else if(Byte.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Byte::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(LinkedList::new));";
                } else if(java.sql.Date.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Date::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(LinkedList::new));";
                } else if(java.sql.Time.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Time::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(LinkedList::new));";
                } else if(java.sql.Timestamp.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> (java.sql.Timestamp)((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Timestamp::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(LinkedList::new));";
                }

            } else {
                body = "List<?> list = new LinkedList<>();";
                body += "result.forEach(r -> list.add(r.get(column)));";
            }
            body += "returnObject = ("+ holder.genericReturnType.getTypeName() +")list;";

            String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
            Map<String, String> result = sqlParameterProcess(holder);
            return result.get("code") + "List<Record> result = " + dbHolder + result.get("audit") + ".find(sql, paramArray);\n" +
                    "returnObject = new LinkedList<>();\n" +
                    "if (result.size() > 0) {\n" +
                        "String column = result.get(0).getColumnNames()[0];" +
                        body +
                    "}";
        } else if (Map.class.equals(type) && typeList.size() == 4 && String.class.equals(typeList.get(2)) && Object.class.equals(typeList.get(3))) { 

            Class<?> sortType = StrUtil.notBlank(holder.sortColumn) ? holder.dao._getTable(true).getColumnTypeMap().get(holder.sortColumn) : null;
            String body = "List<Map<String, Object>> list;";
            if(sortType != null){
                String reversed = (holder.sortBy.equals(Auto.Order.DESC)) ? ".reversed()" : "";
                if(String.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(String::compareTo))" + reversed + ")" +
                            ".map(record -> record.getColumns()).collect(Collectors.toCollection(LinkedList::new));";
                } else if(Integer.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Integer::compareTo))" + reversed + ")" +
                            ".map(record -> record.getColumns()).collect(Collectors.toCollection(LinkedList::new));";
                } else if(Long.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Long::compareTo))" + reversed + ")" +
                            ".map(record -> record.getColumns()).collect(Collectors.toCollection(LinkedList::new));";
                } else if(Float.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Float::compareTo))" + reversed + ")" +
                            ".map(record -> record.getColumns()).collect(Collectors.toCollection(LinkedList::new));";
                } else if(Double.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Double::compareTo))" + reversed + ")" +
                            ".map(record -> record.getColumns()).collect(Collectors.toCollection(LinkedList::new));";
                } else if(Boolean.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Boolean::compareTo))" + reversed + ")" +
                            ".map(record -> record.getColumns()).collect(Collectors.toCollection(LinkedList::new));";
                } else if(BigDecimal.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigDecimal::compareTo))" + reversed + ")" +
                            ".map(record -> record.getColumns()).collect(Collectors.toCollection(LinkedList::new));";
                } else if(BigInteger.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigInteger::compareTo))" + reversed + ")" +
                            ".map(record -> record.getColumns()).collect(Collectors.toCollection(LinkedList::new));";
                } else if(Short.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Short::compareTo))" + reversed + ")" +
                            ".map(record -> record.getColumns()).collect(Collectors.toCollection(LinkedList::new));";
                } else if(Byte.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Byte::compareTo))" + reversed + ")" +
                            ".map(record -> record.getColumns()).collect(Collectors.toCollection(LinkedList::new));";
                } else if(java.sql.Date.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Date::compareTo))" + reversed + ")" +
                            ".map(record -> record.getColumns()).collect(Collectors.toCollection(LinkedList::new));";
                } else if(java.sql.Time.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Time::compareTo))" + reversed + ")" +
                            ".map(record -> record.getColumns()).collect(Collectors.toCollection(LinkedList::new));";
                } else if(java.sql.Timestamp.class.equals(sortType)){
                    body += "list = result.stream().sorted(Comparator.comparing(record -> (java.sql.Timestamp)((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Timestamp::compareTo))" + reversed + ")" +
                            ".map(record -> record.getColumns()).collect(Collectors.toCollection(LinkedList::new));";
                }

            } else {
                body = "List<Map<String, Object>> list = new LinkedList<>();";
                body += "result.forEach(r -> list.add(r.getColumns()));";
            }
            body += "returnObject = ("+ holder.genericReturnType.getTypeName() +")list;";

            String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
            Map<String, String> result = sqlParameterProcess(holder);
            return result.get("code") + "List<Record> result = " + dbHolder + result.get("audit") + ".find(sql, paramArray);\n" +
                    "returnObject = new LinkedList<>();\n" +
                    "if (result.size() > 0) {\n" +
                    body +
                    "}";
        } else if (Record.class.equals(type) && typeList.size() == 2) { 

            Class<?> sortType = StrUtil.notBlank(holder.sortColumn) ? holder.dao._getTable(true).getColumnTypeMap().get(holder.sortColumn) : null;
            String body = "";
            if(sortType != null){
                String reversed = (holder.sortBy.equals(Auto.Order.DESC)) ? ".reversed()" : "";
                if(String.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(String::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(Integer.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Integer::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(Long.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Long::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(Float.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Float::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(Double.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Double::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(Boolean.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Boolean::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(BigDecimal.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigDecimal::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(BigInteger.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigInteger::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(Short.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Short::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(Byte.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Byte::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(java.sql.Date.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Date::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(java.sql.Time.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Time::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(java.sql.Timestamp.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(record -> (java.sql.Timestamp)((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Timestamp::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                }

            }else {
                body = "returnObject = result;";
            }

            String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
            Map<String, String> result = sqlParameterProcess(holder);
            return result.get("code") + "List<Record> result = " + dbHolder + result.get("audit") + ".find(sql, paramArray);\n" +
                    "returnObject = new LinkedList<>();\n" +
                    "if (result.size() > 0) {\n" +
                    body +
                    "}";
        } else if (Model.class.isAssignableFrom(type) && typeList.size() == 2) { 

            Class<?> sortType = StrUtil.notBlank(holder.sortColumn) ? holder.dao._getTable(true).getColumnTypeMap().get(holder.sortColumn) : null;
            String body = "";
            if(sortType != null) {
                String reversed = (holder.sortBy.equals(Auto.Order.DESC)) ? ".reversed()" : "";
                if(String.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getStr(\"" + holder.sortColumn + "\"), Comparator.nullsLast(String::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(Integer.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getInt(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Integer::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(Long.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getLong(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Long::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(Float.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getFloat(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Float::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(Double.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getDouble(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Double::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(Boolean.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getBoolean(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Boolean::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(BigDecimal.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getBigDecimal(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigDecimal::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(BigInteger.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getBigInteger(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigInteger::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(Short.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getShort(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Short::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(Byte.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getByte(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Byte::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(java.sql.Date.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getDate(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.util.Date::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(java.sql.Time.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getTime(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Time::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                } else if(java.sql.Timestamp.class.equals(sortType)){
                    body = "returnObject = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getTimestamp(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Timestamp::compareTo))" + reversed + ")" +
                            ".collect(Collectors.toCollection(LinkedList::new));";
                }

            } else {
                body = "returnObject = result;";
            }

            String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "use(\""+holder.dataSource+"\")" : "dao";
            Map<String, String> result = sqlParameterProcess(holder);
            return result.get("code") + holder.genericReturnType.getTypeName() + " result = " + dbHolder + result.get("audit") + ".find(sql, paramArray);\n" +
                    "returnObject = new LinkedList<>();\n" +
                    "if (result.size() > 0) {\n" +
                    body +
                    "}";
        }
        return null;
    }

    private String returnSet(AutoCodeGenerator holder) {
        holder.addClassImport(Comparator.class);
        holder.addClassImport(Model.class);
        holder.addClassImport(Set.class);
        holder.addClassImport(LinkedHashSet.class);
        holder.addClassImport(TreeSet.class);
        holder.addClassImport(List.class);
        holder.addClassImport(ArrayList.class);
        holder.addClassImport(LinkedList.class);
        holder.addClassImport(Collections.class);
        holder.addClassImport(Collectors.class);

        List<Class<?>> typeList = new ArrayList<>();
        ClassUtil.getGenericType(holder.genericReturnType, typeList, true);

        Class<?> type = typeList.size() > 1 ? typeList.get(1) : null;
        if (type == null || ClassUtil.isWrapped(type)) { 
            Class<?> sortType = StrUtil.notBlank(holder.sortColumn) ? holder.dao._getTable(true).getColumnTypeMap().get(holder.sortColumn) : null;
            String body = "TreeSet<?> set;";
            if(sortType != null){
                String reversed = (holder.sortBy.equals(Auto.Order.DESC)) ? ".reversed()" : "";
                if(String.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(String::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(TreeSet::new));";
                } else if(Integer.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Integer::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(TreeSet::new));";
                } else if(Long.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Long::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(TreeSet::new));";
                } else if(Float.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Float::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(TreeSet::new));";
                } else if(Double.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Double::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(TreeSet::new));";
                } else if(Boolean.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Boolean::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(TreeSet::new));";
                } else if(BigDecimal.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigDecimal::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(TreeSet::new));";
                } else if(BigInteger.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigInteger::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(TreeSet::new));";
                } else if(Short.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Short::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(TreeSet::new));";
                } else if(Byte.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Byte::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(TreeSet::new));";
                } else if(java.sql.Date.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Date::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(TreeSet::new));";
                } else if(java.sql.Time.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Time::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(TreeSet::new));";
                } else if(java.sql.Timestamp.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> (java.sql.Timestamp)((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Timestamp::compareTo))" + reversed + ")" +
                            ".map(record -> record.get(column)).collect(Collectors.toCollection(TreeSet::new));";
                }

            }else {
                body = "Set<?> set = new LinkedHashSet<>();";
                body += "result.forEach(r -> set.add(r.get(column)));";
            }
            body += "returnObject = (" + holder.genericReturnType.getTypeName() + ")set;";

            String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
            Map<String, String> result = sqlParameterProcess(holder);
            return result.get("code") + "List<Record> result = " + dbHolder + result.get("audit") + ".find(sql, paramArray);\n" +
                    "returnObject = new LinkedHashSet<>();\n" +
                    "if (result.size() > 0) {\n" +
                    "String column = result.get(0).getColumnNames()[0];" +
                    body +
                    "}";
        } else if (Map.class.equals(type) && typeList.size() == 4 && String.class.equals(typeList.get(2)) && Object.class.equals(typeList.get(3))) { 
            Class<?> sortType = StrUtil.notBlank(holder.sortColumn) ? holder.dao._getTable(true).getColumnTypeMap().get(holder.sortColumn) : null;
            String body = "Set<Map<String, Object>> set;";
            if(sortType != null){
                String reversed = (holder.sortBy.equals(Auto.Order.DESC)) ? ".reversed()" : "";
                if(String.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(String::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record.getColumns();}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Integer.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Integer::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record.getColumns();}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Long.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Long::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record.getColumns();}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Float.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Float::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record.getColumns();}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Double.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Double::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record.getColumns();}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Boolean.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Boolean::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record.getColumns();}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(BigDecimal.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigDecimal::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record.getColumns();}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(BigInteger.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigInteger::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record.getColumns();}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Short.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Short::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record.getColumns();}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Byte.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Byte::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record.getColumns();}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(java.sql.Date.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Date::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record.getColumns();}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(java.sql.Time.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Time::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record.getColumns();}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(java.sql.Timestamp.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> (java.sql.Timestamp)((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Timestamp::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record.getColumns();}).collect(Collectors.toCollection(LinkedHashSet::new));";
                }

                body += "returnObject = (" + holder.genericReturnType.getTypeName() + ")set;";
            } else {
                body += "for (var record : result) {"+
                            "if (!unique.contains(record.get(groupField, \"null\"))) {" +
                                "returnObject.add(record.getColumns());" +
                                "unique.add(record.get(groupField, \"null\"));" +
                            "}" +
                        "}";
            }

            String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
            Map<String, String> result = sqlParameterProcess(holder);
            return result.get("code") + "List<Record> result = " + dbHolder + result.get("audit") + ".find(sql, paramArray);\n" +
                    "returnObject = new LinkedHashSet<>();\n" +
                    "if (result.size() > 0) {\n" +
                    "String groupField = " + (StrUtil.notBlank(holder.groupColumn) ? "\"" + holder.groupColumn + "\";" : "result.get(0).getColumnNames()[0];") +
                    "List<Object> unique = new ArrayList<>();" +
                    body +
                    "}";
        } else if (Record.class.equals(type) && typeList.size() == 2) {  
            Class<?> sortType = StrUtil.notBlank(holder.sortColumn) ? holder.dao._getTable(true).getColumnTypeMap().get(holder.sortColumn) : null;
            String body = "Set<Record> set;";
            if(sortType != null){
                String reversed = (holder.sortBy.equals(Auto.Order.DESC)) ? ".reversed()" : "";
                if(String.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(String::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Integer.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Integer::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Long.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Long::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Float.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Float::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Double.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Double::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Boolean.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Boolean::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(BigDecimal.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigDecimal::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(BigInteger.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigInteger::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Short.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Short::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Byte.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Byte::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(java.sql.Date.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Date::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(java.sql.Time.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> ((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Time::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(java.sql.Timestamp.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(record -> (java.sql.Timestamp)((Record)record).get(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Timestamp::compareTo))" + reversed + ")" +
                            ".filter(record -> !unique.contains(record.get(groupField, \"null\"))).map(record -> {unique.add(record.get(groupField, \"null\"));return record;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                }

                body += "returnObject = (" + holder.genericReturnType.getTypeName() + ")set;";
            } else {
                body += "for (var record : result) {"+
                        "if (!unique.contains(record.get(groupField, \"null\"))) {" +
                        "returnObject.add(record);" +
                        "unique.add(record.get(groupField, \"null\"));" +
                        "}" +
                        "}";
            }

            String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "db.use(\""+holder.dataSource+"\")" : "db";
            Map<String, String> result = sqlParameterProcess(holder);
            return result.get("code") + "List<Record> result = " + dbHolder + result.get("audit") + ".find(sql, paramArray);\n" +
                    "returnObject = new LinkedHashSet<>();\n" +
                    "if (result.size() > 0) {\n" +
                    "String groupField = " + (StrUtil.notBlank(holder.groupColumn) ? "\"" + holder.groupColumn + "\";" : "result.get(0).getColumnNames()[0];") +
                    "List<Object> unique = new ArrayList<>();" +
                    body +
                    "}";
        } else if (Model.class.isAssignableFrom(type) && typeList.size() == 2) { 
            String modelGenericType = type.equals(Model.class) ? "? extends Model" : type.getTypeName();
            Class<?> sortType = StrUtil.notBlank(holder.sortColumn) ? holder.dao._getTable(true).getColumnTypeMap().get(holder.sortColumn) : null;
            String body = holder.genericReturnType.getTypeName() + " set;";
            if(sortType != null){
                String reversed = (holder.sortBy.equals(Auto.Order.DESC)) ? ".reversed()" : "";
                if(String.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getStr(\"" + holder.sortColumn + "\"), Comparator.nullsLast(String::compareTo))" + reversed + ")" +
                            ".filter(model -> !unique.contains(model.get(groupField, \"null\"))).map(model -> {unique.add(model.get(groupField, \"null\"));return model;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Integer.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getInt(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Integer::compareTo))" + reversed + ")" +
                            ".filter(model -> !unique.contains(model.get(groupField, \"null\"))).map(model -> {unique.add(model.get(groupField, \"null\"));return model;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Long.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getLong(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Long::compareTo))" + reversed + ")" +
                            ".filter(model -> !unique.contains(model.get(groupField, \"null\"))).map(model -> {unique.add(model.get(groupField, \"null\"));return model;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Float.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getFloat(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Float::compareTo))" + reversed + ")" +
                            ".filter(model -> !unique.contains(model.get(groupField, \"null\"))).map(model -> {unique.add(model.get(groupField, \"null\"));return model;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Double.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getDouble(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Double::compareTo))" + reversed + ")" +
                            ".filter(model -> !unique.contains(model.get(groupField, \"null\"))).map(model -> {unique.add(model.get(groupField, \"null\"));return model;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Boolean.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getBoolean(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Boolean::compareTo))" + reversed + ")" +
                            ".filter(model -> !unique.contains(model.get(groupField, \"null\"))).map(model -> {unique.add(model.get(groupField, \"null\"));return model;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(BigDecimal.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getBigDecimal(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigDecimal::compareTo))" + reversed + ")" +
                            ".filter(model -> !unique.contains(model.get(groupField, \"null\"))).map(model -> {unique.add(model.get(groupField, \"null\"));return model;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(BigInteger.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getBigInteger(\"" + holder.sortColumn + "\"), Comparator.nullsLast(BigInteger::compareTo))" + reversed + ")" +
                            ".filter(model -> !unique.contains(model.get(groupField, \"null\"))).map(model -> {unique.add(model.get(groupField, \"null\"));return model;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Short.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getShort(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Short::compareTo))" + reversed + ")" +
                            ".filter(model -> !unique.contains(model.get(groupField, \"null\"))).map(model -> {unique.add(model.get(groupField, \"null\"));return model;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(Byte.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getByte(\"" + holder.sortColumn + "\"), Comparator.nullsLast(Byte::compareTo))" + reversed + ")" +
                            ".filter(model -> !unique.contains(model.get(groupField, \"null\"))).map(model -> {unique.add(model.get(groupField, \"null\"));return model;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(java.sql.Date.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getDate(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.util.Date::compareTo))" + reversed + ")" +
                            ".filter(model -> !unique.contains(model.get(groupField, \"null\"))).map(model -> {unique.add(model.get(groupField, \"null\"));return model;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(java.sql.Time.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getTime(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Time::compareTo))" + reversed + ")" +
                            ".filter(model -> !unique.contains(model.get(groupField, \"null\"))).map(model -> {unique.add(model.get(groupField, \"null\"));return model;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                } else if(java.sql.Timestamp.class.equals(sortType)){
                    body += "set = result.stream().sorted(Comparator.comparing(model -> ((Model)model).getTimestamp(\"" + holder.sortColumn + "\"), Comparator.nullsLast(java.sql.Timestamp::compareTo))" + reversed + ")" +
                            ".filter(model -> !unique.contains(model.get(groupField, \"null\"))).map(model -> {unique.add(model.get(groupField, \"null\"));return model;}).collect(Collectors.toCollection(LinkedHashSet::new));";
                }

                body += "returnObject = set;";
            } else {
                body += "for (var model : result) {"+
                        "if (!unique.contains(model.get(groupField, \"null\"))) {" +
                        "returnObject.add(model);" +
                        "unique.add(model.get(groupField, \"null\"));" +
                        "}" +
                        "}";
            }

            String dbHolder = (StrUtil.notBlank(holder.dataSource)) ? "use(\""+holder.dataSource+"\")" : "dao";
            Map<String, String> result = sqlParameterProcess(holder);
            return result.get("code") + "List<" + modelGenericType + "> result = " + dbHolder + result.get("audit") + ".find(sql, paramArray);\n" +
                    "returnObject = new LinkedHashSet<>();\n" +
                    "if (result.size() > 0) {\n" +
                    "String groupField = " + (StrUtil.notBlank(holder.groupColumn) ? "\"" + holder.groupColumn + "\";" : "result.get(0)._getAttrNames()[0];") +
                    "List<Object> unique = new ArrayList<>();" +
                    body +
                    "}";
        }
        return null;
    }

}
