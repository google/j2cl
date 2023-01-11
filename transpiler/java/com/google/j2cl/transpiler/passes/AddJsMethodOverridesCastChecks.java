/*
 * Copyright 2022 Google Inc.
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
package com.google.j2cl.transpiler.passes;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import java.util.ArrayList;
import java.util.List;

/** Add the missing bridge method cast checks for JsMethod overrides. */
public class AddJsMethodOverridesCastChecks extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Method rewriteMethod(Method method) {
            MethodDescriptor methodDescriptor = method.getDescriptor();
            if (methodDescriptor.getEnclosingTypeDescriptor().isInterface()
                || method.isNative()
                || method.isAbstract()
                || !methodDescriptor.isJsMethod()) {
              return method;
            }

            List<Variable> parameters = new ArrayList<>(method.getParameters());
            List<Statement> preamble = new ArrayList<>();
            for (int index = 0; index < parameters.size(); index++) {
              if (!doesParameterNeedCast(method, index)) {
                continue;
              }

              Variable parameter = parameters.get(index);
              Variable newParameter =
                  Variable.Builder.from(parameter)
                      // Set generic type descriptor so that the cast is not removed, and avoid
                      // trying to figure out what are the types that actually flow here.
                      .setTypeDescriptor(TypeDescriptors.get().javaLangObject)
                      .build();
              parameters.set(index, newParameter);

              preamble.add(
                  VariableDeclarationExpression.newBuilder()
                      .addVariableDeclaration(
                          parameter,
                          CastExpression.newBuilder()
                              .setExpression(newParameter.createReference())
                              .setCastTypeDescriptor(parameter.getTypeDescriptor())
                              .build())
                      .build()
                      .makeStatement(parameter.getSourcePosition()));
            }

            if (preamble.isEmpty()) {
              return method;
            }

            return Method.Builder.from(method)
                .setParameters(parameters)
                .setStatements(preamble)
                .addStatements(method.getBody())
                .build();
          }
        });
  }

  private boolean doesParameterNeedCast(Method method, int index) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    ImmutableList<ParameterDescriptor> parameterDescriptors =
        methodDescriptor.getParameterDescriptors();

    // Look only at the overridden jsmethods which is the case in which there in no bridge
    // and the override takes the same name as the overridden method.
    for (MethodDescriptor overriddenMethod : methodDescriptor.getJsOverriddenMethodDescriptors()) {
      ImmutableList<ParameterDescriptor> overriddenParameterDescriptors =
          overriddenMethod.getDeclarationDescriptor().getParameterDescriptors();

      if (overriddenParameterDescriptors.size() <= index) {
        // J2CL allows to define a native method method  with the same jsname with less parameters,
        // that will be overridden in JavaScript by a Java JsMethod that is not an override (e.g.
        // the Java method declared an extra optional parameter).
        continue;
      }

      if (isCastNeeded(
          parameterDescriptors.get(index), overriddenParameterDescriptors.get(index))) {
        return true;
      }
    }
    return false;
  }

  private boolean isCastNeeded(
      ParameterDescriptor parameterDescriptor, ParameterDescriptor overriddenParameterDescriptor) {
    return !parameterDescriptor.isVarargs()
        && !parameterDescriptor.getTypeDescriptor().isPrimitive()
        // TODO(b/263261275): find out why unboxed JsEnums can be passed to methods expecting
        // Enums and remove the workaround of not checking Enum parameters.
        && !TypeDescriptors.isJavaLangEnum(
            parameterDescriptor.getTypeDescriptor().toRawTypeDescriptor())
        && !parameterDescriptor
            .getTypeDescriptor()
            .toRawTypeDescriptor()
            .isSameBaseType(
                overriddenParameterDescriptor.getTypeDescriptor().toRawTypeDescriptor());
  }
}
