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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangVoid;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor;
import com.google.j2cl.transpiler.ast.NullabilityAnnotation;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** Inserts casts in places where necessary due to nullability differences in type arguments. */
// TODO(b/392084555): Clean-up this pass, as right now it's quite messy.
public final class InsertCastsOnNullabilityMismatch extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ContextRewriter() {
              // Don't insert cast for member qualifiers, as their type is inferred.
              @Override
              public Expression rewriteMemberQualifierContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor declaredTypeDescriptor,
                  Expression expression) {
                return expression;
              }

              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor declaredTypeDescriptor,
                  Expression expression) {
                TypeDescriptor fromTypeDescriptor = expression.getTypeDescriptor();

                // A hack for Void?.
                if (isJavaLangVoid(inferredTypeDescriptor)) {
                  return expression;
                }

                if (isNullabilityAssignableTo(
                    fromTypeDescriptor, inferredTypeDescriptor, ImmutableSet.of())) {
                  return expression;
                }

                TypeDescriptor castTypeDescriptor = project(inferredTypeDescriptor);
                // Reject non-denotable
                // TODO(b/392084555): Maybe projection should project captures?
                if (!castTypeDescriptor.isDenotable()) {
                  return expression;
                }

                // Reject the cast if the cast type descriptor is equal to the original type
                // descriptor, minus non-null to nullable conversion.
                if (castTypeDescriptor.isNullable()
                    && castTypeDescriptor.equals(fromTypeDescriptor.toNullable())) {
                  return expression;
                }

                Describer describer = new Describer();
                getProblems()
                    .debug(
                        getSourcePosition(),
                        "Inserted nullability mismatch cast to '%s' because of assignment from '%s'"
                            + " to '%s'",
                        describer.getDescription(castTypeDescriptor),
                        describer.getDescription(expression.getTypeDescriptor()),
                        describer.getDescription(inferredTypeDescriptor));

                return CastExpression.newBuilder()
                    .setExpression(expression)
                    .setCastTypeDescriptor(castTypeDescriptor)
                    .build();
              }
            }));
  }

  /**
   * Assuming that the first type is already assignable to the second type, returns whether it's
   * also assignable from the nullability perspective.
   */
  private static boolean isNullabilityAssignableTo(
      TypeDescriptor fromTypeDescriptor,
      TypeDescriptor toTypeDescriptor,
      ImmutableSet<TypeVariable> seenTo) {
    TypeDescriptor from = removeRedundantNullabilityAnnotation(fromTypeDescriptor);
    TypeDescriptor to = removeRedundantNullabilityAnnotation(toTypeDescriptor);

    if (from.equals(to)) {
      return true;
    }

    // Handle conversion to wildcard or capture with lower bound.
    TypeDescriptor toLowerBound = getNormalizedLowerBoundTypeDescriptor(toTypeDescriptor);
    if (toLowerBound != null) {
      TypeDescriptor fromLowerBound = getNormalizedLowerBoundTypeDescriptor(fromTypeDescriptor);
      if (fromLowerBound != null) {
        // ? super A is assignable to ? super B if B is assignable to A
        return isNullabilityAssignableTo(fromLowerBound, toLowerBound, seenTo);
      } else {
        // Otherwise A is assignable to ? super B if B is assignable to A
        return isNullabilityAssignableTo(from, toLowerBound, seenTo);
      }
    }

    if (from instanceof PrimitiveTypeDescriptor) {
      return true;
    } else if (from instanceof ArrayTypeDescriptor) {
      ArrayTypeDescriptor fromArray = (ArrayTypeDescriptor) from;
      TypeDescriptor toTarget = getAssignableTarget(to);

      if (toTarget instanceof ArrayTypeDescriptor) {
        ArrayTypeDescriptor toArray = (ArrayTypeDescriptor) toTarget;
        return isNullableAssignableTo(from.isNullable(), toArray.isNullable())
            && isArgumentNullabilityAssignableTo(
                fromArray.getComponentTypeDescriptor(),
                toArray.getComponentTypeDescriptor(),
                seenTo);
      } else if (toTarget instanceof DeclaredTypeDescriptor) {
        // Conversion to java.lang.Object, java.lang.Serializable or java.lang.Comparable.
        return isNullableAssignableTo(from.isNullable(), to.isNullable());
      } else {
        return false;
      }
    } else if (from instanceof DeclaredTypeDescriptor) {
      DeclaredTypeDescriptor fromDeclared = (DeclaredTypeDescriptor) from;
      TypeDescriptor toTarget = getAssignableTarget(to);
      if (toTarget instanceof DeclaredTypeDescriptor) {
        DeclaredTypeDescriptor toDeclared = (DeclaredTypeDescriptor) toTarget;
        TypeDeclaration toDeclaration = toDeclared.getTypeDeclaration();
        if (!isNullableAssignableTo(fromDeclared.isNullable(), toDeclared.isNullable())) {
          return false;
        }

        // Conversion from/to raw types is always possible.
        if (fromDeclared.isRaw() || toDeclared.isRaw()) {
          return true;
        }

        DeclaredTypeDescriptor fromDeclaredBase =
            fromDeclared.getAllSuperTypesIncludingSelf().stream()
                .filter(it -> it.getTypeDeclaration().equals(toDeclaration))
                .findFirst()
                .orElse(null);
        if (fromDeclaredBase == null) {
          // For some reason we are still hitting Object -> NonObject case here. Skip these.
          return false;
        }

        return Streams.zip(
                getTypeArgumentDescriptorsWithValidNullability(fromDeclaredBase).stream(),
                getTypeArgumentDescriptorsWithValidNullability(toDeclared).stream(),
                (fromArgument, toArgument) ->
                    isArgumentNullabilityAssignableTo(fromArgument, toArgument, seenTo))
            .allMatch(Boolean::booleanValue);
      } else {
        return false;
      }
    } else if (from instanceof TypeVariable) {
      TypeVariable fromVariable = (TypeVariable) from;
      if (to instanceof TypeVariable) {
        // TypeVariable -> TypeVariable
        TypeVariable toVariable = (TypeVariable) to;
        if (!toVariable.isWildcardOrCapture()) {
          // TypeVariable -> T
          if (!fromVariable.isWildcardOrCapture()
              && fromVariable.toDeclaration().equals(toVariable.toDeclaration())) {
            // T -> T
            return isAssignableTo(
                fromVariable.getNullabilityAnnotation(), toVariable.getNullabilityAnnotation());
          } else {
            // wildcard -> T
            return isNullabilityAssignableTo(
                getNormalizedUpperBoundTypeDescriptor(fromVariable), toVariable, seenTo);
          }
        } else {
          // TypeVariable -> ? extends V
          return fromVariable.equals(toVariable);
        }
      } else {
        // TypeVariable -> not TypeVariable
        return isNullabilityAssignableTo(
            getNormalizedUpperBoundTypeDescriptor(fromVariable), to, seenTo);
      }
    } else if (from instanceof IntersectionTypeDescriptor) {
      IntersectionTypeDescriptor fromIntersection = (IntersectionTypeDescriptor) from;
      TypeDescriptor toTarget = getAssignableTarget(to);
      return fromIntersection.getIntersectionTypeDescriptors().stream()
          .anyMatch(it -> isNullabilityAssignableTo(it, toTarget, seenTo));
    } else if (from instanceof UnionTypeDescriptor) {
      UnionTypeDescriptor fromUnion = (UnionTypeDescriptor) from;
      TypeDescriptor toTarget = getAssignableTarget(to);
      return fromUnion.getUnionTypeDescriptors().stream()
          .allMatch(it -> isNullabilityAssignableTo(it, toTarget, seenTo));
    } else {
      throw new AssertionError();
    }
  }

  /**
   * Assuming that the first type argument is already assignable to the second type argument,
   * returns whether it's also assignable from the nullability perspective.
   */
  private static boolean isArgumentNullabilityAssignableTo(
      TypeDescriptor fromTypeDescriptor,
      TypeDescriptor toTypeDescriptor,
      ImmutableSet<TypeVariable> seenTo) {
    fromTypeDescriptor = removeRedundantNullabilityAnnotation(fromTypeDescriptor);
    toTypeDescriptor = removeRedundantNullabilityAnnotation(toTypeDescriptor);

    if (!isWildcardOrCapture(toTypeDescriptor)) {
      return fromTypeDescriptor.equals(toTypeDescriptor);
    } else {
      TypeVariable toVariable = (TypeVariable) toTypeDescriptor;
      if (seenTo.contains(toVariable)) {
        return true;
      }

      TypeDescriptor toLowerBound = getNormalizedLowerBoundTypeDescriptor(toVariable);
      if (toLowerBound != null) {
        if (!isWildcardOrCapture(fromTypeDescriptor)) {
          // T -> ? super V
          return isNullabilityAssignableTo(toLowerBound, fromTypeDescriptor, seenTo);
        } else {
          TypeVariable fromVariable = (TypeVariable) fromTypeDescriptor;
          TypeDescriptor fromLowerBound = getNormalizedLowerBoundTypeDescriptor(fromVariable);
          if (fromLowerBound != null) {
            // ? super T -> ? super V
            return isNullabilityAssignableTo(toLowerBound, fromLowerBound, seenTo);
          } else {
            // ? extends T -> ? super V
            return isNullabilityAssignableTo(fromVariable, toLowerBound, seenTo);
          }
        }
      } else /* toLowerBound == null */ {
        TypeDescriptor toUpperBound = getNormalizedUpperBoundTypeDescriptor(toVariable);
        ImmutableSet<TypeVariable> newSeenTo =
            ImmutableSet.<TypeVariable>builder().addAll(seenTo).add(toVariable).build();

        if (!isWildcardOrCapture(fromTypeDescriptor)) {
          // T -> ? extends V
          return isNullabilityAssignableTo(fromTypeDescriptor, toUpperBound, newSeenTo);
        } else {
          TypeVariable fromVariable = (TypeVariable) fromTypeDescriptor;
          TypeDescriptor fromUpperBound = fromVariable.getUpperBoundTypeDescriptor();
          return isNullabilityAssignableTo(fromUpperBound, toUpperBound, newSeenTo);
        }
      }
    }
  }

  /**
   * Returns the type descriptor which will be used as a target when checking for assignability:
   *
   * <ul>
   *   <li>wildcards and captures are projected to upper-bound,
   *   <li>intersection type is reduced to the first type in the intersection,
   *   <li>union types are converted to the closest common super-class.
   * </ul>
   */
  private static TypeDescriptor getAssignableTarget(TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof PrimitiveTypeDescriptor) {
      return typeDescriptor;
    } else if (typeDescriptor instanceof ArrayTypeDescriptor) {
      return typeDescriptor;
    } else if (typeDescriptor instanceof DeclaredTypeDescriptor) {
      return typeDescriptor;
    } else if (typeDescriptor instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) typeDescriptor;
      if (typeVariable.isWildcardOrCapture()) {
        return getAssignableTarget(getNormalizedUpperBoundTypeDescriptor(typeVariable));
      } else {
        return typeVariable;
      }
    } else if (typeDescriptor instanceof IntersectionTypeDescriptor) {
      IntersectionTypeDescriptor intersectionTypeDescriptor =
          (IntersectionTypeDescriptor) typeDescriptor;
      return getAssignableTarget(intersectionTypeDescriptor.getFirstType());
    } else if (typeDescriptor instanceof UnionTypeDescriptor) {
      UnionTypeDescriptor unionTypeDescriptor = (UnionTypeDescriptor) typeDescriptor;
      return getAssignableTarget(unionTypeDescriptor.getClosestCommonSuperClass());
    } else {
      throw new AssertionError();
    }
  }

  private static boolean isWildcardOrCapture(TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) typeDescriptor;
      return typeVariable.isWildcardOrCapture();
    }

    return false;
  }

  /** Projects captures and wildcards appearing at the top-level to bounds. */
  private static TypeDescriptor project(TypeDescriptor typeDescriptor) {
    return project(typeDescriptor, /* isTypeArgument= */ false, ImmutableSet.of());
  }

  private static TypeDescriptor project(
      TypeDescriptor typeDescriptor, boolean isTypeArgument, ImmutableSet<TypeVariable> seen) {
    if (typeDescriptor instanceof PrimitiveTypeDescriptor) {
      return typeDescriptor;
    } else if (typeDescriptor instanceof ArrayTypeDescriptor) {
      ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
      return ArrayTypeDescriptor.Builder.from(arrayTypeDescriptor)
          .setComponentTypeDescriptor(
              projectArgument(
                  arrayTypeDescriptor.getComponentTypeDescriptor(),
                  getArrayComponentTypeParameterDescriptor(),
                  seen))
          .build();
    } else if (typeDescriptor instanceof DeclaredTypeDescriptor) {
      DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;
      return declaredTypeDescriptor.withTypeArguments(
          Streams.zip(
                  declaredTypeDescriptor
                      .getTypeDeclaration()
                      .getTypeParameterDescriptors()
                      .stream(),
                  declaredTypeDescriptor.getTypeArgumentDescriptors().stream(),
                  (typeParameter, typeArgument) ->
                      projectArgument(typeArgument, typeParameter, seen))
              .collect(toImmutableList()));
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
          // Captures and wildcards appearing at the top level (non type arguments) are projected to
          // bounds.
          TypeDescriptor lowerBound = getNormalizedLowerBoundTypeDescriptor(typeVariable);
          if (lowerBound != null) {
            return project(lowerBound, /* isTypeArgument= */ false, newSeen);
          } else {
            TypeDescriptor upperBound = getNormalizedUpperBoundTypeDescriptor(typeVariable);
            return project(upperBound, /* isTypeArgument= */ false, newSeen);
          }
        } else {
          return typeVariable
              .toWildcard()
              .withRewrittenBounds(it -> project(it, /* isTypeArgument= */ false, newSeen));
        }
      }
    } else if (typeDescriptor instanceof IntersectionTypeDescriptor) {
      IntersectionTypeDescriptor intersectionTypeDescriptor =
          (IntersectionTypeDescriptor) typeDescriptor;
      return IntersectionTypeDescriptor.newBuilder()
          .setIntersectionTypeDescriptors(
              intersectionTypeDescriptor.getIntersectionTypeDescriptors().stream()
                  .map(it -> project(it, /* isTypeArgument= */ false, seen))
                  .collect(toImmutableList()))
          .build();
    } else if (typeDescriptor instanceof UnionTypeDescriptor) {
      UnionTypeDescriptor unionTypeDescriptor = (UnionTypeDescriptor) typeDescriptor;
      return UnionTypeDescriptor.newBuilder()
          .setUnionTypeDescriptors(
              unionTypeDescriptor.getUnionTypeDescriptors().stream()
                  .map(it -> project(it, /* isTypeArgument= */ false, seen))
                  .collect(toImmutableList()))
          .build();
    } else {
      throw new AssertionError();
    }
  }

  private static TypeDescriptor projectArgument(
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

    return isWildcardOrCapture(argumentTypeDescriptor)
        ? project(argumentTypeDescriptor, /* isTypeArgument= */ true, seen)
        : argumentTypeDescriptor;
  }

  private static boolean isUnboundWildcardWithRecursiveDeclaration(
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
  private static TypeVariable getArrayComponentTypeParameterDescriptor() {
    return TypeVariable.newBuilder()
        .setName("T")
        .setUniqueKey("kotlin.Array:T")
        .setUpperBoundTypeDescriptorFactory(() -> TypeDescriptors.get().javaLangObject)
        .build();
  }

  /** Returns type argument descriptors with nullability which satisfy their declarations. */
  private static ImmutableList<TypeDescriptor> getTypeArgumentDescriptorsWithValidNullability(
      DeclaredTypeDescriptor typeArgumentDescriptor) {
    return Streams.zip(
            typeArgumentDescriptor.getTypeArgumentDescriptors().stream(),
            typeArgumentDescriptor.getTypeDeclaration().getTypeParameterDescriptors().stream(),
            InsertCastsOnNullabilityMismatch::getTypeArgumentDescriptorWithValidNullability)
        .collect(toImmutableList());
  }

  /** Returns type argument descriptor with nullability which satisfies its declaration. */
  private static TypeDescriptor getTypeArgumentDescriptorWithValidNullability(
      TypeDescriptor typeArgumentDescriptor, TypeVariable typeParameterDescriptor) {
    return !typeParameterDescriptor.canBeNull() && typeArgumentDescriptor.canBeNull()
        ? typeArgumentDescriptor.toNonNullable()
        : typeArgumentDescriptor;
  }

  /**
   * Returns type descriptor with removed redundant nullability annotation - in Kotlin, NOT_NULLABLE
   * is valid only for type variables with nullable bounds.
   */
  private static TypeDescriptor removeRedundantNullabilityAnnotation(
      TypeDescriptor typeDescriptor) {
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

  private static TypeDescriptor getNormalizedUpperBoundTypeDescriptor(TypeVariable typeVariable) {
    return typeVariable
        .getUpperBoundTypeDescriptor()
        .withNullabilityAnnotation(typeVariable.getNullabilityAnnotation());
  }

  @Nullable
  private static TypeDescriptor getNormalizedLowerBoundTypeDescriptor(TypeVariable typeVariable) {
    TypeDescriptor lowerBound = typeVariable.getLowerBoundTypeDescriptor();
    if (lowerBound != null) {
      return lowerBound.withNullabilityAnnotation(typeVariable.getNullabilityAnnotation());
    }
    return null;
  }

  @Nullable
  private static TypeDescriptor getNormalizedLowerBoundTypeDescriptor(
      TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) typeDescriptor;
      if (typeVariable.isWildcardOrCapture()) {
        return getNormalizedLowerBoundTypeDescriptor(typeVariable);
      }
    }
    return null;
  }

  private static boolean isNullableAssignableTo(boolean fromIsNullable, boolean toIsNullable) {
    return toIsNullable || !fromIsNullable;
  }

  private static boolean isAssignableTo(NullabilityAnnotation from, NullabilityAnnotation to) {
    return getAssignabilityLevel(from) <= getAssignabilityLevel(to);
  }

  private static int getAssignabilityLevel(NullabilityAnnotation nullabilityAnnotation) {
    switch (nullabilityAnnotation) {
      case NOT_NULLABLE:
        return -1;
      case NONE:
        return 0;
      case NULLABLE:
        return 1;
    }
    throw new AssertionError();
  }

  /**
   * Produces readable description string for type descriptors, containing information about
   * resolved nullability and captures with unique IDs.
   */
  // TODO(b/382500942): Remove when no longer needed for debugging / development.
  private static final class Describer {
    private final List<TypeVariable> seenCaptures = new ArrayList<>();

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

    private String getDescription(TypeDescriptor typeDescriptor) {
      return getDescription(typeDescriptor, ImmutableList.of());
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

    private String getTypeArgumentsDescription(
        DeclaredTypeDescriptor declaredTypeDescriptor,
        ImmutableList<TypeVariable> enclosingWildcardOrCaptures) {
      ImmutableList<TypeDescriptor> arguments = declaredTypeDescriptor.getTypeArgumentDescriptors();
      if (arguments.isEmpty()) {
        return "";
      } else {
        return arguments.stream()
            .map(it -> getDescription(it, enclosingWildcardOrCaptures))
            .collect(joining(", ", "<", ">"));
      }
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
}
