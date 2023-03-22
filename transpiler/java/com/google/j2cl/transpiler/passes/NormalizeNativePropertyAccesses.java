/*
 * Copyright 2023 Google Inc.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.Variable;

/**
 * Synthesizes getters and setters for fields that are native JS properties, and updates references
 * to call the getter/setter.
 *
 * <p>In order to access JS properties in WASM, they must be imported and called like a JS function.
 */
public class NormalizeNativePropertyAccesses extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    implementJsMethodsForNativeProperties(type);
    updateReferencesToNativeProperties(type);
  }

  private static void implementJsMethodsForNativeProperties(Type type) {
    type.getFields().stream()
        .filter(Field::isNative)
        .forEach(
            f -> {
              synthesizePropertyGetter(type, f);
              if (!f.getDescriptor().isFinal()) {
                synthesizePropertySetter(type, f);
              }
            });
    type.getMembers().removeIf(m -> m.isField() && m.isNative());
  }

  private static void updateReferencesToNativeProperties(Type type) {
    // Rewrite assignments as calls to the setter method. Needs to be done before rewriting access
    // to getter method calls to avoid rewriting references in the lhs of assignments.
    type.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteBinaryExpression(BinaryExpression expression) {
            // We aren't expecting compound assignments due to previous normalizations.
            checkState(
                !expression.isSimpleOrCompoundAssignment() || expression.isSimpleAssignment());
            Expression lhs = expression.getLeftOperand();
            if (!expression.isSimpleAssignment() || !(lhs instanceof FieldAccess)) {
              return expression;
            }
            FieldAccess fieldAccess = (FieldAccess) lhs;
            FieldDescriptor fieldDescriptor = fieldAccess.getTarget();
            if (!fieldDescriptor.isNative()) {
              return expression;
            }
            return MethodCall.Builder.from(AstUtils.getSetterMethodDescriptor(fieldDescriptor))
                .setQualifier(fieldAccess.getQualifier())
                .setArguments(expression.getRightOperand())
                .build();
          }
        });
    type.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteFieldAccess(FieldAccess fieldAccess) {
            FieldDescriptor fieldDescriptor = fieldAccess.getTarget();
            if (!fieldDescriptor.isNative()) {
              return fieldAccess;
            }
            return MethodCall.Builder.from(AstUtils.getGetterMethodDescriptor(fieldDescriptor))
                .setQualifier(fieldAccess.getQualifier())
                .build();
          }
        });
  }

  private static void synthesizePropertyGetter(Type type, Field field) {
    checkArgument(field.isNative());
    FieldDescriptor fieldDescriptor = field.getDescriptor();
    type.addMember(
        Method.newBuilder()
            .setSourcePosition(field.getSourcePosition())
            .setMethodDescriptor(AstUtils.getGetterMethodDescriptor(fieldDescriptor))
            .build());
  }

  private static void synthesizePropertySetter(Type type, Field field) {
    checkArgument(field.isNative());
    FieldDescriptor fieldDescriptor = field.getDescriptor();
    Variable parameter =
        Variable.newBuilder()
            .setName("value")
            .setTypeDescriptor(fieldDescriptor.getTypeDescriptor())
            .setParameter(true)
            .build();
    type.addMember(
        Method.newBuilder()
            .setSourcePosition(field.getSourcePosition())
            .setMethodDescriptor(AstUtils.getSetterMethodDescriptor(fieldDescriptor))
            .setParameters(parameter)
            .build());
  }
}
