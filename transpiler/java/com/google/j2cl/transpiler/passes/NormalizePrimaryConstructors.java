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

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.InitializerBlock;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.Type;
import java.util.List;

/**
 * In Kotlin, instance block initializers and field initializers can refer to the parameters of the
 * primary constructor. In order to avoid variable reference scoping issue, this pass move all
 * instance initializers blocks and field initializers into the body of the primary constructor if
 * it exists.
 */
public class NormalizePrimaryConstructors extends NormalizationPass {
  @Override
  public void applyTo(Type type) {
    Method primaryConstructor = type.getPrimaryConstructor();
    if (primaryConstructor == null) {
      return;
    }

    List<Statement> initStatements =
        type.getInstanceMembers().stream()
            .filter(m -> m.isField() || m.isInitializerBlock())
            .map(this::collectStatements)
            .collect(toList());

    // Remove instance initializers blocks.
    type.getMembers().removeIf(m -> m.isInitializerBlock() && !m.isStatic());

    // Remove field initializers and add initStatement to the body of the primary constructors
    type.accept(
        new AbstractRewriter() {
          @Override
          public boolean shouldProcessType(Type t) {
            return t == type;
          }

          @Override
          public Node rewriteField(Field field) {
            if (field.isStatic() || field.getInitializer() == null) {
              return field;
            }
            return Field.Builder.from(field).setInitializer(null).build();
          }

          @Override
          public Node rewriteMethod(Method method) {
            if (method != primaryConstructor) {
              return method;
            }
            return Method.Builder.from(method).addStatements(initStatements).build();
          }
        });
  }

  private Statement collectStatements(Member member) {
    if (member.isInitializerBlock()) {
      return ((InitializerBlock) member).getBlock();
    }

    checkState(member.isField());
    Field field = (Field) member;
    return BinaryExpression.Builder.asAssignmentTo(field)
        .setRightOperand(field.getInitializer())
        .build()
        .makeStatement(field.getSourcePosition());
  }
}
