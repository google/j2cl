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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CatchClause;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.IfStatement;
import com.google.j2cl.ast.InstanceOfExpression;
import com.google.j2cl.ast.JsDocCastExpression;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.ThrowStatement;
import com.google.j2cl.ast.TryStatement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.UnionTypeDescriptor;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.common.SourcePosition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Since JavaScript doesn't support multiple catch clauses, we convert multiple catch clauses to a
 * single catch clause that contains control flow to route to the correct block of code.
 *
 * <p>In Java: <pre>{@code
 * try {
 *    throw new ClassCastException();
 *  } catch (NullPointerException | ClassCastException e) {
 *    // expected empty body.
 *  } catch (RuntimeException r) {
 *    r = null; // used to show exception variable is transpiled correctly.
 *  }
 *  }</pre>
 *
 * <p>is transpiled to JavaScript: <pre>{@code
 *  try {
 *    throw ClassCastException.$create();
 *  } catch (e) {
 *    if (NullPointerException.$isInstance(e) ||
 *        ClassCastException.$isInstance(e)) {
 *    } else if (RuntimeException.$isInstance(e)) {
 *      let r = e;
 *      r = null;
 *    } else {
 *      throw e;
 *    }
 *  }
 * }</pre>
 */
public class NormalizeCatchClauses extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Statement rewriteTryStatement(TryStatement statement) {
            if (statement.getCatchClauses().isEmpty()) {
              return statement;
            }
            return new TryStatement(
                statement.getSourcePosition(),
                statement.getResourceDeclarations(),
                statement.getBody(),
                Collections.singletonList(mergeClauses(statement.getCatchClauses())),
                statement.getFinallyBlock());
          }
        });
  }

  private static CatchClause mergeClauses(List<CatchClause> clauses) {
    checkArgument(!clauses.isEmpty());

    SourcePosition sourcePosition = clauses.get(0).getBody().getSourcePosition();
    // Create a temporary exception variable.
    Variable exceptionVariable =
        Variable.newBuilder()
            .setName("__$exc")
            .setTypeDescriptor(TypeDescriptors.get().javaLangObject)
            .build();

    Statement body = bodyBuilder(sourcePosition, clauses, exceptionVariable);
    return new CatchClause(
        exceptionVariable,
        Block.newBuilder().setSourcePosition(body.getSourcePosition()).setStatements(body).build());
  }

  private static Statement bodyBuilder(
      SourcePosition firstClauseSourcePosition,
      List<CatchClause> clauses,
      Variable exceptionVariable) {
    // Base case. If no more clauses left the last statement throws the exception.
    if (clauses.isEmpty()) {
      Statement noMatchThrowException =
          new ThrowStatement(firstClauseSourcePosition, exceptionVariable.getReference());
      return Block.newBuilder()
          .setSourcePosition(firstClauseSourcePosition)
          .setStatements(noMatchThrowException)
          .build();
    }

    CatchClause clause = clauses.get(0);
    Variable catchVariable = clause.getExceptionVar();

    TypeDescriptor exceptionTypeDescriptor = catchVariable.getTypeDescriptor();
    List<TypeDescriptor> typesToCheck =
        exceptionTypeDescriptor.isUnion()
            ? ((UnionTypeDescriptor) exceptionTypeDescriptor).getUnionTypeDescriptors()
            : Collections.singletonList(exceptionTypeDescriptor);
    Expression condition = checkTypeExpression(exceptionVariable, typesToCheck);
    SourcePosition currentClauseSourcePosition = clause.getBody().getSourcePosition();
    List<Statement> catchClauseBody = new ArrayList<>(clause.getBody().getStatements());
    ExpressionStatement assignment =
        VariableDeclarationExpression.newBuilder()
            .addVariableDeclaration(
                catchVariable,
                JsDocCastExpression.newBuilder()
                    .setExpression(exceptionVariable.getReference())
                    .setCastType(catchVariable.getTypeDescriptor())
                    .build())
            .build()
            .makeStatement(currentClauseSourcePosition);
    catchClauseBody.add(0, assignment);

    return new IfStatement(
        currentClauseSourcePosition,
        condition,
        Block.newBuilder()
            .setSourcePosition(currentClauseSourcePosition)
            .setStatements(catchClauseBody)
            .build(),
        bodyBuilder(
            firstClauseSourcePosition, clauses.subList(1, clauses.size()), exceptionVariable));
  }

  /**
   * Given a list of types t1, t2, t3.. and an exceptionVariable e, this method generates an
   * expression that checks if e is of type t1 or t2, or t3...
   *
   * <p>e instance of t1 || e instanceof t2 || e instanceof t3 ...
   */
  private static Expression checkTypeExpression(
      Variable exceptionVariable, List<TypeDescriptor> typeDescriptors) {
    List<InstanceOfExpression> instanceofs =
        typeDescriptors
            .stream()
            .map(t -> new InstanceOfExpression(null, exceptionVariable.getReference(), t))
            .collect(toImmutableList());
    return AstUtils.joinExpressionsWithBinaryOperator(BinaryOperator.CONDITIONAL_OR, instanceofs);
  }
}
