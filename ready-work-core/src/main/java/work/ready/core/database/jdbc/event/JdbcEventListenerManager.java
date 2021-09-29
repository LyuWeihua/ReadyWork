/**
 *
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
package work.ready.core.database.jdbc.event;

import work.ready.core.tools.validator.Assert;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class JdbcEventListenerManager {

  private static ServiceLoader<JdbcEventListener> jdbcEventListenerServiceLoader =
          ServiceLoader.load(JdbcEventListener.class, JdbcEventListenerManager.class.getClassLoader());

  private static final CompoundJdbcEventListener jdbcEventListener = new CompoundJdbcEventListener();

  private JdbcEventListenerManager() {
    registerEventListenersFromServiceLoader(jdbcEventListener);
  }

  public static JdbcEventListener getListener() {
    return jdbcEventListener;
  }

  public static void addListener(JdbcListener listener) {
    Assert.notNull(listener, "listener can not be null");
    jdbcEventListener.addListener(listener);
  }

  public static void removeListener(JdbcListener listener) {
    jdbcEventListener.removeListener(listener);
  }

  public static <T extends JdbcListener> Collection<T> getEventListeners(Class<T> type) {
    return jdbcEventListener.getEventListeners(type);
  }

  protected void registerEventListenersFromServiceLoader(CompoundJdbcEventListener compoundEventListener) {
    for (Iterator<JdbcEventListener> iterator = jdbcEventListenerServiceLoader.iterator(); iterator.hasNext(); ) {
      compoundEventListener.addListener(iterator.next());
    }
  }
}
