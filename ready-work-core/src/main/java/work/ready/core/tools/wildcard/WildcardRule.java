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

public class WildcardRule {
    private final String source;
    private final String target;

    public WildcardRule(String source, String target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Empty values are not allowed");
        }

        this.source = source;
        this.target = target;
    }

    public WildcardRule(String regex) {
        if (regex == null) {
            throw new IllegalArgumentException("Empty values are not allowed");
        }

        this.source = regex;
        this.target = regex;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WildcardRule that = (WildcardRule) o;

        if (!source.equals(that.source)) return false;
        return target.equals(that.target);
    }

    @Override
    public int hashCode() {
        int result = source.hashCode();
        result = 31 * result + target.hashCode();
        return result;
    }
}
