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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.HasMethodDescriptor;
import com.google.j2cl.ast.HasParameters;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.common.Problems;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * Utility functions related to source generation in the J2CL AST.
 */
public class GeneratorUtils {
  /** Returns the relative output path for a given type. */
  public static String getRelativePath(Type type) {
    TypeDeclaration typeDeclaration = type.getDeclaration();
    String typeName = typeDeclaration.getSimpleBinaryName();
    String packageName = typeDeclaration.getPackageName();
    return packageName.replace(".", File.separator) + File.separator + typeName;
  }

  /** Returns the absolute binary path for a given type. */
  public static String getAbsolutePath(CompilationUnit compilationUnit, Type type) {
    TypeDeclaration typeDeclaration = type.getDeclaration();
    String typeName = typeDeclaration.getSimpleBinaryName();
    return compilationUnit.getDirectoryPath() + File.separator + typeName;
  }

  /**
   * Returns the js doc annotations for parameter at {@code index} in {@code methodOrFunction}. It
   * is of the form:
   *
   * @param {parameterType} parameterName
   */
  public static <T extends HasParameters & HasMethodDescriptor> String getParameterJsDocAnnotation(
      T methodOrFunctionExpression, int index, GenerationEnvironment environment) {
    Variable parameter = methodOrFunctionExpression.getParameters().get(index);
    TypeDescriptor parameterTypeDescriptor = parameter.getTypeDescriptor();
    String name = environment.aliasForVariable(parameter);
    if (parameter == methodOrFunctionExpression.getJsVarargsParameter()) {
      // The parameter is a js var arg so we convert the type to an array
      checkArgument(parameterTypeDescriptor.isArray());
      // TODO(b/36141178): remove varargs that are typed with a type variable until type checking in
      // jscompiler is fixed.
      TypeDescriptor componentTypeDescriptor = parameterTypeDescriptor.getComponentTypeDescriptor();
      String typeName =
          componentTypeDescriptor.isTypeVariable()
              ? "?"
              : JsDocNameUtils.getJsDocName(componentTypeDescriptor, environment);
      return String.format("@param {...%s} %s", typeName, name);
    } else {
      return String.format(
          "@param {%s%s} %s",
          JsDocNameUtils.getJsDocName(parameterTypeDescriptor, environment),
          methodOrFunctionExpression
                  .getDescriptor()
                  .getParameterDescriptors()
                  .get(index)
                  .isJsOptional()
              ? "="
              : "",
          name);
    }
  }

  public static Expression getInitialValue(Field field) {
    if (field.isCompileTimeConstant()) {
      return field.getInitializer();
    }
    return TypeDescriptors.getDefaultValue(field.getDescriptor().getTypeDescriptor());
  }

  public static String getExtendsClause(Type type, GenerationEnvironment environment) {
    TypeDescriptor superTypeDescriptor = type.getSuperTypeDescriptor();
    if (superTypeDescriptor == null || superTypeDescriptor.isStarOrUnknown()) {
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

  public static void writeToFile(Path outputPath, String content, Problems problems) {
    try {
      // Write using the provided fileSystem (which might be the regular file system or might be a
      // zip file.)
      Files.createDirectories(outputPath.getParent());
      Files.write(outputPath, content.getBytes(StandardCharsets.UTF_8));
      // Wipe entries modification time so that input->output mapping is stable
      // regardless of the time of day.
      Files.setLastModifiedTime(outputPath, FileTime.fromMillis(0));
    } catch (IOException e) {
      problems.error("Could not write to file %s: %s", outputPath, e.getMessage());
      problems.abortIfRequested();
    }
  }
}
