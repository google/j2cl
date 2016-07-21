/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.generator;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.MemberReference;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.errors.Errors;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

/**
 * Utility functions related to source generation in the J2CL AST.
 */
public class GeneratorUtils {
  public static String getBinaryName(TypeDescriptor typeDescriptor) {
    return typeDescriptor.getBinaryName();
  }

  /**
   * Returns the relative output path for a given type.
   */
  public static String getRelativePath(JavaType javaType) {
    TypeDescriptor typeDescriptor = javaType.getDescriptor();
    String typeName = typeDescriptor.getBinaryClassName();
    String packageName = typeDescriptor.getPackageName();
    return packageName.replace(".", File.separator) + File.separator + typeName;
  }

  /**
   * Returns the absolute binary path for a given type.
   */
  public static String getAbsolutePath(CompilationUnit compilationUnit, JavaType javaType) {
    TypeDescriptor typeDescriptor = javaType.getDescriptor();
    String typeName = typeDescriptor.getBinaryClassName();
    return compilationUnit.getDirectoryPath() + File.separator + typeName;
  }

  /**
   * Returns the absolute path for the given output FileSystem and output paths.
   */
  public static Path getAbsolutePath(
      FileSystem outputFileSystem,
      String outputLocationPath,
      String relativeFilePath,
      String suffix) {
    return outputLocationPath != null
        ? outputFileSystem.getPath(outputLocationPath, relativeFilePath + suffix)
        : outputFileSystem.getPath(relativeFilePath + suffix);
  }

  /**
   * Returns the method header including (static) (getter/setter) methodName(parametersList).
   */
  public static String getMethodHeader(Method method, GenerationEnvironment environment) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    String staticQualifier = methodDescriptor.isStatic() ? "static" : null;
    String methodName = ManglingNameUtils.getMangledName(methodDescriptor);
    String parameterList = getParameterList(method, environment);
    return Joiner.on(" ").skipNulls().join(staticQualifier, methodName + "(" + parameterList + ")");
  }

  /**
   * Returns the js doc annotations for parameter at {@code index} in {@code method}. It is of
   * the form:
   */
  public static String getParameterJsDocAnnotation(
      Method method, int index, GenerationEnvironment environment) {
    Variable parameter = method.getParameters().get(index);
    TypeDescriptor parameterTypeDescriptor = parameter.getTypeDescriptor();
    String name = environment.aliasForVariable(parameter);
    if (method.getDescriptor().isJsMethodVarargs() && index + 1 == method.getParameters().size()) {
      // The parameter is a js var arg so we convert the type to an array
      Preconditions.checkArgument(parameterTypeDescriptor.isArray());
      String typeName =
          JsDocNameUtils.getJsDocName(
              parameterTypeDescriptor.getComponentTypeDescriptor(), environment);
      return String.format("@param {...%s} %s", typeName, name);
    } else {
      return String.format(
          "@param {%s%s} %s",
          JsDocNameUtils.getJsDocName(parameterTypeDescriptor, environment),
          method.isParameterOptional(index) ? "=" : "",
          name);
    }
  }

  public static String getParameterList(Method method, final GenerationEnvironment environment) {
    List<String> parameterNameList =
        Lists.transform(
            method.getParameters(),
            new Function<Variable, String>() {
              @Override
              public String apply(Variable variable) {
                return environment.aliasForVariable(variable);
              }
            });
    return Joiner.on(", ").join(parameterNameList);
  }

  /**
   * Returns true if the type has a superclass that is not a native js type.
   */
  public static boolean hasNonNativeSuperClass(JavaType type) {
    return type.getSuperTypeDescriptor() != null && !type.getSuperTypeDescriptor().isNative();
  }

  /**
   * Returns whether the $clinit function should be rewritten as NOP.
   */
  public static boolean needRewriteClinit(JavaType type) {
    for (Object element : type.getStaticFieldsAndInitializerBlocks()) {
      if (element instanceof Field) {
        Field field = (Field) element;
        if (field.hasInitializer() && !field.isCompileTimeConstant()) {
          return true;
        }
      } else if (element instanceof Block) {
        return true;
      } else {
        throw new UnsupportedOperationException("Unsupported element: " + element);
      }
    }
    return false;
  }

  /**
   * If possible, returns the qualifier of the provided expression, otherwise null.
   */
  public static Expression getQualifier(Expression expression) {
    if (!(expression instanceof MemberReference)) {
      return null;
    }
    return ((MemberReference) expression).getQualifier();
  }

  public static Expression getInitialValue(Field field) {
    if (field.isCompileTimeConstant()) {
      return field.getInitializer();
    }
    return TypeDescriptors.getDefaultValue(field.getDescriptor().getTypeDescriptor());
  }

  public static boolean hasJsDoc(JavaType type) {
    return !type.getSuperInterfaceTypeDescriptors().isEmpty()
        || type.getDescriptor().isParameterizedType()
        || (type.getSuperTypeDescriptor() != null
            && type.getSuperTypeDescriptor().isParameterizedType());
  }

  /**
   * If the method is a native method with a different namespace than the current class, or it is a
   * native JsProperty method, no need to output any code for it.
   */
  public static boolean shouldNotEmitCode(Method method) {
    return method.isNative()
        && (method.getDescriptor().isJsProperty() || method.getDescriptor().isJsMethod());
  }

  public static String getExtendsClause(JavaType javaType, GenerationEnvironment environment) {
    TypeDescriptor superTypeDescriptor = javaType.getSuperTypeDescriptor();
    if (superTypeDescriptor == null) {
      return "";
    }
    String superTypeName = environment.aliasForType(superTypeDescriptor);
    return String.format("extends %s ", superTypeName);
  }

  /**
   * The ctor visibility should never be public since it is not intended to be called externally
   * unless a super class calls it as part of the ctor chain.
   */
  public static String visibilityForMethod(Method method) {
    return method.getDescriptor().getVisibility().jsText;
  }

  public static void writeToFile(Path outputPath, String content, Charset charset, Errors errors) {
    try {
      // Write using the provided fileSystem (which might be the regular file system or might be a
      // zip file.)
      Files.createDirectories(outputPath.getParent());
      Files.write(outputPath, content.getBytes(charset));
      // Wipe entries modification time so that input->output mapping is stable
      // regardless of the time of day.
      Files.setLastModifiedTime(outputPath, FileTime.fromMillis(0));
    } catch (IOException e) {
      errors.error(Errors.Error.ERR_ERROR, e.getClass().getSimpleName() + ": " + e.getMessage());
      errors.maybeReportAndExit();
    }
  }
}
