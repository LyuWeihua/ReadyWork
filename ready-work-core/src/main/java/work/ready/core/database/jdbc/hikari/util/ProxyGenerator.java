/**
 * Original work Copyright (C) 2013, 2014 Brett Wooldridge
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
 */

package work.ready.core.database.jdbc.hikari.util;

import work.ready.core.database.jdbc.common.*;
import work.ready.core.database.jdbc.event.JdbcEventListener;
import work.ready.core.database.jdbc.hikari.pool.*;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.StrUtil;
import work.ready.core.tools.javapoet.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

import static javax.lang.model.element.Modifier.*;

public final class ProxyGenerator
{
   private static Path genDirectory;

   public static void main(String... args) throws Exception {

      if (args.length > 0) {
         genDirectory = Path.of(args[0]);
      } else {
         String dir = ProxyGenerator.class.getResource("").getPath();
         String ensure = File.separatorChar + "target" + File.separatorChar + "classes" + File.separatorChar + "work" + File.separatorChar + "ready" + File.separatorChar;
         int pos = dir.indexOf(ensure);
         if(pos > 0) {
            genDirectory = Path.of(dir.substring(0, pos + 1) + "src/main/java/"); 
         } else {
            throw new RuntimeException("Cannot locate source code path.");
         }
      }

      String methodBody = "try { return delegate.method($N); } catch (SQLException e) { throw checkException(e); }";
      generateProxyClass(Connection.class, ProxyConnection.class, methodBody);
      generateProxyClass(Statement.class, ProxyStatement.class, methodBody);
      generateProxyClass(ResultSet.class, ProxyResultSet.class, methodBody);
      generateProxyClass(DatabaseMetaData.class, ProxyDatabaseMetaData.class, methodBody);

      methodBody = "try { return ((cast) delegate).method($N); } catch (SQLException e) { throw checkException(e); }";
      generateProxyClass(PreparedStatement.class, ProxyPreparedStatement.class, methodBody);
      generateProxyClass(CallableStatement.class, ProxyCallableStatement.class, methodBody);

      modifyProxyFactory();
   }

   private static void modifyProxyFactory() throws IOException {
      System.out.println("Generating method bodies for work.ready.core.database.jdbc.hikari.pool.ProxyFactory");

      String packageName = ProxyConnection.class.getPackage().getName();
      Method[] methods = ProxyFactoryTemplate.class.getDeclaredMethods();
      JavaCodeBuilder codeBuilder = new JavaCodeBuilder(ProxyFactoryTemplate.class.getPackageName(), "ProxyFactory");
      for(Method method : methods) {
         var methodBuilder = codeBuilder.copyFrom(method);
         List<String> parameters = new ArrayList<>();
         for(Parameter parameter : method.getParameters()) {
            parameters.add(parameter.getName());
         }
         switch (method.getName()) {
            case "getProxyConnection":
               methodBuilder.addCode("return new $T($N);", ClassName.get(packageName, "HikariProxyConnection"), StrUtil.join(parameters, ", "));
               codeBuilder.addMethods(methodBuilder.build());
               break;
            case "getProxyStatement":
               methodBuilder.addCode(
                       "HikariProxyConnection hikariConnection = (HikariProxyConnection)connection;\n" +
                       "var si = new $T(hikariConnection.getConnectionInformation());\n" +
                       "//si.setStatement(statement);\n", StatementInformation.class);
               methodBuilder.addCode(
                       "Statement proxied = new $T($N).advancedFeatureSupport(si, hikariConnection.getJdbcEventListener());\n" +
                       "si.setStatement(proxied);\n" +
                       "return proxied;\n",
                       ClassName.get(packageName, "HikariProxyStatement"),
                       StrUtil.join(parameters, ", "));
               codeBuilder.addMethods(methodBuilder.build());
               break;
            case "getProxyPreparedStatement":
               methodBuilder.addCode(
                       "HikariProxyConnection hikariConnection = (HikariProxyConnection)connection;\n" +
                       "var si = new $T(hikariConnection.getConnectionInformation(), sql);\n" +
                       "//si.setStatement(statement);\n", PreparedStatementInformation.class
               );
               
               parameters.remove(1);
               methodBuilder.addCode(
                       "PreparedStatement proxied = new $T($N).advancedFeatureSupport(si, hikariConnection.getJdbcEventListener());\n" +
                       "si.setStatement(proxied);\n" +
                       "return proxied;\n",
                       ClassName.get(packageName, "HikariProxyPreparedStatement"),
                       StrUtil.join(parameters, ", "));
               codeBuilder.addMethods(methodBuilder.build());
               break;
            case "getProxyCallableStatement":
               methodBuilder.addCode(
                       "HikariProxyConnection hikariConnection = (HikariProxyConnection)connection;\n" +
                       "var si = new $T(hikariConnection.getConnectionInformation(), sql);\n" +
                       "//si.setStatement(statement);\n", CallableStatementInformation.class
               );
               
               parameters.remove(1);
               methodBuilder.addCode(
                       "CallableStatement proxied = new $T($N).advancedFeatureSupport(si, hikariConnection.getJdbcEventListener());\n" +
                       "si.setStatement(proxied);\n" +
                       "return proxied;\n",
                       ClassName.get(packageName, "HikariProxyCallableStatement"),
                       StrUtil.join(parameters, ", "));
               codeBuilder.addMethods(methodBuilder.build());
               break;
            case "getProxyResultSet":
               methodBuilder.addCode(
                       "$T statementInformation;\n" +
                       "if(statement instanceof $T) {\n" +
                       "   statementInformation = ((HikariProxyPreparedStatement)statement).getStatementInformation();\n" +
                       "} else if(statement instanceof $T) {\n" +
                       "   statementInformation = ((HikariProxyCallableStatement)statement).getStatementInformation();\n" +
                       "} else if(statement != null) {\n" +
                       "   statementInformation = (($T)statement).getStatementInformation();\n" +
                       "} else {\n" +
                       "   statementInformation = new StatementInformation(((HikariProxyConnection)connection).getConnectionInformation());\n" +
                       "}\n" +
                       "var ri = new $T(statementInformation);\n" +
                       "ri.setResultSet(resultSet);\n",
                       StatementInformation.class, HikariProxyPreparedStatement.class, HikariProxyCallableStatement.class, HikariProxyStatement.class, ResultSetInformation.class);
               methodBuilder.addCode("return new $T($N).advancedFeatureSupport(ri, ((HikariProxyConnection)connection).getJdbcEventListener());",
                       ClassName.get(packageName, "HikariProxyResultSet"),
                       StrUtil.join(parameters, ", "));
               codeBuilder.addMethods(methodBuilder.build());
               break;
            case "getProxyDatabaseMetaData":
               methodBuilder.addCode("return new $T($N);", ClassName.get(packageName, "HikariProxyDatabaseMetaData"), StrUtil.join(parameters, ", "));
               codeBuilder.addMethods(methodBuilder.build());
               break;
            default:
               
               break;
         }
      }
      codeBuilder.generateJavaFile(genDirectory);
   }

   private static <T,S> void generateProxyClass(Class<T> primaryInterface, Class<S> superClass, String methodBody) throws Exception
   {
      String newClassName = "Hikari" + superClass.getSimpleName();
      JavaCodeBuilder codeBuilder = new JavaCodeBuilder(superClass.getPackageName(), newClassName, PUBLIC, FINAL);
      codeBuilder.extendSuperClass(superClass);

      advancedFeatureSupport(primaryInterface, superClass, codeBuilder);

      Set<String> superSigs = new HashSet<>();
      for(Method method : superClass.getMethods()){
         if ((method.getModifiers() & Modifier.FINAL) == Modifier.FINAL) {
            superSigs.add(ClassUtil.getMethodSignature(method, false));
         }
      }
      System.out.println("Generating " + newClassName);
      Set<String> methods = new HashSet<>();
      for (Class<?> intf : getAllInterfaces(primaryInterface)) {
         codeBuilder.getClassBuilder().addSuperinterface(intf);
         for (Method intfMethod : intf.getDeclaredMethods()) {
            final String signature = ClassUtil.getMethodSignature(intfMethod, false);
            
            if (superSigs.contains(signature)) {
               continue;
            }
            
            if (methods.contains(signature)) {
               continue;
            }
            
            methods.add(signature);

            var methodBuilder = codeBuilder.copyFrom(intfMethod);
            methodBuilder.addAnnotation(Override.class);
            List<String> parameters = new ArrayList<>();
            for(Parameter parameter : intfMethod.getParameters()) {
               parameters.add(parameter.getName());
            }

            Method superMethod = null;
            String modifiedBody = methodBody;
            try {
               superMethod = superClass.getMethod(intfMethod.getName(), intfMethod.getParameterTypes());
            } catch (NoSuchMethodException e) {}
            if (superMethod != null && (superMethod.getModifiers() & Modifier.ABSTRACT) != Modifier.ABSTRACT && !isDefaultMethod(intf, intfMethod)) {
               modifiedBody = modifiedBody.replace("((cast) ", "");
               modifiedBody = modifiedBody.replace("delegate", "super");
               modifiedBody = modifiedBody.replace("super)", "super");
            }
            modifiedBody = modifiedBody.replace("cast", primaryInterface.getSimpleName());

            String performer = modifiedBody.substring(modifiedBody.indexOf("return ") + 7, modifiedBody.indexOf('.'));
            if(customizeMethod(superClass, signature, parameters, performer, methodBuilder, codeBuilder)) {
               continue;
            }

            if (isThrowsSqlException(intfMethod)) {
               modifiedBody = modifiedBody.replace("method", intfMethod.getName());
            }
            else {
               modifiedBody = "return ((cast) delegate).method($N);".replace("method", intfMethod.getName()).replace("cast", primaryInterface.getSimpleName());
            }

            if (intfMethod.getReturnType() == Void.TYPE) {
               modifiedBody = modifiedBody.replace("return", "");
            }

            methodBuilder.addCode(modifiedBody, StrUtil.join(parameters, ", "));
            codeBuilder.addMethods(methodBuilder.build());
         }
      }
      codeBuilder.generateJavaFile(genDirectory);
      checkMissing(superClass, methods);
   }

   private static <S> void checkMissing(Class<S> superClass, Set<String> methods) {
      if(ProxyConnection.class.equals(superClass)) {
         ProxyConnectionCodeBlock.CodeBlock.keySet().stream()
                 .filter(name -> !methods.contains(name))
                 .forEach(name->System.err.println("Missing " + name + " for " + superClass.getName()));
      } else if (ProxyStatement.class.equals(superClass)) {
         ProxyStatementCodeBlock.CodeBlock.keySet().stream()
                 .filter(name -> !methods.contains(name))
                 .forEach(name->System.err.println("Missing " + name + " for " + superClass.getName()));
      } else if (ProxyResultSet.class.equals(superClass)) {
         ProxyResultSetCodeBlock.CodeBlock.keySet().stream()
                 .filter(name -> !methods.contains(name))
                 .forEach(name->System.err.println("Missing " + name + " for " + superClass.getName()));
      } else if (ProxyDatabaseMetaData.class.equals(superClass)) {
      } else if (ProxyPreparedStatement.class.equals(superClass)) {
         ProxyPreparedStatementCodeBlock.CodeBlock.keySet().stream()
                 .filter(name -> !methods.contains(name))
                 .forEach(name->System.err.println("Missing " + name + " for " + superClass.getName()));
      } else if (ProxyCallableStatement.class.equals(superClass)) {
         ProxyCallableStatementCodeBlock.CodeBlock.keySet().stream()
                 .filter(name -> !methods.contains(name))
                 .forEach(name->System.err.println("Missing " + name + " for " + superClass.getName()));
      }
   }

   private static <S> boolean customizeMethod(Class<S> superClass, String signature, List<String> parameters, String performer, MethodSpec.Builder methodBuilder, JavaCodeBuilder codeBuilder) {
      boolean result;
      if(ProxyConnection.class.equals(superClass)) {
         return customizeProxyConnection(signature, parameters, performer, methodBuilder, codeBuilder);
      } else if (ProxyStatement.class.equals(superClass)) {
         result = customizeProxyStatement(signature, parameters, performer, methodBuilder, codeBuilder);
         if(!result && signature.startsWith("execute") && !ProxyStatementCodeBlock.ignore.contains(signature)) {
            System.err.println(signature);
         }
         return result;
      } else if (ProxyResultSet.class.equals(superClass)) {
         result = customizeProxyResultSet(signature, parameters, performer, methodBuilder, codeBuilder);
         if(!result && signature.startsWith("get") && !ProxyResultSetCodeBlock.ignore.contains(signature)) {
            System.err.println(signature);
         }
         return result;
      } else if (ProxyDatabaseMetaData.class.equals(superClass)) {
         return false;
      } else if (ProxyPreparedStatement.class.equals(superClass)) {
         result = customizeProxyStatement(signature, parameters, performer, methodBuilder, codeBuilder) ||
                 customizeProxyPreparedStatement(signature, parameters, performer, methodBuilder, codeBuilder);
         if(!result && signature.startsWith("set") && !ProxyPreparedStatementCodeBlock.ignore.contains(signature)) {
            System.err.println(signature);
         }
         return result;
      } else if (ProxyCallableStatement.class.equals(superClass)) {
         result = customizeProxyStatement(signature, parameters, performer, methodBuilder, codeBuilder) ||
                 customizeProxyPreparedStatement(signature, parameters, performer, methodBuilder, codeBuilder) ||
                 customizeProxyCallableStatement(signature, parameters, performer, methodBuilder, codeBuilder);
         if(!result && signature.startsWith("set") && !ProxyCallableStatementCodeBlock.ignore.contains(signature)) {
            System.err.println(signature);
         }
         return result;
      }
      return false;
   }

   private static <T,S> void advancedFeatureSupport(Class<T> primaryInterface, Class<S> superClass, JavaCodeBuilder codeBuilder) {
      if(ProxyConnection.class.equals(superClass)) {
         enhanceProxyConnection(codeBuilder);
      } else if (ProxyStatement.class.equals(superClass)) {
         enhanceProxyStatement(codeBuilder);
      } else if (ProxyResultSet.class.equals(superClass)) {
         enhanceProxyResultSet(codeBuilder);
      } else if (ProxyDatabaseMetaData.class.equals(superClass)) {
         
      } else if (ProxyPreparedStatement.class.equals(superClass)) {
         enhanceProxyPreparedStatement(codeBuilder);
      } else if (ProxyCallableStatement.class.equals(superClass)) {
         enhanceProxyCallableStatement(codeBuilder);
      }
   }

   private static void enhanceProxyConnection(JavaCodeBuilder codeBuilder) {
      codeBuilder.getClassBuilder().addField(JdbcEventListener.class, "jdbcEventListener", PRIVATE);
      codeBuilder.getClassBuilder().addField(ConnectionInformation.class, "connectionInformation", PRIVATE);
      var advancedFeatureSupport = MethodSpec.methodBuilder("advancedFeatureSupport")
              .returns(void.class)
              .addModifiers(PUBLIC)
              .addParameter(ConnectionInformation.class, "connectionInformation", FINAL)
              .addParameter(JdbcEventListener.class, "jdbcEventListener", FINAL)
              .addCode("this.connectionInformation = connectionInformation;\n")
              .addCode("this.jdbcEventListener = jdbcEventListener;\n")
              .build();
      var getJdbcEventListener = MethodSpec.methodBuilder("getJdbcEventListener")
              .returns(JdbcEventListener.class)
              .addModifiers(PUBLIC)
              .addCode("return jdbcEventListener;")
              .build();
      var getConnectionInformation = MethodSpec.methodBuilder("getConnectionInformation")
              .returns(ConnectionInformation.class)
              .addModifiers(PUBLIC)
              .addCode("return connectionInformation;")
              .build();
      var getPoolName = MethodSpec.methodBuilder("getPoolName")
              .returns(String.class)
              .addModifiers(PUBLIC)
              .addCode("return getPoolEntry().getPoolName();")
              .build();
      codeBuilder.addMethods(advancedFeatureSupport, getJdbcEventListener, getConnectionInformation, getPoolName);
   }

   private static void enhanceProxyStatement(JavaCodeBuilder codeBuilder) {
      codeBuilder.getClassBuilder().addField(FieldSpec.builder(String.class, "LINE_SEPARATOR", PRIVATE, STATIC, FINAL).initializer("System.getProperty(\"line.separator\")").build());
      codeBuilder.getClassBuilder().addField(JdbcEventListener.class, "jdbcEventListener", PRIVATE);
      codeBuilder.getClassBuilder().addField(StatementInformation.class, "statementInformation", PRIVATE);
      var advancedFeatureSupport = MethodSpec.methodBuilder("advancedFeatureSupport")
              .returns(TypeVariableName.get("Hikari" + ProxyStatement.class.getSimpleName()))
              .addModifiers(PROTECTED)
              .addParameter(StatementInformation.class, "statementInformation", FINAL)
              .addParameter(JdbcEventListener.class, "jdbcEventListener", FINAL)
              .addCode("this.statementInformation = statementInformation;\n")
              .addCode("this.jdbcEventListener = jdbcEventListener;\n")
              .addCode("return this;")
              .build();
      var getJdbcEventListener = MethodSpec.methodBuilder("getJdbcEventListener")
              .returns(JdbcEventListener.class)
              .addModifiers(PUBLIC)
              .addCode("return jdbcEventListener;")
              .build();
      var getStatementInformation = MethodSpec.methodBuilder("getStatementInformation")
              .returns(StatementInformation.class)
              .addModifiers(PUBLIC)
              .addCode("return statementInformation;")
              .build();
      var replaceStatement = MethodSpec.methodBuilder("replaceStatement")
              .returns(void.class)
              .addException(SQLException.class)
              .addModifiers(PUBLIC)
              .addParameter(Statement.class, "statement", FINAL)
              .addCode("this.delegate.close();\nthis.delegate = statement;\n")
              .build();
      codeBuilder.addMethods(advancedFeatureSupport, getJdbcEventListener, getStatementInformation, replaceStatement);
   }

   private static void enhanceProxyResultSet(JavaCodeBuilder codeBuilder) {
      codeBuilder.getClassBuilder().addField(JdbcEventListener.class, "jdbcEventListener", PRIVATE);
      codeBuilder.getClassBuilder().addField(ResultSetInformation.class, "resultSetInformation", PRIVATE);
      var advancedFeatureSupport = MethodSpec.methodBuilder("advancedFeatureSupport")
              .returns(TypeVariableName.get("Hikari" + ProxyResultSet.class.getSimpleName()))
              .addModifiers(PROTECTED)
              .addParameter(ResultSetInformation.class, "resultSetInformation", FINAL)
              .addParameter(JdbcEventListener.class, "jdbcEventListener", FINAL)
              .addCode("this.resultSetInformation = resultSetInformation;\n")
              .addCode("this.jdbcEventListener = jdbcEventListener;\n")
              .addCode("return this;")
              .build();
      var getJdbcEventListener = MethodSpec.methodBuilder("getJdbcEventListener")
              .returns(JdbcEventListener.class)
              .addModifiers(PUBLIC)
              .addCode("return jdbcEventListener;")
              .build();
      var getResultSetInformation = MethodSpec.methodBuilder("getResultSetInformation")
              .returns(ResultSetInformation.class)
              .addModifiers(PUBLIC)
              .addCode("return resultSetInformation;")
              .build();
      codeBuilder.addMethods(advancedFeatureSupport, getJdbcEventListener, getResultSetInformation);
   }

   private static void enhanceProxyPreparedStatement(JavaCodeBuilder codeBuilder) {
      codeBuilder.getClassBuilder().addField(FieldSpec.builder(String.class, "LINE_SEPARATOR", PRIVATE, STATIC, FINAL).initializer("System.getProperty(\"line.separator\")").build());
      codeBuilder.getClassBuilder().addField(JdbcEventListener.class, "jdbcEventListener", PRIVATE);
      codeBuilder.getClassBuilder().addField(PreparedStatementInformation.class, "statementInformation", PRIVATE);
      var advancedFeatureSupport = MethodSpec.methodBuilder("advancedFeatureSupport")
              .returns(TypeVariableName.get("Hikari" + ProxyPreparedStatement.class.getSimpleName()))
              .addModifiers(PROTECTED)
              .addParameter(PreparedStatementInformation.class, "statementInformation", FINAL)
              .addParameter(JdbcEventListener.class, "jdbcEventListener", FINAL)
              .addCode("this.statementInformation = statementInformation;\n")
              .addCode("this.jdbcEventListener = jdbcEventListener;\n")
              .addCode("return this;")
              .build();
      var getJdbcEventListener = MethodSpec.methodBuilder("getJdbcEventListener")
              .returns(JdbcEventListener.class)
              .addModifiers(PUBLIC)
              .addCode("return jdbcEventListener;")
              .build();
      var getStatementInformation = MethodSpec.methodBuilder("getStatementInformation")
              .returns(PreparedStatementInformation.class)
              .addModifiers(PUBLIC)
              .addCode("return statementInformation;")
              .build();
      var replaceStatement = MethodSpec.methodBuilder("replaceStatement")
              .returns(void.class)
              .addException(SQLException.class)
              .addModifiers(PUBLIC)
              .addParameter(Statement.class, "statement", FINAL)
              .addCode("this.delegate.close();\nthis.delegate = statement;\n")
              .build();
      codeBuilder.addMethods(advancedFeatureSupport, getJdbcEventListener, getStatementInformation, replaceStatement);
   }

   private static void enhanceProxyCallableStatement(JavaCodeBuilder codeBuilder) {
      codeBuilder.getClassBuilder().addField(FieldSpec.builder(String.class, "LINE_SEPARATOR", PRIVATE, STATIC, FINAL).initializer("System.getProperty(\"line.separator\")").build());
      codeBuilder.getClassBuilder().addField(JdbcEventListener.class, "jdbcEventListener", PRIVATE);
      codeBuilder.getClassBuilder().addField(CallableStatementInformation.class, "statementInformation", PRIVATE);
      var advancedFeatureSupport = MethodSpec.methodBuilder("advancedFeatureSupport")
              .returns(TypeVariableName.get("Hikari" + ProxyCallableStatement.class.getSimpleName()))
              .addModifiers(PROTECTED)
              .addParameter(CallableStatementInformation.class, "statementInformation", FINAL)
              .addParameter(JdbcEventListener.class, "jdbcEventListener", FINAL)
              .addCode("this.statementInformation = statementInformation;\n")
              .addCode("this.jdbcEventListener = jdbcEventListener;\n")
              .addCode("return this;")
              .build();
      var getJdbcEventListener = MethodSpec.methodBuilder("getJdbcEventListener")
              .returns(JdbcEventListener.class)
              .addModifiers(PUBLIC)
              .addCode("return jdbcEventListener;")
              .build();
      var getStatementInformation = MethodSpec.methodBuilder("getStatementInformation")
              .returns(CallableStatementInformation.class)
              .addModifiers(PUBLIC)
              .addCode("return statementInformation;")
              .build();
      var replaceStatement = MethodSpec.methodBuilder("replaceStatement")
              .returns(void.class)
              .addException(SQLException.class)
              .addModifiers(PUBLIC)
              .addParameter(Statement.class, "statement", FINAL)
              .addCode("this.delegate.close();\nthis.delegate = statement;\n")
              .build();
      codeBuilder.addMethods(advancedFeatureSupport, getJdbcEventListener, getStatementInformation, replaceStatement);
   }

   private static boolean customizeProxyConnection(String signature, List<String> parameters, String performer, MethodSpec.Builder methodBuilder, JavaCodeBuilder codeBuilder) {
      String code = ProxyConnectionCodeBlock.CodeBlock.get(signature);
      if(code != null) {
         if(parameters.size() > 0) {
            List<String> array = new ArrayList<>();
            array.add(StrUtil.join(parameters, ", ")); 
            array.addAll(parameters); 
            methodBuilder.addCode(code.replace("#performer", performer), array.toArray(new Object[]{}));
         } else {
            methodBuilder.addCode(code.replace("#performer", performer));
         }
         codeBuilder.addMethods(methodBuilder.build());
         return true;
      }
      return false;
   }

   private static boolean customizeProxyStatement(String signature, List<String> parameters, String performer, MethodSpec.Builder methodBuilder, JavaCodeBuilder codeBuilder) {
      String code = ProxyStatementCodeBlock.CodeBlock.get(signature);
      if(code != null) {
         if(parameters.size() > 0) {
            List<String> array = new ArrayList<>();
            boolean withReturn = ProxyStatementCodeBlock.listenerWithReturn.contains(signature);
            if(withReturn) {
               if(parameters.size() > 1) { 
                  array.add(", " + StrUtil.join(parameters.subList(1, parameters.size()), ", ")); 
               } else { 
                  array.add(""); 
               }
            } else {
               array.add(StrUtil.join(parameters, ", ")); 
            }
            array.add(parameters.get(0)); 
            methodBuilder.addCode(code.replace("#performer", performer), array.toArray(new Object[]{}));
         } else {
            methodBuilder.addCode(code.replace("#performer", performer));
         }
         codeBuilder.addMethods(methodBuilder.build());
         return true;
      }
      return false;
   }

   private static boolean customizeProxyResultSet(String signature, List<String> parameters, String performer, MethodSpec.Builder methodBuilder, JavaCodeBuilder codeBuilder) {
      String code = ProxyResultSetCodeBlock.CodeBlock.get(signature);
      if(code != null) {
         String performerLine = ProxyResultSetCodeBlock.GetMethodMap.get(signature);
         if(performerLine == null) {
            String name = signature.substring(0, signature.indexOf('('));
            performerLine = ProxyResultSetCodeBlock.GetMethodMap.get(name);
         }
         if(performerLine == null) {
            if(signature.startsWith("get")) {
               System.err.println(signature);
            }
         } else {
            code = code.replace("#performer_line", performerLine);
         }
         if(parameters.size() > 0) {
            List<String> array = new ArrayList<>();
            array.add(StrUtil.join(parameters, ", ")); 
            array.add(parameters.get(0)); 
            methodBuilder.addCode(code.replace("#performer", performer), array.toArray(new Object[]{}));
         } else {
            methodBuilder.addCode(code.replace("#performer", performer));
         }
         codeBuilder.addMethods(methodBuilder.build());
         return true;
      }
      return false;
   }

   private static boolean customizeProxyPreparedStatement(String signature, List<String> parameters, String performer, MethodSpec.Builder methodBuilder, JavaCodeBuilder codeBuilder) {
      String code = ProxyPreparedStatementCodeBlock.CodeBlock.get(signature);
      if(code != null) {
         String name = signature.substring(0, signature.indexOf('('));
         code = code.replace("#method", name);
         if(parameters.size() > 0) {
            List<String> array = new ArrayList<>();
            array.add(StrUtil.join(parameters, ", ")); 
            array.add(parameters.get(0)); 
            array.add(parameters.get(1)); 
            methodBuilder.addCode(code.replace("#performer", performer), array.toArray(new Object[]{}));
         } else {
            methodBuilder.addCode(code.replace("#performer", performer));
         }
         codeBuilder.addMethods(methodBuilder.build());
         return true;
      }
      return false;
   }

   private static boolean customizeProxyCallableStatement(String signature, List<String> parameters, String performer, MethodSpec.Builder methodBuilder, JavaCodeBuilder codeBuilder) {
      String code = ProxyCallableStatementCodeBlock.CodeBlock.get(signature);
      if(code != null) {
         String name = signature.substring(0, signature.indexOf('('));
         code = code.replace("#method", name);
         if(parameters.size() > 0) {
            List<String> array = new ArrayList<>();
            array.add(StrUtil.join(parameters, ", ")); 
            array.add(parameters.get(0)); 
            array.add(parameters.get(1)); 
            methodBuilder.addCode(code.replace("#performer", performer), array.toArray(new Object[]{}));
         } else {
            methodBuilder.addCode(code.replace("#performer", performer));
         }
         codeBuilder.addMethods(methodBuilder.build());
         return true;
      }
      return false;
   }

   private static boolean isThrowsSqlException(Method method)
   {
      for (Class<?> clazz : method.getExceptionTypes()) {
         if (clazz.getSimpleName().equals("SQLException")) {
            return true;
         }
      }
      return false;
   }

   private static boolean isDefaultMethod(Class<?> intf, Method intfMethod) throws Exception
   {
      List<Class<?>> paramTypes = new ArrayList<>();

      for (Class<?> pt : intfMethod.getParameterTypes()) {
         paramTypes.add(toJavaClass(pt));
      }

      return intf.getDeclaredMethod(intfMethod.getName(), paramTypes.toArray(new Class[0])).toString().contains("default ");
   }

   private static Set<Class<?>> getAllInterfaces(Class<?> clazz)
   {
      Set<Class<?>> interfaces = new LinkedHashSet<>();
      for (Class<?> intf : clazz.getInterfaces()) {
         if (intf.getInterfaces().length > 0) {
            interfaces.addAll(getAllInterfaces(intf));
         }
         interfaces.add(intf);
      }
      if (clazz.getSuperclass() != null) {
         interfaces.addAll(getAllInterfaces(clazz.getSuperclass()));
      }

      if (clazz.isInterface()) {
         interfaces.add(clazz);
      }

      interfaces.remove(AutoCloseable.class);

      return interfaces;
   }

   private static Class<?> toJavaClass(Class<?> cls) throws Exception
   {
      if (cls.getName().endsWith("[]")) {
         return Array.newInstance(toJavaClass(cls.getName().replace("[]", "")), 0).getClass();
      }
      else {
         return toJavaClass(cls.getName());
      }
   }

   private static Class<?> toJavaClass(String cn) throws Exception
   {
      switch (cn) {
      case "int":
         return int.class;
      case "long":
         return long.class;
      case "short":
         return short.class;
      case "byte":
         return byte.class;
      case "float":
         return float.class;
      case "double":
         return double.class;
      case "boolean":
         return boolean.class;
      case "char":
         return char.class;
      case "void":
         return void.class;
      default:
         return Class.forName(cn);
      }
   }
}
