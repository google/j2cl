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

import com.google.j2cl.ast.HasMethodDescriptor;
import com.google.j2cl.ast.HasParameters;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;

/**
 * Utility functions related to source generation in the J2CL AST.
 */
public class GeneratorUtils {
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

  private GeneratorUtils() {}
}
