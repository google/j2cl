/*
 * Copyright 2022 Google Inc.
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

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.j2cl.transpiler.ast.AstUtils.replaceDeclarations;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CatchClause;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodLike;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Normalize non-final and vararg variables for Kotlin.
 *
 * <p>Non-final method parameters and catch clause exception variable are re-declared as final,
 * since they are final in Kotlin.
 *
 * <p>Vararg parameters with non-primitive component type are re-declared from {@code Array<out T>}
 * to {@code Array<T>}.
 */
public class NormalizeMethodParametersJ2kt extends NormalizationPass {

  private static class RewriteItem {
    RewriteItem(Variable variable, TypeDescriptor rewrittenTypeDescriptor) {
      this.variable = variable;
      this.rewrittenTypeDescriptor = rewrittenTypeDescriptor;
    }

    RewriteItem(Variable variable) {
      this(variable, variable.getTypeDescriptor());
    }

    final Variable variable;
    final TypeDescriptor rewrittenTypeDescriptor;
  }

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethod(Method method) {
            // skip methods implemented in Javascript
            if (method.getDescriptor().isNative()
                || (method.getDescriptor().isJsConstructor()
                    && method.getDescriptor().getEnclosingTypeDescriptor().isNative())) {
              return method;
            }

            // Redeclare vararg parameter if needed.
            RewriteItem varargRewriteItem = getVarargRewriteItem(method);
            if (varargRewriteItem != null) {
              method.setBody(redeclareItems(method.getBody(), ImmutableList.of(varargRewriteItem)));
            }

            // Redeclare non-final parameters.
            method.setBody(redeclareItems(method.getBody(), getNonFinalRewriteItems(method)));

            return method;
          }

          @Override
          public Node rewriteFunctionExpression(FunctionExpression functionExpression) {
            // Redeclare vararg parameter if needed.
            RewriteItem varargRewriteItem = getVarargRewriteItem(functionExpression);
            if (varargRewriteItem != null) {
              functionExpression =
                  FunctionExpression.Builder.from(functionExpression)
                      .setStatements(
                          redeclareItems(
                                  functionExpression.getBody(), ImmutableList.of(varargRewriteItem))
                              .getStatements())
                      .build();
            }

            // Redeclare non-final parameters.
            return FunctionExpression.Builder.from(functionExpression)
                .setStatements(
                    redeclareItems(
                            functionExpression.getBody(),
                            getNonFinalRewriteItems(functionExpression))
                        .getStatements())
                .build();
          }

          @Override
          public Node rewriteCatchClause(CatchClause catchClause) {
            Variable exceptionVariable = catchClause.getExceptionVariable();
            if (exceptionVariable.isFinal()) {
              return catchClause;
            }

            return CatchClause.Builder.from(catchClause)
                .setBody(
                    redeclareItems(
                        catchClause.getBody(),
                        ImmutableList.of(new RewriteItem(exceptionVariable))))
                .build();
          }

          private Block redeclareItems(Block block, List<RewriteItem> rewriteItems) {
            if (rewriteItems.isEmpty()) {
              return block;
            }

            ImmutableList<Variable> variablesToRewrite =
                rewriteItems.stream()
                    .map(rewriteItem -> rewriteItem.variable)
                    .collect(toImmutableList());

            ImmutableList<VariableDeclarationFragment> variableDeclarationFragments =
                rewriteItems.stream()
                    .map(
                        rewriteItem ->
                            VariableDeclarationFragment.newBuilder()
                                .setVariable(
                                    Variable.Builder.from(rewriteItem.variable)
                                        .setTypeDescriptor(rewriteItem.rewrittenTypeDescriptor)
                                        .build())
                                .setInitializer(rewriteItem.variable.createReference())
                                .build())
                    .collect(toImmutableList());

            variablesToRewrite.forEach(variable -> variable.setFinal(true));

            ImmutableList<Variable> replacementVariables =
                variableDeclarationFragments.stream()
                    .map(VariableDeclarationFragment::getVariable)
                    .collect(toImmutableList());

            List<Statement> statements = block.getStatements();

            Statement constructorInvocationStatement =
                statements.stream()
                    .filter(AstUtils::isConstructorInvocationStatement)
                    .findFirst()
                    .orElse(null);

            int constructorInvocationIndex =
                constructorInvocationStatement == null
                    ? -1
                    : statements.indexOf(constructorInvocationStatement);

            List<Statement> statementsUntilConstructorInvocation =
                constructorInvocationIndex != -1
                    ? statements.subList(0, constructorInvocationIndex + 1)
                    : ImmutableList.of();

            ImmutableList<Statement> variableDeclarationStatements =
                variableDeclarationFragments.stream()
                    .map(
                        fragment ->
                            VariableDeclarationExpression.newBuilder()
                                .setVariableDeclarationFragments(ImmutableList.of(fragment))
                                .build()
                                .makeStatement(fragment.getVariable().getSourcePosition()))
                    .collect(toImmutableList());

            List<Statement> statementsAfterConstructorInvocation =
                constructorInvocationIndex != -1
                    ? statements.subList(constructorInvocationIndex + 1, statements.size())
                    : statements;

            ImmutableList<Statement> rewrittenStatementsAfterConstructorInvocation =
                statementsAfterConstructorInvocation.stream()
                    .map(s -> replaceDeclarations(variablesToRewrite, replacementVariables, s))
                    .collect(toImmutableList());

            return Block.newBuilder()
                .addStatements(statementsUntilConstructorInvocation)
                .addStatements(variableDeclarationStatements)
                .addStatements(rewrittenStatementsAfterConstructorInvocation)
                .setSourcePosition(block.getSourcePosition())
                .build();
          }
        });
  }

  private ImmutableList<RewriteItem> getNonFinalRewriteItems(MethodLike methodLike) {
    return methodLike.getParameters().stream()
        .filter(not(Variable::isFinal))
        .map(RewriteItem::new)
        .collect(toImmutableList());
  }

  @Nullable
  private static RewriteItem getVarargRewriteItem(MethodLike methodLike) {
    MethodDescriptor methodDescriptor = methodLike.getDescriptor();
    if (!methodDescriptor.isVarargs()) {
      return null;
    }

    Variable varargVariable = Iterables.getLast(methodLike.getParameters());
    ArrayTypeDescriptor arrayTypeDescriptor =
        (ArrayTypeDescriptor) varargVariable.getTypeDescriptor();
    TypeDescriptor componentTypeDescriptor = arrayTypeDescriptor.getComponentTypeDescriptor();
    if (componentTypeDescriptor.isPrimitive()) {
      return null;
    }

    // At this point, component type descriptor is assumed to be "out X".
    TypeDescriptor rewrittenTypeDescriptor =
        ArrayTypeDescriptor.Builder.from(arrayTypeDescriptor)
            .setComponentTypeDescriptor(
                ((TypeVariable) componentTypeDescriptor).getUpperBoundTypeDescriptor())
            .build();

    return new RewriteItem(varargVariable, rewrittenTypeDescriptor);
  }
}
