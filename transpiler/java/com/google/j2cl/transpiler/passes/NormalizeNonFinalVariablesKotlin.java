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
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CatchClause;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import java.util.List;

/**
 * Normalize non-final variables for Kotlin.
 *
 * <p>Non-final method parameters and catch clause exception variable are re-declared as final,
 * since they are final in Kotlin.
 */
public class NormalizeNonFinalVariablesKotlin extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethod(Method method) {
            method.setBody(
                redeclareNonFinalVariables(
                    method.getBody(), method.getParameters(), method.getSourcePosition()));
            return method;
          }

          @Override
          public Node rewriteFunctionExpression(FunctionExpression functionExpression) {
            return FunctionExpression.Builder.from(functionExpression)
                .setStatements(
                    redeclareNonFinalVariables(
                            functionExpression.getBody(),
                            functionExpression.getParameters(),
                            functionExpression.getSourcePosition())
                        .getStatements())
                .build();
          }

          @Override
          public Node rewriteCatchClause(CatchClause catchClause) {
            return CatchClause.Builder.from(catchClause)
                .setBody(
                    redeclareNonFinalVariables(
                        catchClause.getBody(),
                        ImmutableList.of(catchClause.getExceptionVariable()),
                        catchClause.getBody().getSourcePosition()))
                .build();
          }

          private Block redeclareNonFinalVariables(
              Block block, List<Variable> variables, SourcePosition sourcePosition) {
            ImmutableList<Variable> variablesToReplace =
                variables.stream().filter(not(Variable::isFinal)).collect(toImmutableList());

            if (variablesToReplace.isEmpty()) {
              return block;
            }

            ImmutableList<VariableDeclarationFragment> variableDeclarationFragments =
                variablesToReplace.stream()
                    .map(
                        variable ->
                            VariableDeclarationFragment.newBuilder()
                                .setVariable(Variable.Builder.from(variable).build())
                                .setInitializer(variable.createReference())
                                .build())
                    .collect(toImmutableList());

            variablesToReplace.forEach(variable -> variable.setFinal(true));

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
                                .makeStatement(sourcePosition))
                    .collect(toImmutableList());

            List<Statement> statementsAfterConstructorInvocation =
                constructorInvocationIndex != -1
                    ? statements.subList(constructorInvocationIndex + 1, statements.size() - 1)
                    : statements;

            ImmutableList<Statement> rewrittenStatementsAfterConstructorInvocation =
                statementsAfterConstructorInvocation.stream()
                    .map(s -> replaceDeclarations(variablesToReplace, replacementVariables, s))
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
}
