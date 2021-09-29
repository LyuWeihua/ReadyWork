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
package work.ready.cloud.transaction.coordination.support.dto;

import java.util.List;

public class ExceptionList {
    private long total;
    private List<ExceptionInfo> exceptions;

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<ExceptionInfo> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<ExceptionInfo> exceptions) {
        this.exceptions = exceptions;
    }
}
