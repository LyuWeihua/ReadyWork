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

package work.ready.core.aop.transformer.match;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class SubTypeMatch implements ConditionMatch {

    private ElementMatcher.Junction<TypeDescription> junction;

    public SubTypeMatch(Class<?> superType) {
        junction = isSubTypeOf(superType); 
    }

    public SubTypeMatch(String superType) {
        junction = ElementMatchers.hasSuperType(ElementMatchers.named(superType));
    }

    public SubTypeMatch or(Class<?> superType) {
        junction = junction.or(isSubTypeOf(superType));
        return this;
    }

    public SubTypeMatch and(Class<?> superType) {
        junction = junction.and(isSubTypeOf(superType));
        return this;
    }

    public SubTypeMatch or(ElementMatcher<? super TypeDescription> matcher) {
        junction = junction.or(matcher);
        return this;
    }

    public SubTypeMatch and(ElementMatcher<? super TypeDescription> matcher) {
        junction = junction.and(matcher);
        return this;
    }

    @Override
    public ElementMatcher.Junction<TypeDescription> buildJunction() {
        return junction;
    }

    @Override
    public boolean isMatch(TypeDescription typeDescription) {
        return junction.matches(typeDescription);
    }
}
