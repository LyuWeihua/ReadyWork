/**
 *
 * Original work Copyright (c) 2016 Network New Technologies Inc.
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

package work.ready.core.config;

import work.ready.core.server.Constant;
import work.ready.core.server.Ready;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.StrUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigInjector {
    
    private static final String INJECTION_ORDER_PROPERTY = "ready.config.injection_order";
    private static final String INJECTION_ORDER_CODE = Ready.getProperty(INJECTION_ORDER_PROPERTY, "2");

    private static final Pattern pattern = Pattern.compile("\\$\\{(.+?)\\}");

    private static final String[] trueArray = {"yes", "Yes", "YES", "true", "True", "TRUE"};
    private static final String[] falseArray = {"no", "No", "NO", "false", "False", "FALSE"};

    public static String getStringValue(String string){ return String.valueOf(getInjectValue(string, false)); }
    public static Object getInjectValue(String string){ return getInjectValue(string, true); }
    
    private static Object getInjectValue(String string, boolean typeCast) {
        if(string != null && string.indexOf("}") > string.indexOf("${")) {
            Matcher m = pattern.matcher(string);
            StringBuffer sb = new StringBuffer();
            
            while (m.find()) {
                
                Object value = getValue(m.group(1), typeCast);
                
                if (!(value instanceof String)) {
                    return value;
                }
                m.appendReplacement(sb, (String) value);
            }
            return m.appendTail(sb).toString();
        } else {
            return string;
        }
    }

    public static String[] getStringValue(String[] value) {
        if (value == null) {
            return null;
        }

        String[] array = new String[value.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = getStringValue(value[i]);
        }
        return array;
    }

    public static boolean isExclusionConfigFile(String configName) {
        return Constant.VALUES_CONFIG_NAME.equals(configName) || Constant.STATUS_CONFIG_NAME.equals(configName) || Constant.BOOTSTRAP_CONFIG_NAME.equals(configName);
    }

    private static Object getValue(String content, boolean typeCast) throws ConfigException {
        InjectionPattern injectionPattern = getInjectionPattern(content);
        Object value = null;
        if (injectionPattern != null) {
            
            Boolean containsField = false;
            
            Object argValue = (Ready.config().getCmdArgs() != null) ? Ready.config().getCmdArgs().get(injectionPattern.getKey()) : null;
            argValue = (typeCast) ? typeCast((String)argValue) : argValue;
            Object envValue = System.getenv(injectionPattern.getKey().replace('.','_'));
            envValue = (typeCast) ? typeCast((String)envValue) : envValue;
            Object jvmValue = System.getProperty(injectionPattern.getKey());
            jvmValue = (typeCast) ? typeCast((String)jvmValue) : jvmValue;
            Map<String, Object> valueMap = Ready.config().getMapConfig(Constant.VALUES_CONFIG_NAME);
            Object fileValue = (valueMap != null) ? valueMap.get(injectionPattern.getKey()) : null;
            
            if (argValue != null) {
                envValue = argValue;
            } else if (jvmValue != null) {
                envValue = jvmValue;
            }
            if ((INJECTION_ORDER_CODE.equals("2") && envValue != null) || (INJECTION_ORDER_CODE.equals("1") && fileValue == null)) {
                value = envValue;
            } else {
                value = fileValue;
            }
            
            if ((valueMap != null && valueMap.containsKey(injectionPattern.getKey())) ||
                    (Ready.config().getCmdArgs() != null && Ready.config().getCmdArgs().containsKey(injectionPattern.getKey())) ||
                    (System.getProperties() != null && System.getProperties().containsKey(injectionPattern.getKey())) ||
                    (System.getenv() != null && System.getenv().containsKey(injectionPattern.getKey().replace('.','_')))) {
                containsField = true;
            }
            
            if (value == null && !containsField) {
                value = (typeCast) ? typeCast(injectionPattern.getDefaultValue()) : injectionPattern.getDefaultValue();
                
                if (value == null || value.equals("")) {
                    String error_text = injectionPattern.getErrorText();
                    if (error_text != null && !error_text.equals("")) {
                        throw new ConfigException(error_text);
                    }
                    
                    throw new ConfigException("\"Config injection tag ${" + content + "}\" cannot be expanded");
                }
            }
        }
        return value;
    }

    public static <T> T getConfigProperty(String prefix, Class<T> beanClass, String appName) throws ConfigException {
        return getConfigProperty(prefix, beanClass, null, appName);
    }

    public static <T> T getConfigProperty(String prefix, Class<T> beanClass, BaseConfig config) throws ConfigException {
        return getConfigProperty(prefix, beanClass, config, null);
    }

    private static <T> T getConfigProperty(String prefix, Class<T> beanClass, BaseConfig config, String appName) throws ConfigException {
        if (StrUtil.notBlank(prefix)) {
            if (prefix.startsWith("${") && prefix.endsWith("}")) {
                Matcher m = pattern.matcher(prefix);
                StringBuffer sb = new StringBuffer();
                
                while (m.find()) {
                    
                    InjectionPattern injectionPattern = getInjectionPattern(m.group(1));
                    Object value = null;
                    if (injectionPattern != null) {
                        try {
                            if(config == null){
                                value = mapConfigExplore(Ready.config().getApplicationConfigMap(appName), injectionPattern.getKey().split("\\."), 0);
                            } else {
                                value = configExplore(config, injectionPattern.getKey().split("\\."), 1);
                            }
                            if (value != null) {
                                if(value instanceof Optional) value = ((Optional) value).get();
                            } else {
                                String defaultValue = injectionPattern.getDefaultValue();
                                
                                if (StrUtil.isBlank(defaultValue)) {
                                    String error_text = injectionPattern.getErrorText();
                                    if (error_text != null && !error_text.equals("")) {
                                        throw new ConfigException(error_text);
                                    }
                                    
                                    throw new ConfigException("Config injection tag ${" + m.group(1) + "} appears on class cannot be expanded.");
                                }
                                try {
                                    value = ClassUtil.typeCast(beanClass, defaultValue);
                                } catch (ParseException e){
                                    throw new ConfigException("'" + defaultValue +"' cannot converter to " + beanClass.getCanonicalName() + ": " + e.getMessage());
                                }
                            }
                            
                            if (!(value instanceof String)) {
                                return (T) value;
                            }

                        } catch (InvocationTargetException | IllegalAccessException e) {
                        }
                    }
                    m.appendReplacement(sb, (String) value);
                }
                return String.class.equals(beanClass) ? (T) m.appendTail(sb).toString() : null;
            } else {
                return String.class.equals(beanClass) ? (T) prefix : null;
            }
        }
        return null;
    }

    public static <T> T getConfigBean(String prefix, Class<T> beanClass, String configFile, String appName) throws ConfigException {
        if(StrUtil.isBlank(prefix)) {
            if(StrUtil.notBlank(configFile)) return (T) Ready.config().getObjectConfig(configFile, beanClass);
        } else {
            Map<String, Object> configMap = (StrUtil.notBlank(configFile)) ? Ready.config().getMapConfig(configFile) : null;
            if(configMap == null) configMap = Ready.config().getApplicationConfigMap(appName);
            if (configMap != null && StrUtil.notBlank(prefix) && prefix.startsWith("${") && prefix.endsWith("}")) {
                Matcher m = pattern.matcher(prefix);
                
                if (m.find()) {
                    
                    InjectionPattern injectionPattern = getInjectionPattern(m.group(1));
                    if (injectionPattern != null) {
                        Object configItem = mapConfigExplore(configMap, injectionPattern.getKey().split("\\."), 0);
                        Object value = Config.convertItemToObject(configItem, beanClass);
                        if (value == null) {
                            String error_text = injectionPattern.getErrorText();
                            if (error_text != null && !error_text.equals("")) {
                                throw new ConfigException(error_text);
                            }
                        }
                        return (T) value;
                    }
                }
            }
        }
        return null;
    }

    public static <T> T getConfigBean(String prefix, Class<T> beanClass, BaseConfig config) throws ConfigException {
        if (config != null && StrUtil.notBlank(prefix) && prefix.startsWith("${") && prefix.endsWith("}")) {
            Matcher m = pattern.matcher(prefix);
            
            if (m.find()) {
                
                InjectionPattern injectionPattern = getInjectionPattern(m.group(1));
                if (injectionPattern != null) {
                    try {
                        Object value = configExplore(config, injectionPattern.getKey().split("\\."), 1);
                        if (value != null) {
                            if(value instanceof Optional) value = ((Optional) value).get();
                        } else {
                            
                            String error_text = injectionPattern.getErrorText();
                            if (error_text != null && !error_text.equals("")) {
                                throw new ConfigException(error_text);
                            }
                        }
                        return (T) value;
                    } catch (InvocationTargetException | IllegalAccessException e) {
                    }
                }
            }
        }
        return null;
    }

    private static Object configExplore(Object config, String[] domain, int depth) throws InvocationTargetException, IllegalAccessException {
        if(depth == domain.length) {
            return config;
        }
        for(int i = depth; i < domain.length; i ++){
            if(config != null) {
                Method[] methods = config.getClass().getMethods();
                for (Method method : methods) {
                    if(Boolean.class.equals(method.getReturnType()) || boolean.class.equals(method.getReturnType())){
                        if (method.getName().equals("is" + StrUtil.firstCharToUpperCase(domain[i])) && method.getParameterCount() == 0) {
                            return configExplore(method.invoke(config), domain, ++i);
                        }
                    }else
                    if (method.getName().equals("get" + StrUtil.firstCharToUpperCase(domain[i])) && method.getParameterCount() == 0) {
                        return configExplore(method.invoke(config), domain, ++i);
                    }
                }
            }
        }
        return null;
    }

    private static Object mapConfigExplore(Object config, String[] domain, int depth){
        if(depth == domain.length) {
            return config;
        }
        for(int i = depth; i < domain.length; i ++){
            if(config instanceof Map) {
                if (((Map)config).containsKey(domain[i])) {
                    return mapConfigExplore(((Map)config).get(domain[i]), domain, ++i);
                }
            }
        }
        return null;
    }

    private static InjectionPattern getInjectionPattern(String contents) {
        if (contents == null || contents.trim().equals("")) {
            return null;
        }
        InjectionPattern injectionPattern = new InjectionPattern();
        contents = contents.trim();
        
        String[] array = contents.split(":", 2);
        array[0] = array[0].trim();
        if ("".equals(array[0])) {
            return null;
        }
        
        injectionPattern.setKey(array[0]);
        if (array.length == 2) {
            
            array[1] = array[1].trim();
            
            if (array[1].startsWith("?")) {
                injectionPattern.setErrorText(array[1].substring(1));
            } else if (array[1].startsWith("$")) {
                
                if (array[1].length() == 1) {
                    injectionPattern.setDefaultValue("\\$\\{" + array[0] + "\\}");

                } else {
                    injectionPattern.setDefaultValue("\\" + array[1]);
                }
                
            } else {
                injectionPattern.setDefaultValue(array[1]);
            }
        }
        return injectionPattern;
    }

    private static class InjectionPattern {
        private String key;
        private String defaultValue;
        private String errorText;

        public String getErrorText() {
            return errorText;
        }

        public void setErrorText(String errorTest) {
            this.errorText = errorTest;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

    private static Object typeCast(String str) {
        if (str == null || str.equals("")) {
            return null;
        }
        
        for (String trueString : trueArray) {
            if (trueString.equals(str)) {
                return true;
            }
        }
        
        for (String falseString : falseArray) {
            if (falseString.equals(str)) {
                return false;
            }
        }
        
        try {
            return Integer.parseInt(str);
        } catch (Exception e1) {
            try {
                return Double.parseDouble(str);
            } catch (Exception e2) {
                return str;
            }
        }
    }
}
