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

import java.util.HashMap;
import java.util.Map;

public class CallableStatementInformation extends PreparedStatementInformation {
  private final Map<String, Value> namedParameterValues = new HashMap<String, Value>();

  public CallableStatementInformation(ConnectionInformation connectionInformation, String query) {
    super(connectionInformation, query);
  }

  @Override
  public String getSqlWithValues() {

    if( namedParameterValues.size() == 0 ) {
      return super.getSqlWithValues();
    }

    final StringBuilder result = new StringBuilder();
    final String statementQuery = getStatementQuery();

    result.append(statementQuery);
    result.append(" ");

    StringBuilder parameters = new StringBuilder();

    if(getParameterValues().size() > 0) {
      for (Map.Entry<Integer, Value> entry : getParameterValues().get(0).entrySet()) {
        appendParameter(parameters, entry.getKey().toString(), entry.getValue());
      }
    }

    for( Map.Entry<String, Value> entry : namedParameterValues.entrySet() ) {
      appendParameter(parameters, entry.getKey(), entry.getValue());
    }

    result.append(parameters);

    return result.toString();
  }

  private void appendParameter(StringBuilder parameters, String name, Value value) {
    if( parameters.length() > 0 ) {
      parameters.append(", ");
    }

    parameters.append(name);
    parameters.append(":");
    parameters.append(value != null ? value.toString() : new Value().toString());
  }

  public void setParameterValue(final String name, final Object value) {
    namedParameterValues.put(name, new Value(value));
  }

  public Map<String, Value> getNamedParameterValues() {
    return namedParameterValues;
  }

  @Override
  public void clearParameters() {
    super.clearParameters();
    namedParameterValues.clear();
  }
}
