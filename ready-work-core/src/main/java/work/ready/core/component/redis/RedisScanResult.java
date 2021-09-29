/**
 *
 * Original work Copyright (c) 2015-2020, Michael Yang 杨福海 (fuhai999@gmail.com).
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
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
package work.ready.core.component.redis;

import java.io.Serializable;
import java.util.List;

public class RedisScanResult<T> implements Serializable {
  private String cursor;
  private List<T> results;

  public RedisScanResult() {

  }

  public RedisScanResult(String cursor, List<T> results) {
    this.cursor = cursor;
    this.results = results;
  }

  public String getCursor() {
    return cursor;
  }

  public void setCursor(String cursor) {
    this.cursor = cursor;
  }

  public List<T> getResults() {
    return results;
  }

  public void setResults(List<T> results) {
    this.results = results;
  }

  public boolean isCompleteIteration() {
    return "0".equals(getCursor());
  }

}
