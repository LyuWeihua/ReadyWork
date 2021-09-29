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

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Result<T> {

    boolean isSuccess();

    default boolean isFailure() {
        return !isSuccess();
    }

    Status getError();

    T getResult();

    default <R> Result<R> map(Function<? super T, ? extends R> mapper) {
        return isSuccess() ?
                Success.of(mapper.apply(getResult())) :
                (Failure<R>) this;
    }

    default <R> Result<R> flatMap(Function<? super T, Result<R>> mapper) {
        return isSuccess() ?
                mapper.apply(getResult()) :
                (Failure<R>) this;
    }

    default <R> R fold(Function<? super T, ? extends R> successFunction, Function<Failure<R>, ? extends R> failureFunction) {
        return isSuccess() ?
                successFunction.apply(getResult()) :
                failureFunction.apply((Failure<R>) this);
    }

    default <R, Z> Result<Z> lift(Result<R> other, BiFunction<? super T, ? super R, ? extends Z> function) {
        return flatMap(first -> other.map(second -> function.apply(first, second)));
    }

    default Result<T> ifSuccess(Consumer<? super T> successConsumer) {
        if (isSuccess()) {
            successConsumer.accept(this.getResult());
        }
        return this;
    }

    default Result<T> ifFailure(Consumer<Failure<T>> failureConsumer) {
        if (isFailure()) {
            failureConsumer.accept((Failure<T>) this);
        }
        return this;
    }
}
