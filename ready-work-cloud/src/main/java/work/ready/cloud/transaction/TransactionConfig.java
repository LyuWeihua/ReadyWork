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

package work.ready.cloud.transaction;

import work.ready.cloud.transaction.common.Transaction;
import work.ready.cloud.transaction.logger.TxLoggerConfig;
import work.ready.core.config.BaseConfig;

public class TransactionConfig extends BaseConfig {

    private TxLoggerConfig txLogger = new TxLoggerConfig();

    private String defaultType = Transaction.LCN;

    private int txTimeout = 15 * 1000;

    private String exceptionListenerUrl = "http://127.0.0.1/notify";

    private int concurrentLevel;

    private int chainLevel = 3;

    private long communicationTimeout;

    private boolean localThreadTransactionIsolation = true;

    private boolean optimizeLoadBalancer = true;

    public String getDefaultType() {
        return defaultType;
    }

    public void setDefaultType(String defaultType) {
        this.defaultType = defaultType;
    }

    public int getTxTimeout() {
        return txTimeout;
    }

    public void setTxTimeout(int txTimeout) {
        this.txTimeout = txTimeout;
    }

    public String getExceptionListenerUrl() {
        return exceptionListenerUrl;
    }

    public void setExceptionListenerUrl(String exceptionListenerUrl) {
        this.exceptionListenerUrl = exceptionListenerUrl;
    }

    public int getConcurrentLevel() {
        return concurrentLevel;
    }

    public void setConcurrentLevel(int concurrentLevel) {
        this.concurrentLevel = concurrentLevel;
    }

    public int getChainLevel() {
        return chainLevel;
    }

    public void setChainLevel(int chainLevel) {
        this.chainLevel = chainLevel;
    }

    public long getCommunicationTimeout() {
        return communicationTimeout;
    }

    public void setCommunicationTimeout(long communicationTimeout) {
        this.communicationTimeout = communicationTimeout;
    }

    public boolean isLocalThreadTransactionIsolation() {
        return localThreadTransactionIsolation;
    }

    public void setLocalThreadTransactionIsolation(boolean localThreadTransactionIsolation) {
        this.localThreadTransactionIsolation = localThreadTransactionIsolation;
    }

    public boolean isOptimizeLoadBalancer() {
        return optimizeLoadBalancer;
    }

    public void setOptimizeLoadBalancer(boolean optimizeLoadBalancer) {
        this.optimizeLoadBalancer = optimizeLoadBalancer;
    }

    public TxLoggerConfig getTxLogger() {
        return txLogger;
    }

}
