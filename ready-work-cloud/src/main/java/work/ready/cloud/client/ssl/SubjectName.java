
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

package work.ready.cloud.client.ssl;

import work.ready.core.tools.validator.Assert;

final class SubjectName {

    static final int DNS = 2;
    static final int IP = 7;

    private final String value;
    private final int type;

    static SubjectName IP(final String value) {
        return new SubjectName(value, IP);
    }

    static SubjectName DNS(final String value) {
        return new SubjectName(value, DNS);
    }

    SubjectName(final String value, final int type) {
        Assert.that(value).notNull("Value cannot be null");
        Assert.that(type).notNull("Type cannot be null");
        this.value = value;
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

}
