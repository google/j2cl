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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayCreationReference;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.JavaScriptConstructorReference;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodReference;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SuperReference;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Converts method references into lambdas. */
public class ConvertMethodReferencesToLambdas extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteMethodReference(MethodReference methodReference) {

            MethodDescriptor referencedMethodDescriptor =
                methodReference.getReferencedMethodDescriptor();
            Expression qualifier = methodReference.getQualifier();
            SourcePosition sourcePosition = methodReference.getSourcePosition();
            TypeDescriptor typeDescriptor = methodReference.getTypeDescriptor();
            MethodDescriptor interfacedMethodDescriptor =
                methodReference.getInterfacedMethodDescriptor();

            if (referencedMethodDescriptor.isConstructor()) {
              return createInstantiationLambda(
                  interfacedMethodDescriptor,
                  referencedMethodDescriptor,
                  qualifier,
                  sourcePosition);
            }

            if (qualifier instanceof SuperReference) {
              return createForwardingFunctionExpression(
                  sourcePosition,
                  typeDescriptor,
                  interfacedMethodDescriptor,
                  qualifier,
                  referencedMethodDescriptor,
                  true);
            }

            return createMethodReferenceLambda(
                sourcePosition,
                qualifier,
                referencedMethodDescriptor,
                typeDescriptor,
                interfacedMethodDescriptor);
          }

          @Override
          public Expression rewriteArrayCreationReference(
              ArrayCreationReference arrayCreationReference) {

            return createArrayCreationLambda(
                arrayCreationReference.getInterfacedMethodDescriptor(),
                arrayCreationReference.getTargetTypeDescriptor(),
                arrayCreationReference.getSourcePosition());
          }
        });
  }

  /**
   * Creates a lambda from a qualified method reference.
   *
   * <p>
   *
   * <pre>{@code
   * q::m into (par1, ..., parN) -> q.m(par1, ..., parN) or
   *           (let $q = q, (par1, ..., parN) -> $q.m(par1, ..., parN)
   * }</pre>
   *
   * <p>depending on whether the qualifier can be evaluated inside the functional expression
   * preserving the original semantics.
   */
  private static Expression createMethodReferenceLambda(
      SourcePosition sourcePosition,
      Expression qualifier,
      MethodDescriptor referencedMethodDescriptor,
      TypeDescriptor expressionTypeDescriptor,
      MethodDescriptor functionalMethodDescriptor) {
    List<Expression> result = new ArrayList<>();

    if (qualifier != null && !qualifier.isEffectivelyInvariant()) {
      // The semantics require that the qualifier be evaluated in the context where the method
      // reference appears, so here we introduce a temporary variable to store the evaluated
      // qualifier.
      Variable variable =
          Variable.newBuilder()
              .setFinal(true)
              .setName("$$q")
              .setTypeDescriptor(qualifier.getTypeDescriptor())
              .build();
      // Declare the temporary variable and initialize to the evaluated qualifier.
      result.add(
          VariableDeclarationExpression.newBuilder()
              .addVariableDeclaration(variable, qualifier)
              .build());
      // Use the newly introduced variable as a qualifier when forwarding the call within the
      // lambda expression.
      qualifier = variable.createReference();
    }

    result.add(
        createForwardingFunctionExpression(
            sourcePosition,
            expressionTypeDescriptor,
            functionalMethodDescriptor,
            qualifier,
            referencedMethodDescriptor,
            false));

    return MultiExpression.newBuilder().setExpressions(result).build();
  }

  /**
   * Creates a FunctionExpression described by {@code functionalMethodDescriptor} that forwards to
   * {@code targetMethodDescriptor}.
   */
  private static FunctionExpression createForwardingFunctionExpression(
      SourcePosition sourcePosition,
      TypeDescriptor expressionTypeDescriptor,
      MethodDescriptor functionalMethodDescriptor,
      Expression qualifier,
      MethodDescriptor targetMethodDescriptor,
      boolean isStaticDispatch) {

    // Remove type parameters for the JsFunction method to avoid introducing them in the parameters
    // of the lambda expression. That is done because the type for functions in Closure can not
    // define templates.
    MethodDescriptor jsFunctionMethodDescriptor = functionalMethodDescriptor;
    List<Variable> parameters =
        AstUtils.createParameterVariables(jsFunctionMethodDescriptor.getParameterTypeDescriptors());

    // Does the method reference have a qualifier? I.e., the qualifier is not null and is not a
    // class name (modeled as a JavaScriptConstructorReference). Used in unqualified instance
    // method/Kotlin extension transformations below.
    boolean hasQualifier =
        qualifier != null && !(qualifier instanceof JavaScriptConstructorReference);

    ImmutableList<Expression> forwardedArguments;
    if (!targetMethodDescriptor.isStatic() && !hasQualifier) {
      // This is a reference to an instance method without an explicit qualifier, e.g.:
      //
      // Class::instanceMethod
      //
      // The first parameter of the functional interface becomes the qualifier for the method call:
      //
      // (a, b) -> { a.instanceMethod(b) }
      checkArgument(
          parameters.size() == targetMethodDescriptor.getParameterTypeDescriptors().size() + 1
              || (parameters.size() >= targetMethodDescriptor.getParameterTypeDescriptors().size()
                  && targetMethodDescriptor.isVarargs()));
      qualifier = parameters.get(0).createReference();
      forwardedArguments =
          parameters.stream().skip(1).map(Variable::createReference).collect(toImmutableList());
    } else if (targetMethodDescriptor.isStatic() && hasQualifier) {
      // This is a reference to a static method but has an explicit qualifier. This path cannot
      // be invoked by Java method references, because references to static methods in Java cannot
      // have qualifiers. It can only be a Kotlin extension method, e.g.:
      //
      // q::extensionMethod
      //
      // We pass the qualifier as the first argument of the referenced method:
      //
      // (a, b) -> { extensionMethod(q, a, b) }
      checkArgument(
          parameters.size() + 1 == targetMethodDescriptor.getParameterTypeDescriptors().size());
      forwardedArguments =
          Stream.concat(Stream.of(qualifier), parameters.stream().map(Variable::createReference))
              .collect(toImmutableList());
      qualifier = null;
    } else {
      // General case: forward all parameters normally.
      forwardedArguments =
          parameters.stream().map(Variable::createReference).collect(toImmutableList());
    }

    Statement forwardingStatement =
        AstUtils.createForwardingStatement(
            sourcePosition,
            qualifier,
            targetMethodDescriptor,
            isStaticDispatch,
            forwardedArguments,
            functionalMethodDescriptor.getReturnTypeDescriptor());
    return FunctionExpression.newBuilder()
        .setTypeDescriptor(expressionTypeDescriptor)
        .setJsAsync(targetMethodDescriptor.isJsAsync() || functionalMethodDescriptor.isJsAsync())
        .setParameters(parameters)
        .setStatements(forwardingStatement)
        .setSourcePosition(sourcePosition)
        .build();
  }

  /**
   * Creates a class instantiation lambda from a method reference.
   *
   * <p>
   *
   * <pre>{@code
   * A:new into (par1, ..., parN) -> new A(par1, ..., parN) or
   *            (par1, ..., parN) -> B.this.new A(par1, ..., parN)
   * }</pre>
   */
  protected Expression createInstantiationLambda(
      MethodDescriptor functionalMethodDescriptor,
      MethodDescriptor targetConstructorMethodDescriptor,
      Expression qualifier,
      SourcePosition sourcePosition) {

    List<Variable> parameters =
        AstUtils.createParameterVariables(functionalMethodDescriptor.getParameterTypeDescriptors());
    checkArgument(
        targetConstructorMethodDescriptor.getParameterTypeDescriptors().size()
            == parameters.size());

    NewInstance instantiation =
        NewInstance.Builder.from(targetConstructorMethodDescriptor)
            .setQualifier(qualifier)
            .setArguments(
                parameters.stream().map(Variable::createReference).collect(toImmutableList()))
            .build();

    return FunctionExpression.newBuilder()
        .setTypeDescriptor(functionalMethodDescriptor.getEnclosingTypeDescriptor())
        .setParameters(parameters)
        .setStatements(
            ReturnStatement.newBuilder()
                .setExpression(instantiation)
                .setSourcePosition(sourcePosition)
                .build())
        .setSourcePosition(sourcePosition)
        .build();
  }

  /**
   * Creates a lambda that implements an array creation method reference.
   *
   * <p>
   *
   * <pre>{@code convert A[]::new into (size) -> new A[size]} </pre>
   */
  protected static Expression createArrayCreationLambda(
      MethodDescriptor targetFunctionalMethodDescriptor,
      ArrayTypeDescriptor arrayType,
      SourcePosition sourcePosition) {

    // Array creation method references always have exactly one parameter.
    Variable parameter =
        Iterables.getOnlyElement(
            AstUtils.createParameterVariables(
                targetFunctionalMethodDescriptor.getParameterTypeDescriptors()));

    // The size of the array is the only parameter in the implemented function. It's legal for
    // the source to provide only one dimension parameter to to create a multidimensional array
    // but our AST expects NewArray nodes to provide an expression for each dimension in the
    // array type, hence the missing dimensions are padded with null.
    ImmutableList<Expression> dimensionExpressions =
        ImmutableList.<Expression>builder()
            .add(parameter.createReference())
            .addAll(AstUtils.createListOfNullValues(arrayType.getDimensions() - 1))
            .build();

    return FunctionExpression.newBuilder()
        .setTypeDescriptor(targetFunctionalMethodDescriptor.getEnclosingTypeDescriptor())
        .setParameters(parameter)
        .setStatements(
            ReturnStatement.newBuilder()
                .setExpression(
                    NewArray.newBuilder()
                        .setTypeDescriptor(arrayType)
                        .setDimensionExpressions(dimensionExpressions)
                        .build())
                .setSourcePosition(sourcePosition)
                .build())
        .setSourcePosition(sourcePosition)
        .build();
  }
}
