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

import work.ready.core.tools.StrUtil;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class I18n {

	public static final String localeParamName = "_locale";
	static String defaultBaseName = "i18n";

	static String defaultLocale = "en_US";

	private static final ConcurrentHashMap<String, Res> resMap = new ConcurrentHashMap<String, Res>();

	private I18n(){
	}

	public static void setDefaultBaseName(String defaultBaseName) {
		if (StrUtil.isBlank(defaultBaseName)) {
			throw new IllegalArgumentException("defaultBaseName can not be blank.");
		}
		I18n.defaultBaseName = defaultBaseName;
	}

	public static void setDefaultLocale(String defaultLocale) {
		if (StrUtil.isBlank(defaultLocale)) {
			throw new IllegalArgumentException("defaultLocale can not be blank.");
		}
		I18n.defaultLocale = defaultLocale;
	}

	public static Res use(String baseName, String locale) {
		String resKey = baseName + locale;
		Res res = resMap.get(resKey);
		if (res == null) {
			res = new Res(baseName, locale);
			resMap.put(resKey, res);
		}
		return res;
	}

	public static Res use(String baseName, Locale locale) {
		return use(baseName, toLocale(locale));
	}

	public static Res use(String locale) {
		return use(defaultBaseName, locale);
	}

	public static Res use() {
		return use(defaultBaseName, defaultLocale);
	}

	public static Locale toLocale(String locale) {
		String[] array = locale.split("_");
		if (array.length == 1) {
			return new Locale(array[0]);
		}
		return new Locale(array[0], array[1]);
	}

	public static String toLocale(Locale locale) {
		return locale.getLanguage() + "_" + locale.getCountry();
	}
}
