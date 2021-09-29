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
package work.ready.cloud.transaction.core.transaction.txc.analyse;

import work.ready.cloud.transaction.core.transaction.txc.analyse.bean.SelectImageParams;
import work.ready.cloud.transaction.core.transaction.txc.analyse.bean.DeleteImageParams;
import work.ready.cloud.transaction.core.transaction.txc.analyse.bean.InsertImageParams;
import work.ready.cloud.transaction.core.transaction.txc.analyse.bean.UpdateImageParams;
import work.ready.cloud.transaction.core.transaction.txc.exception.TxcLogicException;

import java.sql.Connection;

public interface TxcService {

    void lockSelect(Connection connection, SelectImageParams selectImageParams, int lockType) throws TxcLogicException;

    void resolveUpdateImage(Connection connection, UpdateImageParams updateImageParams) throws TxcLogicException;

    void resolveDeleteImage(Connection connection, DeleteImageParams deleteImageParams) throws TxcLogicException;

    void resolveInsertImage(Connection connection, InsertImageParams insertImageParams) throws TxcLogicException;

    void cleanTxc(String groupId, String unitId) throws TxcLogicException;

    void undo(String groupId, String unitId) throws TxcLogicException;
}
