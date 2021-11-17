/*
 * Copyright 2020 Google Inc.
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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BinaryOperator;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;

/**
 * Implements the $copy method in JsFunction implementations.
 *
 * <p>Since JsFunction implementation classes have to be seen as functions, while preserving aspects
 * of being a class, like having fields. This is achieved by constructing an object of the
 * JsFunction implementation class, initializing it according to he called constructor and then
 * copying over the fields to a real JavaScript function object using the $copy method synthesized
 * here.
 */
public class ImplementJsFunctionCopyMethod extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    if (!type.isJsFunctionImplementation()) {
      return;
    }
    synthesizeCopyMethod(type);
  }

  // TODO(b/80269359): may copy Objects methods (equals, hashCode, etc. ) as well.
  private static void synthesizeCopyMethod(Type type) {
    Variable fromParameter =
        Variable.newBuilder()
            .setName("from")
            .setTypeDescriptor(TypeDescriptors.getUnknownType())
            .setParameter(true)
            .setFinal(true)
            .build();

    Variable toParameter =
        Variable.newBuilder()
            .setName("to")
            .setTypeDescriptor(TypeDescriptors.getUnknownType())
            .setParameter(true)
            .setFinal(true)
            .build();

    Method.Builder methodBuilder =
        Method.newBuilder()
            .setMethodDescriptor(type.getTypeDescriptor().getCopyMethodDescriptor())
            .setParameters(fromParameter, toParameter)
            .setSourcePosition(SourcePosition.NONE);

    for (Field field : type.getInstanceFields()) {
      methodBuilder.addStatements(
          BinaryExpression.newBuilder()
              .setLeftOperand(
                  FieldAccess.newBuilder()
                      .setQualifier(toParameter.createReference())
                      .setTarget(field.getDescriptor())
                      .build())
              .setOperator(BinaryOperator.ASSIGN)
              .setRightOperand(
                  FieldAccess.newBuilder()
                      .setQualifier(fromParameter.createReference())
                      .setTarget(field.getDescriptor())
                      .build())
              .build()
              .makeStatement(SourcePosition.NONE));
    }
    methodBuilder.addStatements(
        BinaryExpression.newBuilder()
            .setLeftOperand(
                FieldAccess.newBuilder()
                    .setQualifier(toParameter.createReference())
                    .setTarget(type.getTypeDescriptor().getIsInstanceMarkerField())
                    .build())
            .setOperator(BinaryOperator.ASSIGN)
            .setRightOperand(BooleanLiteral.get(true))
            .build()
            .makeStatement(SourcePosition.NONE));

    type.addMember(methodBuilder.build());
  }
}
