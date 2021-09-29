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

import javax.sql.CommonDataSource;
import javax.sql.PooledConnection;
import java.sql.Connection;
import java.sql.Driver;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionInformation implements Loggable {

  private static final AtomicInteger counter = new AtomicInteger(0);
  private final int connectionId;
  private CommonDataSource dataSource;
  private Driver driver;
  private Connection connection;
  private PooledConnection pooledConnection;
  private long timeToGetConnectionNs;
  private long timeToCloseConnectionNs;
  private String url;

  private ConnectionInformation() {
    this.connectionId = counter.getAndIncrement();
  }

  public static ConnectionInformation fromDriver(Driver driver, Connection connection, long timeToGetConnectionNs) {
    final ConnectionInformation connectionInformation = new ConnectionInformation();
    connectionInformation.driver = driver;
    connectionInformation.connection = connection;
    connectionInformation.timeToGetConnectionNs = timeToGetConnectionNs;
    return connectionInformation;
  }

  public static ConnectionInformation fromDataSource(CommonDataSource dataSource, Connection connection, long timeToGetConnectionNs) {
    final ConnectionInformation connectionInformation = new ConnectionInformation();
    connectionInformation.dataSource = dataSource;
    connectionInformation.connection = connection;
    connectionInformation.timeToGetConnectionNs = timeToGetConnectionNs;
    return connectionInformation;
  }

  public static ConnectionInformation fromPooledConnection(PooledConnection pooledConnection, Connection connection, long timeToGetConnectionNs) {
    final ConnectionInformation connectionInformation = new ConnectionInformation();
    connectionInformation.pooledConnection = pooledConnection;
    connectionInformation.connection = connection;
    connectionInformation.timeToGetConnectionNs = timeToGetConnectionNs;
    return connectionInformation;
  }

  public static ConnectionInformation fromDriver(Driver driver) {
    final ConnectionInformation connectionInformation = new ConnectionInformation();
    connectionInformation.driver = driver;
    return connectionInformation;
  }

  public static ConnectionInformation fromDataSource(CommonDataSource dataSource) {
    final ConnectionInformation connectionInformation = new ConnectionInformation();
    connectionInformation.dataSource = dataSource;
    return connectionInformation;
  }

  public static ConnectionInformation fromPooledConnection(PooledConnection pooledConnection) {
    final ConnectionInformation connectionInformation = new ConnectionInformation();
    connectionInformation.pooledConnection = pooledConnection;
    return connectionInformation;
  }

  public static ConnectionInformation fromTestConnection(Connection connection) {
    final ConnectionInformation connectionInformation = new ConnectionInformation();
    connectionInformation.connection = connection;
    return connectionInformation;
  }

  public int getConnectionId() {
    return connectionId;
  }

  @Override
  public String getSql() {
    return "";
  }

  @Override
  public String getSqlWithValues() {
    return "";
  }

  public CommonDataSource getDataSource() {
    return dataSource;
  }

  public Driver getDriver() {
    return driver;
  }

  public Connection getConnection() {
    return connection;
  }

  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  public PooledConnection getPooledConnection() {
    return pooledConnection;
  }

  public long getTimeToGetConnectionNs() {
    return timeToGetConnectionNs;
  }

  public void setTimeToGetConnectionNs(long timeToGetConnectionNs) {
    this.timeToGetConnectionNs = timeToGetConnectionNs;
  }

  public long getTimeToCloseConnectionNs() {
    return timeToCloseConnectionNs;
  }

  public void setTimeToCloseConnectionNs(long timeToCloseConnectionNs) {
    this.timeToCloseConnectionNs = timeToCloseConnectionNs;
  }

  @Override
  public ConnectionInformation getConnectionInformation() {
    return this;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUrl() {
    return this.url;
  }
}
