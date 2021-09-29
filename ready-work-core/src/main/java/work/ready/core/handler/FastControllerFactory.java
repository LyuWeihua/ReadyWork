/**
 *
 * Original work Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
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
package work.ready.core.handler;

import work.ready.core.server.Ready;

import java.util.HashMap;
import java.util.Map;

public class FastControllerFactory extends ControllerFactory {

	private ThreadLocal<Map<Class<? extends Controller>, Controller>> buffers = new ThreadLocal<Map<Class<? extends Controller>, Controller>>() {
		protected Map<Class<? extends Controller>, Controller> initialValue() {
			return new HashMap<Class<? extends Controller>, Controller>();
		}
	};

	@Override
	public Controller getController(Class<? extends Controller> controllerClass) throws ReflectiveOperationException {
		Controller ret = buffers.get().get(controllerClass);
		if (ret == null) {
			ret = controllerClass.getDeclaredConstructor().newInstance();
			if (injectDependency) {
				Ready.beanManager().inject(ret);
			}
			buffers.get().put(controllerClass, ret);
		}
		return ret;
	}

	@Override
	public void recycle(Controller controller) {
		if (controller != null) {
			controller._clear_();
		}
	}
}

