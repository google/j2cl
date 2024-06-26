/*
 * Copyright 2017 Google Inc.
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

import static com.google.j2cl.transpiler.ast.AstUtils.getIterableElement;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ForEachStatement;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;

/**
 * Normalize for-each statement in Kotlin by extracting loop variable declaration outside the
 * statement, allowing insertion of casts and non-null assertions, when necessary.
 */
public class NormalizeForEachStatementJ2kt extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteForEachStatement(ForEachStatement forEachStatement) {
            TypeDescriptor iterableExpressionTypeDescriptor =
                forEachStatement.getIterableExpression().getTypeDescriptor();
            TypeDescriptor elementTypeDescriptor =
                getIterableElement(iterableExpressionTypeDescriptor);
            Variable loopVariable = forEachStatement.getLoopVariable();
            TypeDescriptor loopVariableTypeDescriptor = loopVariable.getTypeDescriptor();
            if (loopVariable.isFinal() && loopVariableTypeDescriptor.canBeNull()) {
              // The variable is not modified by the body of the loop and since it is nullable, it
              // won't need unboxing/coersions as it cant be a primitive, nor it would need
              // nullability assertions since it is nullable.
              return forEachStatement;
            }

            // Create the loop variable with a type corresponding to the element type of the
            // iterator but make it nullable since the nullability of the return type cannot
            // be trusted since it is obtained from a method call.
            Variable newLoopVariable =
                Variable.Builder.from(loopVariable)
                    .setTypeDescriptor(elementTypeDescriptor.toNullable())
                    .build();
            Statement body = forEachStatement.getBody();
            return ForEachStatement.Builder.from(forEachStatement)
                .setLoopVariable(newLoopVariable)
                .setBody(
                    Block.newBuilder()
                        .addStatement(
                            VariableDeclarationExpression.newBuilder()
                                .addVariableDeclaration(
                                    loopVariable, newLoopVariable.createReference())
                                .build()
                                .makeStatement(body.getSourcePosition()))
                        .addStatements(AstUtils.getBodyStatements(body))
                        .build())
                .build();
          }
        });
  }

}
