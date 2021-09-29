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

import work.ready.core.aop.Interceptor;
import work.ready.core.aop.Invocation;
import work.ready.core.handler.Controller;
import work.ready.core.render.Render;
import work.ready.core.server.Constant;
import work.ready.core.tools.StrUtil;

public class I18nInterceptor implements Interceptor {

	private boolean isSwitchView = false;

	public I18nInterceptor() {
	}

	public I18nInterceptor(boolean isSwitchView) {
		this.isSwitchView = isSwitchView;
	}

	protected String getBaseName() {
		return I18n.defaultBaseName;
	}

	@Override
	public void intercept(Invocation inv) throws Throwable {
		Controller c = inv.getController();
		String locale = c.getParam(I18n.localeParamName);

		if (StrUtil.notBlank(locale)) {	
			c.setCookie(I18n.localeParamName, locale, Constant.DEFAULT_I18N_MAX_AGE_OF_COOKIE);
		}
		else {							
			locale = c.getCookie(I18n.localeParamName);
			if (StrUtil.isBlank(locale))
				locale = I18n.defaultLocale;
		}

		inv.invoke();

		if (isSwitchView) {
			switchView(locale, c);
		}
		else {
			Res res = I18n.use(getBaseName(), locale);
			c.setAttr(I18n.localeParamName, res);
		}
	}

	public void switchView(String locale, Controller c) {
		Render render = c.getRender();
		if (render != null) {
			String view = render.getView();
			if (view != null) {
				if (view.startsWith("/")) {
					view = "/" + locale + view;
				} else {
					view = locale + "/" + view;
				}

				render.setView(view);
			}
		}
	}
}

