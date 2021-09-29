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

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription.InDefinedShape;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.utility.JavaModule;
import work.ready.core.aop.transformer.TransformerManager;
import work.ready.core.aop.transformer.match.ClassMatch;
import work.ready.core.aop.transformer.utils.MethodUtil;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.matcher.ElementMatchers.hasParameters;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class BuilderDecorator {

    private static final Log logger = LogFactory.getLog(BuilderDecorator.class);
    private static final Map<TypeDescription, List<String>> FIELD = new HashMap<>();

    public static Builder<?> decorate(Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader,
            JavaModule module) {
        Builder<?> decoratedBuilder = builder;
        if (decoratedBuilder == null) {
            throw new IllegalArgumentException("builder can't be null.");
        }

        if (typeUnmatched(typeDescription)) {
            return decoratedBuilder;
        }

        for (MethodDescription.InDefinedShape m : typeDescription.getDeclaredMethods().asDefined()) {
            
            final ParameterList<InDefinedShape> otherPds = m.getParameters();

            if (m.isConstructor()) {
                
                decoratedBuilder = forConstructor(decoratedBuilder, typeDescription, m, otherPds);

            } else if (m.isMethod()) {
                
                decoratedBuilder = forMethod(decoratedBuilder, typeDescription, m, otherPds);
            }
        }
        
        return decoratedBuilder;
    }

    private static Builder<?> forMethod(Builder<?> builder, TypeDescription type, MethodDescription.InDefinedShape method,
            ParameterList<InDefinedShape> parameters) {

        boolean hasInterceptor = false;
        for (MethodInterceptor interceptor : TransformerManager.METHOD.getInterceptors()) {
            ClassMatch cm = interceptor.focusOn();
            if (cm == null) {
                logger.info("Interceptor (%s)'s focusOn return null.", interceptor.getClass().getName());
                continue;
            }
            if (cm.isMatch(type)
                    && interceptor.matches(type, method, parameters)) {
                String key = MethodUtil.signature(type, method);
                InterceptorChain<MethodInterceptor> chain = TransformerManager.METHOD.interceptorChainOf(key);
                chain.addInterceptor(interceptor);
                logger.info("%s has interceptors %s.", method, arrayStrOfInterceptors(chain.getInterceptors()));
                hasInterceptor = true;
                builder = buildFieldDefine(builder, type, interceptor);
            }
        }
        if(hasInterceptor) {
            
            builder = builder.method(named(method.getActualName())
                    .and(hasParameters(new ParametersMatcher(parameters))))
                    .intercept(to(MethodInterceptorDispatcher.class));
        }
        return builder;
    }

    private static boolean typeUnmatched(TypeDescription typeDescription) {
        for (Interceptor itr : TransformerManager.getAllInterceptors()) {
            if (itr.focusOn().isMatch(typeDescription)) {
                return false;
            }
        }
        return true;
    }

    private static Builder<?> forConstructor(Builder<?> builder, TypeDescription type,
            MethodDescription.InDefinedShape constructor,
            ParameterList<InDefinedShape> parameters) {
        boolean hasInterceptor = false;
        for (ConstructorInterceptor interceptor : TransformerManager.CONSTRUCTOR.getInterceptors()) {
            ClassMatch cm = interceptor.focusOn();
            if (cm == null) {
                logger.info("Interceptor (%s)'s focusOn return null.", interceptor.getClass().getName());
                continue;
            }
            if (cm.isMatch(type)
                    && interceptor.matches(type, constructor, parameters)) {
                String key = MethodUtil.signature(type, constructor);
                InterceptorChain<ConstructorInterceptor> chain = TransformerManager.CONSTRUCTOR.interceptorChainOf(key);
                chain.addInterceptor(interceptor);
                logger.info("%s has interceptors %s.", constructor, arrayStrOfInterceptors(chain.getInterceptors()));
                hasInterceptor = true;
                builder = buildFieldDefine(builder, type, interceptor);
            }
        }
        if(hasInterceptor) {
            
            builder = builder.constructor(named(constructor.getActualName())
                    .and(hasParameters(new ParametersMatcher(parameters))))
                    .intercept(SuperMethodCall.INSTANCE.andThen(to(ConstructorWeaver.class)));
        }
        return builder;
    }

    private static synchronized Builder<?> buildFieldDefine(Builder<?> builder, TypeDescription type, Interceptor interceptor) {
        FieldDefine[] fields = interceptor.fieldDefine();
        if (fields != null && fields.length > 0) {
            for (FieldDefine field : fields) {
                var list = FIELD.computeIfAbsent(type, (t) -> new ArrayList<>());
                if (!list.contains(field.name)) {
                    list.add(field.name);
                    builder = builder.defineField(field.name, field.type, field.modifiers);
                }
            }
        }
        return builder;
    }

    public static String arrayStrOfInterceptors(List<? extends Interceptor> interceptors) {
        if (interceptors == null || interceptors.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (Interceptor itrcpt : interceptors) {
            sb.append(itrcpt.getClass().getName()).append(",");
        }
        int length = sb.length();
        if (length > 1) {
            sb.deleteCharAt(length - 1);
        }
        sb.append("]");
        return sb.toString();
    }
}
