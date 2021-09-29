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
package work.ready.core.aop.transformer.utils;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import work.ready.core.aop.transformer.TransformerManager;

import java.util.List;
import java.util.Map;

public class MatchUtil {

    public static final String isConstructor = "isConstructor";

    public static ElementMatcher.Junction<TypeDescription> buildTypesMatcher(Map<String, Object> includeMap, Map<String, Object> excludeMap) {
        ElementMatcher.Junction<TypeDescription> matcher = ElementMatchers.none();
        if (includeMap != null && !includeMap.isEmpty()) {
            String namedVal = getString(includeMap.get("named"));
            String nameStartsWithVal = getString(includeMap.get("nameStartsWith"));
            String nameEndsWithVal = getString(includeMap.get("nameEndsWith"));
            String nameContainsVal = getString(includeMap.get("nameContains"));
            String nameMatchesVal = getString(includeMap.get("nameMatches"));
            String hasSuperTypeVal = getString(includeMap.get("hasSuperType"));
            String hasAnnotationVal = getString(includeMap.get("hasAnnotation"));
            ElementMatcher.Junction<TypeDescription> includeMatcher = null;
            if (namedVal != null) {
                String[] arr = namedVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    if (includeMatcher == null) {
                        includeMatcher = ElementMatchers.named(arr[i]);
                        continue;
                    }
                    includeMatcher = includeMatcher.or(ElementMatchers.named(arr[i]));
                }
            }
            if (nameStartsWithVal != null) {
                String[] arr = nameStartsWithVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    if (includeMatcher == null) {
                        includeMatcher = ElementMatchers.nameStartsWith(arr[i]);
                        continue;
                    }
                    includeMatcher = includeMatcher.or(ElementMatchers.nameStartsWith(arr[i]));
                }
            }
            if (nameEndsWithVal != null) {
                String[] arr = nameEndsWithVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    if (includeMatcher == null) {
                        includeMatcher = ElementMatchers.nameEndsWith(arr[i]);
                        continue;
                    }
                    includeMatcher = includeMatcher.or(ElementMatchers.nameEndsWith(arr[i]));
                }
            }
            if (nameContainsVal != null) {
                String[] arr = nameContainsVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    if (includeMatcher == null) {
                        includeMatcher = ElementMatchers.nameContains(arr[i]);
                        continue;
                    }
                    includeMatcher = includeMatcher.or(ElementMatchers.nameContains(arr[i]));
                }
            }
            if (nameMatchesVal != null) {
                String[] arr = nameMatchesVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    if (includeMatcher == null) {
                        includeMatcher = ElementMatchers.nameMatches(arr[i]);
                        continue;
                    }
                    includeMatcher = includeMatcher.or(ElementMatchers.nameMatches(arr[i]));
                }
            }
            if (hasSuperTypeVal != null) {
                String[] arr = hasSuperTypeVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    if (includeMatcher == null) {
                        includeMatcher = ElementMatchers.hasSuperType(ElementMatchers.<TypeDescription>named(arr[i]));
                        continue;
                    }
                    includeMatcher = includeMatcher.or(ElementMatchers.hasSuperType(ElementMatchers.<TypeDescription>named(arr[i])));
                }
            }
            if (hasAnnotationVal != null) {
                String[] arr = hasAnnotationVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    if (includeMatcher == null) {
                        includeMatcher = ElementMatchers.hasAnnotation(ElementMatchers.annotationType(ElementMatchers.<TypeDescription>named(arr[i])));
                        continue;
                    }
                    includeMatcher = includeMatcher.or(ElementMatchers.hasAnnotation(ElementMatchers.annotationType(ElementMatchers.<TypeDescription>named(arr[i]))));
                }
            }
            if (includeMatcher != null) {
                matcher = includeMatcher;
            }
        }
        if (excludeMap != null && !excludeMap.isEmpty()) {
            String namedVal = getString(excludeMap.get("named"));
            String nameStartsWithVal = getString(excludeMap.get("nameStartsWith"));
            String nameEndsWithVal = getString(excludeMap.get("nameEndsWith"));
            String nameContainsVal = getString(excludeMap.get("nameContains"));
            String nameMatchesVal = getString(excludeMap.get("nameMatches"));
            String hasSuperTypeVal = getString(excludeMap.get("hasSuperType"));
            String hasAnnotationVal = getString(excludeMap.get("hasAnnotation"));
            ElementMatcher.Junction<TypeDescription> excludeMatch = ElementMatchers.none();
            if (namedVal != null) {
                String[] arr = namedVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    matcher = matcher.and(ElementMatchers.not(ElementMatchers.named(arr[i])));
                }
            }
            if (nameStartsWithVal != null) {
                String[] arr = nameStartsWithVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    matcher = matcher.and(ElementMatchers.not(ElementMatchers.<TypeDescription>nameStartsWith(arr[i])));
                }
            }
            if (nameEndsWithVal != null) {
                String[] arr = nameEndsWithVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    matcher = matcher.and(ElementMatchers.not(ElementMatchers.<TypeDescription>nameEndsWith(arr[i])));
                }
            }
            if (nameContainsVal != null) {
                String[] arr = nameContainsVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    matcher = matcher.and(ElementMatchers.not(ElementMatchers.<TypeDescription>nameContains(arr[i])));
                }
            }
            if (nameMatchesVal != null) {
                String[] arr = nameMatchesVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    matcher = matcher.and(ElementMatchers.not(ElementMatchers.<TypeDescription>nameMatches(arr[i])));
                }
            }
            if (hasSuperTypeVal != null) {
                String[] arr = hasSuperTypeVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    matcher = matcher.and(ElementMatchers.not(ElementMatchers.hasSuperType(ElementMatchers.<TypeDescription>named(arr[i]))));
                }
            }
            if (hasAnnotationVal != null) {
                String[] arr = hasAnnotationVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    matcher = matcher.and(ElementMatchers.not(ElementMatchers.hasAnnotation(ElementMatchers.annotationType(ElementMatchers.<TypeDescription>named(arr[i])))));
                }
            }
        }
        return matcher;
    }

    public static ElementMatcher.Junction<MethodDescription> buildMethodsMatcher(Map<String, Object> includeMap, Map<String, Object> excludeMap) {
        ElementMatcher.Junction<MethodDescription> matcher = ElementMatchers.isMethod();
        if (includeMap != null && !includeMap.isEmpty()) {
            boolean constructorVal = getBoolean(includeMap.get(isConstructor));
            String namedVal = getString(includeMap.get("named"));
            String nameStartsWithVal = getString(includeMap.get("nameStartsWith"));
            String nameEndsWithVal = getString(includeMap.get("nameEndsWith"));
            String nameContainsVal = getString(includeMap.get("nameContains"));
            String nameMatchesVal = getString(includeMap.get("nameMatches"));
            String isAnnotatedWith = getString(includeMap.get("isAnnotatedWith"));
            ElementMatcher.Junction<MethodDescription> includeMatcher = null;
            if (namedVal != null) {
                String[] arr = namedVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    if (includeMatcher == null) {
                        includeMatcher = ElementMatchers.named(arr[i]);
                        continue;
                    }
                    includeMatcher = includeMatcher.or(ElementMatchers.named(arr[i]));
                }
            }
            if (nameStartsWithVal != null) {
                String[] arr = nameStartsWithVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    if (includeMatcher == null) {
                        includeMatcher = ElementMatchers.nameStartsWith(arr[i]);
                        continue;
                    }
                    includeMatcher = includeMatcher.or(ElementMatchers.nameStartsWith(arr[i]));
                }
            }
            if (nameEndsWithVal != null) {
                String[] arr = nameEndsWithVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    if (includeMatcher == null) {
                        includeMatcher = ElementMatchers.nameEndsWith(arr[i]);
                        continue;
                    }
                    includeMatcher = includeMatcher.or(ElementMatchers.nameEndsWith(arr[i]));
                }
            }
            if (nameContainsVal != null) {
                String[] arr = nameContainsVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    if (includeMatcher == null) {
                        includeMatcher = ElementMatchers.nameContains(arr[i]);
                        continue;
                    }
                    includeMatcher = includeMatcher.or(ElementMatchers.nameContains(arr[i]));
                }
            }
            if (nameMatchesVal != null) {
                String[] arr = nameMatchesVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    if (includeMatcher == null) {
                        includeMatcher = ElementMatchers.nameMatches(arr[i]);
                        continue;
                    }
                    includeMatcher = includeMatcher.or(ElementMatchers.nameMatches(arr[i]));
                }
            }
            if (isAnnotatedWith != null) {
                String[] arr = isAnnotatedWith.split(",");
                for (int i = 0; i < arr.length; i++) {
                    if (includeMatcher == null) {
                        includeMatcher = ElementMatchers.isAnnotatedWith(ElementMatchers.<TypeDescription>named(arr[i]));
                        continue;
                    }
                    includeMatcher = includeMatcher.or(ElementMatchers.<MethodDescription>isAnnotatedWith(ElementMatchers.<TypeDescription>named(arr[i])));
                }
            }
            if (constructorVal) {
                includeMatcher = includeMatcher == null ? ElementMatchers.isConstructor() : includeMatcher.or(ElementMatchers.isConstructor());
            }
            if (includeMatcher != null) {
                matcher = includeMatcher;
            }
        }

        if (excludeMap != null && !excludeMap.isEmpty()) {
            String namedVal = getString(excludeMap.get("named"));
            String nameStartsWithVal = getString(excludeMap.get("nameStartsWith"));
            String nameEndsWithVal = getString(excludeMap.get("nameEndsWith"));
            String nameContainsVal = getString(excludeMap.get("nameContains"));
            String nameMatchesVal = getString(excludeMap.get("nameMatches"));
            String isAnnotatedWith = getString(excludeMap.get("isAnnotatedWith"));
            if (namedVal != null) {
                String[] arr = namedVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    matcher = matcher.and(ElementMatchers.not(ElementMatchers.named(arr[i])));
                }
            }
            if (nameStartsWithVal != null) {
                String[] arr = nameStartsWithVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    matcher = matcher.and(ElementMatchers.not(ElementMatchers.<MethodDescription>nameStartsWith(arr[i])));
                }
            }
            if (nameEndsWithVal != null) {
                String[] arr = nameEndsWithVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    matcher = matcher.and(ElementMatchers.not(ElementMatchers.<MethodDescription>nameEndsWith(arr[i])));
                }
            }
            if (nameContainsVal != null) {
                String[] arr = nameContainsVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    matcher = matcher.and(ElementMatchers.not(ElementMatchers.<MethodDescription>nameContains(arr[i])));
                }
            }
            if (nameMatchesVal != null) {
                String[] arr = nameMatchesVal.split(",");
                for (int i = 0; i < arr.length; i++) {
                    matcher = matcher.and(ElementMatchers.not(ElementMatchers.<MethodDescription>nameMatches(arr[i])));
                }
            }
            if (isAnnotatedWith != null) {
                String[] arr = isAnnotatedWith.split(",");
                for (int i = 0; i < arr.length; i++) {
                    matcher = matcher.and(ElementMatchers.not(ElementMatchers.<MethodDescription>isAnnotatedWith(ElementMatchers.<TypeDescription>named(arr[i]))));
                }
            }
        }
        return matcher;
    }

    private static boolean getBoolean(Object boolOrString) {
        if(boolOrString != null) {
            if(boolOrString.getClass().isAssignableFrom(Boolean.class)) {
                return Boolean.valueOf((Boolean) boolOrString);
            } else if (boolOrString.getClass().isAssignableFrom(String.class)) {
                return Boolean.valueOf((String) boolOrString);
            }
        }
        return false;
    }

    private static String getString(Object listOrString) {
        if(listOrString != null) {
            if(listOrString instanceof List) {
                StringBuffer sb = new StringBuffer();
                ((List) listOrString).forEach(item->{
                    if(item instanceof String) {
                        if(sb.length() > 0) sb.append(',');
                        sb.append(item);
                    }
                });
                return sb.toString();
            } else if(listOrString instanceof String) {
                return (String)listOrString;
            }
        }
        return null;
    }
}
