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

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings({"rawtypes", "unchecked"})
public class DaoTemplate<M extends Model> {

	protected Model<M> dao;
	protected SqlParam sqlParam;

	public DaoTemplate(Model dao, String key, Map<?, ?> data) {
		this.dao = dao;
		this.sqlParam = dao.getSqlParam(key, data);
	}

	public DaoTemplate(Model dao, String key, Object... params) {
		this.dao = dao;
		this.sqlParam = dao.getSqlParam(key, params);
	}

	public DaoTemplate(boolean byString, Model dao, String content, Map<?, ?> data) {
		this.dao = dao;
		this.sqlParam = dao.getSqlParamByString(content, data);
	}

	public DaoTemplate(boolean byString, Model dao, String content, Object... params) {
		this.dao = dao;
		this.sqlParam = dao.getSqlParamByString(content, params);
	}

	public SqlParam getSqlParam() {
		return sqlParam;
	}

	public List<M> find() {
		return dao.find(sqlParam);
	}

	public M findFirst() {
		return dao.findFirst(sqlParam);
	}

	public Page<M> paginate(int pageNumber, int pageSize) {
		return dao.paginate(pageNumber, pageSize, sqlParam);
	}

	public Page<M> paginate(int pageNumber, int pageSize, boolean isGroupBySql) {
		return dao.paginate(pageNumber, pageSize, isGroupBySql, sqlParam);
	}

	public void each(Function<M, Boolean> func) {
		dao.each(func, sqlParam.getSql(), sqlParam.getParam());
	}

	public List<M> findByCache(String cacheName, Object key) {
		return dao.findByCache(cacheName, key, sqlParam.getSql(), sqlParam.getParam());
	}

	public M findFirstByCache(String cacheName, Object key) {
		return dao.findFirstByCache(cacheName, key, sqlParam.getSql(), sqlParam.getParam());
	}

	public Page<M> paginateByCache(String cacheName, Object key, int pageNumber, int pageSize) {
		String[] sqls = PageSqlKit.parsePageSql(sqlParam.getSql());
		return dao.paginateByCache(cacheName, key, pageNumber, pageSize, sqls[0], sqls[1], sqlParam.getParam());
	}

	public Page<M> paginateByCache(String cacheName, Object key, int pageNumber, int pageSize, boolean isGroupBySql) {
		String[] sqls = PageSqlKit.parsePageSql(sqlParam.getSql());
		return dao.paginateByCache(cacheName, key, pageNumber, pageSize, isGroupBySql, sqls[0], sqls[1], sqlParam.getParam());
	}
}

