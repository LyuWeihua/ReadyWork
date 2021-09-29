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

import io.undertow.util.StatusCodes;
import work.ready.core.service.status.Status;

public final class BadRequestException extends BaseException {
    private static final long serialVersionUID = 1L;
    private final Status status;

    public Status getStatus() {
        return status;
    }

    public BadRequestException(String message) {
        this(new Status(StatusCodes.BAD_REQUEST, "", StatusCodes.BAD_REQUEST_STRING, message));
    }

    public BadRequestException(String message, String errorCode) {
        this(new Status(StatusCodes.BAD_REQUEST, errorCode, StatusCodes.BAD_REQUEST_STRING, message));
    }

    public BadRequestException(String message, Throwable cause) {
        this(new Status(StatusCodes.BAD_REQUEST, "", StatusCodes.BAD_REQUEST_STRING, message), cause);
    }

    public BadRequestException(String message, String errorCode, Throwable cause) {
        this(new Status(StatusCodes.BAD_REQUEST, errorCode, StatusCodes.BAD_REQUEST_STRING, message), cause);
    }

    public BadRequestException(Status status) {
        super(status.toString());
        this.status = status;
    }

    public BadRequestException(Status status, Throwable cause) {
        super(status.toString(), cause);
        this.status = status;
    }

}
