/**
 *
 * Original work Copyright (c) 2016 Network New Technologies Inc.
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
package work.ready.core.exception;

import work.ready.core.service.status.Status;

public class ClientException extends BaseException {
    private static final long serialVersionUID = 1L;
    private static Status status = new Status();

    public ClientException() {
        super();
    }

    public ClientException(String message) {
        super(message);
    }

    public ClientException(Status status) {
        this.status = status;
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientException(Throwable cause) {
        super(cause);
    }

    public static Status getStatus() {
        return status;
    }

    public static void setStatus(Status status) {
        ClientException.status = status;
    }
}
