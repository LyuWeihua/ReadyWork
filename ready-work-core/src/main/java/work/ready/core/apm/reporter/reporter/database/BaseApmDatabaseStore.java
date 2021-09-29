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

package work.ready.core.apm.reporter.reporter.database;

import work.ready.core.database.Bean;
import work.ready.core.database.Model;

@SuppressWarnings("serial")
public abstract class BaseApmDatabaseStore<M extends BaseApmDatabaseStore<M>> extends Model<M> implements Bean {

	public void setId(Long id) {
		set(Column.id.field, id);
	}

	public Long getId() {
		return getLong(Column.id.field);
	}

	public void setSpan(String span) {
		set(Column.span.field, span);
	}

	public String getSpan() {
		return getStr(Column.span.field);
	}

	public void setCreatedTime(java.util.Date createdTime) {
		set(Column.createdTime.field, createdTime);
	}

	public java.util.Date getCreatedTime() {
		return get(Column.createdTime.field);
	}

	public enum Column implements Model.Column {
		    id("ID"),
		    span("SPAN"),
		    createdTime("CREATED_TIME");

		private final String field;
        @Override
		public String get(){ return field; }
		Column(String field) {
			this.field = field;
		}
	}
}
