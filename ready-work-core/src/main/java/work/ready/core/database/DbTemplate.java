/**
 * Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package work.ready.core.database;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class DbTemplate {

	protected DbPro db;
	protected SqlParam sqlParam;

	public DbTemplate(DbPro db, String key, Map<?, ?> data) {
		this.db = db;
		this.sqlParam = db.getSqlParam(key, data);
	}

	public DbTemplate(DbPro db, String key, Object... params) {
		this.db = db;
		this.sqlParam = db.getSqlParam(key, params);
	}

	public DbTemplate(boolean byString, DbPro db, String content, Map<?, ?> data) {
		this.db = db;
		this.sqlParam = db.getSqlParamByString(content, data);
	}

	public DbTemplate(boolean byString, DbPro db, String content, Object... params) {
		this.db = db;
		this.sqlParam = db.getSqlParamByString(content, params);
	}

	public List<Record> find() {
		return db.find(sqlParam);
	}

	public Record findFirst() {
		return db.findFirst(sqlParam);
	}

	public int update() {
		return db.update(sqlParam);
	}

	public Page<Record> paginate(int pageNumber, int pageSize) {
		return db.paginate(pageNumber, pageSize, sqlParam);
	}

	public Page<Record> paginate(int pageNumber, int pageSize, boolean isGroupBySql) {
		return db.paginate(pageNumber, pageSize, isGroupBySql, sqlParam);
	}

	public int delete() {
		return db.delete(sqlParam.getSql(), sqlParam.getParam());
	}

	public String queryStr() {
		return db.queryStr(sqlParam.getSql(), sqlParam.getParam());
	}

	public Integer queryInt() {
		return db.queryInt(sqlParam.getSql(), sqlParam.getParam());
	}

	public Long queryLong() {
		return db.queryLong(sqlParam.getSql(), sqlParam.getParam());
	}

	public BigDecimal queryBigDecimal() {
		return db.queryBigDecimal(sqlParam.getSql(), sqlParam.getParam());
	}

	public BigInteger queryBigInteger() {
		return db.queryBigInteger(sqlParam.getSql(), sqlParam.getParam());
	}

	public <T> T queryColumn() {
		return db.queryColumn(sqlParam.getSql(), sqlParam.getParam());
	}

	public <T> List<T> query() {
		return db.query(sqlParam.getSql(), sqlParam.getParam());
	}

	public <T> T queryFirst() {
		return db.queryFirst(sqlParam.getSql(), sqlParam.getParam());
	}

	public List<Record> findByCache(String cacheName, Object key) {
		return db.findByCache(cacheName, key, sqlParam.getSql(), sqlParam.getParam());
	}

	public Record findFirstByCache(String cacheName, Object key) {
		return db.findFirstByCache(cacheName, key, sqlParam.getSql(), sqlParam.getParam());
	}

	public Page<Record> paginateByCache(String cacheName, Object key, int pageNumber, int pageSize) {
		String[] sqls = PageSqlKit.parsePageSql(sqlParam.getSql());
		return db.paginateByCache(cacheName, key, pageNumber, pageSize, sqls[0], sqls[1], sqlParam.getParam());
	}

	public Page<Record> paginateByCache(String cacheName, Object key, int pageNumber, int pageSize, boolean isGroupBySql) {
		String[] sqls = PageSqlKit.parsePageSql(sqlParam.getSql());
		return db.paginateByCache(cacheName, key, pageNumber, pageSize, isGroupBySql, sqls[0], sqls[1], sqlParam.getParam());
	}
}

