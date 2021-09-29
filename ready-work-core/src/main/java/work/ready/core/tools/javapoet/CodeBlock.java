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

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.StreamSupport;

import static work.ready.core.tools.javapoet.Util.checkArgument;

public final class CodeBlock {
  private static final Pattern NAMED_ARGUMENT =
      Pattern.compile("\\$(?<argumentName>[\\w_]+):(?<typeChar>[\\w]).*");
  private static final Pattern LOWERCASE = Pattern.compile("[a-z]+[\\w_]*");

  final List<String> formatParts;
  final List<Object> args;

  private CodeBlock(Builder builder) {
    this.formatParts = Util.immutableList(builder.formatParts);
    this.args = Util.immutableList(builder.args);
  }

  public boolean isEmpty() {
    return formatParts.isEmpty();
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }

  @Override public int hashCode() {
    return toString().hashCode();
  }

  @Override public String toString() {
    StringBuilder out = new StringBuilder();
    try {
      new CodeWriter(out).emit(this);
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  public static CodeBlock of(String format, Object... args) {
    return new Builder().add(format, args).build();
  }

  public static CodeBlock join(Iterable<CodeBlock> codeBlocks, String separator) {
    return StreamSupport.stream(codeBlocks.spliterator(), false).collect(joining(separator));
  }

  public static Collector<CodeBlock, ?, CodeBlock> joining(String separator) {
    return Collector.of(
        () -> new CodeBlockJoiner(separator, builder()),
        CodeBlockJoiner::add,
        CodeBlockJoiner::merge,
        CodeBlockJoiner::join);
  }

  public static Collector<CodeBlock, ?, CodeBlock> joining(
      String separator, String prefix, String suffix) {
    Builder builder = builder().add("$N", prefix);
    return Collector.of(
        () -> new CodeBlockJoiner(separator, builder),
        CodeBlockJoiner::add,
        CodeBlockJoiner::merge,
        joiner -> {
            builder.add(CodeBlock.of("$N", suffix));
            return joiner.join();
        });
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    Builder builder = new Builder();
    builder.formatParts.addAll(formatParts);
    builder.args.addAll(args);
    return builder;
  }

  public static final class Builder {
    final List<String> formatParts = new ArrayList<>();
    final List<Object> args = new ArrayList<>();

    private Builder() {
    }

    public boolean isEmpty() {
      return formatParts.isEmpty();
    }

    public Builder addNamed(String format, Map<String, ?> arguments) {
      int p = 0;

      for (String argument : arguments.keySet()) {
        checkArgument(LOWERCASE.matcher(argument).matches(),
            "argument '%s' must start with a lowercase character", argument);
      }

      while (p < format.length()) {
        int nextP = format.indexOf("$", p);
        if (nextP == -1) {
          formatParts.add(format.substring(p));
          break;
        }

        if (p != nextP) {
          formatParts.add(format.substring(p, nextP));
          p = nextP;
        }

        Matcher matcher = null;
        int colon = format.indexOf(':', p);
        if (colon != -1) {
          int endIndex = Math.min(colon + 2, format.length());
          matcher = NAMED_ARGUMENT.matcher(format.substring(p, endIndex));
        }
        if (matcher != null && matcher.lookingAt()) {
          String argumentName = matcher.group("argumentName");
          checkArgument(arguments.containsKey(argumentName), "Missing named argument for $%s",
              argumentName);
          char formatChar = matcher.group("typeChar").charAt(0);
          addArgument(format, formatChar, arguments.get(argumentName));
          formatParts.add("$" + formatChar);
          p += matcher.regionEnd();
        } else {
          checkArgument(p < format.length() - 1, "dangling $ at end");
          checkArgument(isNoArgPlaceholder(format.charAt(p + 1)),
              "unknown format $%s at %s in '%s'", format.charAt(p + 1), p + 1, format);
          formatParts.add(format.substring(p, p + 2));
          p += 2;
        }
      }

      return this;
    }

    public Builder add(String format, Object... args) {
      boolean hasRelative = false;
      boolean hasIndexed = false;

      int relativeParameterCount = 0;
      int[] indexedParameterCount = new int[args.length];

      for (int p = 0; p < format.length(); ) {
        if (format.charAt(p) != '$') {
          int nextP = format.indexOf('$', p + 1);
          if (nextP == -1) nextP = format.length();
          formatParts.add(format.substring(p, nextP));
          p = nextP;
          continue;
        }

        p++;

        int indexStart = p;
        char c;
        do {
          checkArgument(p < format.length(), "dangling format characters in '%s'", format);
          c = format.charAt(p++);
        } while (c >= '0' && c <= '9');
        int indexEnd = p - 1;

        if (isNoArgPlaceholder(c)) {
          checkArgument(
              indexStart == indexEnd, "$$, $>, $<, $[, $], $W, and $Z may not have an index");
          formatParts.add("$" + c);
          continue;
        }

        int index;
        if (indexStart < indexEnd) {
          index = Integer.parseInt(format.substring(indexStart, indexEnd)) - 1;
          hasIndexed = true;
          if (args.length > 0) {
            indexedParameterCount[index % args.length]++; 
          }
        } else {
          index = relativeParameterCount;
          hasRelative = true;
          relativeParameterCount++;
        }

        checkArgument(index >= 0 && index < args.length,
            "index %d for '%s' not in range (received %s arguments)",
            index + 1, format.substring(indexStart - 1, indexEnd + 1), args.length);
        checkArgument(!hasIndexed || !hasRelative, "cannot mix indexed and positional parameters");

        addArgument(format, c, args[index]);

        formatParts.add("$" + c);
      }

      if (hasRelative) {
        checkArgument(relativeParameterCount >= args.length,
            "unused arguments: expected %s, received %s", relativeParameterCount, args.length);
      }
      if (hasIndexed) {
        List<String> unused = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
          if (indexedParameterCount[i] == 0) {
            unused.add("$" + (i + 1));
          }
        }
        String s = unused.size() == 1 ? "" : "s";
        checkArgument(unused.isEmpty(), "unused argument%s: %s", s, String.join(", ", unused));
      }
      return this;
    }

    private boolean isNoArgPlaceholder(char c) {
      return c == '$' || c == '>' || c == '<' || c == '[' || c == ']' || c == 'W' || c == 'Z';
    }

    private void addArgument(String format, char c, Object arg) {
      switch (c) {
        case 'N':
          this.args.add(argToName(arg));
          break;
        case 'L':
          this.args.add(argToLiteral(arg));
          break;
        case 'S':
          this.args.add(argToString(arg));
          break;
        case 'T':
          this.args.add(argToType(arg));
          break;
        default:
          throw new IllegalArgumentException(
              String.format("invalid format string: '%s'", format));
      }
    }

    private String argToName(Object o) {
      if (o instanceof CharSequence) return o.toString();
      if (o instanceof ParameterSpec) return ((ParameterSpec) o).name;
      if (o instanceof FieldSpec) return ((FieldSpec) o).name;
      if (o instanceof MethodSpec) return ((MethodSpec) o).name;
      if (o instanceof TypeSpec) return ((TypeSpec) o).name;
      throw new IllegalArgumentException("expected name but was " + o);
    }

    private Object argToLiteral(Object o) {
      return o;
    }

    private String argToString(Object o) {
      return o != null ? String.valueOf(o) : null;
    }

    private TypeName argToType(Object o) {
      if (o instanceof TypeName) return (TypeName) o;
      if (o instanceof TypeMirror) return TypeName.get((TypeMirror) o);
      if (o instanceof Element) return TypeName.get(((Element) o).asType());
      if (o instanceof Type) return TypeName.get((Type) o);
      throw new IllegalArgumentException("expected type but was " + o);
    }

    public Builder beginControlFlow(String controlFlow, Object... args) {
      add(controlFlow + " {\n", args);
      indent();
      return this;
    }

    public Builder nextControlFlow(String controlFlow, Object... args) {
      unindent();
      add("} " + controlFlow + " {\n", args);
      indent();
      return this;
    }

    public Builder endControlFlow() {
      unindent();
      add("}\n");
      return this;
    }

    public Builder endControlFlow(String controlFlow, Object... args) {
      unindent();
      add("} " + controlFlow + ";\n", args);
      return this;
    }

    public Builder addStatement(String format, Object... args) {
      add("$[");
      add(format, args);
      add(";\n$]");
      return this;
    }

    public Builder addStatement(CodeBlock codeBlock) {
      return addStatement("$L", codeBlock);
    }

    public Builder add(CodeBlock codeBlock) {
      formatParts.addAll(codeBlock.formatParts);
      args.addAll(codeBlock.args);
      return this;
    }

    public Builder indent() {
      this.formatParts.add("$>");
      return this;
    }

    public Builder unindent() {
      this.formatParts.add("$<");
      return this;
    }

    public Builder clear() {
      formatParts.clear();
      args.clear();
      return this;
    }

    public CodeBlock build() {
      return new CodeBlock(this);
    }
  }

  private static final class CodeBlockJoiner {
    private final String delimiter;
    private final Builder builder;
    private boolean first = true;

    CodeBlockJoiner(String delimiter, Builder builder) {
      this.delimiter = delimiter;
      this.builder = builder;
    }

    CodeBlockJoiner add(CodeBlock codeBlock) {
      if (!first) {
        builder.add(delimiter);
      }
      first = false;

      builder.add(codeBlock);
      return this;
    }

    CodeBlockJoiner merge(CodeBlockJoiner other) {
      CodeBlock otherBlock = other.builder.build();
      if (!otherBlock.isEmpty()) {
        add(otherBlock);
      }
      return this;
    }

    CodeBlock join() {
      return builder.build();
    }
  }
}
