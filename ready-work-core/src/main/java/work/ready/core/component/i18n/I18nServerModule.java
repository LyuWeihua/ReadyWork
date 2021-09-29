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
package work.ready.core.component.i18n;

import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.AttachmentKey;
import work.ready.core.config.Config;
import work.ready.core.handler.BaseHandler;
import work.ready.core.handler.ServerModule;
import work.ready.core.handler.cookie.CookieItem;
import work.ready.core.server.Constant;
import work.ready.core.tools.StrUtil;

import java.util.MissingResourceException;

public class I18nServerModule extends ServerModule {

    private I18nConfig i18nConfig;
    public static final AttachmentKey<Res> i18n = AttachmentKey.create(Res.class);
    private String defaultLocale = "en_US";

    private volatile BaseHandler next;

    public I18nServerModule() {
        setOrder(2); 
        
    }

    @Override
    public void initialize(){
        if(config != null)
            i18nConfig = (I18nConfig) Config.convertItemToObject(config, I18nConfig.class);
        if(i18nConfig != null) {
            setEnabled(i18nConfig.isEnabled());
            defaultLocale = i18nConfig.getDefaultLocale();
            I18n.setDefaultLocale(defaultLocale);
            I18n.setDefaultBaseName(i18nConfig.getDefaultBaseName());
        }
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String locale = (exchange.getQueryParameters().get(I18n.localeParamName) != null) ? exchange.getQueryParameters().get(I18n.localeParamName).peekFirst() : null;
        if(locale == null) locale = exchange.getRequestHeaders().getFirst(I18n.localeParamName);
        if (StrUtil.notBlank(locale)) {	
            exchange.setResponseCookie(new CookieItem(I18n.localeParamName, locale, Constant.DEFAULT_I18N_MAX_AGE_OF_COOKIE).setPath("/"));
        } else {	
            Cookie cookie = exchange.getRequestCookies().get(I18n.localeParamName);
            if((cookie == null || StrUtil.isBlank(cookie.getValue()))) {
                locale = defaultLocale;
                String acceptLang = exchange.getRequestHeaders().getFirst("Accept-language");
                if(StrUtil.notBlank(acceptLang)) {
                    String[] langSegment = StrUtil.split(acceptLang, ';');
                    if(langSegment.length > 0) {
                        String[] lang = StrUtil.split(langSegment[0], ','); 
                        if(lang.length > 0) {
                            for (String s : lang) {
                                if (s.contains("-")) {
                                    locale = s.replace('-', '_');
                                    break;
                                }
                            }
                        }
                    }
                }
            } else {
                locale = cookie.getValue();
            }
        }
        Res res;
        try {
            res = I18n.use(I18n.defaultBaseName, locale);
        } catch (MissingResourceException e){
            res = null;  
        }
        exchange.putAttachment(i18n, res);
        manager.next(exchange, next);
    }

    @Override
    public BaseHandler getNext() {
        return next;
    }

    @Override
    public ServerModule setNext(final BaseHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public void register() {
        manager.registerModule(I18nServerModule.class.getName(), config);
    }

}
