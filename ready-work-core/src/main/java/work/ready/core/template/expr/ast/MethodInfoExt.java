/**
 * Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
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

package work.ready.core.template.expr.ast;

import java.lang.reflect.Method;

public class MethodInfoExt extends MethodInfo {

	protected Object objectOfExtensionClass;

	public MethodInfoExt(Object objectOfExtensionClass, Long key, Class<?> clazz, Method method) {
		super(key, clazz, method);
		this.objectOfExtensionClass = objectOfExtensionClass;

	}

	public Object invoke(Object target, Object... args) throws ReflectiveOperationException {
		Object[] finalArgs = new Object[args.length + 1];
		finalArgs[0] = target;

		if (args.length > 0) {
			System.arraycopy(args, 0, finalArgs, 1, args.length);
		}

		if (isVarArgs) {
			return invokeVarArgsMethod(objectOfExtensionClass, finalArgs);
		} else {
			return method.invoke(objectOfExtensionClass, finalArgs);
		}
	}
}

