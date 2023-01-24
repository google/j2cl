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
package com.google.j2cl.transpiler.passes;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.Type;
import java.util.List;

/**
 * Implements static initialization to comply with Java semantics by keeping track of whether a
 * class has been initialized using a static variable for each class and chekcing the condition to
 * decide whether to execute the initialization code for that class.
 */
public class ImplementStaticInitializationViaConditionChecks
    extends ImplementStaticInitializationBase {

  public ImplementStaticInitializationViaConditionChecks() {
    super(/* triggerClinitInConstructors = */ true);
  }

  @Override
  public void applyTo(Type type) {
    synthesizeClinitCallsOnFieldAccess(type);
    synthesizeClinitMethod(type);
  }

  /** Add clinit calls to field accesses. */
  private void synthesizeClinitCallsOnFieldAccess(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteFieldAccess(FieldAccess fieldAccess) {
            FieldDescriptor target = fieldAccess.getTarget();
            if (target.isMemberOf(type.getDeclaration())) {
              // No need to call clinit when accessing the field from members in the enclosing
              // type.
              return fieldAccess;
            }

            if (triggersClinit(target, type)) {
              // TODO(b/181086258): Move the condition check to the field access to avoid clinit
              // function calls after the class is initialized (also potentially do the same
              // for method calls).
              return MultiExpression.newBuilder()
                  .addExpressions(
                      createClinitCallExpression(target.getEnclosingTypeDescriptor()), fieldAccess)
                  .build();
            }

            return fieldAccess;
          }
        });
  }

  /** Implements the static initialization method ($clinit). */
  private static void synthesizeClinitMethod(Type type) {
    SourcePosition sourcePosition = type.getSourcePosition();

    FieldDescriptor isInitializedFieldDescriptor = getInitializedField(type.getTypeDescriptor());

    // Add the $isInitialized static field to the type.
    type.addMember(
        Field.Builder.from(isInitializedFieldDescriptor).setSourcePosition(sourcePosition).build());

    // if ($isInitialized) { return; }
    Statement checkInitialized =
        IfStatement.newBuilder()
            .setConditionExpression(FieldAccess.Builder.from(isInitializedFieldDescriptor).build())
            .setThenStatement(
                ReturnStatement.newBuilder()
                    .setSourcePosition(sourcePosition)
                    .build())
            .setSourcePosition(sourcePosition)
            .build();

    // $isInitialized = true;
    Statement setInitialized =
        BinaryExpression.Builder.asAssignmentTo(isInitializedFieldDescriptor)
            .setRightOperand(BooleanLiteral.get(true))
            .build()
            .makeStatement(sourcePosition);

    // Code from static initializer blocks.
    List<Statement> clinitStatements =
        type.getStaticInitializerBlocks().stream()
            .flatMap(initializerBlock -> initializerBlock.getBlock().getStatements().stream())
            .collect(toImmutableList());

    type.addMember(
        Method.newBuilder()
            .setMethodDescriptor(type.getTypeDescriptor().getClinitMethodDescriptor())
            .addStatements(checkInitialized, setInitialized)
            .addStatements(clinitStatements)
            .setSourcePosition(sourcePosition)
            .build());

    type.getMembers().removeIf(member -> member.isInitializerBlock() && member.isStatic());
  }

  /** Returns the class initializer property as a field for a particular type */
  private static FieldDescriptor getInitializedField(DeclaredTypeDescriptor typeDescriptor) {
    return FieldDescriptor.newBuilder()
        .setStatic(true)
        .setEnclosingTypeDescriptor(typeDescriptor)
        .setTypeDescriptor(PrimitiveTypes.BOOLEAN)
        .setName("$initialized")
        .build();
  }
}
