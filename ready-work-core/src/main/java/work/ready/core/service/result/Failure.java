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
package work.ready.core.service.result;

import work.ready.core.service.status.Status;

import java.util.NoSuchElementException;

public final class Failure<T> implements Result<T> {

    private final Status error;

    private Failure(Status error) {
        this.error = error;
    }

    public static <T> Result<T> of(Status error) {
        return new Failure<>(error);
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public Status getError() {
        return error;
    }

    @Override
    public T getResult() {
        throw new NoSuchElementException("There is no result is Failure");
    }

    @Override
    public String toString() {
        return String.format("Failure[%s]", error.toString());
    }
}
