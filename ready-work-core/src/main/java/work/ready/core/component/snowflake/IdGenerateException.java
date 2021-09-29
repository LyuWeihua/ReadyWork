/**
 *
 * Original work Copyright Snowflake
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
package work.ready.core.component.snowflake;

public class IdGenerateException extends RuntimeException {

    private static final long serialVersionUID = -27048199131316992L;

    public IdGenerateException() {
        super();
    }

    public IdGenerateException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdGenerateException(String message) {
        super(message);
    }

    public IdGenerateException(String msgFormat, Object... args) {
        super(String.format(msgFormat, args));
    }

    public IdGenerateException(Throwable cause) {
        super(cause);
    }

}
