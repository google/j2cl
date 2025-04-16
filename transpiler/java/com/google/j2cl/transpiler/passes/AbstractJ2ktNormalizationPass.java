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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.FormatMethod;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.HasSourcePosition;
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.NullabilityAnnotation;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import javax.annotation.Nullable;

/**
 * Abstract base class for Kotlin passes, providing shared functionality:
 *
 * <ul>
 *   <li>verbose type descriptions,
 * </ul>
 */
public abstract class AbstractJ2ktNormalizationPass extends NormalizationPass {
  // Prevent overriding and force using {@code applyTo(CompilationUnit)}.
  @Override
  public final void applyTo(Type type) {}

  @FormatMethod
  final void debug(SourcePosition sourcePosition, String detailMessage, Object... args) {
    getProblems().info(sourcePosition, detailMessage, args);
  }

  @FormatMethod
  final void debug(String detailMessage, Object... args) {
    debug(SourcePosition.NONE, detailMessage, args);
  }

  /** Returns the closest meaningful source position from an enclosing node. */
  final SourcePosition getSourcePosition(AbstractRewriter rewriter) {
    HasSourcePosition hasSourcePosition =
        (HasSourcePosition) rewriter.getParent(HasSourcePosition.class::isInstance);
    return hasSourcePosition != null ? hasSourcePosition.getSourcePosition() : SourcePosition.NONE;
  }

  private static Describer newDescriber() {
    return new Describer();
  }

  static String getDescription(TypeDescriptor typeDescriptor) {
    return newDescriber().getDescription(typeDescriptor, ImmutableList.of());
  }

  static String getDescription(TypeDeclaration typeDeclaration) {
    return newDescriber().getDescription(typeDeclaration);
  }

  static String getDescription(MethodDescriptor methodDescriptor) {
    return newDescriber().getDescription(methodDescriptor);
  }

  static String getTypeParameterDescription(TypeVariable typeVariable) {
    return newDescriber().getTypeParameterDescription(typeVariable);
  }

  /**
   * Converts captures to wildcards if they appear as a type argument, or project them to bounds if
   * they appear at the top-level (non as type argument).
   *
   * <ul>
   *   <li>{@code Foo<capture-of ? extends V>} -> {@code Foo<? extends V>}
   *   <li>{@code Foo<capture-of ? super V>} -> {@code Foo<? extends V>}
   *   <li>{@code capture-of ? extends V} -> {@code V}
   *   <li>{@code capture-of ? super V} -> {@code V}
   *   <li>{@code Foo<? extends V>} -> {@code Foo<? extends V>}
   *   <li>{@code Foo<? super V>} -> {@code Foo<? super V>}
   *   <li>{@code ? extends V} -> {@code V}
   *   <li>{@code ? super V} -> {@code V}
   * </ul>
   */
  static TypeDescriptor projectCaptures(TypeDescriptor typeDescriptor) {
    return projectCaptures(typeDescriptor, /* isTypeArgument= */ false, ImmutableSet.of());
  }

  private static TypeDescriptor projectCaptures(
      TypeDescriptor typeDescriptor, boolean isTypeArgument, ImmutableSet<TypeVariable> seen) {
    if (typeDescriptor instanceof PrimitiveTypeDescriptor) {
      return typeDescriptor;
    } else if (typeDescriptor instanceof ArrayTypeDescriptor) {
      ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
      return arrayTypeDescriptor.withComponentTypeDescriptor(
          projectArgumentCaptures(
              arrayTypeDescriptor.getComponentTypeDescriptor(),
              getArrayComponentTypeParameterDescriptor(),
              seen));
    } else if (typeDescriptor instanceof DeclaredTypeDescriptor) {
      DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;
      return declaredTypeDescriptor.withTypeArguments(
          zip(
              declaredTypeDescriptor.getTypeDeclaration().getTypeParameterDescriptors(),
              declaredTypeDescriptor.getTypeArgumentDescriptors(),
              (typeParameter, typeArgument) ->
                  projectArgumentCaptures(typeArgument, typeParameter, seen)));
    } else if (typeDescriptor instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) typeDescriptor;
      if (!typeVariable.isWildcardOrCapture()) {
        return typeVariable;
      } else {
        if (seen.contains(typeVariable)) {
          return typeVariable;
        }
        ImmutableSet<TypeVariable> newSeen =
            ImmutableSet.<TypeVariable>builder().addAll(seen).add(typeVariable).build();

        if (!isTypeArgument) {
          TypeDescriptor lowerBound = getNormalizedLowerBoundTypeDescriptor(typeVariable);
          if (lowerBound != null) {
            return projectCaptures(lowerBound, /* isTypeArgument= */ false, newSeen);
          } else {
            TypeDescriptor upperBound = getNormalizedUpperBoundTypeDescriptor(typeVariable);
            return projectCaptures(upperBound, /* isTypeArgument= */ false, newSeen);
          }
        } else {
          return typeVariable
              .toWildcard()
              .withRewrittenBounds(it -> projectCaptures(it, /* isTypeArgument= */ false, newSeen));
        }
      }
    } else if (typeDescriptor instanceof IntersectionTypeDescriptor) {
      IntersectionTypeDescriptor intersectionTypeDescriptor =
          (IntersectionTypeDescriptor) typeDescriptor;
      return IntersectionTypeDescriptor.newBuilder()
          .setIntersectionTypeDescriptors(
              intersectionTypeDescriptor.getIntersectionTypeDescriptors().stream()
                  .map(it -> projectCaptures(it, /* isTypeArgument= */ false, seen))
                  .collect(toImmutableList()))
          .build();
    } else if (typeDescriptor instanceof UnionTypeDescriptor) {
      UnionTypeDescriptor unionTypeDescriptor = (UnionTypeDescriptor) typeDescriptor;
      return UnionTypeDescriptor.newBuilder()
          .setUnionTypeDescriptors(
              unionTypeDescriptor.getUnionTypeDescriptors().stream()
                  .map(it -> projectCaptures(it, /* isTypeArgument= */ false, seen))
                  .collect(toImmutableList()))
          .build();
    } else {
      throw new AssertionError();
    }
  }

  private static TypeDescriptor projectArgumentCaptures(
      TypeDescriptor argumentTypeDescriptor,
      TypeVariable typeParameter,
      ImmutableSet<TypeVariable> seen) {
    argumentTypeDescriptor =
        getTypeArgumentDescriptorWithValidNullability(argumentTypeDescriptor, typeParameter);

    // Don't project unbound wildcards with recursive declaration, as it'll result in recursive type
    // parameter appearing in the projection. Without this check, given the
    // {@code Foo<T extends Foo<T>>} declaration, the projection of {@code Foo<?>} would appear as
    // {@code Foo<Foo<T>>}.
    if (isUnboundWildcardWithRecursiveDeclaration(argumentTypeDescriptor, typeParameter)) {
      return argumentTypeDescriptor;
    }

    return argumentTypeDescriptor.isWildcardOrCapture()
        ? projectCaptures(argumentTypeDescriptor, /* isTypeArgument= */ true, seen)
        : argumentTypeDescriptor;
  }

  static boolean isUnboundWildcardWithRecursiveDeclaration(
      TypeDescriptor typeDescriptor, TypeVariable typeParameter) {
    if (typeDescriptor instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) typeDescriptor;
      return typeParameter.hasRecursiveDefinition()
          && typeVariable.isWildcardOrCapture()
          && typeVariable
              .getUpperBoundTypeDescriptor()
              .toNullable()
              .equals(typeParameter.getUpperBoundTypeDescriptor().toNullable());
    }
    return false;
  }

  /** Returns synthetic type parameter for kotlin.Array class. */
  static TypeVariable getArrayComponentTypeParameterDescriptor() {
    return TypeVariable.newBuilder()
        .setName("T")
        .setUniqueKey("kotlin.Array:T")
        .setUpperBoundTypeDescriptorFactory(() -> TypeDescriptors.get().javaLangObject)
        .build();
  }

  /** Returns type argument descriptors with nullability which satisfy their declarations. */
  static ImmutableList<TypeDescriptor> getTypeArgumentDescriptorsWithValidNullability(
      DeclaredTypeDescriptor typeArgumentDescriptor) {
    return typeArgumentDescriptor.isRaw()
        ? ImmutableList.of()
        : zip(
            typeArgumentDescriptor.getTypeArgumentDescriptors(),
            typeArgumentDescriptor.getTypeDeclaration().getTypeParameterDescriptors(),
            AbstractJ2ktNormalizationPass::getTypeArgumentDescriptorWithValidNullability);
  }

  /** Returns type argument descriptor with nullability which satisfies its declaration. */
  static TypeDescriptor getTypeArgumentDescriptorWithValidNullability(
      TypeDescriptor typeArgumentDescriptor, TypeVariable typeParameterDescriptor) {
    return !typeParameterDescriptor.canBeNull() && typeArgumentDescriptor.canBeNull()
        ? typeArgumentDescriptor.toNonNullable()
        : typeArgumentDescriptor;
  }

  /**
   * Returns type descriptor with removed redundant nullability annotation - in Kotlin, NOT_NULLABLE
   * is valid only for type variables with nullable bounds.
   */
  static TypeDescriptor removeRedundantNullabilityAnnotation(TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) typeDescriptor;
      if (typeVariable.getNullabilityAnnotation() == NullabilityAnnotation.NOT_NULLABLE
          && !typeVariable.getUpperBoundTypeDescriptor().canBeNull()) {
        return TypeVariable.Builder.from(typeVariable)
            .setNullabilityAnnotation(NullabilityAnnotation.NONE)
            .build();
      }
    }
    return typeDescriptor;
  }

  static TypeDescriptor getNormalizedUpperBoundTypeDescriptor(TypeVariable typeVariable) {
    return typeVariable
        .getUpperBoundTypeDescriptor()
        .withNullabilityAnnotation(typeVariable.getNullabilityAnnotation());
  }

  @Nullable
  static TypeDescriptor getNormalizedLowerBoundTypeDescriptor(TypeVariable typeVariable) {
    TypeDescriptor lowerBound = typeVariable.getLowerBoundTypeDescriptor();
    if (lowerBound != null) {
      return lowerBound.withNullabilityAnnotation(typeVariable.getNullabilityAnnotation());
    }
    return null;
  }

  @Nullable
  static TypeDescriptor getNormalizedLowerBoundTypeDescriptor(TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) typeDescriptor;
      if (typeVariable.isWildcardOrCapture()) {
        return getNormalizedLowerBoundTypeDescriptor(typeVariable);
      }
    }
    return null;
  }

  /**
   * Produces readable description string for AST nodes, containing information about resolved
   * nullability and captures with unique IDs. Used in Kotlin passes for debugging / development.
   */
  // TODO(b/382500942): Remove when no longer needed for debugging / development.
  private static final class Describer {
    private final List<TypeVariable> seenCaptures = new ArrayList<>();

    private Describer() {}

    private String getDescription(TypeDescriptor typeDescriptor) {
      return getDescription(typeDescriptor, ImmutableList.of());
    }

    private String getDescription(TypeDeclaration typeDeclaration) {
      return typeDeclaration.getReadableDescription()
          + getTypeParametersDescription(typeDeclaration.getTypeParameterDescriptors());
    }

    @Nullable
    private static String getDescription(NullabilityAnnotation nullabilityAnnotation) {
      switch (nullabilityAnnotation) {
        case NULLABLE:
          return "@Nullable";
        case NONE:
          return null;
        case NOT_NULLABLE:
          return "@NonNull";
      }
      throw new AssertionError();
    }

    private String getDescription(MethodDescriptor methodDescriptor) {
      return String.format(
          "%s%s (%s).%s%s%s",
          appendSpaceIfNotEmpty(
              getTypeParametersDescription(methodDescriptor.getTypeParameterTypeDescriptors())),
          getDescription(methodDescriptor.getReturnTypeDescriptor()),
          getDescription(methodDescriptor.getEnclosingTypeDescriptor()),
          getTypeArgumentsDescription(methodDescriptor.getTypeArgumentTypeDescriptors()),
          methodDescriptor.getName(),
          getParametersDescription(methodDescriptor.getParameterDescriptors()));
    }

    private String getDescription(ParameterDescriptor parameterDescriptor) {
      TypeDescriptor typeDescriptor = parameterDescriptor.getTypeDescriptor();
      if (parameterDescriptor.isVarargs()) {
        ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
        return getDescription(arrayTypeDescriptor.getComponentTypeDescriptor()) + "...";
      } else {
        return getDescription(parameterDescriptor.getTypeDescriptor());
      }
    }

    private String getDescription(
        TypeDescriptor typeDescriptor, ImmutableList<TypeVariable> enclosingWildcardOrCaptures) {
      if (typeDescriptor instanceof PrimitiveTypeDescriptor) {
        PrimitiveTypeDescriptor primitiveTypeDescriptor = (PrimitiveTypeDescriptor) typeDescriptor;
        return primitiveTypeDescriptor.getSimpleSourceName();
      } else if (typeDescriptor instanceof ArrayTypeDescriptor) {
        ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
        return getDescription(
                arrayTypeDescriptor.getComponentTypeDescriptor(), enclosingWildcardOrCaptures)
            + getDescriptionInfix(getNullabilityAnnotation(typeDescriptor))
            + "[]";
      } else if (typeDescriptor instanceof DeclaredTypeDescriptor) {
        DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;
        return getDescriptionPrefix(getNullabilityAnnotation(typeDescriptor))
            + declaredTypeDescriptor.getTypeDeclaration().getReadableDescription()
            + getTypeArgumentsDescription(declaredTypeDescriptor, enclosingWildcardOrCaptures);
      } else if (typeDescriptor instanceof TypeVariable) {
        TypeVariable typeVariable = (TypeVariable) typeDescriptor;
        return getDescriptionPrefix(getNullabilityAnnotation(typeDescriptor))
            + getDescriptionWithoutNullabilityAnnotation(typeVariable, enclosingWildcardOrCaptures);
      } else if (typeDescriptor instanceof IntersectionTypeDescriptor) {
        IntersectionTypeDescriptor intersectionTypeDescriptor =
            (IntersectionTypeDescriptor) typeDescriptor;
        return intersectionTypeDescriptor.getIntersectionTypeDescriptors().stream()
            .map(it -> getDescription(it, enclosingWildcardOrCaptures))
            .collect(joining(" & ", "(", ")"));
      } else if (typeDescriptor instanceof UnionTypeDescriptor) {
        UnionTypeDescriptor unionTypeDescriptor = (UnionTypeDescriptor) typeDescriptor;
        return unionTypeDescriptor.getUnionTypeDescriptors().stream()
            .map(it -> getDescription(it, enclosingWildcardOrCaptures))
            .collect(joining(" | ", "(", ")"));
      } else {
        throw new AssertionError();
      }
    }

    private String getParametersDescription(
        ImmutableList<ParameterDescriptor> parameterDescriptors) {
      return parameterDescriptors.stream()
          .map(this::getDescription)
          .collect(joining(", ", "(", ")"));
    }

    private String getTypeParameterDescription(TypeVariable typeVariable) {
      return typeVariable.getName()
          + " extends "
          + getDescription(typeVariable.getUpperBoundTypeDescriptor());
    }

    private String getTypeParametersDescription(ImmutableList<TypeVariable> typeParameters) {
      return inAngleBracketsIfNotEmpty(
          typeParameters.stream().map(this::getTypeParameterDescription).collect(joining(", ")));
    }

    private String getTypeArgumentsDescription(ImmutableList<TypeDescriptor> typeArguments) {
      return inAngleBracketsIfNotEmpty(
          typeArguments.stream().map(this::getDescription).collect(joining(", ")));
    }

    private String getTypeArgumentsDescription(
        DeclaredTypeDescriptor declaredTypeDescriptor,
        ImmutableList<TypeVariable> enclosingWildcardOrCaptures) {
      ImmutableList<TypeDescriptor> arguments = declaredTypeDescriptor.getTypeArgumentDescriptors();
      return inAngleBracketsIfNotEmpty(
          arguments.stream()
              .map(it -> getDescription(it, enclosingWildcardOrCaptures))
              .collect(joining(", ")));
    }

    private String getDescriptionWithoutNullabilityAnnotation(
        TypeVariable typeVariable, ImmutableList<TypeVariable> enclosingWildcardOrCapture) {
      if (!typeVariable.isWildcardOrCapture()) {
        return typeVariable.getName();
      } else {
        return getCaptureDescription(typeVariable)
            + getBoundDescription(typeVariable, enclosingWildcardOrCapture);
      }
    }

    private String getCaptureDescription(TypeVariable typeVariable) {
      if (!typeVariable.isCapture()) {
        return "";
      }

      int index = seenCaptures.indexOf(typeVariable);
      if (index == -1) {
        index = seenCaptures.size();
        seenCaptures.add(typeVariable);
      }
      return "capture#" + (index + 1) + "-of ";
    }

    private String getBoundDescription(
        TypeVariable typeVariable, ImmutableList<TypeVariable> enclosingWildcardOrCaptures) {
      int index = enclosingWildcardOrCaptures.indexOf(typeVariable);
      if (index != -1) {
        return "rec#" + (enclosingWildcardOrCaptures.size() - index);
      }
      enclosingWildcardOrCaptures =
          ImmutableList.<TypeVariable>builder()
              .addAll(enclosingWildcardOrCaptures)
              .add(typeVariable)
              .build();
      TypeDescriptor lowerBound = typeVariable.getLowerBoundTypeDescriptor();
      if (lowerBound != null) {
        return "? super " + getDescription(lowerBound, enclosingWildcardOrCaptures);
      } else {
        return "? extends "
            + getDescription(
                typeVariable.getUpperBoundTypeDescriptor(), enclosingWildcardOrCaptures);
      }
    }

    private static NullabilityAnnotation getNullabilityAnnotation(TypeDescriptor typeDescriptor) {
      if (typeDescriptor instanceof TypeVariable) {
        TypeVariable typeVariable = (TypeVariable) typeDescriptor;
        return typeVariable.getNullabilityAnnotation();
      } else {
        return typeDescriptor.isNullable()
            ? NullabilityAnnotation.NULLABLE
            : NullabilityAnnotation.NOT_NULLABLE;
      }
    }

    private static String getDescriptionPrefix(NullabilityAnnotation nullabilityAnnotation) {
      String description = getDescription(nullabilityAnnotation);
      return description == null ? "" : description + " ";
    }

    private static String getDescriptionInfix(NullabilityAnnotation nullabilityAnnotation) {
      String description = getDescription(nullabilityAnnotation);
      return description == null ? "" : " " + description + " ";
    }
  }

  private static String appendSpaceIfNotEmpty(String string) {
    return string.isEmpty() ? string : string + " ";
  }

  private static String inAngleBracketsIfNotEmpty(String string) {
    return string.isEmpty() ? string : "<" + string + ">";
  }

  static <A, B, R> ImmutableList<R> zip(
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

  static <A, B, C, R> ImmutableList<R> zip(
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

  interface TriFunction<A, B, C, R> {
    R apply(A a, B b, C c);
  }
}
