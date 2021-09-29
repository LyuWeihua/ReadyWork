/**
 *
 * Original work Copyright Spring
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
package work.ready.core.tools.wildcard;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class WildcardRules {
    private Set<WildcardRule> rules;

    public WildcardRules() {
        rules = new HashSet<>();
    }

    public WildcardRules(final Set<WildcardRule> rules) {
        this.rules = (rules != null) ? new HashSet<>(rules) : new HashSet<>();
    }

    public boolean addRule(WildcardRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Rule can't be null");
        }
        return rules.add(rule);
    }

    public boolean addRules(Collection<WildcardRule> rules) {
        if (rules == null) {
            throw new IllegalArgumentException("Rules list can't be null");
        }

        return this.rules.addAll(rules);
    }

    public boolean removeRule(WildcardRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Rule to remove can't be null");
        }

        return rules.remove(rule);
    }

    public Set<WildcardRule> getRules() {
        return rules;
    }
}
