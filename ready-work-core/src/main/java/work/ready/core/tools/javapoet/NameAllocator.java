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

import javax.lang.model.SourceVersion;
import java.util.*;

import static work.ready.core.tools.javapoet.Util.checkNotNull;

public final class NameAllocator implements Cloneable {
  private final Set<String> allocatedNames;
  private final Map<Object, String> tagToName;

  public NameAllocator() {
    this(new LinkedHashSet<>(), new LinkedHashMap<>());
  }

  private NameAllocator(LinkedHashSet<String> allocatedNames,
                        LinkedHashMap<Object, String> tagToName) {
    this.allocatedNames = allocatedNames;
    this.tagToName = tagToName;
  }

  public String newName(String suggestion) {
    return newName(suggestion, UUID.randomUUID().toString());
  }

  public String newName(String suggestion, Object tag) {
    checkNotNull(suggestion, "suggestion");
    checkNotNull(tag, "tag");

    suggestion = toJavaIdentifier(suggestion);

    while (SourceVersion.isKeyword(suggestion) || !allocatedNames.add(suggestion)) {
      suggestion = suggestion + "_";
    }

    String replaced = tagToName.put(tag, suggestion);
    if (replaced != null) {
      tagToName.put(tag, replaced); 
      throw new IllegalArgumentException("tag " + tag + " cannot be used for both '" + replaced
          + "' and '" + suggestion + "'");
    }

    return suggestion;
  }

  public static String toJavaIdentifier(String suggestion) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < suggestion.length(); ) {
      int codePoint = suggestion.codePointAt(i);
      if (i == 0
          && !Character.isJavaIdentifierStart(codePoint)
          && Character.isJavaIdentifierPart(codePoint)) {
        result.append("_");
      }

      int validCodePoint = Character.isJavaIdentifierPart(codePoint) ? codePoint : '_';
      result.appendCodePoint(validCodePoint);
      i += Character.charCount(codePoint);
    }
    return result.toString();
  }

  public String get(Object tag) {
    String result = tagToName.get(tag);
    if (result == null) {
      throw new IllegalArgumentException("unknown tag: " + tag);
    }
    return result;
  }

  @Override
  public NameAllocator clone() {
    return new NameAllocator(
        new LinkedHashSet<>(this.allocatedNames),
        new LinkedHashMap<>(this.tagToName));
  }

}
