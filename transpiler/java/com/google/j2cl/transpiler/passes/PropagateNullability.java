/*
 * Copyright 2024 Google Inc.
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
import static com.google.j2cl.transpiler.ast.NullabilityPropagationUtils.propagateNullabilityFrom;
import static com.google.j2cl.transpiler.ast.NullabilityPropagationUtils.propagateNullabilityFromArguments;
import static com.google.j2cl.transpiler.ast.NullabilityPropagationUtils.propagateNullabilityFromQualifier;
import static com.google.j2cl.transpiler.ast.NullabilityPropagationUtils.propagateNullabilityTo;
import static com.google.j2cl.transpiler.ast.NullabilityPropagationUtils.propagateTypeArgumentNullabilityFromInferredType;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangVoid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodLike;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import com.google.j2cl.transpiler.ast.VariableReference;
import java.util.List;

/**
 * Propagates nullability in inferred types from actual nullability in expressions.
 *
 * <p>It includes nullability in:
 *
 * <ul>
 *   <li>array literals in new array expressions
 *   <li>array literals in method invocations
 *   <li>constructor type arguments
 *   <li>method type arguments
 * </ul>
 *
 * <p>In the following statement:
 *
 * <pre>{@code
 * @Nullable String[] arr = {null};
 * }</pre>
 *
 * the inferred type of array literal is {@code String[]} instead of {@code @Nullable String[]}. It
 * causes insertion of {@code !!} in transpiled Kotlin code:
 *
 * <pre>{@code
 * val arr: Array<String?> = arrayOf<String>(null!!) as Array<String?>
 * }</pre>
 *
 * This pass fixes the inferred array component type to be {@code @Nullable String[]}, which fixes
 * the problem in transpiled Kotlin code:
 *
 * <pre>{@code
 * val arr: Array<String?> = arrayOf<String?>(null)
 * }</pre>
 *
 * <p>Similar problem appears in generic constructor calls:
 *
 * <pre>{@code
 * new List<>("", null);
 * }</pre>
 *
 * The inferred type is {@code List<String>} instead of {@code List<@Nullable String>}, which causes
 * insertion of {@code !!} in transpiled Kotlin code:
 *
 * <pre>{@code
 * listOf<String>("", null!!)
 * }</pre>
 *
 * Propagating nullability from arguments to type arguments fixes the problem in transpiled Kotlin:
 *
 * <pre>{@code
 * listOf<String?>("", null)
 * }</pre>
 *
 * <p>And, in generic method calls:
 *
 * <pre>{@code
 * void <T extends @Nullable Object> accept(T t1, T t2) {}
 * }</pre>
 *
 * <pre>{@code
 * accept("", null);
 * }</pre>
 *
 * The inferred type argument of {@code accept} is {@code String} instead of {@code @Nullable
 * String}, which causes insertion of {@code !!} in transpiled Kotlin code:
 *
 * <pre>{@code
 * accept<String>("", null!!)
 * }</pre>
 *
 * Propagating nullability from arguments to type arguments fixes the problem in transpiled Kotlin:
 *
 * <pre>{@code
 * accept<String?>("", null)
 * }</pre>
 */
public class PropagateNullability extends AbstractJ2ktNormalizationPass {
  // Max number of iterations to prevent infinite loop in case of bug.
  private static final int MAX_PROPAGATE_NULLABILITY_ITERATIONS = 100;

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    fixVariableNullability(compilationUnit);

    // TODO(b/406815802): See whether this can be improved.
    for (int i = 0; i < MAX_PROPAGATE_NULLABILITY_ITERATIONS; i++) {
      if (!propagateNullability(compilationUnit)) {
        // No changes were made in this iteration, exit the loop.
        break;
      }
    }
  }

  /** Makes variables and lambda parameters nullable if they are compared to {@code null}. */
  private static void fixVariableNullability(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitBinaryExpression(BinaryExpression binaryExpression) {
            if (!binaryExpression.isReferenceComparison()) {
              return;
            }

            if (!(getParent(MethodLike.class::isInstance)
                instanceof FunctionExpression functionExpression)) {
              return;
            }

            Variable variable = getVariableIfComparedToNull(binaryExpression);
            if (variable == null
                || variable.isParameter()
                    && !functionExpression.getParameters().contains(variable)) {
              // If the variable compared to null is a parameter but not of the enclosing lambda
              // leave unchanged.
              return;
            }

            var variableTypeDescriptor = variable.getTypeDescriptor();
            if (!variableTypeDescriptor.canBeNull()) {
              variable.setTypeDescriptor(variableTypeDescriptor.toNullable());
            }
          }

          private static Variable getVariableIfComparedToNull(BinaryExpression binaryExpression) {
            if (binaryExpression.getLeftOperand() instanceof NullLiteral
                && binaryExpression.getRightOperand()
                    instanceof VariableReference variableReference) {
              return variableReference.getTarget();
            }
            if (binaryExpression.getRightOperand() instanceof NullLiteral
                && binaryExpression.getLeftOperand()
                    instanceof VariableReference variableReference) {
              return variableReference.getTarget();
            }
            return null;
          }
        });
  }

  /**
   * Runs one iteration of nullability propagation over the compilation unit.
   *
   * <p>It rewrites array literals, new array expressions, method calls, new instance expressions,
   * functional expressions, and cast expressions to propagate nullability.
   *
   * @return {@code true} if any type descriptors were modified during this pass.
   */
  private static boolean propagateNullability(CompilationUnit compilationUnit) {
    boolean[] changed = {false};

    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteArrayLiteral(ArrayLiteral arrayLiteral) {
            ArrayTypeDescriptor arrayTypeDescriptor = arrayLiteral.getTypeDescriptor();
            TypeDescriptor componentTypeDescriptor =
                propagateNullabilityFrom(
                    arrayTypeDescriptor.getComponentTypeDescriptor(),
                    arrayLiteral.getValueExpressions().stream().map(Expression::getTypeDescriptor));
            if (componentTypeDescriptor.equals(arrayTypeDescriptor.getComponentTypeDescriptor())) {
              return arrayLiteral;
            }
            changed[0] = true;
            return arrayLiteral.toBuilder()
                .setTypeDescriptor(
                    arrayTypeDescriptor.withComponentTypeDescriptor(componentTypeDescriptor))
                .build();
          }

          @Override
          public Node rewriteNewArray(NewArray newArray) {
            Expression initializer = newArray.getInitializer();
            if (initializer == null
                || initializer.getTypeDescriptor() == newArray.getTypeDescriptor()) {
              return newArray;
            }
            // Update type of NewArray expression from rewritten initializer.
            changed[0] = true;
            return newArray.toBuilder()
                .setTypeDescriptor((ArrayTypeDescriptor) initializer.getTypeDescriptor())
                .build();
          }

          @Override
          public Node rewriteInvocation(Invocation invocation) {
            MethodDescriptor methodDescriptor = invocation.getTarget();
            MethodDescriptor declarationMethodDescriptor =
                methodDescriptor.getDeclarationDescriptor();
            MethodDescriptor rewrittenMethodDescriptor = methodDescriptor;

            // TODO(b/406815802): See if all the typeVar->typeParam assignments can be
            // processed in one go instead of first rewriting for the qualifier and then
            // for the parameters.

            Expression qualifier = invocation.getQualifier();
            if (qualifier != null && methodDescriptor.isInstanceMember()) {
              // Propagate nullability from qualifier to method enclosing type descriptor.
              ImmutableList<TypeVariable> typeParameterTypeDescriptors =
                  declarationMethodDescriptor
                      .getEnclosingTypeDescriptor()
                      .getTypeDeclaration()
                      .getTypeParameterDescriptors();
              ImmutableList<TypeDescriptor> typeArgumentTypeDescriptors =
                  toNonRawTypeDescriptor(methodDescriptor.getEnclosingTypeDescriptor())
                      .getTypeArgumentDescriptors();
              if (!typeArgumentTypeDescriptors.isEmpty()) {
                rewrittenMethodDescriptor =
                    propagateNullabilityFromQualifier(
                        rewrittenMethodDescriptor,
                        typeParameterTypeDescriptors,
                        typeArgumentTypeDescriptors,
                        qualifier.getTypeDescriptor());
              }
            }

            // Propagate nullability from arguments to method type arguments.
            ImmutableList<TypeVariable> typeParameterTypeDescriptors;
            ImmutableList<TypeDescriptor> typeArgumentTypeDescriptors;
            if (invocation instanceof NewInstance newInstance) {
              // For invocations, get the parameterization of the type to be instantiated.
              DeclaredTypeDescriptor typeDescriptor = newInstance.getTypeDescriptor();
              typeParameterTypeDescriptors =
                  typeDescriptor.getTypeDeclaration().getTypeParameterDescriptors();
              typeArgumentTypeDescriptors =
                  toNonRawTypeDescriptor(typeDescriptor).getTypeArgumentDescriptors();
            } else {
              // For method calls, get the parameterization of the method.
              typeParameterTypeDescriptors =
                  rewrittenMethodDescriptor
                      .getDeclarationDescriptor()
                      .getTypeParameterTypeDescriptors();
              typeArgumentTypeDescriptors =
                  rewrittenMethodDescriptor.getTypeArgumentTypeDescriptors();
            }

            if (!typeArgumentTypeDescriptors.isEmpty()) {
              rewrittenMethodDescriptor =
                  propagateNullabilityFromArguments(
                      rewrittenMethodDescriptor,
                      typeParameterTypeDescriptors,
                      typeArgumentTypeDescriptors,
                      invocation.getArguments().stream()
                          .map(Expression::getTypeDescriptor)
                          .collect(toImmutableList()));
            }

            // Propagate nullability from parameters into arguments, this covers the cases where the
            // expression is a lambda and its parameters are inferred from the surrounding context.
            ImmutableList<Expression> rewrittenArguments =
                zip(
                    invocation.getArguments(),
                    rewrittenMethodDescriptor.getParameterTypeDescriptors(),
                    PropagateNullability::propagateNullabilityToExpression);

            if (rewrittenMethodDescriptor.equals(methodDescriptor)
                && rewrittenArguments.equals(invocation.getArguments())) {
              return invocation;
            }
            changed[0] = true;
            return invocation.toBuilder()
                .setTarget(rewrittenMethodDescriptor)
                .setArguments(rewrittenArguments)
                .build();
          }

          @Override
          public Node rewriteFunctionExpression(FunctionExpression functionExpression) {
            DeclaredTypeDescriptor functionalInterface =
                functionExpression.getTypeDescriptor().getFunctionalInterface();
            ImmutableList<TypeVariable> typeParameterDescriptors =
                functionalInterface.getTypeDeclaration().getTypeParameterDescriptors();
            ImmutableList<TypeDescriptor> typeArgumentDescriptors =
                toNonRawTypeDescriptor(functionalInterface).getTypeArgumentDescriptors();
            ImmutableList<TypeDescriptor> inferredTypeArgumentDescriptors =
                zip(
                    typeParameterDescriptors,
                    typeArgumentDescriptors,
                    (typeParameterDescriptor, typeArgumentDescriptor) ->
                        propagateTypeArgumentNullabilityFromReturnExpressions(
                            typeParameterDescriptor, typeArgumentDescriptor, functionExpression));
            DeclaredTypeDescriptor inferredFunctionalInterface =
                functionalInterface.withTypeArguments(inferredTypeArgumentDescriptors);
            Streams.forEachPair(
                functionExpression.getParameters().stream(),
                inferredFunctionalInterface
                    .getSingleAbstractMethodDescriptor()
                    .getParameterTypeDescriptors()
                    .stream(),
                (variable, typeDescriptor) -> {
                  var variableTypeDescriptor =
                      propagateNullabilityTo(variable.getTypeDescriptor(), typeDescriptor);
                  if (variableTypeDescriptor != variable.getTypeDescriptor()) {
                    changed[0] = true;
                    variable.setTypeDescriptor(variableTypeDescriptor);
                  }
                });
            if (inferredFunctionalInterface == functionalInterface) {
              return functionExpression;
            }
            changed[0] = true;
            return functionExpression.toBuilder()
                .setTypeDescriptor(inferredFunctionalInterface)
                .build();
          }

          @Override
          public CastExpression rewriteCastExpression(CastExpression castExpression) {
            if (castExpression.getExpression().getTypeDescriptor().isNullable()
                && !castExpression.getCastTypeDescriptor().isNullable()) {
              changed[0] = true;
              return castExpression.toBuilder()
                  .setCastTypeDescriptor(castExpression.getCastTypeDescriptor().toNullable())
                  .build();
            }
            return castExpression;
          }

          @Override
          public Node rewriteVariableDeclarationFragment(VariableDeclarationFragment fragment) {
            Variable variable = fragment.getVariable();
            Expression initializer = fragment.getInitializer();
            if (initializer == null || !variable.isExplicitlyTyped()) {
              return fragment;
            }
            Expression rewrittenInitializer =
                propagateNullabilityToExpression(initializer, variable.getTypeDescriptor());
            if (rewrittenInitializer.equals(initializer)) {
              return fragment;
            }
            changed[0] = true;
            return fragment.toBuilder().setInitializer(rewrittenInitializer).build();
          }

          @Override
          public Node rewriteReturnStatement(ReturnStatement returnStatement) {
            Expression expression = returnStatement.getExpression();
            if (expression == null) {
              return returnStatement;
            }

            MethodLike enclosingMethod = (MethodLike) getParent(MethodLike.class::isInstance);
            Expression rewrittenExpression =
                propagateNullabilityToExpression(
                    expression, enclosingMethod.getDescriptor().getReturnTypeDescriptor());
            if (rewrittenExpression.equals(expression)) {
              return returnStatement;
            }
            changed[0] = true;
            return returnStatement.toBuilder().setExpression(rewrittenExpression).build();
          }

          @Override
          public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
            if (!binaryExpression.getOperator().isSimpleAssignment()) {
              return binaryExpression;
            }
            Expression rewrittenExpression =
                propagateNullabilityToExpression(
                    binaryExpression.getRightOperand(),
                    binaryExpression.getLeftOperand().getTypeDescriptor());
            if (rewrittenExpression.equals(binaryExpression.getRightOperand())) {
              return binaryExpression;
            }
            changed[0] = true;
            return binaryExpression.toBuilder().setRightOperand(rewrittenExpression).build();
          }
        });

    return changed[0];
  }

  /**
   * Propagates nullability from the usage site type to an expression.
   *
   * <p>If the expression is a {@link FunctionExpression} (lambda) or {@link NewInstance}, this
   * method propagates nullability from the type expected at the usage site ({@code expectedType})
   * to the expression's type descriptor.
   *
   * <p>For example, if a lambda is passed as an argument to a method expecting {@code
   * Consumer<@Nullable String>}, and the lambda has inferred type {@code Consumer<String>}, this
   * method updates the lambda's type to {@code Consumer<@Nullable String>}.
   */
  private static Expression propagateNullabilityToExpression(
      Expression expression, TypeDescriptor expectedType) {
    if (expression instanceof NewInstance newInstance
        && newInstance.getTypeArguments().isEmpty()
        && newInstance.getAnonymousInnerClass() != null) {
      // Propagate nullability to the anonymous class independently of the propagation to the
      // NewInstance expression.
      return propagateNullabilityToAnonymousClass(newInstance, expectedType);
    }

    TypeDescriptor propagatedTypeDescriptor =
        propagateNullabilityTo(expression.getTypeDescriptor(), expectedType);
    if (propagatedTypeDescriptor == expression.getTypeDescriptor()) {
      return expression;
    }
    return switch (expression) {
      case FunctionExpression functionExpression ->
          functionExpression.toBuilder().setTypeDescriptor(propagatedTypeDescriptor).build();
      case NewInstance newInstance
          when propagatedTypeDescriptor instanceof DeclaredTypeDescriptor declaredTypeDescriptor
              && newInstance.getTypeArguments().isEmpty() ->
          newInstance.toBuilder()
              .setTarget(
                  newInstance.getTarget().toBuilder()
                      .setEnclosingTypeDescriptor(declaredTypeDescriptor)
                      .build())
              .build();
      default -> expression;
    };
  }

  /**
   * Propagates nullability to an anonymous class's superclass and interfaces from the expected
   * type.
   */
  private static NewInstance propagateNullabilityToAnonymousClass(
      NewInstance newInstance, TypeDescriptor expectedType) {
    if (!(expectedType instanceof DeclaredTypeDescriptor expectedDeclaredType)) {
      return newInstance;
    }

    Type anonymousClass = newInstance.getAnonymousInnerClass();
    List<DeclaredTypeDescriptor> superInterfaces =
        anonymousClass.getSuperInterfaceTypeDescriptors();
    if (superInterfaces.isEmpty()) {
      // Propagate to superclass
      DeclaredTypeDescriptor superClass = anonymousClass.getSuperTypeDescriptor();
      DeclaredTypeDescriptor newSuperClass =
          propagateNullabilityTo(superClass, expectedDeclaredType);
      if (!newSuperClass.equals(superClass)) {
        anonymousClass.setSuperTypeDescriptor(newSuperClass);
        // Even though the type was updated directly in the AST, create a new NewInstance expression
        // to signal that propagation has made a change.
        return newInstance.toBuilder().setAnonymousInnerClass(anonymousClass).build();
      }
    } else {
      // Propagate to interfaces
      ImmutableList<DeclaredTypeDescriptor> newSuperInterfaces =
          superInterfaces.stream()
              .map(it -> propagateNullabilityTo(it, expectedDeclaredType))
              .collect(toImmutableList());
      if (!newSuperInterfaces.equals(superInterfaces)) {
        anonymousClass.setSuperInterfaceTypeDescriptors(newSuperInterfaces);
        // Even though the type was updated directly in the AST, create a new NewInstance expression
        // to signal that propagation has made a change.
        return newInstance.toBuilder().setAnonymousInnerClass(anonymousClass).build();
      }
    }

    return newInstance;
  }

  /**
   * Propagates nullability to a type argument of a functional interface based on the types of the
   * expressions returned by the lambda body.
   *
   * <p>Visits all return statements in the lambda and propagates nullability from the returned
   * expression types to the corresponding type argument of the functional interface.
   */
  private static TypeDescriptor propagateTypeArgumentNullabilityFromReturnExpressions(
      TypeVariable typeParameterDescriptor,
      TypeDescriptor typeArgumentDescriptor,
      MethodLike methodLike) {
    TypeDescriptor[] propagatedTypeArgumentDescriptorRef = {typeArgumentDescriptor};
    methodLike.accept(
        new AbstractVisitor() {
          @Override
          public void exitReturnStatement(ReturnStatement returnStatement) {
            MethodLike returnMethodLike = (MethodLike) getParent(MethodLike.class::isInstance);
            if (returnMethodLike != methodLike) {
              return;
            }

            Expression expression = returnStatement.getExpression();
            if (expression == null) {
              return;
            }

            var returnedExpressionTypeDescriptor = expression.getTypeDescriptor();
            if (isJavaLangVoid(returnedExpressionTypeDescriptor)) {
              // If a method returns an expression of type `Void`, it must be the null value.
              returnedExpressionTypeDescriptor = returnedExpressionTypeDescriptor.toNullable();
            }

            propagatedTypeArgumentDescriptorRef[0] =
                propagateTypeArgumentNullabilityFromInferredType(
                    typeParameterDescriptor,
                    propagatedTypeArgumentDescriptorRef[0],
                    methodLike.getDescriptor().getDeclarationDescriptor().getReturnTypeDescriptor(),
                    returnedExpressionTypeDescriptor);
          }
        });
    return propagatedTypeArgumentDescriptorRef[0];
  }

  /** Returns non-RAW type descriptor, by using RAW type parameters as type arguments. */
  private static DeclaredTypeDescriptor toNonRawTypeDescriptor(
      DeclaredTypeDescriptor typeDescriptor) {
    return !typeDescriptor.isRaw()
        ? typeDescriptor
        : typeDescriptor.withTypeArguments(
            typeDescriptor.getTypeDeclaration().getTypeParameterDescriptors().stream()
                .map(TypeVariable::toRawTypeDescriptor)
                .collect(toImmutableList()));
  }
}
