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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodLike;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NullabilityAnnotation;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import java.util.List;
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

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteArrayLiteral(ArrayLiteral arrayLiteral) {
            return propagateNullabilityFromValueExpressions(arrayLiteral);
          }

          @Override
          public Node rewriteNewArray(NewArray newArray) {
            Expression initializer = newArray.getInitializer();
            if (initializer == null) {
              return newArray;
            }
            // Update type of NewArray expression from rewritten initializer.
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
                  methodDescriptor.getEnclosingTypeDescriptor().getTypeArgumentDescriptors();
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

            return Invocation.Builder.from(methodCall).setTarget(rewrittenMethodDescriptor).build();
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
                typeDescriptor.getTypeArgumentDescriptors();
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

            return NewInstance.Builder.from(newInstance).setTarget(fixedMethodDescriptor).build();
          }

          @Override
          public Node rewriteFunctionExpression(FunctionExpression functionExpression) {
            DeclaredTypeDescriptor functionalInterface =
                functionExpression.getTypeDescriptor().getFunctionalInterface();
            if (functionalInterface.isRaw()) {
              return functionExpression;
            }
            ImmutableList<TypeVariable> typeParameterDescriptors =
                functionalInterface.getTypeDeclaration().getTypeParameterDescriptors();
            ImmutableList<TypeDescriptor> typeArgumentDescriptors =
                functionalInterface.getTypeArgumentDescriptors();
            ImmutableList<TypeDescriptor> inferredTypeArgumentDescriptors =
                zip(
                    typeParameterDescriptors,
                    typeArgumentDescriptors,
                    (typeParameterDescriptor, typeArgumentDescriptor) ->
                        propagateTypeArgumentNullabilityFromReturnExpressions(
                            typeParameterDescriptor, typeArgumentDescriptor, functionExpression));
            DeclaredTypeDescriptor inferredFunctionalInterface =
                functionalInterface.withTypeArguments(inferredTypeArgumentDescriptors);
            if (inferredFunctionalInterface.equals(functionalInterface)) {
              return functionExpression;
            }
            Streams.forEachPair(
                functionExpression.getParameters().stream(),
                inferredFunctionalInterface
                    .getSingleAbstractMethodDescriptor()
                    .getParameterTypeDescriptors()
                    .stream(),
                Variable::setTypeDescriptor);
            return FunctionExpression.Builder.from(functionExpression)
                .setTypeDescriptor(inferredFunctionalInterface)
                .build();
          }
        });
  }

  /**
   * Returns all the different type assignments to {@code typeParameterTypeDescriptor} from the
   * declaration {@code declarationTypeDescriptor} as parameterized in {@code typeDescriptor}.
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
  private Stream<TypeDescriptor> getParameterizationsIn(
      TypeDescriptor declarationTypeDescriptor,
      TypeVariable typeParameter,
      TypeDescriptor typeDescriptor) {
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
                    arrayTypeDescriptor.getComponentTypeDescriptor());

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
                            getParameterizationsIn(typeArgument, typeParameter, targetTypeArgument))
                    .flatMap(it -> it);

            case TypeVariable typeVariable ->
                getParameterizationsIn(
                    declarationTypeDescriptor,
                    typeParameter,
                    getNormalizedUpperBoundTypeDescriptor(typeVariable));

            case IntersectionTypeDescriptor intersectionTypeDescriptor ->
                intersectionTypeDescriptor.getIntersectionTypeDescriptors().stream()
                    .filter(it -> it.isAssignableTo(declarationTypeDescriptor))
                    .flatMap(
                        it -> getParameterizationsIn(declarationTypeDescriptor, typeParameter, it));

            // For a union to be assignable to a type, all of its components have to be assignable
            // to that type, so collect these parameterizations from all the types in the union
            case UnionTypeDescriptor unionTypeDescriptor ->
                unionTypeDescriptor.getUnionTypeDescriptors().stream()
                    .flatMap(
                        it -> getParameterizationsIn(declarationTypeDescriptor, typeParameter, it));

            default -> throw new AssertionError();
          };

      case TypeVariable declarationTypeVariable
          when declarationTypeVariable.isWildcardOrCapture() ->
          getParameterizationsIn(
              getNormalizedUpperBoundTypeDescriptor(declarationTypeVariable),
              typeParameter,
              typeDescriptor instanceof TypeVariable typeVariable
                      && typeVariable.isWildcardOrCapture()
                  ? getNormalizedUpperBoundTypeDescriptor(typeVariable)
                  : typeDescriptor);

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
              .flatMap(it -> getParameterizationsIn(it, typeParameter, typeDescriptor));

      case UnionTypeDescriptor declarationUnionTypeDescriptor ->
          declarationUnionTypeDescriptor.getUnionTypeDescriptors().stream()
              .flatMap(it -> getParameterizationsIn(it, typeParameter, typeDescriptor));

      default -> throw new AssertionError();
    };
  }

  private ArrayLiteral propagateNullabilityFromValueExpressions(ArrayLiteral arrayLiteral) {
    ArrayTypeDescriptor arrayTypeDescriptor = arrayLiteral.getTypeDescriptor();
    TypeDescriptor componentTypeDescriptor =
        propagateNullabilityFrom(
            arrayTypeDescriptor.getComponentTypeDescriptor(),
            arrayLiteral.getValueExpressions().stream().map(Expression::getTypeDescriptor));
    return arrayLiteral.toBuilder()
        .setTypeDescriptor(arrayTypeDescriptor.withComponentTypeDescriptor(componentTypeDescriptor))
        .build();
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
  private MethodDescriptor propagateNullabilityFromArguments(
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

  // TODO(b/406815802): Add JavaDoc
  private MethodDescriptor propagateNullabilityFromQualifier(
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

  // TODO(b/406815802): Add JavaDoc
  private TypeDescriptor propagateTypeArgumentNullabilityFromReturnExpressions(
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

            propagatedTypeArgumentDescriptorRef[0] =
                propagateTypeArgumentNullabilityFromInferredType(
                    typeParameterDescriptor,
                    propagatedTypeArgumentDescriptorRef[0],
                    methodLike.getDescriptor().getDeclarationDescriptor().getReturnTypeDescriptor(),
                    expression.getTypeDescriptor());
          }
        });
    return propagatedTypeArgumentDescriptorRef[0];
  }

  // TODO(b/406815802): Add JavaDoc
  private TypeDescriptor propagateTypeArgumentNullabilityFromInferredTypes(
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

  // TODO(b/406815802): Add JavaDoc comment.
  private TypeDescriptor propagateTypeArgumentNullabilityFromInferredType(
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

  // TODO(b/406815802): Add JavaDoc comment.
  private TypeDescriptor propagateTypeArgument(
      TypeVariable typeParameter, TypeDescriptor typeArgument) {
    return typeParameter.canBeNull() ? typeArgument : typeArgument.toNonNullable();
  }

  /**
   * Propagates nullability from {@code fromTypeDescriptor} to {@code toTypeDescriptor} assuming
   * that {@code fromTypeDescriptor} is assignable to {@code toTypeDescriptor}.
   */
  private static TypeDescriptor propagateNullabilityTo(
      TypeDescriptor to, TypeDescriptor from, ImmutableSet<TypeVariable> seen) {
    if (to.equals(from)) {
      return to;
    }

    return switch (to) {
      case DeclaredTypeDescriptor descriptor ->
          propagateNullabilityToDeclared(descriptor, from, seen);

      case ArrayTypeDescriptor descriptor -> propagateNullabilityToArray(descriptor, from, seen);

      case TypeVariable typeVariable -> propagateNullabilityToVariable(typeVariable, from, seen);

      // TODO(b/406815802): Handle intersection and union type descriptors, if necessary.
      default -> to;
    };
  }

  private static TypeDescriptor propagateNullabilityToDeclared(
      DeclaredTypeDescriptor toDeclared, TypeDescriptor from, ImmutableSet<TypeVariable> seen) {
    switch (from) {
      case DeclaredTypeDescriptor fromDeclared -> {
        // For RAW type descriptors, propagate outer nullability only without type arguments.
        if (toDeclared.isRaw()) {
          return toDeclared.toNullable(toDeclared.isNullable() || fromDeclared.isNullable());
        }

        DeclaredTypeDescriptor fromDeclaredSuper =
            checkNotNull(fromDeclared.findSupertype(toDeclared.getTypeDeclaration()));

        return toDeclared
            .withTypeArguments(
                zip(
                    toDeclared.getTypeDeclaration().getTypeParameterDescriptors(),
                    toDeclared.getTypeArgumentDescriptors(),
                    fromDeclaredSuper.getTypeArgumentDescriptors(),
                    (a, b, c) -> propagateTypeArgumentNullabilityFrom(a, b, c, seen)))
            .toNullable(toDeclared.isNullable() || fromDeclared.isNullable());
      }

      case TypeVariable fromVariable -> {
        return PropagateNullability.propagateNullabilityToDeclared(
            toDeclared, getNormalizedUpperBoundTypeDescriptor(fromVariable), seen);
      }

      default -> {
        return toDeclared;
      }
    }
  }

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
}
