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

package work.ready.cloud.transaction.coordination.support.dto;

import work.ready.cloud.transaction.coordination.support.dto.model.TxException;
import work.ready.core.database.ModelService;
import work.ready.core.database.Page;
import work.ready.core.database.annotation.Auto;
import work.ready.core.database.cleverorm.IfFailure;
import work.ready.core.ioc.annotation.Service;

@Service
public class TxExceptionDao extends ModelService<TxException> {

    @Auto
    public TxException getFirstByGroupId(String groupId){
        return IfFailure.get(null);
    }

    @Auto("update _TABLE_ set EX_STATE = ? where ID = ?")
    public Long updateExStateById(Short exState, Long id) {
        return IfFailure.get(null);
    }

    @Auto("select * from _TABLE_ where GROUP_ID = ? and UNIT_ID = ? limit 1")
    public TxException getFirstByGroupIdAndUnitId(String groupId, String unitId){
        return IfFailure.get(null);
    }

    @Auto("select * from _TABLE_ where EX_STATE = ? and REGISTRAR = ?")
    public Page<TxException> getAllByExStateAndRegistrar(int page, int size, Integer exState, Integer registrar){
        return IfFailure.get(null);
    }

    @Auto("select * from _TABLE_ where EX_STATE = ?")
    public Page<TxException> getAllByExState(int page, int size, Integer exState){
        return IfFailure.get(null);
    }

    @Auto("select * from _TABLE_ where REGISTRAR = ?")
    public Page<TxException> getAllByRegistrar(int page, int size, Integer registrar){
        return IfFailure.get(null);
    }

    @Auto("select * from _TABLE_")
    public Page<TxException> getAll(int page, int size){
        return IfFailure.get(null);
    }
}
