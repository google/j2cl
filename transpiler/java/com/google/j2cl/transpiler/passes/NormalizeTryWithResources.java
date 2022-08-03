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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CatchClause;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.ThrowStatement;
import com.google.j2cl.transpiler.ast.TryStatement;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import java.util.ArrayList;
import java.util.List;

/** Normalize Java try with resource blocks such that they can be represented in JavaScript. */
public class NormalizeTryWithResources extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          /**
           * We want to rewrite the try with resource statement to use vanilla try catch statements
           * as described by the JLS 14.20.3.1.
           */
          @Override
          public Statement rewriteTryStatement(TryStatement tryStatement) {
            SourcePosition sourcePosition = tryStatement.getSourcePosition();
            if (tryStatement.getResourceDeclarations().isEmpty()) {
              return tryStatement;
            }
            if (tryStatement.getFinallyBlock() != null
                || !tryStatement.getCatchClauses().isEmpty()) {
              // See JLS 14.20.3.2
              TryStatement tryBlock =
                  TryStatement.newBuilder()
                      .setSourcePosition(sourcePosition)
                      .setResourceDeclarations(tryStatement.getResourceDeclarations())
                      .setBody(tryStatement.getBody())
                      .build();
              Block refactoredTryBlock =
                  Block.newBuilder()
                      .setSourcePosition(tryBlock.getSourcePosition())
                      .setStatements(removeResourceDeclarations(tryBlock))
                      .build();
              return TryStatement.newBuilder()
                  .setSourcePosition(sourcePosition)
                  .setBody(refactoredTryBlock)
                  .setCatchClauses(tryStatement.getCatchClauses())
                  .setFinallyBlock(tryStatement.getFinallyBlock())
                  .build();
            }
            return Block.newBuilder()
                .setSourcePosition(sourcePosition)
                .setStatements(removeResourceDeclarations(tryStatement))
                .build();
          }
        });
  }

  /**
   * We transform:
   *
   * <pre>{@code
   * try (ClosableThing thing = new ClosableThing(), ClosableThing thing2 = new ClosableThing()) {
   *   ....
   * }
   * }</pre>
   *
   * to:
   *
   * <pre>{@code
   * let $primaryExc = null;
   * let thing = null;
   * let thing2 = null;
   * try {
   *   thing = ClosableThing.$create();
   *   thing2 = ClosableThing.$create();
   *   ...
   * } catch ($exceptionFromTry) {
   *   $primaryExc = $exceptionFromTry;
   *   throw $exceptionFromTry;
   * } finally {
   *   $primaryExc = $Exceptions.safeClose(thing2, $primaryExc);
   *   $primaryExc = $Exceptions.safeClose(thing, $primaryExc);
   *   if ($primaryExc != null) {
   *     throw $primaryExc;
   *   }
   * }
   * }</pre>
   */
  private static List<Statement> removeResourceDeclarations(TryStatement tryStatement) {
    SourcePosition sourcePosition = tryStatement.getSourcePosition();
    Variable primaryException =
        Variable.newBuilder()
            .setName("$primaryExc")
            .setTypeDescriptor(TypeDescriptors.get().javaLangThrowable)
            .build();

    List<Statement> transformedStatements = new ArrayList<>();
    transformedStatements.add(
        VariableDeclarationExpression.newBuilder()
            .addVariableDeclaration(
                primaryException, primaryException.getTypeDescriptor().getNullValue())
            .build()
            // TODO(b/65465035): this should be the source position for the variable declaration,
            // but it is not currently available.
            .makeStatement(sourcePosition));

    List<Statement> tryBlockBodyStatements = new ArrayList<>();

    List<VariableDeclarationExpression> resourceDeclarations =
        tryStatement.getResourceDeclarations();
    for (VariableDeclarationExpression declaration : resourceDeclarations) {
      VariableDeclarationFragment originalResourceDeclaration = declaration.getFragments().get(0);
      Variable originalVariable = originalResourceDeclaration.getVariable();
      originalVariable.setFinal(false);
      transformedStatements.add(
          VariableDeclarationExpression.newBuilder()
              .addVariableDeclaration(
                  originalVariable, originalVariable.getTypeDescriptor().getNullValue())
              .build()
              .makeStatement(sourcePosition));

      Expression assignResourceInitializer =
          BinaryExpression.Builder.asAssignmentTo(originalVariable)
              .setRightOperand(originalResourceDeclaration.getInitializer())
              .build();
      tryBlockBodyStatements.add(assignResourceInitializer.makeStatement(sourcePosition));
    }
    tryBlockBodyStatements.addAll(tryStatement.getBody().getStatements());

    Variable exceptionFromTry =
        Variable.newBuilder()
            .setName("$exceptionFromTry")
            .setTypeDescriptor(TypeDescriptors.get().javaLangThrowable)
            .build();

    ImmutableList<Statement> catchBlockStatements =
        ImmutableList.of(
            BinaryExpression.Builder.asAssignmentTo(primaryException)
                .setRightOperand(exceptionFromTry)
                .build()
                .makeStatement(sourcePosition),
            ThrowStatement.newBuilder()
                .setSourcePosition(sourcePosition)
                .setExpression(exceptionFromTry.createReference())
                .build());

    List<Statement> finallyBlockStatements = new ArrayList<>();
    for (VariableDeclarationExpression declaration : Lists.reverse(resourceDeclarations)) {
      MethodCall safeCloseCall =
          RuntimeMethods.createExceptionsMethodCall(
              "safeClose",
              declaration.getFragments().get(0).getVariable().createReference(),
              primaryException.createReference());

      Expression assignExceptionFromSafeCloseCall =
          BinaryExpression.Builder.asAssignmentTo(primaryException)
              .setRightOperand(safeCloseCall)
              .build();

      finallyBlockStatements.add(assignExceptionFromSafeCloseCall.makeStatement(sourcePosition));
    }

    ThrowStatement throwPrimaryException =
        ThrowStatement.newBuilder()
            .setSourcePosition(sourcePosition)
            .setExpression(primaryException.createReference())
            .build();
    Expression primaryExceptionNotEqualsNull =
        primaryException.createReference().infixNotEqualsNull();
    IfStatement primaryExceptionNullStatement =
        IfStatement.newBuilder()
            .setSourcePosition(sourcePosition)
            .setConditionExpression(primaryExceptionNotEqualsNull)
            .setThenStatement(throwPrimaryException)
            .build();
    finallyBlockStatements.add(primaryExceptionNullStatement);

    CatchClause catchTryException =
        CatchClause.newBuilder()
            .setExceptionVariable(exceptionFromTry)
            .setBody(
                Block.newBuilder()
                    .setSourcePosition(sourcePosition)
                    .setStatements(catchBlockStatements)
                    .build())
            .build();

    transformedStatements.add(
        TryStatement.newBuilder()
            .setSourcePosition(sourcePosition)
            .setBody(
                Block.newBuilder()
                    .setSourcePosition(sourcePosition)
                    .setStatements(tryBlockBodyStatements)
                    .build())
            .setCatchClauses(catchTryException)
            .setFinallyBlock(
                Block.newBuilder()
                    .setSourcePosition(sourcePosition)
                    .setStatements(finallyBlockStatements)
                    .build())
            .build());
    return transformedStatements;
  }
}
