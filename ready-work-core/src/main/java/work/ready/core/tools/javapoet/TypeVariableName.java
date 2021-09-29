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

import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import static work.ready.core.tools.javapoet.Util.checkArgument;
import static work.ready.core.tools.javapoet.Util.checkNotNull;

public final class TypeVariableName extends TypeName {
  public final String name;
  public final List<TypeName> bounds;

  private TypeVariableName(String name, List<TypeName> bounds) {
    this(name, bounds, new ArrayList<>());
  }

  private TypeVariableName(String name, List<TypeName> bounds, List<AnnotationSpec> annotations) {
    super(annotations);
    this.name = checkNotNull(name, "name == null");
    this.bounds = bounds;

    for (TypeName bound : this.bounds) {
      checkArgument(!bound.isPrimitive() && bound != VOID, "invalid bound: %s", bound);
    }
  }

  @Override public TypeVariableName annotated(List<AnnotationSpec> annotations) {
    return new TypeVariableName(name, bounds, annotations);
  }

  @Override public TypeName withoutAnnotations() {
    return new TypeVariableName(name, bounds);
  }

  public TypeVariableName withBounds(Type... bounds) {
    return withBounds(TypeName.list(bounds));
  }

  public TypeVariableName withBounds(TypeName... bounds) {
    return withBounds(Arrays.asList(bounds));
  }

  public TypeVariableName withBounds(List<? extends TypeName> bounds) {
    ArrayList<TypeName> newBounds = new ArrayList<>();
    newBounds.addAll(this.bounds);
    newBounds.addAll(bounds);
    return new TypeVariableName(name, newBounds, annotations);
  }

  private static TypeVariableName of(String name, List<TypeName> bounds) {
    
    List<TypeName> boundsNoObject = new ArrayList<>(bounds);
    boundsNoObject.remove(OBJECT);
    return new TypeVariableName(name, Collections.unmodifiableList(boundsNoObject));
  }

  @Override CodeWriter emit(CodeWriter out) throws IOException {
    emitAnnotations(out);
    return out.emitAndIndent(name);
  }

  public static TypeVariableName get(String name) {
    return TypeVariableName.of(name, Collections.emptyList());
  }

  public static TypeVariableName get(String name, TypeName... bounds) {
    return TypeVariableName.of(name, Arrays.asList(bounds));
  }

  public static TypeVariableName get(String name, Type... bounds) {
    return TypeVariableName.of(name, TypeName.list(bounds));
  }

  public static TypeVariableName get(TypeVariable mirror) {
    return get((TypeParameterElement) mirror.asElement());
  }

  static TypeVariableName get(
      TypeVariable mirror, Map<TypeParameterElement, TypeVariableName> typeVariables) {
    TypeParameterElement element = (TypeParameterElement) mirror.asElement();
    TypeVariableName typeVariableName = typeVariables.get(element);
    if (typeVariableName == null) {

      List<TypeName> bounds = new ArrayList<>();
      List<TypeName> visibleBounds = Collections.unmodifiableList(bounds);
      typeVariableName = new TypeVariableName(element.getSimpleName().toString(), visibleBounds);
      typeVariables.put(element, typeVariableName);
      for (TypeMirror typeMirror : element.getBounds()) {
        bounds.add(TypeName.get(typeMirror, typeVariables));
      }
      bounds.remove(OBJECT);
    }
    return typeVariableName;
  }

  public static TypeVariableName get(TypeParameterElement element) {
    String name = element.getSimpleName().toString();
    List<? extends TypeMirror> boundsMirrors = element.getBounds();

    List<TypeName> boundsTypeNames = new ArrayList<>();
    for (TypeMirror typeMirror : boundsMirrors) {
      boundsTypeNames.add(TypeName.get(typeMirror));
    }

    return TypeVariableName.of(name, boundsTypeNames);
  }

  public static TypeVariableName get(java.lang.reflect.TypeVariable<?> type) {
    return get(type, new LinkedHashMap<>());
  }

  static TypeVariableName get(java.lang.reflect.TypeVariable<?> type,
      Map<Type, TypeVariableName> map) {
    TypeVariableName result = map.get(type);
    if (result == null) {
      List<TypeName> bounds = new ArrayList<>();
      List<TypeName> visibleBounds = Collections.unmodifiableList(bounds);
      result = new TypeVariableName(type.getName(), visibleBounds);
      map.put(type, result);
      for (Type bound : type.getBounds()) {
        bounds.add(TypeName.get(bound, map));
      }
      bounds.remove(OBJECT);
    }
    return result;
  }
}
