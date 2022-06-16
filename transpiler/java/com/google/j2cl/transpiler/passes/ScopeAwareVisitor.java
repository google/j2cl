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

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CatchClause;
import com.google.j2cl.transpiler.ast.ForStatement;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.TryStatement;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/** An abstract class for traversals that need awareness of JavaScript scoping rules. */
public class ScopeAwareVisitor<T> extends AbstractVisitor {

  private static class Scope<T> {
    private Scope() {
      parent = null;
    }

    private Scope(Scope<T> parent) {
      this.parent = parent;
    }

    // Elements that are declared in the current scope.
    private final Set<T> elementsDeclaredInScope = new HashSet<>();
    private final Scope<T> parent;

    public boolean addElementToScope(T element) {
      // Check if the element is already in a parent scope to be able to detect when duplicates are
      // added.
      if (isElementInScope(element)) {
        return false;
      }
      return elementsDeclaredInScope.add(element);
    }

    public boolean isElementInScope(T element) {
      return elementsDeclaredInScope.contains(element)
          || (parent != null && parent.isElementInScope(element));
    }
  }

  @Override
  public final boolean enterForStatement(ForStatement statement) {
    enterScope();
    return true;
  }

  @Override
  public final void exitForStatement(ForStatement statement) {
    exitScope();
  }

  @Override
  public final boolean enterLabeledStatement(LabeledStatement labeledStatement) {
    enterScope();
    return true;
  }

  @Override
  public final void exitLabeledStatement(LabeledStatement labeledStatement) {
    exitScope();
  }

  @Override
  public final boolean enterMethod(Method method) {
    enterScope();
    return true;
  }

  @Override
  public final void exitMethod(Method method) {
    exitScope();
  }

  @Override
  public final boolean enterCatchClause(CatchClause catchClause) {
    enterScope();
    return true;
  }

  @Override
  public final void exitCatchClause(CatchClause catchClause) {
    exitScope();
  }

  @Override
  public final boolean enterBlock(Block block) {
    enterScope();
    return true;
  }

  @Override
  public final void exitBlock(Block block) {
    exitScope();
  }

  @Override
  public final boolean enterFunctionExpression(FunctionExpression expression) {
    enterScope();
    return true;
  }

  @Override
  public final void exitFunctionExpression(FunctionExpression expression) {
    exitScope();
  }

  @Override
  public final boolean enterTryStatement(TryStatement statement) {
    enterScope();
    return true;
  }

  @Override
  public final void exitTryStatement(TryStatement statement) {
    exitScope();
  }

  private final Deque<Scope<T>> scopeStack = new ArrayDeque<>(ImmutableList.of(new Scope<>()));

  private void enterScope() {
    scopeStack.push(new Scope<T>(scopeStack.peek()));
  }

  private void exitScope() {
    scopeStack.pop();
  }

  public boolean isElementInScope(T element) {
    return scopeStack.peek().isElementInScope(element);
  }

  public boolean addElementsToScope(T element) {
    return scopeStack.peek().addElementToScope(element);
  }
}
