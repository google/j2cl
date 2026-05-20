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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangVoid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
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
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodLike;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.NullabilityAnnotation;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import com.google.j2cl.transpiler.ast.VariableReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

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
            return NewArray.Builder.from(newArray)
                .setTypeDescriptor((ArrayTypeDescriptor) initializer.getTypeDescriptor())
                .build();
          }

          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor methodDescriptor = methodCall.getTarget();
            MethodDescriptor declarationMethodDescriptor =
                methodDescriptor.getDeclarationDescriptor();
            MethodDescriptor rewrittenMethodDescriptor = methodDescriptor;

            // TODO(b/406815802): See if all the typeVar->typeParam assignments can be
            // processed in one go instead of first rewriting for the qualifier and then
            // for the parameters.

            // Propagate nullability from qualifier to method enclosing type descriptor.
            Expression qualifier = methodCall.getQualifier();
            if (qualifier != null && !methodDescriptor.isConstructor()) {
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
                        qualifier);
              }
            }

            // Propagate nullability from arguments to method type arguments.
            ImmutableList<TypeVariable> typeParameterTypeDescriptors =
                declarationMethodDescriptor.getTypeParameterTypeDescriptors();
            ImmutableList<TypeDescriptor> typeArgumentTypeDescriptors =
                methodDescriptor.getTypeArgumentTypeDescriptors();
            if (!typeArgumentTypeDescriptors.isEmpty()) {
              rewrittenMethodDescriptor =
                  propagateNullabilityFromArguments(
                      rewrittenMethodDescriptor,
                      typeParameterTypeDescriptors,
                      typeArgumentTypeDescriptors,
                      methodCall.getArguments());
            }

            // Propagate nullability from parameters into arguments, this covers the cases where the
            // expression is a lambda and its parameters are inferred from the surrounding context.
            ImmutableList<Expression> rewrittenArguments =
                zip(
                    methodCall.getArguments(),
                    rewrittenMethodDescriptor.getParameterTypeDescriptors(),
                    PropagateNullability::propagateNullabilityToExpression);

            if (rewrittenMethodDescriptor.equals(methodDescriptor)
                && rewrittenArguments.equals(methodCall.getArguments())) {
              return methodCall;
            }
            changed[0] = true;
            return MethodCall.Builder.from(methodCall)
                .setTarget(rewrittenMethodDescriptor)
                .setArguments(rewrittenArguments)
                .build();
          }

          // TODO(b/406815802): See if rewriteInvocation can be refactored to seamlessly
          // handle this case since instantiations are very similar to static method calls.
          @Override
          public Node rewriteNewInstance(NewInstance newInstance) {
            // Propagate nullability from arguments to type arguments of the enclosing type
            // descriptor.
            MethodDescriptor methodDescriptor = newInstance.getTarget();
            DeclaredTypeDescriptor typeDescriptor = newInstance.getTypeDescriptor();
            ImmutableList<TypeVariable> typeParameterDescriptors =
                typeDescriptor.getTypeDeclaration().getTypeParameterDescriptors();
            ImmutableList<TypeDescriptor> typeArgumentDescriptors =
                toNonRawTypeDescriptor(typeDescriptor).getTypeArgumentDescriptors();
            if (typeArgumentDescriptors.isEmpty()) {
              return newInstance;
            }

            MethodDescriptor fixedMethodDescriptor =
                propagateNullabilityFromArguments(
                    methodDescriptor,
                    typeParameterDescriptors,
                    typeArgumentDescriptors,
                    newInstance.getArguments());

            if (fixedMethodDescriptor.equals(methodDescriptor)) {
              return newInstance;
            }
            changed[0] = true;
            return NewInstance.Builder.from(newInstance).setTarget(fixedMethodDescriptor).build();
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
                      propagateNullabilityTo(
                          variable.getTypeDescriptor(), typeDescriptor, ImmutableSet.of());
                  if (variableTypeDescriptor != variable.getTypeDescriptor()) {
                    changed[0] = true;
                    variable.setTypeDescriptor(variableTypeDescriptor);
                  }
                });
            if (inferredFunctionalInterface == functionalInterface) {
              return functionExpression;
            }
            changed[0] = true;
            return FunctionExpression.Builder.from(functionExpression)
                .setTypeDescriptor(inferredFunctionalInterface)
                .build();
          }

          @Override
          public CastExpression rewriteCastExpression(CastExpression castExpression) {
            if (castExpression.getExpression().getTypeDescriptor().isNullable()
                && !castExpression.getCastTypeDescriptor().isNullable()) {
              changed[0] = true;
              return CastExpression.Builder.from(castExpression)
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
            return VariableDeclarationFragment.Builder.from(fragment)
                .setInitializer(rewrittenInitializer)
                .build();
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
            return ReturnStatement.Builder.from(returnStatement)
                .setExpression(rewrittenExpression)
                .build();
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
            return BinaryExpression.Builder.from(binaryExpression)
                .setRightOperand(rewrittenExpression)
                .build();
          }
        });

    return changed[0];
  }

  /**
   * Returns all the different type assignments to {@code typeParameter} from the declaration {@code
   * declarationTypeDescriptor} as parameterized in {@code typeDescriptor}.
   *
   * <p>Example:
   *
   * <ul>
   *   <li>declaration type descriptor: {@code Foo<T, V, Bar<T>>}
   *   <li>type parameter: {@code T}
   *   <li>given type descriptor: {@code Foo<String, Number, Bar<@Nullable String>>}
   *   <li>results: {@code String, @Nullable String}
   * </ul>
   *
   * @param declarationTypeDescriptor declaration (unparameterized) type descriptor
   * @param typeParameter type parameter to get parameterizations for
   * @param typeDescriptor type descriptor to look for parameterizations in
   * @return a stream with parameterizations
   */
  private static Stream<TypeDescriptor> getParameterizationsIn(
      TypeDescriptor declarationTypeDescriptor,
      TypeVariable typeParameter,
      TypeDescriptor typeDescriptor) {
    return getParameterizationsIn(
        declarationTypeDescriptor, typeParameter, typeDescriptor, new HashSet<>());
  }

  private record DescriptorPair(TypeDescriptor declaration, TypeDescriptor parameterized) {}

  private static Stream<TypeDescriptor> getParameterizationsIn(
      TypeDescriptor declarationTypeDescriptor,
      TypeVariable typeParameter,
      TypeDescriptor typeDescriptor,
      Set<DescriptorPair> seen) {

    if (!seen.add(new DescriptorPair(declarationTypeDescriptor, typeDescriptor))) {
      // This pair of declaration and descriptor has already been processed.
      return Stream.of();
    }
    // TODO(b/406815802): Investigate how is it possible. The problem is reproduced in
    //  PropagateNullabilityProblem readable.
    if (!typeDescriptor.isAssignableTo(declarationTypeDescriptor)) {
      return Stream.of();
    }

    return switch (declarationTypeDescriptor) {
      // Primitive type descriptors are never parameterized.
      case PrimitiveTypeDescriptor primitiveTypeDescriptor -> Stream.of();

      case ArrayTypeDescriptor declarationArrayTypeDescriptor ->
          switch (typeDescriptor) {
            case ArrayTypeDescriptor arrayTypeDescriptor ->
                getParameterizationsIn(
                    declarationArrayTypeDescriptor.getComponentTypeDescriptor(),
                    typeParameter,
                    arrayTypeDescriptor.getComponentTypeDescriptor(),
                    seen);

            // Non-arrays are not assignable to arrays.
            default -> throw new IllegalStateException();
          };

      case DeclaredTypeDescriptor declarationDeclaredTypeDescriptor ->
          switch (typeDescriptor) {
            case PrimitiveTypeDescriptor primitiveTypeDescriptor -> Stream.of();

            // Array -> Object / Cloneable / Serializable
            case ArrayTypeDescriptor arrayTypeDescriptor -> Stream.of();

            // Look for the parameterized instance of the declared parameter type
            case DeclaredTypeDescriptor declaredTypeDescriptor ->
                Streams.zip(
                        declarationDeclaredTypeDescriptor.getTypeArgumentDescriptors().stream(),
                        checkNotNull(
                            declaredTypeDescriptor.findSupertype(
                                declarationDeclaredTypeDescriptor.getTypeDeclaration()))
                            .getTypeArgumentDescriptors()
                            .stream(),
                        (typeArgument, targetTypeArgument) ->
                            getParameterizationsIn(
                                typeArgument, typeParameter, targetTypeArgument, seen))
                    .flatMap(Function.identity());

            case TypeVariable typeVariable ->
                getParameterizationsIn(
                    declarationTypeDescriptor,
                    typeParameter,
                    getNormalizedUpperBoundTypeDescriptor(typeVariable),
                    seen);

            case IntersectionTypeDescriptor intersectionTypeDescriptor ->
                intersectionTypeDescriptor.getIntersectionTypeDescriptors().stream()
                    .filter(it -> it.isAssignableTo(declarationTypeDescriptor))
                    .flatMap(
                        it ->
                            getParameterizationsIn(
                                declarationTypeDescriptor, typeParameter, it, seen));

            // For a union to be assignable to a type, all of its components have to be assignable
            // to that type, so collect these parameterizations from all the types in the union
            case UnionTypeDescriptor unionTypeDescriptor ->
                unionTypeDescriptor.getUnionTypeDescriptors().stream()
                    .flatMap(
                        it ->
                            getParameterizationsIn(
                                declarationTypeDescriptor, typeParameter, it, seen));
          };

      case TypeVariable declarationTypeVariable
          when declarationTypeVariable.isWildcardOrCapture() ->
          getParameterizationsIn(
              getNormalizedUpperBoundTypeDescriptor(declarationTypeVariable),
              typeParameter,
              typeDescriptor instanceof TypeVariable typeVariable
                      && typeVariable.isWildcardOrCapture()
                  ? getNormalizedUpperBoundTypeDescriptor(typeVariable)
                  : typeDescriptor,
              seen);
      case TypeVariable declarationTypeVariable
          when declarationTypeVariable.toDeclaration().equals(typeParameter) ->
          Stream.of(
              declarationTypeVariable.getNullabilityAnnotation() == NullabilityAnnotation.NULLABLE
                      || !declarationTypeVariable.canBeNull()
                  ? typeDescriptor.toNonNullable()
                  : typeDescriptor);

      case TypeVariable declarationTypeVariable -> Stream.of();

      case IntersectionTypeDescriptor declarationIntersectionTypeDescriptor ->
          declarationIntersectionTypeDescriptor.getIntersectionTypeDescriptors().stream()
              .flatMap(it -> getParameterizationsIn(it, typeParameter, typeDescriptor, seen));

      case UnionTypeDescriptor declarationUnionTypeDescriptor ->
          declarationUnionTypeDescriptor.getUnionTypeDescriptors().stream()
              .flatMap(it -> getParameterizationsIn(it, typeParameter, typeDescriptor, seen));
    };
  }

  /** Propagate nullability from one type argument to another, respecting parameter nullability. */
  private static TypeDescriptor propagateTypeArgumentNullabilityFrom(
      TypeVariable typeParameterDescriptor,
      TypeDescriptor typeDescriptor,
      TypeDescriptor fromTypeDescriptor,
      ImmutableSet<TypeVariable> seen) {
    return !typeParameterDescriptor.getUpperBoundTypeDescriptor().isNullable()
        ? typeDescriptor
        : propagateNullabilityTo(typeDescriptor, fromTypeDescriptor, seen);
  }

  /** Propagate nullability from all type arguments to another, respecting parameter nullability. */
  private static MethodDescriptor propagateNullabilityFromArguments(
      MethodDescriptor methodDescriptor,
      ImmutableList<TypeVariable> typeParameterDescriptors,
      ImmutableList<TypeDescriptor> typeArgumentDescriptors,
      List<Expression> arguments) {
    ImmutableList<TypeDescriptor> parameterTypeDescriptors =
        methodDescriptor.getDeclarationDescriptor().getParameterTypeDescriptors();
    ImmutableList<TypeDescriptor> inferredTypeDescriptors =
        arguments.stream().map(Expression::getTypeDescriptor).collect(toImmutableList());
    ImmutableList<TypeDescriptor> inferredTypeArgumentDescriptors =
        zip(
            typeParameterDescriptors,
            typeArgumentDescriptors,
            (typeParameterDescriptor, typeArgumentDescriptor) ->
                propagateTypeArgumentNullabilityFromInferredTypes(
                    typeParameterDescriptor,
                    typeArgumentDescriptor,
                    parameterTypeDescriptors,
                    inferredTypeDescriptors));

    return reparameterize(
        methodDescriptor, typeParameterDescriptors, inferredTypeArgumentDescriptors);
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
        propagateNullabilityTo(expression.getTypeDescriptor(), expectedType, ImmutableSet.of());
    if (propagatedTypeDescriptor == expression.getTypeDescriptor()) {
      return expression;
    }
    return switch (expression) {
      case FunctionExpression functionExpression ->
          FunctionExpression.Builder.from(functionExpression)
              .setTypeDescriptor(propagatedTypeDescriptor)
              .build();
      case NewInstance newInstance
          when propagatedTypeDescriptor instanceof DeclaredTypeDescriptor declaredTypeDescriptor
              && newInstance.getTypeArguments().isEmpty() ->
          NewInstance.Builder.from(newInstance)
              .setTarget(
                  MethodDescriptor.Builder.from(newInstance.getTarget())
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
          propagateNullabilityTo(superClass, expectedDeclaredType, ImmutableSet.of());
      if (!newSuperClass.equals(superClass)) {
        anonymousClass.setSuperTypeDescriptor(newSuperClass);
        // Even though the type was updated directly in the AST, create a new NewInstance expression
        // to signal that propagation has made a change.
        return NewInstance.Builder.from(newInstance).setAnonymousInnerClass(anonymousClass).build();
      }
    } else {
      // Propagate to interfaces
      ImmutableList<DeclaredTypeDescriptor> newSuperInterfaces =
          superInterfaces.stream()
              .map(it -> propagateNullabilityTo(it, expectedDeclaredType, ImmutableSet.of()))
              .collect(toImmutableList());
      if (!newSuperInterfaces.equals(superInterfaces)) {
        anonymousClass.setSuperInterfaceTypeDescriptors(newSuperInterfaces);
        // Even though the type was updated directly in the AST, create a new NewInstance expression
        // to signal that propagation has made a change.
        return NewInstance.Builder.from(newInstance).setAnonymousInnerClass(anonymousClass).build();
      }
    }

    return newInstance;
  }

  /**
   * Propagates nullability from the qualifier expression to the type arguments of the enclosing
   * type of the method.
   *
   * <p>For example, if we have a call {@code qualifier.method()} where {@code qualifier} has type
   * {@code Foo<@Nullable String>} and the method is declared in {@code Foo<T>}, this method will
   * propagate the {@code @Nullable} from the qualifier's type argument to the method's enclosing
   * type arguments.
   */
  private static MethodDescriptor propagateNullabilityFromQualifier(
      MethodDescriptor methodDescriptor,
      ImmutableList<TypeVariable> typeParameterDescriptors,
      ImmutableList<TypeDescriptor> typeArgumentDescriptors,
      Expression qualifier) {
    TypeDescriptor declaredTypeDescriptor =
        methodDescriptor.getDeclarationDescriptor().getEnclosingTypeDescriptor();
    TypeDescriptor inferredTypeDescriptor = qualifier.getTypeDescriptor();
    ImmutableList<TypeDescriptor> inferredTypeArgumentDescriptors =
        zip(
            typeParameterDescriptors,
            typeArgumentDescriptors,
            (typeParameterDescriptor, typeArgumentDescriptor) ->
                propagateTypeArgumentNullabilityFromInferredType(
                    typeParameterDescriptor,
                    typeArgumentDescriptor,
                    declaredTypeDescriptor,
                    inferredTypeDescriptor));

    return reparameterize(
        methodDescriptor, typeParameterDescriptors, inferredTypeArgumentDescriptors);
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

  /**
   * Propagates nullability to a type argument from a list of inferred types.
   *
   * <p>Iterates over corresponding declaration and inferred types (e.g., parameter types and actual
   * argument types) and propagates nullability to the type argument.
   */
  private static TypeDescriptor propagateTypeArgumentNullabilityFromInferredTypes(
      TypeVariable typeParameterDescriptor,
      TypeDescriptor typeArgumentDescriptor,
      List<TypeDescriptor> declarationTypeDescriptors,
      List<TypeDescriptor> inferredTypeDescriptors) {
    for (int i = 0; i < declarationTypeDescriptors.size(); i++) {
      typeArgumentDescriptor =
          propagateTypeArgumentNullabilityFromInferredType(
              typeParameterDescriptor,
              typeArgumentDescriptor,
              declarationTypeDescriptors.get(i),
              inferredTypeDescriptors.get(i));
    }
    return typeArgumentDescriptor;
  }

  /**
   * Propagates nullability to a type argument from a single inferred type.
   *
   * <p>Uses the declaration type to determine how the type parameter is used, finds the
   * corresponding parameterizations in the inferred type, and propagates their nullability to the
   * type argument.
   */
  private static TypeDescriptor propagateTypeArgumentNullabilityFromInferredType(
      TypeVariable typeParameterDescriptor,
      TypeDescriptor typeArgumentDescriptor,
      TypeDescriptor declarationTypeDescriptor,
      TypeDescriptor inferredTypeDescriptor) {
    return getParameterizationsIn(
            declarationTypeDescriptor, typeParameterDescriptor, inferredTypeDescriptor)
        .reduce(
            propagateTypeArgument(typeParameterDescriptor, typeArgumentDescriptor),
            (typeArgument, typeDescriptor) ->
                propagateTypeArgumentNullabilityFrom(
                    typeParameterDescriptor, typeArgument, typeDescriptor, ImmutableSet.of()),
            (a, b) -> a);
  }

  /**
   * Initializes or adjusts a type argument's nullability based on the type parameter's capability
   * to be null.
   *
   * <p>If the type parameter cannot be null (e.g., it has a non-nullable bound), the type argument
   * is forced to be non-nullable.
   */
  private static TypeDescriptor propagateTypeArgument(
      TypeVariable typeParameter, TypeDescriptor typeArgument) {
    return typeParameter.canBeNull() ? typeArgument : typeArgument.toNonNullable();
  }

  /**
   * Propagates nullability from {@code fromTypeDescriptor} to {@code toTypeDescriptor} assuming
   * that {@code fromTypeDescriptor} is assignable to {@code toTypeDescriptor}.
   */
  @SuppressWarnings("unchecked")
  private static <T extends TypeDescriptor> T propagateNullabilityTo(
      T to, TypeDescriptor from, ImmutableSet<TypeVariable> seen) {
    if (to.equals(from)) {
      return to;
    }

    return switch (to) {
      case DeclaredTypeDescriptor descriptor ->
          (T) propagateNullabilityToDeclared(descriptor, from, seen);

      case ArrayTypeDescriptor descriptor ->
          (T) propagateNullabilityToArray(descriptor, from, seen);

      case TypeVariable typeVariable ->
          (T) propagateNullabilityToVariable(typeVariable, from, seen);

      // TODO(b/406815802): Handle intersection and union type descriptors, if necessary.
      default -> to;
    };
  }

  /**
   * Propagates nullability from a type descriptor to a declared type descriptor.
   *
   * <p>Handles propagation of outer nullability and nullability of type arguments.
   */
  private static TypeDescriptor propagateNullabilityToDeclared(
      DeclaredTypeDescriptor toDeclared, TypeDescriptor from, ImmutableSet<TypeVariable> seen) {
    switch (from) {
      case DeclaredTypeDescriptor fromDeclared -> {
        if (toDeclared.isRaw()) {
          return toDeclared.toNullable(toDeclared.isNullable() || fromDeclared.isNullable());
        }

        DeclaredTypeDescriptor fromDeclaredSuper =
            fromDeclared.findSupertype(toDeclared.getTypeDeclaration());

        if (fromDeclaredSuper != null) {
          if (fromDeclaredSuper.isRaw()) {
            return toDeclared.toNullable(toDeclared.isNullable() || fromDeclared.isNullable());
          }

          // Propagate nullability from a subtype to a supertype. This kind of situation happens,
          // for example, when we inferring the type of a parameter from the type of the argument.
          return toDeclared
              .withTypeArguments(
                  zip(
                      toDeclared.getTypeDeclaration().getTypeParameterDescriptors(),
                      toDeclared.getTypeArgumentDescriptors(),
                      fromDeclaredSuper.getTypeArgumentDescriptors(),
                      (a, b, c) -> propagateTypeArgumentNullabilityFrom(a, b, c, seen)))
              .toNullable(toDeclared.isNullable() || fromDeclared.isNullable());
        }

        // Find the supertype of the expression to see if we can propagate from a supertype. This
        // kind of situation happens, for example, when inferring the type of a diamond
        // instantiation from the type of the left-hand side of an assignment.
        // Imagine this scenario:
        //   Parent<String> parent = new Child<>();
        DeclaredTypeDescriptor toDeclaredAsSuper =
            toDeclared.findSupertype(fromDeclared.getTypeDeclaration());

        if (toDeclaredAsSuper == null || toDeclaredAsSuper.isRaw()) {
          return toDeclared.toNullable(toDeclared.isNullable() || fromDeclared.isNullable());
        }

        // Propagate nullability from expected supertype (fromDeclared) to actual subtype viewed
        // as supertype (toDeclaredAsSuper). Following the example above, from Child<...> we
        // find Parent<...> and we can propagate now from Parent<String>.
        DeclaredTypeDescriptor propagatedSuper =
            propagateNullabilityTo(toDeclaredAsSuper, fromDeclared, seen);

        // Get the supertype of the type to propagate to starting from the declaration, to see what
        // is the parameterization if the supertype.
        DeclaredTypeDescriptor unparameterizedTo = toDeclared.getTypeDeclaration().toDescriptor();
        // Retrieve the (potentially transitive) supertype, starting from the unparameterized type
        // to deduce the parameterization
        DeclaredTypeDescriptor inducedSupertype =
            unparameterizedTo.findSupertype(fromDeclared.getTypeDeclaration());

        ImmutableList<TypeVariable> typeParameters =
            toDeclared.getTypeDeclaration().getTypeParameterDescriptors();
        ImmutableList<TypeDescriptor> typeArguments = toDeclared.getTypeArgumentDescriptors();

        ImmutableList<TypeDescriptor> newTypeArguments =
            zip(
                typeParameters,
                typeArguments,
                (typeParameter, typeArgument) ->
                    propagateTypeArgumentNullabilityFromInferredType(
                        typeParameter, typeArgument, inducedSupertype, propagatedSuper));

        return toDeclared
            .withTypeArguments(newTypeArguments)
            .toNullable(toDeclared.isNullable() || fromDeclared.isNullable());
      }

      case TypeVariable fromVariable -> {
        if (fromVariable.getLowerBoundTypeDescriptor() == null) {
          return PropagateNullability.propagateNullabilityToDeclared(
              toDeclared, getNormalizedUpperBoundTypeDescriptor(fromVariable), seen);
        } else {
          return PropagateNullability.propagateNullabilityToDeclared(
              toDeclared, getNormalizedLowerBoundTypeDescriptor(fromVariable), seen);
        }
      }

      default -> {
        return toDeclared;
      }
    }
  }

  /**
   * Propagates nullability from a type descriptor to an array type descriptor.
   *
   * <p>Propagates nullability to the component type and outer nullability.
   */
  private static TypeDescriptor propagateNullabilityToArray(
      ArrayTypeDescriptor toArray, TypeDescriptor from, ImmutableSet<TypeVariable> seen) {
    return switch (from) {
      case ArrayTypeDescriptor fromArray ->
          toArray
              .withComponentTypeDescriptor(
                  propagateNullabilityTo(
                      toArray.getComponentTypeDescriptor(),
                      fromArray.getComponentTypeDescriptor(),
                      seen))
              .toNullable(toArray.isNullable() || fromArray.isNullable());

      case TypeVariable fromVariable ->
          PropagateNullability.propagateNullabilityToArray(
              toArray, getNormalizedUpperBoundTypeDescriptor(fromVariable), seen);

      default -> toArray;
    };
  }

  /**
   * Propagates nullability from a type descriptor to a type variable.
   *
   * <p>Propagates nullability to the bounds if it is a wildcard, or to the variable itself.
   */
  private static TypeDescriptor propagateNullabilityToVariable(
      TypeVariable toVariable, TypeDescriptor from, ImmutableSet<TypeVariable> seen) {
    if (seen.contains(toVariable)) {
      return toVariable;
    }

    ImmutableSet<TypeVariable> newSeen =
        ImmutableSet.<TypeVariable>builder().addAll(seen).add(toVariable).build();

    if (from instanceof TypeVariable fromVariable) {
      if (toVariable.isWildcard()) {
        return propagateNullabilityAnnotationFrom(
            toVariable.withRewrittenBounds(
                upperBound ->
                    propagateNullabilityTo(
                        upperBound, fromVariable.getUpperBoundTypeDescriptor(), newSeen),
                lowerBound -> lowerBound),
            fromVariable);
      }
      return propagateNullabilityAnnotationFrom(toVariable, fromVariable);
    }

    if (toVariable.isWildcard()) {
      // Propagate wildcard upper bounds only.
      // TODO(b/407688032): Do we care about lower bounds?
      return toVariable.withRewrittenBounds(
          upperBound -> propagateNullabilityTo(upperBound, from, newSeen),
          lowerBound -> lowerBound);
    }

    return toVariable.toNullable(toVariable.isNullable() || from.isNullable());
  }

  /**
   * Propagates nullability annotation from one type variable to another, choosing the most nullable
   * annotation.
   */
  private static TypeDescriptor propagateNullabilityAnnotationFrom(
      TypeVariable toTypeVariable, TypeVariable fromTypeVariable) {
    return toTypeVariable.withNullabilityAnnotation(
        mostNullableOf(
            toTypeVariable.getNullabilityAnnotation(),
            fromTypeVariable.getNullabilityAnnotation()));
  }

  /**
   * Propagate nullability from all types to the given type, assuming they are already assignable
   * without nullability annotations.
   */
  private static TypeDescriptor propagateNullabilityFrom(
      TypeDescriptor typeDescriptor, Stream<TypeDescriptor> fromTypeDescriptors) {
    return fromTypeDescriptors.reduce(
        typeDescriptor, (a, b) -> propagateNullabilityTo(a, b, ImmutableSet.of()));
  }

  /**
   * Replaces parameterization for {@code typeParameterDescriptor} with {@code
   * typeArgumentDescriptor}.
   */
  private static MethodDescriptor reparameterize(
      MethodDescriptor methodDescriptor,
      TypeVariable typeParameterDescriptor,
      TypeDescriptor typeArgumentDescriptor) {
    MethodDescriptor declarationDescriptor = methodDescriptor.getDeclarationDescriptor();
    return MethodDescriptor.Builder.from(methodDescriptor)
        .setReturnTypeDescriptor(
            reparameterize(
                declarationDescriptor.getReturnTypeDescriptor(),
                methodDescriptor.getReturnTypeDescriptor(),
                typeParameterDescriptor,
                typeArgumentDescriptor,
                ImmutableSet.of()))
        .updateParameterTypeDescriptors(
            zip(
                methodDescriptor.getParameterTypeDescriptors(),
                declarationDescriptor.getParameterTypeDescriptors(),
                (parameterTypeDescriptor, declarationParameterTypeDescriptor) ->
                    reparameterize(
                        declarationParameterTypeDescriptor,
                        parameterTypeDescriptor,
                        typeParameterDescriptor,
                        typeArgumentDescriptor,
                        ImmutableSet.of())))
        .setEnclosingTypeDescriptor(
            methodDescriptor.isStatic()
                ? methodDescriptor.getEnclosingTypeDescriptor()
                : (DeclaredTypeDescriptor)
                    reparameterize(
                        declarationDescriptor.getEnclosingTypeDescriptor(),
                        methodDescriptor.getEnclosingTypeDescriptor(),
                        typeParameterDescriptor,
                        typeArgumentDescriptor,
                        ImmutableSet.of()))
        .setTypeArgumentTypeDescriptors(
            zip(
                declarationDescriptor.getTypeParameterTypeDescriptors(),
                methodDescriptor.getTypeArgumentTypeDescriptors(),
                (typeParameter, typeArgument) ->
                    typeParameter.equals(typeParameterDescriptor)
                        ? typeArgumentDescriptor
                        : typeArgument))
        .build();
  }

  /**
   * Replaces parameterization for {@code typeParameterDescriptors} with {@code
   * typeArgumentDescriptors}.
   */
  private static MethodDescriptor reparameterize(
      MethodDescriptor methodDescriptor,
      ImmutableList<TypeVariable> typeParameters,
      ImmutableList<TypeDescriptor> typeDescriptors) {
    for (int i = 0; i < typeParameters.size(); i++) {
      methodDescriptor =
          reparameterize(methodDescriptor, typeParameters.get(i), typeDescriptors.get(i));
    }
    return methodDescriptor;
  }

  /**
   * Assuming that {@code typeDescriptor} is a specialized version of this type descriptor, it
   * replaces all instances of type arguments corresponding to {@code typeParameterDescriptor} with
   * {@code typeArgumentDescriptor}.
   *
   * <p>Example:
   *
   * <ul>
   *   <li>this: {@code Map<K, V>}
   *   <li>type descriptor: {@code Map<String, Number>}
   *   <li>type parameter descriptor: {@code V}
   *   <li>type argument descriptor: {@code @Nullable Number}
   *   <li>result: {@code Map<String, @Nullable Number>}
   * </ul>
   *
   * @param typeDescriptor type descriptor to re-parameterize
   * @param typeParameterDescriptor type parameter descriptor to replace argument for
   * @param typeArgumentDescriptor type argument descriptor to replace with
   * @return re-parameterized type descriptor
   */
  private static TypeDescriptor reparameterize(
      TypeDescriptor declarationTypeDescriptor,
      TypeDescriptor typeDescriptor,
      TypeVariable typeParameterDescriptor,
      TypeDescriptor typeArgumentDescriptor,
      ImmutableSet<TypeVariable> seen) {
    switch (declarationTypeDescriptor) {
      case PrimitiveTypeDescriptor primitiveTypeDescriptor -> {
        return typeDescriptor;
      }

      case ArrayTypeDescriptor declarationArrayTypeDescriptor -> {
        ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
        return arrayTypeDescriptor.withComponentTypeDescriptor(
            reparameterize(
                declarationArrayTypeDescriptor.getComponentTypeDescriptor(),
                arrayTypeDescriptor.getComponentTypeDescriptor(),
                typeParameterDescriptor,
                typeArgumentDescriptor,
                seen));
      }

      case DeclaredTypeDescriptor declarationDeclaredTypeDescriptor -> {
        DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;
        if (declaredTypeDescriptor.isRaw()) {
          return typeDescriptor;
        }
        return declaredTypeDescriptor.withTypeArguments(
            zip(
                declarationDeclaredTypeDescriptor.getTypeArgumentDescriptors(),
                declaredTypeDescriptor.getTypeArgumentDescriptors(),
                (declaration, specialized) ->
                    reparameterize(
                        declaration,
                        specialized,
                        typeParameterDescriptor,
                        typeArgumentDescriptor,
                        seen)));
      }

      case TypeVariable declarationTypeVariable -> {
        if (typeDescriptor.equals(typeParameterDescriptor)) {
          return typeDescriptor;
        }

        if (!declarationTypeVariable.isWildcardOrCapture()) {
          if (declarationTypeVariable.toDeclaration().equals(typeParameterDescriptor)) {
            return typeArgumentDescriptor.withNullabilityAnnotation(
                declarationTypeVariable.getNullabilityAnnotation());
          } else {
            return typeDescriptor;
          }
        }

        if (typeDescriptor instanceof TypeVariable typeVariable) {
          // We only reparameterize wildcards.
          if (typeVariable.isWildcard()) {
            if (seen.contains(typeVariable)) {
              return typeVariable;
            }
            ImmutableSet<TypeVariable> newSeen =
                new ImmutableSet.Builder<TypeVariable>().addAll(seen).add(typeVariable).build();
            return typeVariable.withRewrittenBounds(
                upperBound ->
                    reparameterize(
                        declarationTypeVariable.getUpperBoundTypeDescriptor(),
                        upperBound,
                        typeParameterDescriptor,
                        typeArgumentDescriptor,
                        newSeen),
                lowerBound ->
                    reparameterize(
                        declarationTypeVariable.getLowerBoundTypeDescriptor(),
                        lowerBound,
                        typeParameterDescriptor,
                        typeArgumentDescriptor,
                        newSeen));
          }
        }
        return typeDescriptor;
      }

      case IntersectionTypeDescriptor declarationIntersectionTypeDescriptor -> {
        IntersectionTypeDescriptor intersectionTypeDescriptor =
            (IntersectionTypeDescriptor) typeDescriptor;
        return IntersectionTypeDescriptor.newBuilder()
            .setIntersectionTypeDescriptors(
                zip(
                    declarationIntersectionTypeDescriptor.getIntersectionTypeDescriptors(),
                    intersectionTypeDescriptor.getIntersectionTypeDescriptors(),
                    (declaration, specialized) ->
                        reparameterize(
                            declaration,
                            specialized,
                            typeParameterDescriptor,
                            typeArgumentDescriptor,
                            seen)))
            .build();
      }

      case UnionTypeDescriptor declarationUnionTypeDescriptor -> {
        UnionTypeDescriptor unionTypeDescriptor = (UnionTypeDescriptor) typeDescriptor;
        return UnionTypeDescriptor.newBuilder()
            .setUnionTypeDescriptors(
                zip(
                    declarationUnionTypeDescriptor.getUnionTypeDescriptors(),
                    unionTypeDescriptor.getUnionTypeDescriptors(),
                    (declaration, specialized) ->
                        reparameterize(
                            declaration,
                            specialized,
                            typeParameterDescriptor,
                            typeArgumentDescriptor,
                            seen)))
            .build();
      }

      default -> throw new AssertionError();
    }
  }

  private static final Ordering<NullabilityAnnotation> NULLABILITY_ANNOTATION_ORDERING =
      Ordering.explicit(
          NullabilityAnnotation.NOT_NULLABLE,
          NullabilityAnnotation.NONE,
          NullabilityAnnotation.NULLABLE);

  private static NullabilityAnnotation mostNullableOf(
      NullabilityAnnotation first, NullabilityAnnotation second) {
    return NULLABILITY_ANNOTATION_ORDERING.max(first, second);
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
