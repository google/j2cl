/*
 * Copyright 2024 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.passes;

import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveVoid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.ThrowStatement;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import java.util.NoSuchElementException;

/**
 * Inserts {@code throw new AssertionError("Unreachable")} statements which are necessary for
 * translated Kotlin to compile.
 *
 * <p>An example of that is a do-while loop used inside lambda. The following Java code:
 *
 * <pre>{@code
 * Supplier<String> supplier =
 *     () -> {
 *       do {
 *         return "foo";
 *       } while (false);
 *     };
 *
 * }</pre>
 *
 * is translated to Kotlin as:
 *
 * <pre>{@code
 * val supplier = Supplier<String> {
 *   do {
 *     return@Supplier "foo"
 *   } while (false)
 *   throw UnreachableStatementException()
 * }
 * }</pre>
 *
 * <p>The Kotlin code would not compile in Kotlin without throw statement, with the following error:
 * "Argument type mismatch: actual type is 'kotlin.Unit', but 'kotlin.String' was expected.".
 *
 * <p>Kotlin bug: https://youtrack.jetbrains.com/issue/KT-70505
 */
public class InsertUnreachableAssertionErrors extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteFunctionExpression(FunctionExpression functionExpression) {
            if (isPrimitiveVoid(functionExpression.getDescriptor().getReturnTypeDescriptor())) {
              return functionExpression;
            }

            Statement lastStatement;
            try {
              lastStatement = Iterables.getLast(functionExpression.getBody().getStatements());
            } catch (NoSuchElementException e) {
              return functionExpression;
            }

            if (lastStatement instanceof ReturnStatement
                || lastStatement instanceof ThrowStatement) {
              return functionExpression;
            }

            MethodDescriptor errorConstructor =
                TypeDescriptors.get()
                    .javaLangAssertionError
                    .getMethodDescriptor("<init>", TypeDescriptors.get().javaLangObject);

            Statement throwStatement =
                ThrowStatement.newBuilder()
                    .setExpression(
                        NewInstance.newBuilder()
                            .setTarget(errorConstructor)
                            .setArguments(new StringLiteral("Unreachable"))
                            .build())
                    .setSourcePosition(functionExpression.getSourcePosition())
                    .build();

            return FunctionExpression.Builder.from(functionExpression)
                .setStatements(
                    new ImmutableList.Builder<Statement>()
                        .addAll(functionExpression.getBody().getStatements())
                        .add(throwStatement)
                        .build())
                .build();
          }
        });
  }
}
