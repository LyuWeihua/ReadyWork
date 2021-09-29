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

import java.util.Collections;
import java.util.List;

public final class PublicSuffixList {

    private final DomainType type;
    private final List<String> rules;
    private final List<String> exceptions;

    public PublicSuffixList(final DomainType type, final List<String> rules, final List<String> exceptions) {
        Assert.that(type).notNull("Domain type is null");
        Assert.that(rules).notNull("Domain suffix rules is null");
        this.type = type;
        this.rules = Collections.unmodifiableList(rules);
        this.exceptions = Collections.unmodifiableList(exceptions != null ? exceptions : Collections.<String>emptyList());
    }

    public PublicSuffixList(final List<String> rules, final List<String> exceptions) {
        this(DomainType.UNKNOWN, rules, exceptions);
    }

    public DomainType getType() {
        return type;
    }

    public List<String> getRules() {
        return rules;
    }

    public List<String> getExceptions() {
        return exceptions;
    }

}
