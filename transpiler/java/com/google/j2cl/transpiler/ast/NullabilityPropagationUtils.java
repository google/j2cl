/*
 * Copyright 2026 Google Inc.
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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.j2cl.transpiler.ast.NullabilityAnnotation.mostNullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/** Utility methods for propagating nullability in type descriptors. */
public final class NullabilityPropagationUtils {

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
  public static MethodDescriptor propagateNullabilityFromArguments(
      MethodDescriptor methodDescriptor,
      ImmutableList<TypeVariable> typeParameterDescriptors,
      ImmutableList<TypeDescriptor> typeArgumentDescriptors,
      ImmutableList<TypeDescriptor> argumentTypeDescriptors) {
    ImmutableList<TypeDescriptor> parameterTypeDescriptors =
        methodDescriptor.getDeclarationDescriptor().getParameterTypeDescriptors();
    ImmutableList<TypeDescriptor> inferredTypeArgumentDescriptors =
        zip(
            typeParameterDescriptors,
            typeArgumentDescriptors,
            (typeParameterDescriptor, typeArgumentDescriptor) ->
                propagateTypeArgumentNullabilityFromInferredTypes(
                    typeParameterDescriptor,
                    typeArgumentDescriptor,
                    parameterTypeDescriptors,
                    argumentTypeDescriptors));

    return reparameterize(
        methodDescriptor, typeParameterDescriptors, inferredTypeArgumentDescriptors);
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
  public static MethodDescriptor propagateNullabilityFromQualifier(
      MethodDescriptor methodDescriptor,
      ImmutableList<TypeVariable> typeParameterDescriptors,
      ImmutableList<TypeDescriptor> typeArgumentDescriptors,
      TypeDescriptor qualifierTypeDescriptor) {
    TypeDescriptor declaredTypeDescriptor =
        methodDescriptor.getDeclarationDescriptor().getEnclosingTypeDescriptor();
    ImmutableList<TypeDescriptor> inferredTypeArgumentDescriptors =
        zip(
            typeParameterDescriptors,
            typeArgumentDescriptors,
            (typeParameterDescriptor, typeArgumentDescriptor) ->
                propagateTypeArgumentNullabilityFromInferredType(
                    typeParameterDescriptor,
                    typeArgumentDescriptor,
                    declaredTypeDescriptor,
                    qualifierTypeDescriptor));

    return reparameterize(
        methodDescriptor, typeParameterDescriptors, inferredTypeArgumentDescriptors);
  }

  /**
   * Propagates nullability to a type argument from a list of inferred types.
   *
   * <p>Iterates over corresponding declaration and inferred types (e.g., parameter types and actual
   * argument types) and propagates nullability to the type argument.
   */
  public static TypeDescriptor propagateTypeArgumentNullabilityFromInferredTypes(
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
  public static TypeDescriptor propagateTypeArgumentNullabilityFromInferredType(
      TypeVariable typeParameterDescriptor,
      TypeDescriptor typeArgumentDescriptor,
      TypeDescriptor declarationTypeDescriptor,
      TypeDescriptor inferredTypeDescriptor) {
    return declarationTypeDescriptor
        .getParameterizationsIn(typeParameterDescriptor, inferredTypeDescriptor)
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
  public static <T extends TypeDescriptor> T propagateNullabilityTo(T to, TypeDescriptor from) {
    return propagateNullabilityTo(to, from, ImmutableSet.of());
  }

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
          return propagateNullabilityToDeclared(
              toDeclared, fromVariable.getUpperBoundTypeDescriptorWithAppliedNullability(), seen);
        } else {
          return propagateNullabilityToDeclared(
              toDeclared, fromVariable.getLowerBoundTypeDescriptorWithAppliedNullability(), seen);
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
          propagateNullabilityToArray(
              toArray, fromVariable.getUpperBoundTypeDescriptorWithAppliedNullability(), seen);

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
        mostNullable(
            toTypeVariable.getNullabilityAnnotation(),
            fromTypeVariable.getNullabilityAnnotation()));
  }

  /**
   * Propagate nullability from all types to the given type, assuming they are already assignable
   * without nullability annotations.
   */
  public static TypeDescriptor propagateNullabilityFrom(
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
    return methodDescriptor.toBuilder()
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
        return IntersectionTypeDescriptor.builder()
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
        return UnionTypeDescriptor.builder()
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

  private static <A, B, R> ImmutableList<R> zip(
      List<A> listA, List<B> listB, BiFunction<? super A, ? super B, ? extends R> function) {
    checkArgument(
        listA.size() == listB.size(),
        "Lists are of different sizes (%s, %s)",
        listA.size(),
        listB.size());
    ImmutableList.Builder<R> builder = ImmutableList.builder();
    for (int i = 0; i < listA.size(); i++) {
      builder.add(function.apply(listA.get(i), listB.get(i)));
    }
    return builder.build();
  }

  private static <A, B, C, R> ImmutableList<R> zip(
      List<A> listA,
      List<B> listB,
      List<C> listC,
      TriFunction<? super A, ? super B, ? super C, ? extends R> function) {
    checkArgument(
        listA.size() == listB.size() && listA.size() == listC.size(),
        "Lists are of different sizes (%s, %s, %s)",
        listA.size(),
        listB.size(),
        listC.size());
    ImmutableList.Builder<R> builder = ImmutableList.builder();
    for (int i = 0; i < listA.size(); i++) {
      builder.add(function.apply(listA.get(i), listB.get(i), listC.get(i)));
    }
    return builder.build();
  }

  /** A functional interface that accepts three arguments and produces a result. */
  private interface TriFunction<A, B, C, R> {
    R apply(A a, B b, C c);
  }

  private NullabilityPropagationUtils() {}
}
