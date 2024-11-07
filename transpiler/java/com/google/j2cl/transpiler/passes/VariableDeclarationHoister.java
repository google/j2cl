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
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.Math.min;
import static java.util.Collections.reverse;
import static java.util.stream.Collectors.toCollection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import com.google.j2cl.transpiler.ast.VariableReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Moves variable declarations to enclosing blocks to restore proper scoping.
 *
 * <p>Scoping rules are different in the source language and the languages targeted by the different
 * backends. This pass moves to an enclosing block variable declarations that in the target language
 * would have out of scope references or are defined in places that are not valid.
 */
public class VariableDeclarationHoister extends NormalizationPass {
  // The idea is to collect the definition scopes for all variable declarations, and check that the
  // references are all within the scope. If a variable is accessed out of scope, or declared in a
  // scope that is not supported by the target language, the variable is removed from the tracking
  // map; the absence of a variable from the map denotes that it needs to be hoisted.
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

            variableDeclarationExpression.getFragments().stream()
                .map(VariableDeclarationFragment::getVariable)
                .forEach(v -> enclosingScopesByVariable.put(v, enclosingScopes));
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

    // Keeps track of the block that will have the declarations for the variables that need
    // to be hoisted.
    SetMultimap<Block, Variable> variableByTargetScopeBlock = LinkedHashMultimap.create();
    Map<Block, Integer> declarationPointByTargetScopeBlock = new HashMap<>();

    // Transform declarations into assignments, so that the effects of the expressions are kept
    // in the original location.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteVariableDeclarationExpression(
              VariableDeclarationExpression variableDeclarationExpression) {
            // Get any of the variables in the declaration; the declaration, with all its variables,
            // is moved as a unit.
            Variable variable =
                Iterables.getLast(variableDeclarationExpression.getFragments()).getVariable();

            List<Object> enclosingScopes = enclosingScopesByVariable.get(variable);

            Object declarationScope = getParent(VariableDeclarationHoister::isScopeNode);
            if (isValidScopeNode(declarationScope)
                && Iterables.getLast(enclosingScopes) == declarationScope) {
              // All variables declared here are only accessed with in their scope and are declared
              // in a valid context.
              return variableDeclarationExpression;
            }

            // Get the block where the variable declarations will be moved to.
            Block block = getLastBlock(enclosingScopes);

            // Get the full list of parents for the declaration (these are innermost first). Find
            // the node immediately inside the target block to determine the latest valid insertion
            // point for the declaration.
            ImmutableList<Object> declarationSiteParents = getParents().collect(toImmutableList());
            Object statementContainingDeclaration =
                declarationSiteParents.get(declarationSiteParents.indexOf(block) - 1);

            // The variable can be inserted before statement that contains the current declaration.
            int insertionPoint = block.getStatements().indexOf(statementContainingDeclaration);

            // Since all the variables that are moved to a block are declared together, the
            // insertion point will be the closest to the top of the block.
            declarationPointByTargetScopeBlock.merge(
                block,
                insertionPoint,
                (Integer lastInsertionPoint, Integer newValue) ->
                    min(newValue, lastInsertionPoint.intValue()));

            // Collect the variables that need to be moved and the target block.
            variableDeclarationExpression.getFragments().stream()
                .map(VariableDeclarationFragment::getVariable)
                .forEach(v -> variableByTargetScopeBlock.put(checkNotNull(block), v));

            // Replace the declarations by assignments.
            ImmutableList<Expression> assignments =
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

    // Insert declarations into corresponding blocks, just before where the first moved declaration
    // was.
    for (Block block : variableByTargetScopeBlock.keySet()) {
      Set<Variable> variablesToRelocate = variableByTargetScopeBlock.get(block);
      int insertionPointInBlock = declarationPointByTargetScopeBlock.get(block);

      // Make the variables nullable since the declaration will assign the default value.
      variablesToRelocate.forEach(v -> v.setTypeDescriptor(v.getTypeDescriptor().toNullable()));

      block
          .getStatements()
          .add(
              // Note: since we insert all the variables in a single declaration there is no need
              // to adjust insertion points.
              insertionPointInBlock,
              VariableDeclarationExpression.newBuilder()
                  .addVariableDeclarations(variablesToRelocate)
                  .build()
                  .makeStatement(block.getSourcePosition()));
    }
  }

  /** Returns the Block that is closer to the end of the list. */
  private static Block getLastBlock(List<Object> enclosingScopes) {
    // Find the deeper enclosing block to declare the variable there.
    return Streams.findLast(
            enclosingScopes.stream().filter(Block.class::isInstance).map(Block.class::cast))
        .get();
  }

  /** Trims {@code toTrim} to only contain the common prefix. */
  private static void trimToCommonPrefix(List<Object> toTrim, List<Object> other) {
    int i = 0;
    while (i < other.size() && i < toTrim.size() && other.get(i) == toTrim.get(i)) {
      i++;
    }

    if (toTrim.size() == i) {
      // toTrim is already the common prefix.
      return;
    }
    for (int j = toTrim.size() - 1; j >= i; j--) {
      toTrim.remove(j);
    }
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
}
