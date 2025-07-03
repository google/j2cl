/*
 * Copyright 2025 Google Inc.
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.JsMemberType;
import com.google.j2cl.transpiler.ast.LocalFunctionDeclarationStatement;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.Visibility;
import java.util.HashMap;
import java.util.Map;

/**
 * Rewrites local function declarations to a variable assignment to a function expression and
 * rewrite the call accordingly.
 *
 * <p>For example:
 *
 * <pre>{@code
 * fun foo(): Int {
 *   fun local(a: Int): Int {
 *     return a + 1;
 *   }
 *   return local(1);
 * }
 * }</pre>
 *
 * <p>is rewritten to:
 *
 * <pre>{@code
 * fun foo(): Int {
 *   var local: $LocalFunctionJsFunction0 =  { a: Int -> a + 1; }
 *   return local.local(1);
 * }
 * }</pre>
 */
public class ConvertLocalFunctionDeclarationToFunctionExpressions extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {

    Map<MethodDescriptor, Variable> localFunctionVariableByMethodDescriptor = new HashMap<>();
    compilationUnit.accept(
        new AbstractRewriter() {
          // Keep a counter to make all the synthetic jsfunction types unique.
          private int jsFunctionTypeIndex = 0;

          @Override
          public Statement rewriteLocalFunctionDeclarationStatement(
              LocalFunctionDeclarationStatement localFunctionDeclarationStatement) {

            MethodDescriptor localFunctionMethodDescriptor =
                localFunctionDeclarationStatement.getDescriptor();

            TypeDescriptor jsFunctionTypeDescriptor =
                createJsFunctionTypeDeclaration(
                        localFunctionMethodDescriptor, jsFunctionTypeIndex++)
                    .toDescriptor();

            FunctionExpression functionExpression =
                FunctionExpression.newBuilder()
                    .setParameters(localFunctionDeclarationStatement.getParameters())
                    .setStatements(localFunctionDeclarationStatement.getBody().getStatements())
                    .setTypeDescriptor(jsFunctionTypeDescriptor)
                    .setSourcePosition(localFunctionDeclarationStatement.getSourcePosition())
                    .setJsAsync(localFunctionMethodDescriptor.getJsInfo().isJsAsync())
                    .build();

            Variable variable =
                Variable.newBuilder()
                    .setName(localFunctionMethodDescriptor.getName())
                    .setTypeDescriptor(jsFunctionTypeDescriptor)
                    .setFinal(true)
                    .build();

            localFunctionVariableByMethodDescriptor.put(
                localFunctionMethodDescriptor.getDeclarationDescriptor(), variable);

            return VariableDeclarationExpression.newBuilder()
                .addVariableDeclaration(variable, functionExpression)
                .build()
                .makeStatement(localFunctionDeclarationStatement.getSourcePosition());
          }
        });

    compilationUnit.accept(
        new AbstractRewriter() {

          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor target = methodCall.getTarget();
            if (!target.isLocalFunction()) {
              return methodCall;
            }

            Variable variable =
                checkNotNull(
                    localFunctionVariableByMethodDescriptor.get(target.getDeclarationDescriptor()));

            MethodDescriptor newTarget =
                checkNotNull(
                    ((DeclaredTypeDescriptor) variable.getTypeDescriptor())
                        .getJsFunctionMethodDescriptor());

            return MethodCall.Builder.from(methodCall)
                .setQualifier(variable.createReference())
                .setTarget(newTarget)
                .build();
          }
        });
  }

  private static TypeDeclaration createJsFunctionTypeDeclaration(
      MethodDescriptor localFunctionMethodDescriptor, int jsFunctionTypeIndex) {
    TypeDeclaration enclosingTypeDeclaration =
        localFunctionMethodDescriptor.getEnclosingTypeDescriptor().getTypeDeclaration();

    String typeName = "$LocalFunctionJsFunction" + jsFunctionTypeIndex;

    var classComponents = enclosingTypeDeclaration.synthesizeInnerClassComponents(typeName);

    return TypeDeclaration.newBuilder()
        .setEnclosingTypeDeclaration(enclosingTypeDeclaration)
        .setTypeParameterDescriptors(
            localFunctionMethodDescriptor.getTypeParameterTypeDescriptors())
        .setClassComponents(classComponents)
        .setJsFunctionInterface(true)
        .setFunctionalInterface(true)
        .setDeclaredMethodDescriptorsFactory(
            td ->
                ImmutableList.of(
                    createJsFunctionMethodDescriptor(
                        td.toDescriptor(), localFunctionMethodDescriptor)))
        .setSingleAbstractMethodDescriptorFactory(
            td -> Iterables.getOnlyElement(td.getDeclaredMethodDescriptors()))
        .setVisibility(Visibility.PUBLIC)
        .setKind(Kind.INTERFACE)
        .build();
  }

  private static MethodDescriptor createJsFunctionMethodDescriptor(
      DeclaredTypeDescriptor jsfunctionTypeDescriptor, MethodDescriptor singleAbstractMethod) {
    return MethodDescriptor.Builder.from(singleAbstractMethod)
        .setDeclarationDescriptor(null)
        .setEnclosingTypeDescriptor(jsfunctionTypeDescriptor)
        .setEnclosingMethodDescriptor(null)
        .setVisibility(Visibility.PUBLIC)
        .setOriginalJsInfo(
            JsInfo.newBuilder()
                .setJsMemberType(JsMemberType.NONE)
                .setJsAsync(singleAbstractMethod.getJsInfo().isJsAsync())
                .build())
        .setNative(false)
        .build();
  }
}
