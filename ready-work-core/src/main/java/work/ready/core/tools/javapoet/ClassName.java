/*
 * Copyright (C) 2014 Google, Inc.
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

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.SimpleElementVisitor8;
import java.io.IOException;
import java.util.*;

import static work.ready.core.tools.javapoet.Util.checkArgument;
import static work.ready.core.tools.javapoet.Util.checkNotNull;

public final class ClassName extends TypeName implements Comparable<ClassName> {
  public static final ClassName OBJECT = ClassName.get(Object.class);

  private static final String NO_PACKAGE = "";

  final String packageName;

  final ClassName enclosingClassName;

  final String simpleName;

  private List<String> simpleNames;

  final String canonicalName;

  private ClassName(String packageName, ClassName enclosingClassName, String simpleName) {
    this(packageName, enclosingClassName, simpleName, Collections.emptyList());
  }

  private ClassName(String packageName, ClassName enclosingClassName, String simpleName,
      List<AnnotationSpec> annotations) {
    super(annotations);
    this.packageName = Objects.requireNonNull(packageName, "packageName == null");
    this.enclosingClassName = enclosingClassName;
    this.simpleName = simpleName;
    this.canonicalName = enclosingClassName != null
        ? (enclosingClassName.canonicalName + '.' + simpleName)
        : (packageName.isEmpty() ? simpleName : packageName + '.' + simpleName);
  }

  @Override public ClassName annotated(List<AnnotationSpec> annotations) {
    return new ClassName(packageName, enclosingClassName, simpleName,
        concatAnnotations(annotations));
  }

  @Override public ClassName withoutAnnotations() {
    if (!isAnnotated()) return this;
    ClassName resultEnclosingClassName = enclosingClassName != null
        ? enclosingClassName.withoutAnnotations()
        : null;
    return new ClassName(packageName, resultEnclosingClassName, simpleName);
  }

  @Override public boolean isAnnotated() {
    return super.isAnnotated() || (enclosingClassName != null && enclosingClassName.isAnnotated());
  }

  public String packageName() {
    return packageName;
  }

  public ClassName enclosingClassName() {
    return enclosingClassName;
  }

  public ClassName topLevelClassName() {
    return enclosingClassName != null ? enclosingClassName.topLevelClassName() : this;
  }

  public String reflectionName() {
    return enclosingClassName != null
        ? (enclosingClassName.reflectionName() + '$' + simpleName)
        : (packageName.isEmpty() ? simpleName : packageName + '.' + simpleName);
  }

  public List<String> simpleNames() {
    if (simpleNames != null) {
      return simpleNames;
    }

    if (enclosingClassName == null) {
      simpleNames = Collections.singletonList(simpleName);
    } else {
      List<String> mutableNames = new ArrayList<>();
      mutableNames.addAll(enclosingClassName().simpleNames());
      mutableNames.add(simpleName);
      simpleNames = Collections.unmodifiableList(mutableNames);
    }
    return simpleNames;
  }

  public ClassName peerClass(String name) {
    return new ClassName(packageName, enclosingClassName, name);
  }

  public ClassName nestedClass(String name) {
    return new ClassName(packageName, this, name);
  }

  public String simpleName() {
    return simpleName;
  }

  public String canonicalName() {
    return canonicalName;
  }

  public static ClassName get(Class<?> clazz) {
    checkNotNull(clazz, "clazz == null");
    checkArgument(!clazz.isPrimitive(), "primitive types cannot be represented as a ClassName");
    checkArgument(!void.class.equals(clazz), "'void' type cannot be represented as a ClassName");
    checkArgument(!clazz.isArray(), "array types cannot be represented as a ClassName");

    String anonymousSuffix = "";
    while (clazz.isAnonymousClass()) {
      int lastDollar = clazz.getName().lastIndexOf('$');
      anonymousSuffix = clazz.getName().substring(lastDollar) + anonymousSuffix;
      clazz = clazz.getEnclosingClass();
    }
    String name = clazz.getSimpleName() + anonymousSuffix;

    if (clazz.getEnclosingClass() == null) {
      
      int lastDot = clazz.getName().lastIndexOf('.');
      String packageName = (lastDot != -1) ? clazz.getName().substring(0, lastDot) : NO_PACKAGE;
      return new ClassName(packageName, null, name);
    }

    return ClassName.get(clazz.getEnclosingClass()).nestedClass(name);
  }

  public static ClassName bestGuess(String classNameString) {
    
    int p = 0;
    while (p < classNameString.length() && Character.isLowerCase(classNameString.codePointAt(p))) {
      p = classNameString.indexOf('.', p) + 1;
      checkArgument(p != 0, "couldn't make a guess for %s", classNameString);
    }
    String packageName = p == 0 ? NO_PACKAGE : classNameString.substring(0, p - 1);

    ClassName className = null;
    for (String simpleName : classNameString.substring(p).split("\\.", -1)) {
      checkArgument(!simpleName.isEmpty() && Character.isUpperCase(simpleName.codePointAt(0)),
          "couldn't make a guess for %s", classNameString);
      className = new ClassName(packageName, className, simpleName);
    }

    return className;
  }

  public static ClassName get(String packageName, String simpleName, String... simpleNames) {
    ClassName className = new ClassName(packageName, null, simpleName);
    for (String name : simpleNames) {
      className = className.nestedClass(name);
    }
    return className;
  }

  public static ClassName get(TypeElement element) {
    checkNotNull(element, "element == null");
    String simpleName = element.getSimpleName().toString();

    return element.getEnclosingElement().accept(new SimpleElementVisitor8<ClassName, Void>() {
      @Override public ClassName visitPackage(PackageElement packageElement, Void p) {
        return new ClassName(packageElement.getQualifiedName().toString(), null, simpleName);
      }

      @Override public ClassName visitType(TypeElement enclosingClass, Void p) {
        return ClassName.get(enclosingClass).nestedClass(simpleName);
      }

      @Override public ClassName visitUnknown(Element unknown, Void p) {
        return get("", simpleName);
      }

      @Override public ClassName defaultAction(Element enclosingElement, Void p) {
        throw new IllegalArgumentException("Unexpected type nesting: " + element);
      }
    }, null);
  }

  @Override public int compareTo(ClassName o) {
    return canonicalName.compareTo(o.canonicalName);
  }

  @Override CodeWriter emit(CodeWriter out) throws IOException {
    boolean charsEmitted = false;
    for (ClassName className : enclosingClasses()) {
      String simpleName;
      if (charsEmitted) {
        
        out.emit(".");
        simpleName = className.simpleName;

      } else if (className.isAnnotated() || className == this) {
        
        String qualifiedName = out.lookupName(className);
        int dot = qualifiedName.lastIndexOf('.');
        if (dot != -1) {
          out.emitAndIndent(qualifiedName.substring(0, dot + 1));
          simpleName = qualifiedName.substring(dot + 1);
          charsEmitted = true;
        } else {
          simpleName = qualifiedName;
        }

      } else {
        
        continue;
      }

      if (className.isAnnotated()) {
        if (charsEmitted) out.emit(" ");
        className.emitAnnotations(out);
      }

      out.emit(simpleName);
      charsEmitted = true;
    }

    return out;
  }

  private List<ClassName> enclosingClasses() {
    List<ClassName> result = new ArrayList<>();
    for (ClassName c = this; c != null; c = c.enclosingClassName) {
      result.add(c);
    }
    Collections.reverse(result);
    return result;
  }
}
