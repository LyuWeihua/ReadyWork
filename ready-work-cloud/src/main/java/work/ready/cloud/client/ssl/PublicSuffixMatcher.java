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

import java.net.IDN;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PublicSuffixMatcher {

    private final Map<String, DomainType> rules;
    private final Map<String, DomainType> exceptions;

    public PublicSuffixMatcher(final Collection<String> rules, final Collection<String> exceptions) {
        this(DomainType.UNKNOWN, rules, exceptions);
    }

    public PublicSuffixMatcher(
            final DomainType domainType, final Collection<String> rules, final Collection<String> exceptions) {
        Assert.that(domainType).notNull("Domain type is null");
        Assert.that(rules).notNull("Domain suffix rules is null");
        this.rules = new ConcurrentHashMap<String, DomainType>(rules.size());
        for (final String rule: rules) {
            this.rules.put(rule, domainType);
        }
        this.exceptions = new ConcurrentHashMap<String, DomainType>();
        if (exceptions != null) {
            for (final String exception: exceptions) {
                this.exceptions.put(exception, domainType);
            }
        }
    }

    public PublicSuffixMatcher(final Collection<PublicSuffixList> lists) {
        Assert.that(lists).notNull("Domain suffix lists is null");
        this.rules = new ConcurrentHashMap<String, DomainType>();
        this.exceptions = new ConcurrentHashMap<String, DomainType>();
        for (final PublicSuffixList list: lists) {
            final DomainType domainType = list.getType();
            final List<String> rules = list.getRules();
            for (final String rule: rules) {
                this.rules.put(rule, domainType);
            }
            final List<String> exceptions = list.getExceptions();
            if (exceptions != null) {
                for (final String exception: exceptions) {
                    this.exceptions.put(exception, domainType);
                }
            }
        }
    }

    private static boolean hasEntry(final Map<String, DomainType> map, final String rule, final DomainType expectedType) {
        if (map == null) {
            return false;
        }
        final DomainType domainType = map.get(rule);
        if (domainType == null) {
            return false;
        } else {
            return expectedType == null || domainType.equals(expectedType);
        }
    }

    private boolean hasRule(final String rule, final DomainType expectedType) {
        return hasEntry(this.rules, rule, expectedType);
    }

    private boolean hasException(final String exception, final DomainType expectedType) {
        return hasEntry(this.exceptions, exception, expectedType);
    }

    public String getDomainRoot(final String domain) {
        return getDomainRoot(domain, null);
    }

    public String getDomainRoot(final String domain, final DomainType expectedType) {
        if (domain == null) {
            return null;
        }
        if (domain.startsWith(".")) {
            return null;
        }
        String domainName = null;
        String segment = domain.toLowerCase(Locale.ROOT);
        while (segment != null) {

            if (hasException(IDN.toUnicode(segment), expectedType)) {
                return segment;
            }

            if (hasRule(IDN.toUnicode(segment), expectedType)) {
                break;
            }

            final int nextdot = segment.indexOf('.');
            final String nextSegment = nextdot != -1 ? segment.substring(nextdot + 1) : null;

            if (nextSegment != null) {
                if (hasRule("*." + IDN.toUnicode(nextSegment), expectedType)) {
                    break;
                }
            }
            if (nextdot != -1) {
                domainName = segment;
            }
            segment = nextSegment;
        }
        return domainName;
    }

    public boolean matches(final String domain) {
        return matches(domain, null);
    }

    public boolean matches(final String domain, final DomainType expectedType) {
        if (domain == null) {
            return false;
        }
        final String domainRoot = getDomainRoot(
                domain.startsWith(".") ? domain.substring(1) : domain, expectedType);
        return domainRoot == null;
    }

}
