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

package work.ready.core.component.i18n;

import java.util.ResourceBundle;

public class Res {

	private final ResourceBundle resourceBundle;

	public Res(String baseName, String locale) {
		if (baseName == null) {
			throw new IllegalArgumentException("baseName can not be blank");
		}
		if (locale == null) {
			throw new IllegalArgumentException("locale can not be blank, the format like this: zh_CN or en_US");
		}

		this.resourceBundle = ResourceBundle.getBundle(baseName, I18n.toLocale(locale));
	}

	public String get(String key) {
		return resourceBundle.getString(key);
	}

	public String format(String key, Object... arguments) {
		return work.ready.core.tools.StrUtil.format(resourceBundle.getString(key), arguments);
	}

	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
}
