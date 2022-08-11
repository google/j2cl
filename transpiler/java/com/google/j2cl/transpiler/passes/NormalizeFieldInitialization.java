/*
 * Copyright 2018 Google Inc.
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
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.InitializerBlock;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.Type;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Makes sure all fields are initialized correctly wrt Java semantics by making implicit default
 * values explicit and moving explicit initialization to corresponding initialization block.
 */
public class NormalizeFieldInitialization extends NormalizationPass {
  @Override
  public void applyTo(Type type) {
    // Move field initialization to InitializerBlocks keeping them in source order.
    List<Field> fieldDeclarations = new ArrayList<>();
    type.accept(
        new AbstractRewriter() {
          @Nullable
          @Override
          public Member rewriteField(Field field) {
            fieldDeclarations.add(
                Field.Builder.from(field)
                    .setInitializer(getDeclarationValue(field))
                    .setSourcePosition(field.getSourcePosition())
                    .build());

            if (!field.hasInitializer() || field.isCompileTimeConstant()) {
              // Not initialized in <clinit> nor <init>.
              return null;
            }

            // Replace the field declaration with an initializer block inplace to preserve
            // ordering.
            DeclaredTypeDescriptor enclosingTypeDescriptor =
                field.getDescriptor().getEnclosingTypeDescriptor();
            return InitializerBlock.newBuilder()
                .setDescriptor(
                    field.isStatic()
                        ? enclosingTypeDescriptor.getClinitMethodDescriptor()
                        : enclosingTypeDescriptor.getInitMethodDescriptor())
                .setSourcePosition(field.getSourcePosition())
                .setBlock(createInitializerBlockFromFieldInitializer(field))
                .build();
          }
        });
    // Keep the fields for declaration purpose.
    type.addMembers(fieldDeclarations);
  }

  private static Expression getDeclarationValue(Field field) {
    Expression declarationValue =
        field.isCompileTimeConstant()
            ? field.getInitializer()
            : field.getDescriptor().getTypeDescriptor().getDefaultValue();

    if (declarationValue instanceof NullLiteral) {
      // Skip initialization for fields that are initialized with null as an optimization.
      declarationValue = null;
    }
    return declarationValue;
  }

  private static Block createInitializerBlockFromFieldInitializer(Field field) {
    FieldDescriptor fieldDescriptor = field.getDescriptor();
    SourcePosition sourcePosition = field.getSourcePosition();
    return Block.newBuilder()
        .setSourcePosition(sourcePosition)
        .setStatements(
            BinaryExpression.Builder.asAssignmentTo(fieldDescriptor)
                .setRightOperand(field.getInitializer())
                .build()
                .makeStatement(sourcePosition))
        .build();
  }
}
