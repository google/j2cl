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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CatchClause;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.ForStatement;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchCase;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import com.google.j2cl.transpiler.ast.TryStatement;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Moves variable declarations to enclosing blocks.
 *
 * <p>Our AST treats variable declarations as expressions, and as such they can appear as
 * subexpressions. JavaScript only allows variable declarations as Statements and in some specific
 * contexts like for loops, etc.
 */
public class MoveVariableDeclarationsToEnclosingBlock extends NormalizationPass {
  private final boolean fromSwitchStatementsOnly;

  public MoveVariableDeclarationsToEnclosingBlock() {
    this(/* fromSwitchStatementsOnly= */ false);
  }

  public MoveVariableDeclarationsToEnclosingBlock(boolean fromSwitchStatementsOnly) {
    this.fromSwitchStatementsOnly = fromSwitchStatementsOnly;
  }

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    SetMultimap<Block, VariableDeclarationExpression> variableDeclarationsToRelocateByBlock =
        LinkedHashMultimap.create();

    // Variable declarations in JavaScript can only appear in a few constructs, namely as the top
    // level expression in a statement and in the first component of a for each loops.
    // This visitor collects all variable declarations that appear in constructs where it would be
    // illegal to have them in JavaScript.
    // TODO(b/216517933): Simplify using getParent()
    compilationUnit.accept(
        new AbstractVisitor() {
          Deque<Block> enclosingBlocks = new ArrayDeque<>();

          @Override
          public boolean enterBlock(Block block) {
            enclosingBlocks.push(block);
            return true;
          }

          /**
           * In Java, you may declare a variable within a switch statement branch and use it within
           * another branch. J2CL, by default, translates variable definitions to 'let' which are
           * not hoisted up in switch cases in js so we manually hoist them here. See the
           * switchstatement/Main#testSwitchVariableDeclarations test case.
           *
           * <p>We normalize variable definitions such that: <code>
           * switch value {
           *   case 1:
           *     let i = 5;
           *   case 2:
           *     i = 2; // i is undefined here.
           * }
           *  </code> Which becomes: <code>
           *  let i;
           *  switch value {
           *   case 1:
           *     i = 5;
           *   case 2:
           *     i = 2;
           *  }
           *  </code>
           */
          @Override
          public void exitSwitchStatement(SwitchStatement node) {
            for (SwitchCase switchCase : node.getCases()) {
              for (Statement statement : switchCase.getStatements()) {
                if (statement instanceof ExpressionStatement) {
                  ExpressionStatement expression = (ExpressionStatement) statement;
                  if (expression.getExpression() instanceof VariableDeclarationExpression) {
                    relocateToEnclosingBlock(
                        (VariableDeclarationExpression) expression.getExpression());
                  }
                }
              }
            }
          }

          @Override
          public void exitBlock(Block block) {
            checkState(block == enclosingBlocks.pop());
          }

          @Override
          public boolean enterExpressionStatement(ExpressionStatement expressionStatement) {
            Expression expression = expressionStatement.getExpression();
            if (expression instanceof VariableDeclarationExpression) {
              // Skip the top level declaration in a ExpressionStatement as it does not need to
              // be rewritten but do collect variables that need to be rewritten from the
              // right hand of the variable declaration.
              visitInitializers((VariableDeclarationExpression) expression);
              return false;
            }
            return true;
          }

          @Override
          public boolean enterForStatement(ForStatement forStatement) {
            for (Expression initializer : forStatement.getInitializers()) {
              if (initializer instanceof VariableDeclarationExpression) {
                // Skip the top level declaration in a for loop initializer as it does not need to
                // be rewritten but do collect variables that need to be rewritten from the
                // right hand of the variable declaration.
                visitInitializers((VariableDeclarationExpression) initializer);
              } else {
                initializer.accept(this);
              }
            }
            forStatement.getConditionExpression().accept(this);
            for (Expression update : forStatement.getUpdates()) {
              update.accept(this);
            }
            forStatement.getBody().accept(this);
            return false;
          }

          @Override
          public boolean enterTryStatement(TryStatement tryStatement) {
            for (VariableDeclarationExpression resourceDeclaration :
                tryStatement.getResourceDeclarations()) {
              // Skip the top level resource declaration as it does not need to
              // be rewritten.
              visitInitializers(resourceDeclaration);
            }
            tryStatement.getBody().accept(this);
            for (CatchClause catchClause : tryStatement.getCatchClauses()) {
              catchClause.accept(this);
            }
            Block finallyBlock = tryStatement.getFinallyBlock();
            if (finallyBlock != null) {
              finallyBlock.accept(this);
            }
            return false;
          }

          @Override
          public void exitVariableDeclarationExpression(
              VariableDeclarationExpression variableDeclarationExpression) {
            // In Kotlin backend, the only thing we need is moving variable declarations out of
            // switch statements which are explicitly handled in enterSwitchStatement.
            if (!fromSwitchStatementsOnly) {
              relocateToEnclosingBlock(variableDeclarationExpression);
            }
          }

          private void relocateToEnclosingBlock(
              VariableDeclarationExpression variableDeclarationExpression) {
            variableDeclarationsToRelocateByBlock.put(
                checkNotNull(enclosingBlocks.peek()), variableDeclarationExpression);
          }

          private void visitInitializers(
              VariableDeclarationExpression variableDeclarationExpression) {
            for (VariableDeclarationFragment variableDeclarationFragment :
                variableDeclarationExpression.getFragments()) {
              if (variableDeclarationFragment.getInitializer() != null) {
                // Collect variables defined in initializers.
                variableDeclarationFragment.getInitializer().accept(this);
              }
            }
          }
        });

    // Transform declarations into assignments
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteVariableDeclarationExpression(
              VariableDeclarationExpression variableDeclarationExpression) {
            if (!variableDeclarationsToRelocateByBlock
                .values()
                .contains(variableDeclarationExpression)) {
              return variableDeclarationExpression;
            }
            List<Expression> assignments =
                variableDeclarationExpression.getFragments().stream()
                    .filter(fragment -> fragment.getInitializer() != null)
                    .map(
                        fragment ->
                            BinaryExpression.Builder.asAssignmentTo(fragment.getVariable())
                                .setRightOperand(fragment.getInitializer())
                                .build())
                    .collect(toImmutableList());

            if (assignments.isEmpty()) {
              return TypeDescriptors.get().javaLangObject.getNullValue();
            }
            return MultiExpression.newBuilder().addExpressions(assignments).build();
          }
        });

    // Insert declarations into corresponding blocks.
    for (Block block : variableDeclarationsToRelocateByBlock.keySet()) {
      List<Variable> variablesToRelocate =
          variableDeclarationsToRelocateByBlock
              .get(block)
              .stream()
              .flatMap(declaration -> declaration.getFragments().stream())
              .map(VariableDeclarationFragment::getVariable)
              .collect(toImmutableList());

      block
          .getStatements()
          .add(
              0,
              VariableDeclarationExpression.newBuilder()
                  .addVariableDeclarations(variablesToRelocate)
                  .build()
                  .makeStatement(block.getSourcePosition()));
    }
  }
}
