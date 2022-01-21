/*
 * Copyright 2021 Google Inc.
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

import com.google.common.collect.ImmutableMap;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayAccess;
import com.google.j2cl.transpiler.ast.ArrayLength;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.ForEachStatement;
import com.google.j2cl.transpiler.ast.ForStatement;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PostfixExpression;
import com.google.j2cl.transpiler.ast.PostfixOperator;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;

/** Replaces for each statements with for statements. */
public class NormalizeForEachStatement extends NormalizationPass {

  private final boolean useDoubleForIndexVariable;

  public NormalizeForEachStatement(boolean useDoubleForIndexVariable) {
    this.useDoubleForIndexVariable = useDoubleForIndexVariable;
  }

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Statement rewriteForEachStatement(ForEachStatement forEachStatement) {
            TypeDescriptor typeDescriptor =
                forEachStatement.getIterableExpression().getTypeDescriptor();
            if (typeDescriptor.toRawTypeDescriptor().isArray()) {
              return convertForEachArray(forEachStatement);
            } else {
              return convertForEachIterable(forEachStatement);
            }
          }
        });
  }

  /**
   * Converts a for each statement on an array into a regular for loop. E.g.
   *
   * <p>
   *
   * <pre>{@code
   * for(T v : exp) {
   *   S;
   * }
   * }</pre>
   *
   * <p>into
   *
   * <p>
   *
   * <pre>{@code
   *  for(T[] $array = (exp), int $index = 0; $index < $array.length; $index++ ) {
   *    T v = (T) $array[$index];
   *    S;
   * }
   * }</pre>
   */
  private ForStatement convertForEachArray(ForEachStatement forEachStatement) {
    Variable loopVariable = forEachStatement.getLoopVariable();
    Expression iterableExpression = forEachStatement.getIterableExpression();

    // T[] array = exp.
    Variable arrayVariable =
        Variable.newBuilder()
            .setName("$array")
            .setTypeDescriptor(iterableExpression.getTypeDescriptor())
            .setFinal(true)
            .build();

    // If useDoubleForIndexVariable is true, declare the indexing variable double instead of int
    // to avoid integer coercions, which in closure would make
    //      index++
    // be
    //      index = (index + 1)|0 .
    // Since Java arrays can only be up to Integer.MAX_VALUE size and the index is only used for
    // indexing an array, changing its type to double in Closure does not have any observable
    // effect.

    // int $index = 0; or double $index = 0;
    Variable indexVariable =
        Variable.newBuilder()
            .setName("$index")
            .setTypeDescriptor(
                useDoubleForIndexVariable ? PrimitiveTypes.DOUBLE : PrimitiveTypes.INT)
            .build();

    // $index < $array.length
    Expression condition =
        indexVariable
            .createReference()
            .infixLessThan(
                ArrayLength.newBuilder()
                    .setArrayExpression(arrayVariable.createReference())
                    .build());

    // T t = $array[$index];
    SourcePosition sourcePosition = forEachStatement.getSourcePosition();
    ExpressionStatement forVariableDeclarationStatement =
        VariableDeclarationExpression.newBuilder()
            .addVariableDeclaration(
                loopVariable,
                ArrayAccess.newBuilder()
                    .setArrayExpression(arrayVariable.createReference())
                    .setIndexExpression(indexVariable.createReference())
                    .build())
            .build()
            .makeStatement(sourcePosition);

    return ForStatement.newBuilder()
        .setInitializers(
            VariableDeclarationExpression.newBuilder()
                .addVariableDeclaration(arrayVariable, iterableExpression)
                .addVariableDeclaration(indexVariable, NumberLiteral.fromInt(0))
                .build())
        .setConditionExpression(condition)
        .setUpdates(
            PostfixExpression.newBuilder()
                .setOperand(indexVariable.createReference())
                .setOperator(PostfixOperator.INCREMENT)
                .build())
        .setBodyStatements(forVariableDeclarationStatement, forEachStatement.getBody())
        .setSourcePosition(sourcePosition)
        .build();
  }

  /**
   * Converts a for each statement on an iterable a regular for loop. E.g.
   *
   * <p>
   *
   * <pre>{@code
   * for(T v : exp) {
   *   S;
   * }
   * }</pre>
   *
   * <p>into
   *
   * <p>
   *
   * <pre>{@code
   *  for(Iterator<T> $iterator = (exp).iterator(); $iterator.hasNext(); ) {
   *    T v = (T) $iterator.next();
   *    S;
   * }
   * }</pre>
   */
  private ForStatement convertForEachIterable(ForEachStatement forEachStatement) {
    Variable loopVariable = forEachStatement.getLoopVariable();

    Expression iterableExpression =
        maybeAddCast(
            forEachStatement.getIterableExpression(),
            TypeDescriptors.get().javaLangIterable,
            loopVariable.getTypeDescriptor());

    MethodDescriptor iteratorMethod =
        ((DeclaredTypeDescriptor) iterableExpression.getTypeDescriptor())
            .getMethodDescriptor("iterator");

    Expression iteratorExpression =
        maybeAddCast(
            MethodCall.Builder.from(iteratorMethod).setQualifier(iterableExpression).build(),
            TypeDescriptors.get().javaUtilIterator,
            loopVariable.getTypeDescriptor());
    DeclaredTypeDescriptor iteratorType =
        (DeclaredTypeDescriptor) iteratorExpression.getTypeDescriptor();

    // Iterator<T> $iterator = (exp).iterator();
    Variable iteratorVariable =
        Variable.newBuilder()
            .setName("$iterator")
            .setTypeDescriptor(iteratorMethod.getReturnTypeDescriptor())
            .setFinal(true)
            .build();

    VariableDeclarationExpression iteratorDeclaration =
        VariableDeclarationExpression.newBuilder()
            .addVariableDeclaration(iteratorVariable, iteratorExpression)
            .build();

    // $iterator.hasNext();
    MethodDescriptor hasNextMethod = iteratorType.getMethodDescriptor("hasNext");
    Expression condition =
        MethodCall.Builder.from(hasNextMethod)
            .setQualifier(iteratorVariable.createReference())
            .build();

    // T v = $iterator.next();
    MethodDescriptor nextMethod = iteratorType.getMethodDescriptor("next");
    ExpressionStatement forVariableDeclarationStatement =
        VariableDeclarationExpression.newBuilder()
            .addVariableDeclaration(
                loopVariable,
                MethodCall.Builder.from(nextMethod)
                    .setQualifier(iteratorVariable.createReference())
                    .build())
            .build()
            .makeStatement(forEachStatement.getSourcePosition());

    return ForStatement.newBuilder()
        .setInitializers(iteratorDeclaration)
        .setConditionExpression(condition)
        .setBodyStatements(forVariableDeclarationStatement, forEachStatement.getBody())
        .setSourcePosition(forEachStatement.getSourcePosition())
        .build();
  }

  /**
   * Add a cast to expression to ensure the type of the expression is a DeclaredTypeDescriptor (i.e.
   * a class, interface or enum), to allow for method lookup.
   *
   * <p>This is to workaround the fact that method lookup is only implemented by
   * DeclaredTypeDescriptors. So if the iterable expression is typed as a TypeVariable,
   * IntersectionTypeDescriptor or UnionTypeDescriptor, the cast will allow for method lookup.
   */
  private Expression maybeAddCast(
      Expression iterableExpression,
      DeclaredTypeDescriptor castToType,
      TypeDescriptor loopVariableType) {
    if (iterableExpression.getTypeDescriptor() instanceof DeclaredTypeDescriptor) {
      return iterableExpression;
    }

    // If the loop variable types is a primitive, the iterable element type must be its boxed type.
    TypeDescriptor iterableElementType =
        loopVariableType.isPrimitive() ? loopVariableType.toBoxedType() : loopVariableType;
    return CastExpression.newBuilder()
        .setCastTypeDescriptor(
            castToType.specializeTypeVariables(
                ImmutableMap.of(
                    castToType.getTypeDeclaration().getTypeParameterDescriptors().get(0),
                    iterableElementType)))
        .setExpression(iterableExpression)
        .build();
  }
}
