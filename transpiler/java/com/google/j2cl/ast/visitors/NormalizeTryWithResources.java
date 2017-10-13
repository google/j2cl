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
package com.google.j2cl.ast.visitors;

import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CatchClause;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.IfStatement;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NullLiteral;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.ThrowStatement;
import com.google.j2cl.ast.TryStatement;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.common.SourcePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
                  new TryStatement(
                      sourcePosition,
                      tryStatement.getResourceDeclarations(),
                      tryStatement.getBody(),
                      Collections.emptyList(),
                      null);
              Block refactoredTryBlock =
                  new Block(tryBlock.getSourcePosition(), removeResourceDeclarations(tryBlock));
              return new TryStatement(
                  sourcePosition,
                  Collections.emptyList(),
                  refactoredTryBlock,
                  tryStatement.getCatchClauses(),
                  tryStatement.getFinallyBlock());
            }
            return new Block(sourcePosition, removeResourceDeclarations(tryStatement));
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
   *   let thing = ClosableThing.$create();
   *   let thing2 = ClosableThing.$create();
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
    MethodDescriptor safeClose =
        MethodDescriptor.newBuilder()
            .setJsInfo(JsInfo.RAW)
            .setStatic(true)
            .setEnclosingTypeDescriptor(BootstrapType.EXCEPTIONS.getDescriptor())
            .setName("safeClose")
            .setParameterTypeDescriptors(
                TypeDescriptors.get().javaLangObject, TypeDescriptors.get().javaLangThrowable)
            .setReturnTypeDescriptor(TypeDescriptors.get().javaLangThrowable)
            .build();

      Variable primaryException =
          Variable.newBuilder()
              .setName("$primaryExc")
              .setTypeDescriptor(TypeDescriptors.get().javaLangThrowable)
              .build();

      List<Statement> transformedStatements = new ArrayList<>();
    transformedStatements.add(
        VariableDeclarationExpression.newBuilder()
            .addVariableDeclaration(primaryException, NullLiteral.get())
            .build()
            // TODO(b/65465035): this should be the source position for the variable declaration,
            // but it is not currently available.
            .makeStatement(sourcePosition));

      List<Statement> tryBlockBodyStatements = new ArrayList<>();

      List<VariableDeclarationExpression> resourceDeclarations =
          tryStatement.getResourceDeclarations();
      for (VariableDeclarationExpression declaration : resourceDeclarations) {
        VariableDeclarationFragment originalResourceDeclaration = declaration.getFragments().get(0);
      transformedStatements.add(
          VariableDeclarationExpression.newBuilder()
              .addVariableDeclaration(originalResourceDeclaration.getVariable(), NullLiteral.get())
              .build()
              .makeStatement(sourcePosition));

        Expression assignResourceInitializer =
            BinaryExpression.Builder.asAssignmentTo(originalResourceDeclaration.getVariable())
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

    List<Statement> catchBlockStatements =
        Arrays.asList(
            BinaryExpression.Builder.asAssignmentTo(primaryException)
                .setRightOperand(exceptionFromTry.getReference())
                .build()
                .makeStatement(sourcePosition),
            new ThrowStatement(sourcePosition, exceptionFromTry.getReference()));

      List<Statement> finallyBlockStatements = new ArrayList<>();
      for (VariableDeclarationExpression declaration : Lists.reverse(resourceDeclarations)) {
        MethodCall safeCloseCall =
            MethodCall.Builder.from(safeClose)
                .setArguments(
                    declaration.getFragments().get(0).getVariable().getReference(),
                    primaryException.getReference())
                .build();
        Expression assignExceptionFromSafeCloseCall =
            BinaryExpression.Builder.asAssignmentTo(primaryException)
                .setRightOperand(safeCloseCall)
                .build();
      finallyBlockStatements.add(assignExceptionFromSafeCloseCall.makeStatement(sourcePosition));
      }

    ThrowStatement throwPrimaryException =
        new ThrowStatement(sourcePosition, primaryException.getReference());
    Expression primaryExceptionNotEqualsNull =
        BinaryExpression.newBuilder()
            .setLeftOperand(primaryException.getReference())
            .setOperator(BinaryOperator.NOT_EQUALS)
            .setRightOperand(NullLiteral.get())
            .build();
    IfStatement primaryExceptionNullStatement =
        new IfStatement(sourcePosition, primaryExceptionNotEqualsNull, throwPrimaryException);
      finallyBlockStatements.add(primaryExceptionNullStatement);

    CatchClause catchTryException =
        new CatchClause(exceptionFromTry, new Block(sourcePosition, catchBlockStatements));

    transformedStatements.add(
        new TryStatement(
            sourcePosition,
            Collections.emptyList(),
            new Block(sourcePosition, tryBlockBodyStatements),
            Collections.singletonList(catchTryException),
            new Block(sourcePosition, finallyBlockStatements)));
      return transformedStatements;
    }
}
