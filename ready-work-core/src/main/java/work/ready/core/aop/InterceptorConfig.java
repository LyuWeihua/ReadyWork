/**
 *
 * Original work (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package work.ready.core.aop;

import work.ready.core.config.BaseConfig;
import work.ready.core.server.Ready;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class InterceptorConfig extends BaseConfig {

    public static final Interceptor[] NULL_INTERS = new Interceptor[0];

    private Interceptor[] globalActionInterceptors = NULL_INTERS;
    private Interceptor[] globalServiceInterceptors = NULL_INTERS;

    private List<AopComponent> aopComponents = new ArrayList<>();

    private final ConcurrentHashMap<Class<? extends Interceptor>, Interceptor> singletonMap = new ConcurrentHashMap<Class<? extends Interceptor>, Interceptor>(32, 0.5F);

    @Override
    public void validate(){

    }

    public synchronized void addAopComponent(AopComponent component){
        aopComponents.add(component);
    }

    public List<AopComponent> getAopComponents(){
        return aopComponents;
    }

    public Interceptor[] getGlobalActionInterceptors() {
        return globalActionInterceptors;
    }

    public Interceptor[] getGlobalServiceInterceptors() {
        return globalServiceInterceptors;
    }

    Interceptor getSingletonMap(Class<? extends Interceptor> interceptorClass){
        return singletonMap.get(interceptorClass);
    }

    synchronized void setSingletonMap(Class<? extends Interceptor> interceptorClass, Interceptor interceptor){
        singletonMap.put(interceptorClass, interceptor);
    }

    public void addGlobalActionInterceptor(Interceptor... inters) {
        addGlobalInterceptor(true, inters);
    }

    public void addGlobalServiceInterceptor(Interceptor... inters) {
        addGlobalInterceptor(false, inters);
    }

    private synchronized void addGlobalInterceptor(boolean forAction, Interceptor... interceptors) {
        if (interceptors == null || interceptors.length == 0) {
            throw new IllegalArgumentException("interceptors can not be null.");
        }

        for (Interceptor interceptor : interceptors) {
            if (interceptor == null) {
                throw new IllegalArgumentException("interceptor can not be null.");
            }
            if (singletonMap.containsKey(interceptor.getClass())) {
                throw new IllegalArgumentException("interceptor already exists, interceptor must be singleton, do not create more then one instance of the same Interceptor Class.");
            }
        }

        for (Interceptor interceptor : interceptors) {
            singletonMap.put(interceptor.getClass(), Ready.beanManager().inject(interceptor));
        }

        Interceptor[] globalInterceptors = forAction ? globalActionInterceptors : globalServiceInterceptors;
        Interceptor[] temp = new Interceptor[globalInterceptors.length + interceptors.length];
        System.arraycopy(globalInterceptors, 0, temp, 0, globalInterceptors.length);
        System.arraycopy(interceptors, 0, temp, globalInterceptors.length, interceptors.length);

        if (forAction) {
            globalActionInterceptors = temp;
        } else {
            globalServiceInterceptors = temp;
        }
    }
}
