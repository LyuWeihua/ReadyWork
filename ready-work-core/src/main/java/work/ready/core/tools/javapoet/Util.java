/*
 * Copyright (C) 2015 Square, Inc.
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
 */
package work.ready.core.tools.javapoet;

import javax.lang.model.element.Modifier;
import java.util.*;

import static java.lang.Character.isISOControl;

final class Util {
  private Util() {
  }

  static <K, V> Map<K, List<V>> immutableMultimap(Map<K, List<V>> multimap) {
    LinkedHashMap<K, List<V>> result = new LinkedHashMap<>();
    for (Map.Entry<K, List<V>> entry : multimap.entrySet()) {
      if (entry.getValue().isEmpty()) continue;
      result.put(entry.getKey(), immutableList(entry.getValue()));
    }
    return Collections.unmodifiableMap(result);
  }

  static <K, V> Map<K, V> immutableMap(Map<K, V> map) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(map));
  }

  static void checkArgument(boolean condition, String format, Object... args) {
    if (!condition) throw new IllegalArgumentException(String.format(format, args));
  }

  static <T> T checkNotNull(T reference, String format, Object... args) {
    if (reference == null) throw new NullPointerException(String.format(format, args));
    return reference;
  }

  static void checkState(boolean condition, String format, Object... args) {
    if (!condition) throw new IllegalStateException(String.format(format, args));
  }

  static <T> List<T> immutableList(Collection<T> collection) {
    return Collections.unmodifiableList(new ArrayList<>(collection));
  }

  static <T> Set<T> immutableSet(Collection<T> set) {
    return Collections.unmodifiableSet(new LinkedHashSet<>(set));
  }

  static <T> Set<T> union(Set<T> a, Set<T> b) {
    Set<T> result = new LinkedHashSet<>();
    result.addAll(a);
    result.addAll(b);
    return result;
  }

  static void requireExactlyOneOf(Set<Modifier> modifiers, Modifier... mutuallyExclusive) {
    int count = 0;
    for (Modifier modifier : mutuallyExclusive) {
      if (modifiers.contains(modifier)) count++;
    }
    checkArgument(count == 1, "modifiers %s must contain one of %s",
        modifiers, Arrays.toString(mutuallyExclusive));
  }

  static String characterLiteralWithoutSingleQuotes(char c) {
    
    switch (c) {
      case '\b': return "\\b"; 
      case '\t': return "\\t"; 
      case '\n': return "\\n"; 
      case '\f': return "\\f"; 
      case '\r': return "\\r"; 
      case '\"': return "\"";  
      case '\'': return "\\'"; 
      case '\\': return "\\\\";  
      default:
        return isISOControl(c) ? String.format("\\u%04x", (int) c) : Character.toString(c);
    }
  }

  static String stringLiteralWithDoubleQuotes(String value, String indent) {
    StringBuilder result = new StringBuilder(value.length() + 2);
    result.append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      
      if (c == '\'') {
        result.append("'");
        continue;
      }
      
      if (c == '\"') {
        result.append("\\\"");
        continue;
      }
      
      result.append(characterLiteralWithoutSingleQuotes(c));
      
      if (c == '\n' && i + 1 < value.length()) {
        result.append("\"\n").append(indent).append(indent).append("+ \"");
      }
    }
    result.append('"');
    return result.toString();
  }
}
