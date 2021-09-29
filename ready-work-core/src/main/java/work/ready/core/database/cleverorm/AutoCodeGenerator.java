/**
 *
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http:www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package work.ready.core.database.cleverorm;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.MultiExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import work.ready.core.component.proxy.CodeGenerator;
import work.ready.core.config.ConfigInjector;
import work.ready.core.database.DatabaseManager;
import work.ready.core.database.Model;
import work.ready.core.database.annotation.Auto;
import work.ready.core.database.ModelService;
import work.ready.core.server.Ready;
import work.ready.core.template.TemplateException;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.StrUtil;
import work.ready.core.tools.define.BiTuple;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoCodeGenerator implements CodeGenerator {
    protected final DatabaseManager dbManager;
    protected final Map<DmlSqlType, Class<? extends SqlSupporter>> commandSupporter = new HashMap<>();

    protected Parameter[] parameters;
    protected Auto annotation;
    protected String sqlOrId;
    protected String realSql;
    protected boolean isMagicMethod = false;
    protected Integer topN;
    protected String sortColumn;
    protected Auto.Order sortBy;
    protected Auto.Order orderBy;
    protected int status;
    protected String groupColumn;
    protected Class<?> returnType;
    protected Type genericReturnType;
    protected Class<? extends ModelService<?>> modelServiceClass;
    protected ModelService<? extends Model> modelService;
    protected Model<? extends Model> dao;
    protected String dataSource;
    protected Map<String, Class<?>> columnTypeMap;
    protected Method method;

    protected Select selectStatement;
    protected Update updateStatement;
    protected Insert insertStatement;
    protected Delete deleteStatement;
    protected FromItem from;
    protected List<String> tables;
    protected List<SelectItem> selectItems;
    protected List<Join> joins;
    protected Expression where;
    protected Limit limit;
    protected Top top;
    protected Offset offset;
    protected GroupByElement groupByElement;
    protected List<OrderByElement> orderByElements;

    protected Distinct distinct;
    protected Table forUpdateTable;

    protected List<Column> fields;
    protected List<Expression> values;
    protected List<ExpressionList> valuesList;

    protected Map<String, BiTuple<String, String>> autoCoderSchemaConfig;
    protected Map<String, Object> clazzData;
    protected Map<String, Object> methodData;

    protected String replace; 
    protected String insert; 
    protected String append; 

    public AutoCodeGenerator(DatabaseManager dbManager){
        this.dbManager = dbManager;
        
        commandSupporter.put(DmlSqlType.SELECT, SelectSupporter.class);
        commandSupporter.put(DmlSqlType.INSERT, InsertSupporter.class);
        commandSupporter.put(DmlSqlType.UPDATE, UpdateSupporter.class);
        commandSupporter.put(DmlSqlType.DELETE, DeleteSupporter.class);
    }

    @Override
    public AutoCodeGenerator generatorCode(Class<?> target, Method method, Map<String, Object> clazzData, Map<String, Object> methodData){
        AutoCodeGenerator thisGenerator = new AutoCodeGenerator(dbManager); 
        thisGenerator.method = method;
        thisGenerator.modelServiceClass = (Class<? extends ModelService<?>>)target;
        thisGenerator.autoCoderSchemaConfig = autoCoderSchemaConfig;

        try {
            if(clazzData.containsKey(thisGenerator.modelServiceClass.getCanonicalName())){
                thisGenerator.modelService = (ModelService<? extends Model>)clazzData.get(thisGenerator.modelServiceClass.getCanonicalName());
            } else {
                thisGenerator.modelService = thisGenerator.modelServiceClass.getConstructor().newInstance();
                clazzData.put(thisGenerator.modelServiceClass.getCanonicalName(), thisGenerator.modelService);
            }
            thisGenerator.dao = thisGenerator.modelService.getDao();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e){
            throw new RuntimeException("Couldn't initialize model service " + thisGenerator.modelServiceClass.getCanonicalName() + ": ", e);
        }

        thisGenerator.clazzData = clazzData;
        thisGenerator.methodData = methodData;

        thisGenerator.annotation = thisGenerator.method.getAnnotation(Auto.class);
        thisGenerator.dataSource = ConfigInjector.getStringValue(thisGenerator.annotation.db());
        if(StrUtil.isBlank(thisGenerator.dataSource)){
            thisGenerator.dataSource = dbManager.getConfig(thisGenerator.dao.getClass()).getName();
        } else {
            thisGenerator.dao = thisGenerator.modelService.use(thisGenerator.dataSource);
        }
        thisGenerator.sqlOrId = ConfigInjector.getStringValue(thisGenerator.annotation.value().trim());
        thisGenerator.sortColumn = ConfigInjector.getStringValue(thisGenerator.annotation.sortColumn());
        thisGenerator.sortBy = thisGenerator.annotation.sortBy();
        thisGenerator.orderBy = thisGenerator.annotation.orderBy();
        thisGenerator.status = thisGenerator.annotation.status();
        thisGenerator.groupColumn = ConfigInjector.getStringValue(thisGenerator.annotation.groupColumn());
        thisGenerator.parameters = thisGenerator.method.getParameters();
        thisGenerator.returnType = thisGenerator.method.getReturnType();
        thisGenerator.genericReturnType = thisGenerator.method.getGenericReturnType();
        thisGenerator.start();

        return thisGenerator;
    }

    private void start() {
        
        BiTuple<String, String> config = null;
        if(autoCoderSchemaConfig != null){
            config = autoCoderSchemaConfig.get(modelServiceClass.getCanonicalName() + "_" + ClassUtil.getMethodSignature(method, false));
        }
        if(config != null){
            if(StrUtil.notBlank(config.get1()))
                dataSource = config.get1();
            sqlOrId = config.get2();
        }

        if(StrUtil.isBlank(sqlOrId)) {
            sqlOrId = methodNameParser();
        }
        if(StrUtil.isBlank(sqlOrId)){
            throw new RuntimeException("CleverORM: sql command or sqlId is empty on " + modelServiceClass.getCanonicalName() + "." + method.getName() + "()");
        }
        String sqlOrSqlId = sqlOrId;
        int firstSpace = sqlOrSqlId.trim().indexOf(" "); 
        if(firstSpace <= 0) {
            try {
                String sql = StrUtil.isBlank(dataSource) ? dbManager.getDb().getSql(sqlOrSqlId) : dbManager.getDb().use(dataSource).getSql(sqlOrSqlId);
                if (StrUtil.notBlank(sql)) {
                    sqlOrSqlId = sql.trim();
                } else throw new RuntimeException("CleverORM: unknown sql command or sqlId '" + sqlOrSqlId + "' on " + modelServiceClass.getCanonicalName() + "." + method.getName() + "()");
            } catch (TemplateException e) {
                if(e.getMessage().contains("#param")){
                    throw new RuntimeException(Auto.class.getSimpleName() + " annotation doesn't support sql template with #param notation" + " on " + modelServiceClass.getCanonicalName() + "." + method.getName() + "(). " + e.getMessage());
                } else {
                    throw e;
                }
            }
        }

        Class<? extends SqlSupporter> supporterClazz = parseSql(sqlOrSqlId);

        SqlSupporter supporter = Ready.beanManager().get(supporterClazz);
        realSql = supporter.syntaxCheck(this, sqlOrSqlId);
        if(realSql != null){
            Class<? extends SqlSupporter> clazz = parseSql(realSql);
            if(!clazz.equals(supporter.getClass())) throw new RuntimeException("CleverORM: Sql has been functionally changed during syntax check process");

            replace = generate(supporter);
        }
    }

    private static Pattern getMethodNamePattern;  
    private String methodNameParser(){
        String findPattern = "get(All|First|Top(\\d+)|Count|Max|Sum|Min|Avg|((?!By|OrderBy|GroupBy).)+)(By(((?!OrderBy|GroupBy).)+))?((OrderBy|GroupBy)([A-Z][a-zA-Z0-9]+))?";
        String updatePattern = "updateBy([A-Z][A-Za-z0-9]+)";
        String deletePattern = "deleteBy([A-Z][A-Za-z0-9]+)";
        if(method.getName().startsWith("get")) {
            if(getMethodNamePattern == null) getMethodNamePattern = Pattern.compile(findPattern);
            Matcher matcher = getMethodNamePattern.matcher(method.getName());
            if(matcher.find() && matcher.group().equals(method.getName())) {
                
                if(Character.isUpperCase(matcher.group(1).charAt(0))){
                    if((StrUtil.isBlank(matcher.group(5)) || Character.isUpperCase(matcher.group(5).charAt(0))) &&
                       (StrUtil.isBlank(matcher.group(9)) || Character.isUpperCase(matcher.group(9).charAt(0)))
                    ) {
                        return parseGetMethod(matcher);
                    }
                }
            }
        } else
        if(method.getName().startsWith("updateBy")){
            Pattern regex = Pattern.compile(updatePattern);
            Matcher matcher = regex.matcher(method.getName());
            if(matcher.find() && matcher.group().equals(method.getName()))
                return parseUpdateMethod(matcher);
        } else
        if(method.getName().startsWith("deleteBy")) {
            Pattern regex = Pattern.compile(deletePattern);
            Matcher matcher = regex.matcher(method.getName());
            if(matcher.find() && matcher.group().equals(method.getName()))
                return parseDeleteMethod(matcher);
        }
        return null;
    }

    private String parseGetMethod(Matcher matcher){
        String scope = matcher.group(1);
        String topN = matcher.group(2); 
        if(StrUtil.notBlank(topN)) scope = "Top"; 
        String field = null;
        String byColumn = null;
        String orderByColumn = null;
        String groupByColumn = null;
        String[] functions = new String[]{"Count","Max","Sum","Min","Avg"};
        String[] scopes = new String[]{"All","First","Top"};
        if(!Arrays.asList(functions).contains(scope) && !Arrays.asList(scopes).contains(scope))
            field = StrUtil.firstCharToLowerCase(scope);
        if(StrUtil.notBlank(matcher.group(5))) byColumn = StrUtil.firstCharToLowerCase(matcher.group(5));
        if(StrUtil.notBlank(matcher.group(8))){
            if("OrderBy".equals(matcher.group(8))) orderByColumn = StrUtil.firstCharToLowerCase(matcher.group(9));
            if("GroupBy".equals(matcher.group(8))) groupByColumn = StrUtil.firstCharToLowerCase(matcher.group(9));
        }

        if(StrUtil.notBlank(byColumn)) {
            String realColumn = resolveColumnName(byColumn);
            if(realColumn == null)
                throw new RuntimeException("CleverORM: Wrong method name, field " + byColumn + " doesn't exist in table " + dao._getTableName() + ", on " + modelServiceClass.getCanonicalName() + "." + method.getName() + "()");
            else byColumn = realColumn;
        }
        if(StrUtil.notBlank(field)) {
            String realField = resolveColumnName(field);
            if(realField == null)
                throw new RuntimeException("CleverORM: Wrong method name, field " + field + " doesn't exist in table " + dao._getTableName() + ", on " + modelServiceClass.getCanonicalName() + "." + method.getName() + "()");
            else field = realField;
        }

        if(Arrays.asList(functions).contains(scope)) {
            if(StrUtil.isBlank(byColumn) && !scope.equals("Count")){ 
                throw new RuntimeException("CleverORM: Incompatible method type, method name should have 'By' column to indicate target, on " + modelServiceClass.getCanonicalName() + "." + method.getName() + "()");
            }
            if(StrUtil.isBlank(groupByColumn)) {
                if (!returnType.equals(int.class) && !returnType.equals(long.class) && !returnType.equals(Integer.class) && !returnType.equals(Long.class) &&
                        !returnType.equals(float.class) && !returnType.equals(double.class) && !returnType.equals(Float.class) && !returnType.equals(Double.class)) {
                    throw new RuntimeException("CleverORM: Incompatible return type, only int,long,float,double types are allowed for numeric type of query: " + returnType + ", on " + modelServiceClass.getCanonicalName() + "." + method.getName() + "()");
                }
            } else {
                if (!returnType.equals(List.class)) {
                    throw new RuntimeException("CleverORM: Incompatible return type, only List<Map> and List<Record> types are allowed for numeric type of query with groupBy : " + returnType + ", on " + modelServiceClass.getCanonicalName() + "." + method.getName() + "()");
                }
            }
            if(parameters.length > 0)
                throw new RuntimeException("CleverORM: Incompatible method parameters, functional methods shouldn't have parameters: " + Arrays.asList(parameters) + ", on " + modelServiceClass.getCanonicalName() + "." + method.getName() + "()");
        } else if(StrUtil.notBlank(byColumn)) {
            if(parameters.length != 1)
                throw new RuntimeException("CleverORM: Incompatible method parameters, should have exactly one parameter for " + byColumn + ": " + Arrays.asList(parameters) + ", on " + modelServiceClass.getCanonicalName() + "." + method.getName() + "()");
            Class<?> parameterType = parameters[0].getType();
            boolean validParameterType = ClassUtil.isSimpleType(parameterType);
            if(parameterType.getComponentType() != null) { 
                parameterType = parameterType.getComponentType();
                validParameterType = ClassUtil.isSimpleType(parameterType);
            } else
            if(parameterType.equals(List.class)) { 
                parameterType = ClassUtil.getGenericType(parameters[0].getParameterizedType());
                validParameterType = ClassUtil.isSimpleType(parameterType);
            }
            if(!validParameterType){
                throw new RuntimeException("CleverORM: Incompatible method parameter type, parameter can be Basic type, Array, List for " + byColumn + ": " + Arrays.asList(parameters) + ", on " + modelServiceClass.getCanonicalName() + "." + method.getName() + "()");
            }
        }

        if(StrUtil.notBlank(orderByColumn)) {
            orderByColumn = resolveColumnName(orderByColumn);
        }
        if(StrUtil.notBlank(groupByColumn)) {
            groupByColumn = resolveColumnName(groupByColumn);
        }

        String sql = " FROM _TABLE_ ";
        if(dao._getTable(true).hasColumnLabel(dbManager.getModelConfig().getStatusColumn())){
            if(status == Auto.Status.VALID){
                sql += " WHERE " + dbManager.getModelConfig().getStatusColumn() + " > " + dbManager.getModelConfig().getInvalidStatusUpperBound();
            } else if(status == Auto.Status.INVALID){
                sql += " WHERE " + dbManager.getModelConfig().getStatusColumn() + " <= " + dbManager.getModelConfig().getInvalidStatusUpperBound();
            } else if(status != Auto.Status.ALL) {
                sql += " WHERE " + dbManager.getModelConfig().getStatusColumn() + " = " + status;
            }
        }
        if(StrUtil.notBlank(byColumn) && !Arrays.asList(functions).contains(scope)) {
            sql += !sql.contains(" WHERE ") ? " WHERE" : " AND";
            Class<?> parameterType = parameters[0].getType();
            if(parameterType.getComponentType() != null){ 
                sql += " " + byColumn + " IN ( ? ) ";
            } else
            if(parameterType.equals(List.class)) { 
                sql += " " + byColumn + " IN ( ? ) ";
            } else {
                sql += " " + byColumn + " = ? ";
            }
        }
        if(StrUtil.notBlank(orderByColumn)) {
            sql += " ORDER BY " + orderByColumn;
            sql += (Auto.Order.ASC.equals(orderBy)) ? " ASC" : " DESC";
        }
        String groupColumn = "";
        if(StrUtil.notBlank(groupByColumn) && Arrays.asList(functions).contains(scope)) {
            sql += " GROUP BY " + groupByColumn;
            groupColumn = groupByColumn + ", ";
        }
        switch (scope){
            case "All" :
                sql = "SELECT *" + sql;
                break;
            case "Top" :
                this.topN = Integer.parseInt(topN);
                sql = "SELECT *" + sql;
                break;
            case "First" :
                this.topN = 1;
                sql = "SELECT *" + sql;
                break;
            case "Count" :
                sql = byColumn != null ? "SELECT " + groupColumn + "count(" + byColumn + ") as count" + sql : "SELECT " + groupColumn + "count(1) as count" + sql;
                break;
            case "Max" :
                sql = "SELECT " + groupColumn + "max(" + byColumn + ") as max" + sql;
                break;
            case "Sum" :
                sql = "SELECT " + groupColumn + "sum(" + byColumn + ") as sum" + sql;
                break;
            case "Min" :
                sql = "SELECT " + groupColumn + "min(" + byColumn + ") as min" + sql;
                break;
            case "Avg" :
                sql = "SELECT " + groupColumn + "avg(" + byColumn + ") as avg" + sql;
                break;
            default:
                sql = "SELECT " + field + sql;
                break;
        }

        isMagicMethod = true;
        return sql;
    }

    private String resolveColumnName(String name){
        if(dao._getTable(true).getColumnNameSet().contains(name)) return name;
        for(String field : dao._getTable(true).getColumnNameSet()){
            if(field.contains("_")){
                if(StrUtil.toCamelCase(field).equalsIgnoreCase(name)){
                    return field;
                }
            } else if(field.equalsIgnoreCase(name)){
                return field;
            }
        }
        return null;
    }

    private String parseUpdateMethod(Matcher matcher){
        String column = StrUtil.firstCharToLowerCase(matcher.group(1));
        String realColumn = resolveColumnName(column);
        if(realColumn == null)
            throw new RuntimeException("CleverORM: Wrong method name, field " + column + " doesn't exist in table " + dao._getTableName() + ", on " + modelServiceClass.getCanonicalName() + "." + method.getName() + "()");
        else column = realColumn;

        boolean validParameterType;
        if(parameters.length == 2){
            Class<?> parameterType = parameters[0].getType();
            validParameterType = ClassUtil.isSimpleType(parameterType);
            if(parameterType.getComponentType() != null) { 
                parameterType = parameterType.getComponentType();
                validParameterType = ClassUtil.isSimpleType(parameterType);
            } else
            if(parameterType.equals(List.class)) { 
                parameterType = ClassUtil.getGenericType(parameters[0].getParameterizedType());
                validParameterType = ClassUtil.isSimpleType(parameterType);
            }
            if(!Map.class.equals(parameters[1].getType())) validParameterType = false;
        } else {
            validParameterType = false;
        }
        if(!validParameterType)
            throw new RuntimeException("CleverORM: Incompatible update method parameters, should have one [Basic type, Array, List] parameter as condition for column " + column + " and one Map type for update fields: " + Arrays.asList(parameters) + ", on " + modelServiceClass.getCanonicalName() + "." + method.getName() + "()");

        String sql = "UPDATE _TABLE_ set placeHolder = 1 WHERE";
        if(dao._getTable(true).hasColumnLabel(dbManager.getModelConfig().getStatusColumn())){
            if(status == Auto.Status.VALID){
                sql += " " + dbManager.getModelConfig().getStatusColumn() + " > " + dbManager.getModelConfig().getInvalidStatusUpperBound() + " ";
            } else if(status == Auto.Status.INVALID){
                sql += " " + dbManager.getModelConfig().getStatusColumn() + " <= " + dbManager.getModelConfig().getInvalidStatusUpperBound() + " ";
            } else if(status != Auto.Status.ALL) {
                sql += " " + dbManager.getModelConfig().getStatusColumn() + " = " + status + " ";
            }
        }
        Class<?> parameterType = parameters[0].getType();
        sql += sql.endsWith("WHERE") ? "" : " AND ";
        if(parameterType.getComponentType() != null){ 
            sql += " " + column + " IN ( ? ) ";
        } else
        if(parameterType.equals(List.class)) { 
            sql += " " + column + " IN ( ? ) ";
        } else {
            sql += " " + column + " = ? ";
        }
        isMagicMethod = true;
        return sql;
    }

    private String parseDeleteMethod(Matcher matcher){
        String column = StrUtil.firstCharToLowerCase(matcher.group(1));
        String realColumn = resolveColumnName(column);
        if(realColumn == null)
            throw new RuntimeException("CleverORM: Wrong method name, field " + column + " doesn't exist in table " + dao._getTableName() + ", on " + modelServiceClass.getCanonicalName() + "." + method.getName() + "()");
        else column = realColumn;

        boolean validParameterType;
        if(parameters.length == 1){
            Class<?> parameterType = parameters[0].getType();
            validParameterType = ClassUtil.isSimpleType(parameterType);
            if(parameterType.getComponentType() != null) { 
                parameterType = parameterType.getComponentType();
                validParameterType = ClassUtil.isSimpleType(parameterType);
            } else
            if(parameterType.equals(List.class)) { 
                parameterType = ClassUtil.getGenericType(parameters[0].getParameterizedType());
                validParameterType = ClassUtil.isSimpleType(parameterType);
            }
        } else {
            validParameterType = false;
        }
        if(!validParameterType)
            throw new RuntimeException("CleverORM: Incompatible delete method parameters, should have exactly one [Basic type, Array, List] parameter for " + column + ": " + Arrays.asList(parameters) + ", on " + modelServiceClass.getCanonicalName() + "." + method.getName() + "()");
        String sql = "DELETE FROM _TABLE_ WHERE";
        if(dao._getTable(true).hasColumnLabel(dbManager.getModelConfig().getStatusColumn())){
            if(status == Auto.Status.VALID){
                sql += " " + dbManager.getModelConfig().getStatusColumn() + " > " + dbManager.getModelConfig().getInvalidStatusUpperBound() + " ";
            } else if(status == Auto.Status.INVALID){
                sql += " " + dbManager.getModelConfig().getStatusColumn() + " <= " + dbManager.getModelConfig().getInvalidStatusUpperBound() + " ";
            } else if(status != Auto.Status.ALL) {
                sql += " " + dbManager.getModelConfig().getStatusColumn() + " = " + status + " ";
            }
        }
        Class<?> parameterType = parameters[0].getType();
        sql += sql.endsWith("WHERE") ? "" : " AND ";
        if(parameterType.getComponentType() != null){ 
            sql += " " + column + " IN ( ? ) ";
        } else
        if(parameterType.equals(List.class)) { 
            sql += " " + column + " IN ( ? ) ";
        } else {
            sql += " " + column + " = ? ";
        }
        isMagicMethod = true;
        return sql;
    }

    private Class<? extends SqlSupporter> parseSql(String sql) {
        try {
            Statement statement = dbManager.sqlParser(sql, true);

            selectStatement = null; insertStatement = null; deleteStatement = null; updateStatement = null;
            selectItems = null; from = null; joins = null; where = null; groupByElement = null;
            orderByElements = null; limit = null; top = null; offset = null; forUpdateTable = null;
            distinct = null; tables = null; fields = null; values = null; valuesList = null;

            if (statement instanceof Select) {
                Select select = (Select) statement;
                selectStatement = parseSelect(select, sql);
            } else if (statement instanceof Insert) {
                Insert insert = (Insert) statement;
                insertStatement = parseInsert(insert, sql);
            } else if (statement instanceof Delete) {
                Delete delete = (Delete) statement;
                deleteStatement = parseDelete(delete, sql);
            } else if (statement instanceof Update) {
                Update update = (Update) statement;
                updateStatement = parseUpdate(update, sql);

            } else {
                throw new RuntimeException("CleverORM: unsupported sql command: " + sql);
            }
            String command = statement.getClass().getTypeName().split("\\.")[4].toUpperCase();
            Class<? extends SqlSupporter> supporter = commandSupporter.get(DmlSqlType.valueOf(command));
            if (supporter == null)
                throw new RuntimeException("CleverORM: " + command + " sql command support is not implemented yet");

            return supporter;
        } catch (JSQLParserException e){
            throw new RuntimeException("CleverORM: sql parser exception: " + sql + ", on " + modelServiceClass.getCanonicalName() + "." + method.getName() + "()");
        }
    }

    private String generate(SqlSupporter supporter){
        return supporter.generate(this);
    }

    protected Select parseSelect(Select selectStatement, String sql){
        PlainSelect plain = (PlainSelect) selectStatement.getSelectBody();
        selectItems = plain.getSelectItems();
        from = plain.getFromItem();
        joins = plain.getJoins();
        where = plain.getWhere();
        groupByElement = plain.getGroupBy();
        orderByElements = plain.getOrderByElements();
        limit = plain.getLimit();
        top = plain.getTop();
        offset = plain.getOffset();
        forUpdateTable = plain.getForUpdateTable();
        distinct = plain.getDistinct();

        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        tables = tablesNamesFinder.getTableList(selectStatement);
        return selectStatement;
    }

    protected Update parseUpdate(Update updateStatement, String sql){
        fields = updateStatement.getColumns();
        values = updateStatement.getExpressions();
        where = updateStatement.getWhere();
        limit = updateStatement.getLimit();
        from = updateStatement.getFromItem();
        joins = updateStatement.getJoins();
        Select select = updateStatement.getSelect();
        if(select != null) {
            PlainSelect plain = (PlainSelect) select.getSelectBody();
            
        }
        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        tables = tablesNamesFinder.getTableList(updateStatement);
        return updateStatement;
    }

    protected Insert parseInsert(Insert insertStatement, String sql){
        if(insertStatement.isUseSet()) {
            fields = insertStatement.getSetColumns();
            values = insertStatement.getSetExpressionList();
        }
        if(insertStatement.isUseValues()) {
            fields = insertStatement.getColumns();
            if(insertStatement.getItemsList() instanceof MultiExpressionList) {
                MultiExpressionList multiExpressionList = (MultiExpressionList) insertStatement.getItemsList();
                valuesList = multiExpressionList.getExprList();
            } else
            if(insertStatement.getItemsList() instanceof ExpressionList){
                values = ((ExpressionList) insertStatement.getItemsList()).getExpressions();
            }
        }

        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        tables = tablesNamesFinder.getTableList(insertStatement);
        return insertStatement;
    }

    protected Delete parseDelete(Delete deleteStatement, String sql){
        joins = deleteStatement.getJoins();
        where = deleteStatement.getWhere();
        limit = deleteStatement.getLimit();

        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        tables = tablesNamesFinder.getTableList(deleteStatement);
        return deleteStatement;
    }

    protected String generatorParamNames(){
        return generatorParamNames(0);
    }

    protected String generatorParamNames(int skip){
        String names = "";
        if(parameters.length > skip)
        for(int i = skip; i < parameters.length; i++){
            names += ", p"+i;
        }
        return names;
    }

    @Override
    public boolean isReplace(){
        return replace != null;
    }

    @Override
    public String getInsertCode(){
        return insert;
    }

    @Override
    public String getReplaceCode(){
        return replace;
    }

    @Override
    public String getAppendCode(){
        return append;
    }

    protected void addClassProperty(String line){
        if(StrUtil.isBlank(line)) return;
        if(line.endsWith(";")) line = line.substring(0, line.length()-1);
        addClassData("classProperties", line);
    }

    protected void addClassImport(Class<?> clazz){
        addClassImport(clazz.getCanonicalName());
    }

    protected void addClassImport(String line){
        if(StrUtil.isBlank(line)) return;
        if(line.endsWith(";")) line = line.substring(0, line.length()-1);
        addClassData("classImports", line);
    }

    private void addClassData(String name, String value){
        LinkedList<String> properties = null;
        properties = (LinkedList<String>)clazzData.get(name);
        if(properties == null) properties = new LinkedList<>();
        if(!properties.contains(value)) properties.add(value);
        clazzData.put(name, properties);
    }

    public static class Property {
        private int[] modifiers;
        private Class<?> type;
        private String name;
        private Object value;

        public Property(int[] modifiers, Class<?> type, String name, Object value){
            this.modifiers = modifiers;
            this.type = type;
            this.name = name;
            this.value = value;
        }

        public int[] getModifiers() {
            return modifiers;
        }

        public Property setModifiers(int[] modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public Class<?> getType() {
            return type;
        }

        public Property setType(Class<?> type) {
            this.type = type;
            return this;
        }

        public String getName() {
            return name;
        }

        public Property setName(String name) {
            this.name = name;
            return this;
        }

        public Object getValue() {
            return value;
        }

        public Property setValue(Object value) {
            this.value = value;
            return this;
        }
    }
}
