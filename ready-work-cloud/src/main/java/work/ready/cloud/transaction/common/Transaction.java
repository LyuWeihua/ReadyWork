/**
 *
 * Original work Copyright 2017-2019 CodingApi
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
package work.ready.cloud.transaction.common;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.core.transaction.lcn.LcnTransactionType;
import work.ready.cloud.transaction.core.transaction.tcc.TccTransactionType;
import work.ready.cloud.transaction.core.transaction.txc.TxcTransactionType;

public class Transaction {

    public static String APPLICATION_ID = Cloud.getNodeNameWithIp();

    public static final String LCN = LcnTransactionType.name;

    public static final String TCC = TccTransactionType.name;

    public static final String TXC = TxcTransactionType.name;

    public static final String TX_ERROR = "Transaction Error";

    public static final String TAG_TRANSACTION = "Transaction";

    public static final String TAG_TASK = "Transaction Task";

}
