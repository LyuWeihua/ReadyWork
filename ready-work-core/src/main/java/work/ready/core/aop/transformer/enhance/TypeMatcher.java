/**
 *
 * Original work copyright dyagent
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

package work.ready.core.aop.transformer.enhance;

import work.ready.core.aop.transformer.TransformerManager;
import work.ready.core.aop.transformer.match.ClassMatch;
import work.ready.core.aop.transformer.match.ConditionMatch;
import work.ready.core.aop.transformer.match.MultiNameOrMatch;
import work.ready.core.aop.transformer.match.NameMatch;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.*;

public enum TypeMatcher {
    INSTANCE;

    private final String BYTEBUDDY_AUXILIARY = "auxiliary";
    private final Set<String> nameMatchSet = new HashSet<>(16);
    private ElementMatcher.Junction<TypeDescription> junction;
    private ElementMatcher.Junction<TypeDescription> junctionWarp;

    private final ElementMatcher.Junction<NamedElement> ignoreRule = nameStartsWith("java.")
            .or(nameStartsWith("javax."))
            .or(nameStartsWith("com.intellij.rt."))
            .or(nameStartsWith("sun."))
            .or(nameStartsWith("third."))
            .or(nameStartsWith(TransformerManager.class.getPackageName() + '.'))
            .or(nameContains(BYTEBUDDY_AUXILIARY));

    public ElementMatcher.Junction<NamedElement> ignoreRule() {
        return ignoreRule;
    }

    public ElementMatcher.Junction<NamedElement> ignoreRule(List<String> prefix) {
        prefix.forEach(item->{
            ignoreRule.or(nameStartsWith(item));
        });
        return ignoreRule;
    }

    public ElementMatcher<? super TypeDescription> passRule(List<Interceptor> itrcpts) {
        if(junction == null) {
            junction = new ElementMatcher.Junction<TypeDescription>() {
                @Override
                public <U extends TypeDescription> Junction<U> and(ElementMatcher<? super U> elementMatcher) {
                    return new Conjunction<>(this, elementMatcher);
                }

                @Override
                public <U extends TypeDescription> Junction<U> or(ElementMatcher<? super U> elementMatcher) {
                    return new Disjunction<>(this, elementMatcher);
                }

                @Override
                public boolean matches(TypeDescription target) {
                    return nameMatchSet.contains(target.getActualName());
                }
            };
            junction = junction.and(not(isInterface()));
            junctionWarp = new ElementMatcher.Junction<TypeDescription>() {
                @Override
                public <U extends TypeDescription> Junction<U> and(ElementMatcher<? super U> elementMatcher) {
                    return new Conjunction<>(this, elementMatcher);
                }

                @Override
                public <U extends TypeDescription> Junction<U> or(ElementMatcher<? super U> elementMatcher) {
                    return new Disjunction<>(this, elementMatcher);
                }

                @Override
                public boolean matches(TypeDescription target) {
                    return junction.matches(target);
                }
            };
        }
        for (Interceptor itrcpt : itrcpts) {
            ClassMatch match = itrcpt.focusOn();
            if (match instanceof ConditionMatch) {
                junction = junction.or(((ConditionMatch) match).buildJunction());
            } else if (match instanceof NameMatch) {
                nameMatchSet.add(((NameMatch) match).getClassName());
            } else if (match instanceof MultiNameOrMatch) {
                nameMatchSet.addAll(((MultiNameOrMatch) match).getClassNames());
            }
        }
        return junctionWarp;
    }
}

