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

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import work.ready.core.tools.StrUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectHelper {

    private PlainSelect plainSelect;
    private Map<String, Table> mapJPTable;
    private SetOperationList unionSelect;
    private String sql;

    public SelectHelper() {
        super();
        setPlainSelect(new PlainSelect());
    }

    public SelectHelper(String sql) {
        try{
            Statement stmt = CCJSqlParserUtil.parse(sql);
            Select selectStatement = (Select) stmt;
            setPlainSelect((PlainSelect) selectStatement.getSelectBody());
        } catch (JSQLParserException e){
            e.printStackTrace();
        }
    }

    public SelectHelper(Select selectStatement) {
        super();
        setPlainSelect((PlainSelect) selectStatement.getSelectBody());
    }

    public SelectHelper(PlainSelect plainSelect) {
        super();
        setPlainSelect(plainSelect);
    }

    public void generateTableAlias(String key,String tableName,String aliasName,boolean useAs){
        Table jpTable = new Table(tableName);
        jpTable.setAlias(new Alias(aliasName, useAs));
        if(mapJPTable==null){
            mapJPTable = new HashMap<String, Table>();
        }
        mapJPTable.put(key, jpTable);
    }

    public void generateTableAlias(String key,String tableName,String aliasName){
        generateTableAlias(key, tableName, aliasName, true);
    }

    public Table getTableByKey(String key){
        if(mapJPTable==null){
            return null;
        }
        return mapJPTable.get(key);
    }

    public String getTableAlias(String key){
        Table table = getTableByKey(key);
        if(table!=null&&table.getAlias()!=null){
            return table.getAlias().getName();
        }
        return null;
    }

    public List<SelectItem> getSelectItems(){
        return plainSelect.getSelectItems();
    }

    public void addSelectItem(Table table,String columnName,String aliasName){
        addSelectItem(table, columnName, aliasName, true);
    }

    public void addSelectItem(Table table,String columnName,String aliasName,boolean useAs){
        SelectExpressionItem selectItem = new SelectExpressionItem();
        selectItem.setExpression(new Column(table,columnName));
        if(StrUtil.notBlank(aliasName)){
            selectItem.setAlias(new Alias(aliasName,useAs));
        }
        addSelectItem(selectItem);
    }

    public void addSelectItem(SelectItem selectItem){
        if(plainSelect.getSelectItems()==null){
            plainSelect.addSelectItems(selectItem);
            return;
        }
        if(!contains(plainSelect.getSelectItems(),selectItem)){
            plainSelect.addSelectItems(selectItem);
        }
    }

    public  void addSelectItem(String selectString){
        SelectExpressionItem selectItem = new SelectExpressionItem();
        try {
            selectItem.setExpression(CCJSqlParserUtil.parseCondExpression(selectString));
            addSelectItem(selectItem);
        } catch (JSQLParserException e) {
            e.printStackTrace();
        }
    }

    public  void addSelectItem(String selectString,String aliasName,boolean useAs){
        SelectExpressionItem selectItem = new SelectExpressionItem();
        try {
            selectItem.setExpression(CCJSqlParserUtil.parseCondExpression(selectString));
            if(StrUtil.notBlank(aliasName)){
                selectItem.setAlias(new Alias(aliasName,useAs));
            }
            addSelectItem(selectItem);
        } catch (JSQLParserException e) {
            e.printStackTrace();
        }
    }

    public  void addSelectItem(String selectString,String aliasName){
        addSelectItem(selectString, aliasName, true);
    }
    
    public void setFromItem(FromItem item) {
        plainSelect.setFromItem(item);
    }
    
    public Join addJoinLO(FromItem rightItem){
        return addJoin(rightItem, true, false, true, false);
    }

    public Join addJoinLI(FromItem rightItem){
        return addJoin(rightItem, true, true, false, false);
    }

    public Join addJoin(FromItem rightItem){
        return addJoin(rightItem, false, false, false, false);
    }

    public Join addJoin(Join join){
        return addJoin(join, false, false, false, false);
    }

    public Join addJoin(String tableName){
        return addJoin(tableName, null,false);
    }

    public Join addJoin(String tableName,String aliasName,boolean useAs){
        Table table = new Table(tableName);
        if(StrUtil.notBlank(aliasName)){
            table.setAlias(new Alias(aliasName, useAs));
        }
        return addJoin(table, false, false, false, false);
    }

    public Join addJoin(FromItem rightItem,boolean left,boolean inner,boolean outter,boolean right){
        Join join = new Join();
        join.setRightItem(rightItem);
        return addJoin(join, left, inner, outter, right);
    }

    public Join addJoin(Join join,boolean left,boolean inner,boolean outter,boolean right){
        if(left){
            join.setLeft(left);
        }else if(right){
            join.setRight(right);
        }
        if(inner){
            join.setInner(inner);
        }else if(outter){
            join.setOuter(outter);
        }
        if(!contains(getListJoin(),join)){
            getListJoin().add(join);
        }
        return join;
    }
    
    public static void addJoinOnExpression(Join join, Expression onExpression, boolean isOr){
        if(join==null){
            return;
        }
        if(join.getOnExpression() ==null){
            join.setOnExpression(onExpression);
            return;
        }
        if(isOr){
            join.setOnExpression(new OrExpression(join.getOnExpression(), onExpression));
        }else{
            join.setOnExpression(new AndExpression(join.getOnExpression(),onExpression));
        }
    }

    public static void addJoinOnExpression(Join join,Expression onExpression){
        addJoinOnExpression(join,onExpression, false);
    }

    public static void addJoinOnExpression(Join join,String onExpression) throws JSQLParserException{
        addJoinOnExpression(join,CCJSqlParserUtil.parseCondExpression(onExpression));
    }

    public static void addJoinOnExpression(Join join,String onExpression,boolean isOr) throws JSQLParserException{
        addJoinOnExpression(join,CCJSqlParserUtil.parseCondExpression(onExpression),isOr);
    }

    public void addWhereExpression(Expression whereExpression,boolean isOr){
        if(plainSelect.getWhere() ==null){
            plainSelect.setWhere(whereExpression);
            return;
        }
        if(isOr){
            plainSelect.setWhere(new OrExpression(plainSelect.getWhere(), whereExpression));
        }else{
            plainSelect.setWhere(new AndExpression(plainSelect.getWhere(),whereExpression));
        }
    }

    public void addWhereExpression(Expression whereExpression){
        addWhereExpression(whereExpression, false);
    }

    public void addWhereExpression(String whereExpression) throws JSQLParserException{
        addWhereExpression(CCJSqlParserUtil.parseCondExpression(whereExpression));
    }

    public void addWhereExpression(String whereExpression,boolean isOr) throws JSQLParserException{
        addWhereExpression(CCJSqlParserUtil.parseCondExpression(whereExpression),isOr);
    }

    public  void addGroupByExpression(String groupByString){
        try {
            Expression groupByExpression = CCJSqlParserUtil.parseCondExpression(groupByString);
            addGroupByExpression(groupByExpression);
        } catch (JSQLParserException e) {
            e.printStackTrace();
        }
    }

    public  void addGroupByExpression(Expression groupByExpression) {
        GroupByElement groupByElement = plainSelect.getGroupBy();
        if(groupByElement == null){
            plainSelect.addGroupByColumnReference(groupByExpression);
            return;
        }
        if (!groupByElement.getGroupByExpressions().contains(groupByExpression)){
            plainSelect.addGroupByColumnReference(groupByExpression);
        }
    }

    public void addHavingExpression(Expression havingExpression,boolean isOr){
        if(plainSelect.getHaving() ==null){
            plainSelect.setHaving(havingExpression);
            return;
        }
        if(isOr){
            plainSelect.setHaving(new OrExpression(plainSelect.getHaving(), havingExpression));
        }else{
            plainSelect.setHaving(new AndExpression(plainSelect.getHaving(),havingExpression));
        }
    }

    public void addHavingExpression(Expression havingExpression){
        addWhereExpression(havingExpression, false);
    }

    public void addHavingExpression(String havingExpression) throws JSQLParserException{
        addWhereExpression(CCJSqlParserUtil.parseCondExpression(havingExpression));
    }

    public void addHavingExpression(String havingExpression,boolean isOr) throws JSQLParserException{
        addWhereExpression(CCJSqlParserUtil.parseCondExpression(havingExpression),isOr);
    }

    public boolean addOrderByExpression(OrderByElement orderByElement){
        boolean result=false;
        if(!contains(getOrderByElements(),orderByElement)){
            
            result |=getOrderByElements().add(orderByElement);
        }
        return result;
    }

    public boolean addOrderByExpression(Expression orderByExpression,boolean isDesc,boolean isNullsFirst){
        OrderByElement orderByElement = new OrderByElement();
        orderByElement.setExpression(orderByExpression);
        if(isDesc){
            orderByElement.setAsc(!isDesc);
        }else{
            orderByElement.setAscDescPresent(!isDesc);
        }
        if(isNullsFirst){
            orderByElement.setNullOrdering(OrderByElement.NullOrdering.NULLS_FIRST);
        }else{
            orderByElement.setNullOrdering(OrderByElement.NullOrdering.NULLS_LAST);
        }
        return addOrderByExpression(orderByElement);
    }
    
    public boolean addOrderByExpression(Expression orderByExpression,boolean isDesc){
        OrderByElement orderByElement = new OrderByElement();
        orderByElement.setExpression(orderByExpression);
        if(isDesc){
            orderByElement.setAsc(!isDesc);
        }else{
            orderByElement.setAscDescPresent(!isDesc);
        }
        return addOrderByExpression(orderByElement);
    }

    public boolean addOrderByExpression(Expression orderByExpression){
        return addOrderByExpression(orderByExpression,false);
    }

    public boolean addOrderByExpression(String orderByExpression,boolean isDesc,boolean isNullsFirst) throws JSQLParserException{
        return addOrderByExpression(CCJSqlParserUtil.parseCondExpression(orderByExpression), isDesc, isNullsFirst);
    }

    public boolean addOrderByExpression(String orderByExpression,boolean isDesc) throws JSQLParserException{
        return addOrderByExpression(CCJSqlParserUtil.parseCondExpression(orderByExpression),isDesc);
    }

    public boolean addOrderByExpression(String orderByExpression) throws JSQLParserException{
        return addOrderByExpression(orderByExpression,false);
    }

    private void setSubselectBody(SelectBody selectBody,String aliasName,boolean userAs){
        SubSelect subSelect =new SubSelect();
        subSelect.setSelectBody(selectBody);
        if(StrUtil.notBlank(aliasName)){
            subSelect.setAlias(new Alias(aliasName,userAs));
        }
        setFromItem(subSelect);
    }
    
    public void setSubselectObject(SelectHelper selectHelper, String aliasName, boolean userAs){
        if(selectHelper !=null){
            setSubselectBody(selectHelper.getSelectBody(),aliasName,userAs);
        }
    }

    public void setSubselectObject(SelectHelper selectHelper){
        setSubselectObject(selectHelper, null, false);
    }

    public void setLimit(String rowCount){
        Limit limit = new Limit();
        limit.setRowCount(new LongValue(rowCount));
        plainSelect.setLimit(limit);
    }

    public Limit getLimit(){
        return plainSelect.getLimit();
    }

    public void setOffset(long start){
        Offset offset = new Offset();
        offset.setOffset(start);
        plainSelect.setOffset(offset);
    }

    public Offset getOffset(){
        return plainSelect.getOffset();
    }

    public void addUnionSelect(SelectHelper selectHelper, boolean isBracket, boolean isUnionAll){
        if(selectHelper !=null){
            addUnionSelectBody(selectHelper.getSelectBody(), isBracket, isUnionAll);
        }
    }

    public void addUnionSelect(SelectHelper selectHelper, boolean isBracket){
        addUnionSelect(selectHelper, isBracket, false);
    }

    public void addUnionSelect(SelectHelper selectHelper){
        addUnionSelect(selectHelper, false, false);
    }

    public void addUnionSelect(PlainSelect plainSelect,boolean isBracket,boolean isUnionAll){
        if(plainSelect!=null){
            addUnionSelectBody(plainSelect, isBracket, isUnionAll);
        }
    }

    public void addUnionSelect(PlainSelect plainSelect,boolean isBracket){
        addUnionSelect(plainSelect, isBracket, false);
    }

    public void addUnionSelect(PlainSelect plainSelect){
        addUnionSelect(plainSelect, false, false);
    }

    private void addUnionSelectBody(SelectBody selectBody,boolean isBracket,boolean isUnionAll){
        if(selectBody!=null){
            if(unionSelect==null){
                unionSelect = new SetOperationList();
            }
            List<SelectBody> selects = unionSelect.getSelects();
            List<Boolean> brackets = unionSelect.getBrackets();
            if(selects==null){
                selects = new ArrayList<SelectBody>();
            }
            if(brackets==null){
                brackets = new ArrayList<Boolean>();
            }
            List<SetOperation> operations = unionSelect.getOperations();
            if(operations==null){
                operations = new ArrayList<SetOperation>();
            }
            
            if(selects.size()==0){
                selects.add(getSelectBody());
                brackets.add(false);
            }

            selects.add(selectBody);
            brackets.add(isBracket);
            UnionOp operation=new UnionOp();
            if(isUnionAll){
                operation.setAll(isUnionAll);
            }
            operations.add(operation);
            unionSelect.setBracketsOpsAndSelects(brackets, selects, operations);
        }
    }

    public boolean addUnionOrderByExpression(OrderByElement orderByElement){
        boolean result=false;
        if(!contains(getUnionOrderByElements(),orderByElement)){
            
            result |=getUnionOrderByElements().add(orderByElement);
        }
        return result;
    }

    public boolean addUnionOrderByExpression(Expression orderByExpression,boolean isDesc,boolean isNullsFirst){
        OrderByElement orderByElement = new OrderByElement();
        orderByElement.setExpression(orderByExpression);
        if(isDesc){
            orderByElement.setAsc(!isDesc);
        }else{
            orderByElement.setAscDescPresent(!isDesc);
        }
        if(isNullsFirst){
            orderByElement.setNullOrdering(OrderByElement.NullOrdering.NULLS_FIRST);
        }else{
            orderByElement.setNullOrdering(OrderByElement.NullOrdering.NULLS_LAST);
        }
        return addUnionOrderByExpression(orderByElement);
    }
    
    public boolean addUnionOrderByExpression(Expression orderByExpression,boolean isDesc){
        OrderByElement orderByElement = new OrderByElement();
        orderByElement.setExpression(orderByExpression);
        if(isDesc){
            orderByElement.setAsc(!isDesc);
        }else{
            orderByElement.setAscDescPresent(!isDesc);
        }
        return addUnionOrderByExpression(orderByElement);
    }

    public boolean addUnionOrderByExpression(Expression orderByExpression){
        return addUnionOrderByExpression(orderByExpression,false);
    }

    public boolean addUnionOrderByExpression(String orderByExpression,boolean isDesc,boolean isNullsFirst) throws JSQLParserException{
        return addUnionOrderByExpression(CCJSqlParserUtil.parseCondExpression(orderByExpression), isDesc, isNullsFirst);
    }

    public boolean addUnionOrderByExpression(String orderByExpression,boolean isDesc) throws JSQLParserException{
        return addUnionOrderByExpression(CCJSqlParserUtil.parseCondExpression(orderByExpression),isDesc);
    }

    public boolean addUnionOrderByExpression(String orderByExpression) throws JSQLParserException{
        return addUnionOrderByExpression(orderByExpression,false);
    }

    public void setUnionLimit(String rowCount){
        Limit limit = new Limit();
        limit.setRowCount(new LongValue(rowCount));
        unionSelect.setLimit(limit);
    }

    public Limit getUnionLimit(){
        return unionSelect.getLimit();
    }

    public void setUnionOffset(long start){
        Offset offset = new Offset();
        offset.setOffset(start);
        unionSelect.setOffset(offset);
    }

    public Offset getUnionOffset(){
        return unionSelect.getOffset();
    }

    public PlainSelect getPlainSelect() {
        return plainSelect;
    }
    public void setPlainSelect(PlainSelect plainSelect) {
        if(plainSelect==null){
            plainSelect =new PlainSelect();
        }
        this.plainSelect = plainSelect;
    }
    public Map<String, Table> getMapJPTable() {
        if(mapJPTable==null){
            mapJPTable =new HashMap<String,Table>();
        }
        return mapJPTable;
    }
    public void setMapJPTable(Map<String, Table> mapJPTable) {
        this.mapJPTable = mapJPTable;
    }
    public List<Join> getListJoin() {
        if(plainSelect.getJoins()==null){
            plainSelect.setJoins(new ArrayList<Join>());
        }
        return plainSelect.getJoins();
    }
    public void setListJoin(List<Join> listJoin) {
        plainSelect.setJoins(listJoin);
    }
    public List<OrderByElement> getOrderByElements() {
        if(plainSelect.getOrderByElements()==null){
            plainSelect.setOrderByElements(new ArrayList<OrderByElement>());
        }
        return plainSelect.getOrderByElements();
    }
    public void setOrderByElements(List<OrderByElement> orderByElements) {
        plainSelect.setOrderByElements(orderByElements);
    }
    public SetOperationList getUnionSelect() {
        return unionSelect;
    }
    public void setUnionSelect(SetOperationList unionSelect) {
        this.unionSelect = unionSelect;
    }

    public List<OrderByElement> getUnionOrderByElements() {
        if(unionSelect.getOrderByElements()==null){
            unionSelect.setOrderByElements(new ArrayList<OrderByElement>());
        }
        return unionSelect.getOrderByElements();
    }
    @Override
    public String toString() {
        return plainSelect==null?"":getSelectBody().toString();
    }

    public SelectBody getSelectBody(){
        getPlainSelect();

        if(unionSelect!=null&&unionSelect.getSelects()!=null){
            return unionSelect;
        }
        
        return plainSelect;
    }
    public static boolean contains(List<?> list,Object object){
        if(list==null||list.size()==0){
            return false;
        }
        for(Object obj:list){
            if(obj==null) continue;
            if(object.toString().trim().equalsIgnoreCase(obj.toString().trim())){
                return true;
            }
        }
        return false;
    }
}
