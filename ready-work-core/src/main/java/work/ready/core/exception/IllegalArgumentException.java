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

package work.ready.core.exception;

import static work.ready.core.tools.StrUtil.format;

public class IllegalArgumentException extends BaseException {

    private static final long serialVersionUID = 356820722163858521L;

    private transient Object[] args;

    public IllegalArgumentException(String message) {
        super(message);
    }

    public IllegalArgumentException(String message, Throwable cause) { super(message, cause);}

    public IllegalArgumentException(String message, Object... args) {
        super(format(message, args));
        this.args = args;
    }

    public Object[] getArgs(){
        return args;
    }
}
