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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.Math.min;
import static java.util.Collections.reverse;
import static java.util.stream.Collectors.toCollection;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Moves variable declarations to enclosing blocks to restore proper scoping.
 *
 * <p>Scoping rules are different in the source language and the languages targeted by the different
 * backends. This pass moves to an enclosing block variable declarations that in the target language
 * would have out of scope references or are defined in places that are not valid.
 */
public class VariableDeclarationHoister extends NormalizationPass {
  // The idea is to collect, for each variable, the scopes that enclose its declaration and all its
  // references. If a variable is accessed out of the scope of its declaration, or declared in a
  // scope that is not supported by the target language, its declaration is moved to the innermost
  // enclosing scope that can hold declarations: a block or, if declarations are allowed in
  // expressions, a multi-expression.
  //
  // To simplify the implementation declarations of multiple variables are moved all together to
  // the right scope.

  // Whether to hoist variable declarations in expressions or not.
  private final boolean allowDeclarationsInExpressions;

  public VariableDeclarationHoister(boolean allowDeclarationsInExpressions) {
    this.allowDeclarationsInExpressions = allowDeclarationsInExpressions;
  }

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    if (allowDeclarationsInExpressions) {
      // Arguments of this() and super() calls and field initializers have no valid enclosing block
      // to hoist declarations to, provide multi-expression scopes for them.
      createPlaceHolderScopes(compilationUnit);
    }

    // Keeps track of whether all references to the variable and the outermost scope where a
    // variable is accessed if not in scope of its definition.
    Map<Variable, List<Object>> enclosingScopesByVariable = new HashMap<>();

    // Compute scopes for variable declarations and whether the variables are accessed within their
    // scopes.
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitVariableDeclarationExpression(
              VariableDeclarationExpression variableDeclarationExpression) {

            List<Object> enclosingScopes = collectEnclosingScopes();
            enclosingScopesByVariable.put(
                variableDeclarationExpression.getVariable(), enclosingScopes);
          }

          @Override
          public void exitVariableReference(VariableReference variableReference) {
            Variable variable = variableReference.getTarget();
            List<Object> enclosingScopes = enclosingScopesByVariable.get(variable);
            if (enclosingScopes == null) {
              // This is a reference to a method or lambda parameter. Ignore.
              return;
            }

            List<Object> parents = collectEnclosingScopes();

            // Find the common ancestors in scope. Note that all the variables that have the same
            // declaration share this list since they all need to be moved together.
            trimToCommonPrefix(enclosingScopes, parents);
          }

          /** Collects the enclosing scopes with outermost scope appearing first. */
          private List<Object> collectEnclosingScopes() {
            var parentScopes =
                getParents()
                    .filter(VariableDeclarationHoister::isScopeNode)
                    .collect(toCollection(ArrayList::new));
            reverse(parentScopes);
            return parentScopes;
          }
        });

    // Keeps track of the scope (block or multi-expression) that will have the declarations for the
    // variables that need to be hoisted.
    SetMultimap<Scope<?>, Variable> variableByTargetScope = LinkedHashMultimap.create();
    Map<Scope<?>, Integer> declarationPointByTargetScope = new HashMap<>();

    // Transform declarations into assignments, so that the effects of the expressions are kept
    // in the original location.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteVariableDeclarationExpression(
              VariableDeclarationExpression variableDeclarationExpression) {
            Variable variable = variableDeclarationExpression.getVariable();

            List<Object> enclosingScopes = enclosingScopesByVariable.get(variable);

            Object declarationScope = getParent(VariableDeclarationHoister::isScopeNode);
            if (isValidScopeNode(declarationScope)
                && Iterables.getLast(enclosingScopes) == declarationScope) {
              // Variable declared here is only accessed within its scope and is declared
              // in a valid context.
              return variableDeclarationExpression;
            }

            // Get the scope where the variable declarations will be moved to.
            Scope<?> scope = getTargetScope(enclosingScopes);

            // The variable can be inserted before the element of the scope that contains the
            // current declaration.
            int insertionPoint =
                scope.indexOfElementContaining(getParents().collect(toImmutableList()));

            // Since all the variables that are moved to a scope are declared together, the
            // insertion point will be the closest to the top of the scope.
            declarationPointByTargetScope.merge(
                scope,
                insertionPoint,
                (Integer lastInsertionPoint, Integer newValue) ->
                    min(newValue, lastInsertionPoint.intValue()));

            // Collect the variables that need to be moved and the target scope.
            variableByTargetScope.put(scope, variable);

            // Replace the declaration by assignment if initialized.
            if (variableDeclarationExpression.getInitializer() != null) {
              return variable.infixAssign(variableDeclarationExpression.getInitializer());
            }
            return TypeDescriptors.get().javaLangObject.getNullValue();
          }
        });

    // Insert declarations into corresponding scopes, just before where the first moved declaration
    // was.
    variableByTargetScope
        .asMap()
        .forEach((scope, variablesToRelocate) -> {
          // Make the variables nullable since the declaration will assign the default value.
          variablesToRelocate.forEach(v -> v.setTypeDescriptor(v.getTypeDescriptor().toNullable()));

          scope.insertDeclarations(
              declarationPointByTargetScope.get(scope),
              variablesToRelocate.stream()
                  .map(v -> VariableDeclarationExpression.builder().setVariable(v).build())
                  .collect(toImmutableList()));
        });
  }

  /** A scope where hoisted variable declarations can be inserted. */
  private sealed interface Scope<T extends Node> {
    /** Returns the node that defines the scope. */
    Node node();

    /** Returns the elements of the scope, where the declarations are inserted. */
    List<T> elements();

    /** Returns the element of the scope that holds {@code declaration}. */
    T toElement(VariableDeclarationExpression declaration);

    /**
     * Returns the index of the element of the scope that contains the node whose parents, innermost
     * first, are {@code parents}.
     */
    default int indexOfElementContaining(List<Object> parents) {
      return elements().indexOf(parents.get(parents.indexOf(node()) - 1));
    }

    /** Inserts {@code declarations} at {@code index}. */
    default void insertDeclarations(int index, List<VariableDeclarationExpression> declarations) {
      elements()
          .addAll(index, declarations.stream().map(this::toElement).collect(toImmutableList()));
    }
  }

  /** A block scope, where declarations are inserted as statements. */
  private record BlockScope(Block node) implements Scope<Statement> {
    @Override
    public List<Statement> elements() {
      return node.getStatements();
    }

    @Override
    public Statement toElement(VariableDeclarationExpression declaration) {
      return declaration.makeStatement(node.getSourcePosition());
    }
  }

  /** A multi-expression scope, where declarations are inserted as expressions. */
  private record MultiExpressionScope(MultiExpression node) implements Scope<Expression> {
    @Override
    public List<Expression> elements() {
      return node.getExpressions();
    }

    @Override
    public Expression toElement(VariableDeclarationExpression declaration) {
      return declaration;
    }
  }

  /**
   * Encloses the arguments of {@code this()} and {@code super()} calls and the field initializers
   * in a multi-expression, to provide a scope to hoist declarations to.
   *
   * <p>In Kotlin, declarations in these expressions cannot be hoisted to an enclosing block, since
   * {@code this()} and {@code super()} calls are emitted in the constructor header, outside of the
   * constructor body, and field initializers are not inside any block.
   */
  private static void createPlaceHolderScopes(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteField(Field field) {
            if (!field.hasInitializer()) {
              return field;
            }
            return field.toBuilder().setInitializer(createScope(field.getInitializer())).build();
          }

          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            if (!methodCall.getTarget().isConstructor()) {
              return methodCall;
            }
            return methodCall.toBuilder()
                .setArguments(
                    methodCall.getArguments().stream()
                        .map(this::createScope)
                        .collect(toImmutableList()))
                .build();
          }

          private Expression createScope(Expression expression) {
            // The multi-expression is the scope for the hoisted declarations. The null placeholder
            // is needed since a multi-expression with a single expression collapses into it; it has
            // no side effects, so NormalizeMultiExpressions removes it, together with the
            // multi-expression if no declarations were hoisted into it.
            return MultiExpression.builder()
                .addExpressions(TypeDescriptors.get().javaLangObject.getNullValue(), expression)
                .build();
          }
        });
  }

  /**
   * Returns the innermost scope in {@code enclosingScopes} where declarations can be hoisted to,
   * i.e. a block or, if declarations are allowed in expressions, a multi-expression.
   */
  private Scope<?> getTargetScope(List<Object> enclosingScopes) {
    var node = Streams.findLast(enclosingScopes.stream().filter(this::isValidTargetScope)).get();
    return switch (node) {
      case Block block -> new BlockScope(block);
      case MultiExpression multiExpression -> new MultiExpressionScope(multiExpression);
      default -> throw new IllegalStateException();
    };
  }

  /**
   * Returns true for nodes that can define a scope.
   *
   * <p>In our model, the parent of a declaration is normally considered to be its scope but since
   * variable declarations are expressions, we need to exclude ExpressionStatement, which is just an
   * adapter and consider the parent of the expression statement to be the scope for the variable.
   */
  private static boolean isScopeNode(Object node) {
    return !(node instanceof ExpressionStatement);
  }

  private boolean isValidScopeNode(Object scope) {
    if (allowDeclarationsInExpressions) {
      return true;
    }
    return !(scope instanceof Expression);
  }

  /** Returns true if hoisted declarations can be inserted into {@code node}. */
  private boolean isValidTargetScope(Object node) {
    return node instanceof Block
        || (allowDeclarationsInExpressions && node instanceof MultiExpression);
  }

  /** Trims {@code toTrim} to only contain the common prefix. */
  private static void trimToCommonPrefix(List<Object> toTrim, List<Object> other) {
    int i = 0;
    while (i < other.size() && i < toTrim.size() && other.get(i) == toTrim.get(i)) {
      i++;
    }

    if (toTrim.size() > i) {
      toTrim.subList(i, toTrim.size()).clear();
    }
  }
}
