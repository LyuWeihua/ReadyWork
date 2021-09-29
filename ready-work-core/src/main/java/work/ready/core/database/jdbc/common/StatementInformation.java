/**
 *
 * Original work Copyright (c) 2002 P6Spy
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
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
package work.ready.core.database.jdbc.common;

import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class StatementInformation implements Loggable {

  private final ConnectionInformation connectionInformation;
  private Statement statement;
  private String statementQuery;
  private long totalTimeElapsed;
  protected Map<String, Object> attachment = new HashMap<>();

  public StatementInformation(final ConnectionInformation connectionInformation) {
    this.connectionInformation = connectionInformation;
  }

  public Object getAttachment(String name) {
    return attachment.get(name);
  }

  public StatementInformation setAttachment(String name, Object attachment) {
    this.attachment.put(name, attachment);
    return this;
  }

  public Statement getStatement() {
    return statement;
  }

  public void setStatement(Statement statement) {
    this.statement = statement;
  }

  public String getStatementQuery() {
    return statementQuery;
  }

  public void setStatementQuery(final String statementQuery) {
    this.statementQuery = statementQuery;
  }

  @Override
  public ConnectionInformation getConnectionInformation() {
    return this.connectionInformation;
  }

  @Override
  public String getSqlWithValues() {
    return getSql();
  }

  @Override
  public String getSql() {
    return getStatementQuery();
  }

  public long getTotalTimeElapsed() {
    return totalTimeElapsed;
  }

  public void incrementTimeElapsed(long timeElapsedNanos) {
    totalTimeElapsed += timeElapsedNanos;
  }

}
