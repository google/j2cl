/*
 * Copyright 2016 Google Inc.
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

import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.Label;
import com.google.j2cl.transpiler.ast.LabelReference;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.MethodLike;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.NameDeclaration;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Reference;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchCase;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableReference;
import java.util.HashMap;
import java.util.Map;

/** Verifies that variables and labels are referenced within their scopes. */
public class VerifyReferenceScoping extends NormalizationPass {

  private final boolean allowStatementScoping;

  private final boolean allowExpressionScoping;

  /**
   * Creates a verifier that check that all variable reference occur in the enclosing member they
   * are created in.
   */
  public VerifyReferenceScoping() {
    this(/* allowStatementScoping= */ false, /* allowExpressionScoping= */ false);
  }

  private VerifyReferenceScoping(boolean allowStatementScoping, boolean allowExpressionScoping) {
    this.allowStatementScoping = allowStatementScoping;
    this.allowExpressionScoping = allowExpressionScoping;
  }

  /**
   * Creates a verifier that checks that all variable references occur in the enclosing statement
   * they are declared in.
   */
  public static VerifyReferenceScoping allowOnlyStatementScopes() {
    return new VerifyReferenceScoping(
        /* allowStatementScoping= */ true, /* allowExpressionScoping= */ false);
  }

  /**
   * Creates a verifier that checks that all variable references occur in the enclosing statement or
   * expression they are declared in.
   */
  public static VerifyReferenceScoping allowExpressionScopes() {
    return new VerifyReferenceScoping(
        /* allowStatementScoping= */ true, /* allowExpressionScoping= */ true);
  }

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractVisitor() {
          private final Map<NameDeclaration, Object> declarationScopeByNameDeclaration =
              new HashMap<>();

          @Override
          public void exitVariableDeclarationExpression(
              VariableDeclarationExpression variableDeclarationExpression) {
            Object parent = getParent();
            checkState(
                parent instanceof Statement || parent instanceof MultiExpression,
                "Variable declaration expressions `%s` can only appear directly under a"
                    + " Statement or a MultiExpression",
                variableDeclarationExpression.toString());
          }

          @Override
          public void exitVariable(Variable variable) {
            recordDeclarationScope(variable, getParent(this::isAllowedVariableScope));
          }

          @Override
          public void exitLabel(Label label) {
            checkState(
                getParent() instanceof LabeledStatement,
                "Label %s declaration should only be declared in LabelStatement.",
                label.getName());
            recordDeclarationScope(label, getParent());
          }

          public void recordDeclarationScope(NameDeclaration declaration, Object declarationScope) {
            Object previous = declarationScopeByNameDeclaration.put(declaration, declarationScope);
            checkState(
                previous == null,
                "%s %s already in scope.",
                declaration.getClass().getSimpleName(),
                declaration.getName());
          }

          @Override
          public boolean enterLabelReference(LabelReference labelReference) {
            checkReference(labelReference);
            return false;
          }

          @Override
          public boolean enterVariableReference(VariableReference variableReference) {
            checkReference(variableReference);
            return false;
          }

          private void checkReference(Reference<? extends NameDeclaration> reference) {
            // Verify that the reference references a declaration that is in scope.
            Object declarationScope = declarationScopeByNameDeclaration.get(reference.getTarget());
            if (getParents().anyMatch(declarationScope::equals)) {
              return;
            }

            Statement currentStatement = (Statement) getParent(Statement.class::isInstance);
            final Node context =
                currentStatement != null
                    ? currentStatement
                    : getCurrentMember() != null ? getCurrentMember() : getCurrentType();
            throw new AssertionError(
                String.format(
                    "%s %s in %s not defined in enclosing scope.",
                    reference.getTarget().getClass().getSimpleName(),
                    reference.getTarget().getName(),
                    context));
          }

          private boolean isAllowedVariableScope(Object o) {
            if (o instanceof Member || o instanceof MethodLike) {
              // Members and function expressions are always allowed as scopes.
              return true;
            }

            // If statements are allowed as scopes, then any statement (except the adaptor
            // expression statement) and switch cases are considered scopes.
            if ((o instanceof Statement && !(o instanceof ExpressionStatement))
                || o instanceof SwitchCase) {
              return allowStatementScoping;
            }

            return allowExpressionScoping && o instanceof MultiExpression;
          }
        });
  }


}
