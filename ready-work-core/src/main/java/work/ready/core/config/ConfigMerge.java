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

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Constant;
import work.ready.core.server.Ready;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ConfigMerge {
    private static final Log logger = LogFactory.getLog(ConfigMerge.class);

    private  static String configMergeByReplace = "configMergeByReplace";

    public static boolean isExclusionConfigFile(String configName) {
        return Constant.VALUES_CONFIG_NAME.equals(configName) || Constant.STATUS_CONFIG_NAME.equals(configName);
    }

    protected static void mergeMap(String configName, Map<String, Object> config) {
        if(isExclusionConfigFile(configName)) return;
        merge(config, !ConfigInjector.isExclusionConfigFile(configName), null);
    }

    private static void merge(Object m1, boolean inject, String path) {
        if (m1 instanceof Map) {
            Iterator<Object> fieldNames = ((Map<Object, Object>) m1).keySet().iterator();
            String fieldName = null;
            while (fieldNames.hasNext()) {
                fieldName = String.valueOf(fieldNames.next());
                String currentPath = (path != null) ? path + "." + fieldName : fieldName;
                Object field1 = null;

                field1 = Ready.config().getCmdArg(currentPath);
                
                if(field1 == null){
                    field1 = System.getProperty(currentPath);
                }
                
                if(field1 == null){
                    field1 = System.getenv(currentPath.replace('.','_'));
                }
                
                if(field1 == null){
                    field1 = ((Map<String, Object>) m1).get(fieldName);
                }
                if (field1 != null) {
                    if (field1 instanceof Map || field1 instanceof List) {
                        merge(field1, inject, currentPath);
                    
                    } else if (field1 instanceof String) {
                        if(inject) {
                            
                            Object injectValue = ConfigInjector.getInjectValue((String) field1);
                            ((Map<String, Object>) m1).put(fieldName, injectValue);
                        } else {
                            ((Map<String, Object>) m1).put(fieldName, field1);
                        }
                    }
                }
            }
        } else if (m1 instanceof List) {
            for (int i = 0; i < ((List<Object>) m1).size(); i++) {
                Object field1 = ((List<Object>) m1).get(i);
                if (field1 instanceof Map || field1 instanceof List) {
                    merge(field1, inject, path == null ? ""+i : path + "." + i);
                
                } else if (field1 instanceof String && inject) {
                    
                    Object injectValue = ConfigInjector.getInjectValue((String) field1);
                    ((List<Object>) m1).set(i, injectValue);
                }
            }
        }
    }

    private static ArrayList<String> types = new ArrayList<>(Arrays.asList(
            "java.lang.Integer",
            "java.lang.Double",
            "java.lang.Float",
            "java.lang.Long",
            "java.lang.Short",
            "java.lang.Byte",
            "java.lang.Boolean",
            "java.lang.Character",
            "java.lang.String",
            "int","double","long","short","byte","boolean","char","float"));

    protected static void mergeConfigMap(Map<String, Object> mainConfig, Map<String, Object> extendedConfig){
        mergeConfigMap(mainConfig, extendedConfig, false);
    }

    protected static void mergeConfigMap(Map<String, Object> mainConfig, Map<String, Object> extendedConfig, boolean forBootStrap){
        if(mainConfig != null && extendedConfig != null) {
            List<String> replaceRule = (List<String>) extendedConfig.get(configMergeByReplace);
            if (replaceRule == null) replaceRule = new ArrayList<>();
            for (String key : extendedConfig.keySet()) {
                if (!configMergeByReplace.equals(key)) {
                    if (mainConfig.get(key) == null || replaceRule.contains(key) || replaceRule.contains(extendedConfig.get(key).getClass().getCanonicalName())) {
                        mainConfig.put(key, extendedConfig.get(key));
                    } else {
                        if (Map.class.isAssignableFrom(mainConfig.get(key).getClass()) && Map.class.isAssignableFrom(extendedConfig.get(key).getClass())) {
                            mergeConfigMap((Map<String, Object>)mainConfig.get(key), (Map<String, Object>)extendedConfig.get(key), forBootStrap);
                        } else if (List.class.isAssignableFrom(mainConfig.get(key).getClass()) && List.class.isAssignableFrom(extendedConfig.get(key).getClass())) {
                            List mainList = (List)mainConfig.get(key);
                            List extendedList = (List)extendedConfig.get(key);
                            if(forBootStrap) {
                                for (int i = extendedList.size() - 1; i > 0; i--) { 
                                    mainList.remove(extendedList.get(i));
                                    mainList.add(0, extendedList.get(i));
                                }
                            } else{
                                for (int i = 0; i < extendedList.size(); i++) { 
                                    mainList.remove(extendedList.get(i));
                                    mainList.add(extendedList.get(i));
                                }
                            }
                        } else {
                            mainConfig.put(key, extendedConfig.get(key));
                        }
                    }
                } else {
                    mainConfig.put(key, extendedConfig.get(key));
                }
            }
        }
    }

    protected static void deepCopy(Map<String, Object> targetMap, Map<String, Object> originalMap){
        if(originalMap == null) return;
        for (String key : originalMap.keySet()) {
            if (originalMap.get(key) != null && Map.class.isAssignableFrom(originalMap.get(key).getClass())) {
                Map<String, Object> newMap = new LinkedHashMap<>();
                targetMap.put(key, newMap);
                deepCopy(newMap, (Map<String, Object>)originalMap.get(key));
            } else if (originalMap.get(key) != null && List.class.isAssignableFrom(originalMap.get(key).getClass())) {
                List<Object> newList = new LinkedList<>();
                targetMap.put(key, newList);
                deepCopy(newList, (List)originalMap.get(key));
            } else {
                targetMap.put(key, originalMap.get(key));
            }
        }
    }
    protected static void deepCopy(List targetList, List originalList){
        if(originalList == null) return;
        for(var val : originalList) {
            if (val != null && Map.class.isAssignableFrom(val.getClass())) {
                Map<String, Object> newMap = new LinkedHashMap<>();
                targetList.add(newMap);
                deepCopy(newMap, (Map<String, Object>)val);
            } else if (val != null && List.class.isAssignableFrom(val.getClass())) {
                List<Object> newList = new LinkedList<>();
                targetList.add(newList);
                deepCopy(newList, (List)val);
            } else {
                targetList.add(val);
            }
        }
    }

    protected static <T> void compareAndMergeConfig(T mainConfig, T defaultConfig, T extendedConfig){
        Class<? extends Object> defaultConfigClass = defaultConfig.getClass();
        Class<? extends Object> extendedConfigClass = extendedConfig.getClass();
        Class<? extends Object> mainConfigClass = mainConfig.getClass();

        Field[] defaultConfigFields = defaultConfigClass.getDeclaredFields();
        Field[] mainConfigFields = mainConfigClass.getDeclaredFields();
        Field[] extendedConfigFields = extendedConfigClass.getDeclaredFields();

        for(int i = 0; i < extendedConfigFields.length; i++) {
            Field mainField = mainConfigFields[i];
            if (Modifier.isStatic(mainField.getModifiers()) || Modifier.isFinal(mainField.getModifiers())) {
                continue;
            }
            mainField.setAccessible(true);
            Field defaultField = defaultConfigFields[i];
            defaultField.setAccessible(true);
            Field extendedField = extendedConfigFields[i];
            extendedField.setAccessible(true);
            try {
                var defaultFieldValue = defaultField.get(defaultConfig);
                var extendedFieldValue = extendedField.get(extendedConfig);
                var mainFieldValue = mainField.get(mainConfig);
                if(extendedFieldValue != null && !extendedFieldValue.equals(defaultFieldValue)) {
                    if(types.contains(extendedField.getType().getName()) || mainFieldValue == null) {
                        mainField.set(mainConfig, extendedFieldValue);
                    } else {
                        
                        if(List.class.isAssignableFrom(extendedField.getType())) {
                            List defaultList = (List)defaultFieldValue;
                            List extendedList = (List)extendedFieldValue;
                            if(!extendedList.equals(defaultList)) {
                                if(defaultList != null && defaultList.size() == extendedList.size() && defaultList.containsAll(extendedList)) {
                                    continue;
                                }
                                List mainList = (List) mainFieldValue;
                                mainList.addAll(extendedList);
                                mainField.set(mainConfig, mainList);
                            }
                        } else if(Set.class.isAssignableFrom(extendedField.getType())) {
                            Set defaultSet = (Set)defaultFieldValue;
                            Set extendedSet = (Set)extendedFieldValue;
                            if(!extendedSet.equals(defaultSet)) {
                                if(defaultSet != null && defaultSet.size() == extendedSet.size() && defaultSet.containsAll(extendedSet)) {
                                    continue;
                                }
                                Set mainSet = (Set) mainFieldValue;
                                mainSet.addAll(extendedSet);
                                mainField.set(mainConfig, mainSet);
                            }
                        } else if(Map.class.isAssignableFrom(extendedField.getType())) {
                            Map defaultMap = (Map)defaultFieldValue;
                            Map extendedMap = (Map)extendedFieldValue;
                            if(!extendedMap.equals(defaultMap)) {
                                if(defaultMap != null && defaultMap.size() == extendedMap.size()) {
                                    boolean equal = true;
                                    for(Object key : defaultMap.keySet()){
                                        if(!extendedMap.containsKey(key) || !defaultMap.get(key).equals(extendedMap.get(key))) {
                                            equal = false;
                                        }
                                    }
                                    if(equal) {
                                        continue;
                                    }
                                }
                                Map mainMap = (Map) mainFieldValue;
                                extendedMap.forEach((k, v) -> mainMap.put(k, v));
                                mainField.set(mainConfig, mainMap);
                            }
                        } else {
                            
                            compareAndMergeConfig(mainFieldValue, defaultFieldValue, extendedFieldValue);
                        }
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    protected static <T> void mergeConfig(T mainConfig, T extendedConfig){
        Class<? extends Object> extendedConfigClass = extendedConfig.getClass();
        Class<? extends Object> mainConfigClass = mainConfig.getClass();

        Field[] mainConfigFields = mainConfigClass.getDeclaredFields();
        Field[] extendedConfigFields = extendedConfigClass.getDeclaredFields();

        List<String> replaceRule = new ArrayList<>();
        try {
            Field replaceRuleField = extendedConfigClass.getDeclaredField(configMergeByReplace);
            if(replaceRuleField != null){
                replaceRuleField.setAccessible(true);
                Object replaceRuleObject = replaceRuleField.get(extendedConfig);
                if (replaceRuleObject != null) {
                    if (List.class.isAssignableFrom(replaceRuleObject.getClass())) {
                        List<String> list = (List<String>) replaceRuleObject;
                        replaceRule.addAll(list);
                    }
                }
            }
        }catch (NoSuchFieldException | IllegalAccessException e){
        }

        for(int i = 0; i < extendedConfigFields.length; i++){
            Field mainField = mainConfigFields[i];
            if(Modifier.isStatic(mainField.getModifiers()) || Modifier.isFinal(mainField.getModifiers())){
                continue;
            }
            Field extendedField = extendedConfigFields[i];
            extendedField.setAccessible(true);
            mainField.setAccessible(true);
            try {
                var extendedFieldValue = extendedField.get(extendedConfig);
                var mainFieldValue = mainField.get(mainConfig);
                if(!(extendedFieldValue == null) && !"serialVersionUID".equals(extendedField.getName())){
                    if(!configMergeByReplace.equals(extendedField.getName())) {
                        if (!extendedFieldValue.equals(mainFieldValue)) {

                            if (types.contains(extendedField.getType().getName()) || mainFieldValue == null || replaceRule.contains(extendedField.getName()) || replaceRule.contains(extendedField.getType().getCanonicalName())) {
                                mainField.set(mainConfig, extendedFieldValue);
                            } else {
                                if (List.class.isAssignableFrom(extendedField.getType())) {
                                    List mainList = (List) mainFieldValue;
                                    List extendedList = (List) extendedFieldValue;
                                    mainList.addAll(extendedList);
                                    mainField.set(mainConfig, mainList);
                                } else if (Set.class.isAssignableFrom(extendedField.getType())) {
                                    Set mainSet = (Set) mainFieldValue;
                                    Set extendedSet = (Set) extendedFieldValue;
                                    mainSet.addAll(extendedSet);
                                    mainField.set(mainConfig, mainSet);
                                } else if (Map.class.isAssignableFrom(extendedField.getType())) {
                                    Map mainMap = (Map) mainFieldValue;
                                    Map extendedMap = (Map) extendedFieldValue;
                                    extendedMap.forEach((k, v) -> mainMap.put(k, v));
                                    mainField.set(mainConfig, mainMap);
                                } else {
                                    
                                    mergeConfig(mainFieldValue, extendedFieldValue);
                                }
                            }
                        }
                    } else {
                        mainField.set(mainConfig, extendedFieldValue);
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    protected static void mergeStatusConfig(Map<String, Object> statusConfig, Map<String, Object> extStatusConfig) {
        if (extStatusConfig == null) {
            return;
        }
        
        Set<String> duplicatedStatusSet = new HashSet<>(statusConfig.keySet());
        duplicatedStatusSet.retainAll(extStatusConfig.keySet());
        if (!duplicatedStatusSet.isEmpty()) {
            logger.error("The status code(s): " + duplicatedStatusSet.toString() + " is already in use, cannot be overwritten," +
                    " please change to another status code if necessary.");
            throw new RuntimeException("The status code(s): " + duplicatedStatusSet.toString() + " in status configs are duplicated.");
        }
        statusConfig.putAll(extStatusConfig);
    }

}
