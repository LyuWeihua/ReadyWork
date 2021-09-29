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

import java.lang.reflect.Method;

public class MethodProceeding {

    private final Object instance;
    private final Method originAccessor;
    private final Method originMethod;
    private final Object[] args;
    private Object rtVal;

    public MethodProceeding(Object instance, Method originAccessor, Method originMethod, Object[] args) {
        this.instance = instance;
        this.originAccessor = originAccessor;
        this.originMethod = originMethod;
        this.args = args;
    }

    public final void proceed() throws Exception {
        originAccessor.setAccessible(true);
        rtVal = originAccessor.invoke(instance, args);
    }

    public Method getMethod() {
        return originMethod;
    }

    public Object getInstance() {
        return instance;
    }

    public Class<?> getDeclaringClassOfMethod() {
        return originMethod.getDeclaringClass();
    }

    public Object[] getArgs() {
        return args;
    }

    public Object getRtVal() {
        return rtVal;
    }

    public void setRtVal(Object rtVal) {
        this.rtVal = rtVal;
    }

}
