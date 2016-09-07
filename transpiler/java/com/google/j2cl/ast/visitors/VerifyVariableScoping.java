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
package com.google.j2cl.ast.visitors;

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CatchClause;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.ForStatement;
import com.google.j2cl.ast.FunctionExpression;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.TryStatement;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableReference;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/** Verifies that variables are referenced within their scopes. */
public class VerifyVariableScoping {

  public static void applyTo(CompilationUnit compilationUnit) {

    class Scope {
      Scope() {}

      Scope(Scope parent) {
        accessibleVariables.addAll(parent.accessibleVariables);
      }

      // Variables that are accessible in a given scope. It is meant to include all the
      // variables defined in parent scopes to make checking simpler.
      Set<Variable> accessibleVariables = new HashSet<>();
    }

    compilationUnit.accept(
        new AbstractVisitor() {
          // Keep track of current statement for nicer error messages.
          Deque<Statement> statementStack = new ArrayDeque<>();
          // Keep track of scopes.
          Deque<Scope> scopeStack = new ArrayDeque<>(Collections.singleton(new Scope()));

          @Override
          public boolean enterVariableReference(VariableReference variableReference) {
            // Verify that the VariableReference references a variable that is in scope.
            final Node context =
                !statementStack.isEmpty()
                    ? statementStack.peek()
                    : getCurrentMember() != null ? getCurrentMember() : getCurrentType();
            checkState(
                getCurrentScope().accessibleVariables.contains(variableReference.getTarget())
                    // Raw variables are not defined in any scope.
                    || variableReference.getTarget().isRaw(),
                "%s in %s not defined in enclosing scope.",
                variableReference.getTarget(),
                context);
            return false;
          }

          @Override
          public boolean enterVariable(Variable variable) {
            // Check that the variable is defined only once.
            checkState(
                getCurrentScope().accessibleVariables.add(variable),
                "Variable %s already in scope.",
                variable.getName());
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
