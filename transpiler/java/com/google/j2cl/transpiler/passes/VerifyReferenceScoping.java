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

import com.google.common.collect.ImmutableSet;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CatchClause;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ForStatement;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.LabelReference;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.NameDeclaration;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Reference;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.TryStatement;
import com.google.j2cl.transpiler.ast.VariableReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/** Verifies that variables and labels are referenced within their scopes. */
public class VerifyReferenceScoping extends NormalizationPass {

  private static class Scope {
    Scope() {}

    Scope(Scope parent) {
      accessibleDeclarations.addAll(parent.accessibleDeclarations);
    }

    // Variables and labels that are accessible in a given scope. It is meant to include all the
    // variables and labels defined in parent scopes to make checking simpler.
    Set<NameDeclaration> accessibleDeclarations = new HashSet<>();
  }

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // Keep track of current statement for nicer error messages.
    Deque<Statement> statementStack = new ArrayDeque<>();
    // Keep track of scopes.
    Deque<Scope> scopeStack = new ArrayDeque<>(ImmutableSet.of(new Scope()));

    compilationUnit.accept(
        new AbstractVisitor() {

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
            final Node context =
                !statementStack.isEmpty()
                    ? statementStack.peek()
                    : getCurrentMember() != null ? getCurrentMember() : getCurrentType();
            checkState(
                getCurrentScope().accessibleDeclarations.contains(reference.getTarget()),
                "%s %s in %s not defined in enclosing scope.",
                reference.getTarget().getClass().getSimpleName(),
                reference.getTarget().getName(),
                context);
          }

          @Override
          public boolean enterNameDeclaration(NameDeclaration declaration) {
            // Check that the name is declared only once, and was not accidentally duplicated.
            checkState(
                getCurrentScope().accessibleDeclarations.add(declaration),
                "%s %s already in scope.",
                declaration.getClass().getSimpleName(),
                declaration.getName());
            return true;
          }

          @Override
          public boolean enterForStatement(ForStatement statement) {
            enterScopedStatement(statement);
            return true;
          }

          @Override
          public void exitForStatement(ForStatement statement) {
            exitScopedStatement(statement);
          }

          @Override
          public boolean enterLabeledStatement(LabeledStatement labeledStatement) {
            enterScope();
            return true;
          }

          @Override
          public void exitLabeledStatement(LabeledStatement labeledStatement) {
            exitScope();
          }

          @Override
          public boolean enterMethod(Method method) {
            enterScope();
            return true;
          }

          @Override
          public void exitMethod(Method method) {
            exitScope();
          }

          @Override
          public boolean enterCatchClause(CatchClause catchClause) {
            enterScope();
            return true;
          }

          @Override
          public void exitCatchClause(CatchClause catchClause) {
            exitScope();
          }

          @Override
          public boolean enterBlock(Block block) {
            enterScopedStatement(block);
            return true;
          }

          @Override
          public void exitBlock(Block block) {
            exitScopedStatement(block);
          }

          @Override
          public boolean enterFunctionExpression(FunctionExpression expression) {
            enterScope();
            return true;
          }

          @Override
          public void exitFunctionExpression(FunctionExpression expression) {
            exitScope();
          }

          @Override
          public boolean enterStatement(Statement statement) {
            statementStack.push(statement);
            return true;
          }

          @Override
          public void exitStatement(Statement statement) {
            checkState(statementStack.pop() == statement);
          }

          @Override
          public boolean enterTryStatement(TryStatement statement) {
            enterScopedStatement(statement);
            return true;
          }

          @Override
          public void exitTryStatement(TryStatement statement) {
            exitScopedStatement(statement);
          }

          private void enterScopedStatement(Statement statement) {
            enterScope();
            enterStatement(statement);
          }

          private void exitScopedStatement(Statement statement) {
            exitScope();
            exitStatement(statement);
          }

          private void enterScope() {
            // Some AST nodes define scopes, i.e. blocks, for statements, method definitions.
            scopeStack.push(new Scope(getCurrentScope()));
          }

          private void exitScope() {
            scopeStack.pop();
          }

          private Scope getCurrentScope() {
            return scopeStack.peek();
          }
        });
  }
}
