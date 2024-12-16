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
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** Inserts casts in places where necessary due to nullability differences in type arguments. */
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
                if (!needsCast(fromTypeDescriptor, projectForNeedsCast(inferredTypeDescriptor))) {
                  return expression;
                }

                // Reject the cast if the cast type descriptor is equal to the original type
                // descriptor, minus non-null to nullable conversion.
                TypeDescriptor castTypeDescriptor = project(inferredTypeDescriptor);
                if (castTypeDescriptor.isNullable()
                    && castTypeDescriptor.equals(fromTypeDescriptor.toNullable())) {
                  return expression;
                }

                Describer describer = new Describer();
                getProblems()
                    .debug(
                        getSourcePosition(),
                        "Inserted nullability mismatch cast from '%s' to '%s'",
                        describer.getDescription(expression.getTypeDescriptor()),
                        describer.getDescription(inferredTypeDescriptor));

                return CastExpression.newBuilder()
                    .setExpression(expression)
                    .setCastTypeDescriptor(castTypeDescriptor)
                    .build();
              }
            }));
  }

  // TODO(b/361769898): Rewrite to improve detection of cast.
  private static boolean needsCast(TypeDescriptor from, TypeDescriptor to) {
    if (!to.isDenotable() || TypeDescriptors.isJavaLangVoid(to)) {
      return false;
    }

    if (from.isNullable() && !to.isNullable()) {
      return true;
    }

    return typeArgumentsNeedsCast(from, to);
  }

  private static boolean typeArgumentsNeedsCast(TypeDescriptor from, TypeDescriptor to) {
    return Streams.zip(
            getTypeArgumentDescriptors(from).stream(),
            getTypeArgumentDescriptors(to).stream(),
            InsertCastsOnNullabilityMismatch::typeArgumentNeedsCast)
        .anyMatch(Boolean::booleanValue);
  }

  private static boolean typeArgumentNeedsCast(TypeDescriptor from, TypeDescriptor to) {
    // Insert explicit cast from wildcard to non-wildcard, since in some cases wildcard type
    // arguments are inferred as non-wildcard in the AST.
    if (isWildcard(from) && !isWildcard(to)) {
      return true;
    }

    return from.isNullable() != to.isNullable() || typeArgumentsNeedsCast(from, to);
  }

  // TODO(b/361769898): Remove when needsCast() is rewritten.
  private static TypeDescriptor projectForNeedsCast(TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) typeDescriptor;
      if (typeVariable.isWildcardOrCapture()) {
        TypeDescriptor projected;
        TypeDescriptor lowerBound = typeVariable.getLowerBoundTypeDescriptor();
        if (lowerBound != null) {
          projected = projectForNeedsCast(lowerBound);
        } else {
          TypeDescriptor upperBound = typeVariable.getUpperBoundTypeDescriptor();
          projected = projectForNeedsCast(upperBound);
        }
        return projected.withNullabilityAnnotation(typeVariable.getNullabilityAnnotation());
      }
    }

    return typeDescriptor;
  }

  private static boolean isWildcard(TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) typeDescriptor;
      return typeVariable.isWildcardOrCapture();
    }

    return false;
  }

  private static ImmutableList<TypeDescriptor> getTypeArgumentDescriptors(
      TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof DeclaredTypeDescriptor) {
      DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;
      return declaredTypeDescriptor.getTypeArgumentDescriptors();
    }

    if (typeDescriptor instanceof ArrayTypeDescriptor) {
      ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
      return ImmutableList.of(arrayTypeDescriptor.getComponentTypeDescriptor());
    }

    return ImmutableList.of();
  }

  private static TypeDescriptor project(TypeDescriptor typeDescriptor) {
    return project(typeDescriptor, Variance.OUT, ImmutableSet.of());
  }

  private static TypeDescriptor project(
      TypeDescriptor typeDescriptor, Variance variance, ImmutableSet<TypeVariable> seen) {
    if (typeDescriptor instanceof PrimitiveTypeDescriptor) {
      return typeDescriptor;
    } else if (typeDescriptor instanceof ArrayTypeDescriptor) {
      ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
      return ArrayTypeDescriptor.Builder.from(arrayTypeDescriptor)
          .setComponentTypeDescriptor(
              projectArgument(
                  arrayTypeDescriptor.getComponentTypeDescriptor(),
                  /* typeParameter= */ null,
                  variance,
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
                      projectArgument(typeArgument, typeParameter, variance, seen))
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

        TypeDescriptor lowerBound = typeVariable.getLowerBoundTypeDescriptor();
        TypeDescriptor upperBound = typeVariable.getUpperBoundTypeDescriptor();

        switch (variance) {
          case IN_OUT:
            return typeVariable
                .toWildcard()
                .withRewrittenBounds(it -> project(it, variance, newSeen));
          case IN:
            if (lowerBound != null) {
              return project(lowerBound, Variance.IN, newSeen);
            } else {
              return project(upperBound, Variance.IN_OUT, newSeen);
            }
          case OUT:
            if (lowerBound != null) {
              return project(lowerBound, Variance.IN_OUT, newSeen);
            } else {
              return project(upperBound, Variance.OUT, newSeen);
            }
        }
        throw new AssertionError();
      }
    } else if (typeDescriptor instanceof IntersectionTypeDescriptor) {
      IntersectionTypeDescriptor intersectionTypeDescriptor =
          (IntersectionTypeDescriptor) typeDescriptor;
      return IntersectionTypeDescriptor.newBuilder()
          .setIntersectionTypeDescriptors(
              intersectionTypeDescriptor.getIntersectionTypeDescriptors().stream()
                  .map(it -> project(it, variance, seen))
                  .collect(toImmutableList()))
          .build();
    } else if (typeDescriptor instanceof UnionTypeDescriptor) {
      UnionTypeDescriptor unionTypeDescriptor = (UnionTypeDescriptor) typeDescriptor;
      return UnionTypeDescriptor.newBuilder()
          .setUnionTypeDescriptors(
              unionTypeDescriptor.getUnionTypeDescriptors().stream()
                  .map(it -> project(it, variance, seen))
                  .collect(toImmutableList()))
          .build();
    } else {
      throw new AssertionError();
    }
  }

  private static TypeDescriptor projectArgument(
      TypeDescriptor argumentTypeDescriptor,
      @Nullable TypeVariable typeParameter,
      Variance variance,
      ImmutableSet<TypeVariable> seen) {
    // Don't project unbound wildcards with recursive declaration, as it'll result in recursive type
    // parameter appearing in the projection. Without this check, given the
    // {@code Foo<T extends Foo<T>>} declaration, the projection of {@code Foo<?>} would appear as
    // {@code Foo<Foo<T>>}.
    if (isUnboundWildcardWithRecursiveDeclaration(argumentTypeDescriptor, typeParameter)) {
      return argumentTypeDescriptor;
    }

    return project(
        argumentTypeDescriptor,
        isWildcard(argumentTypeDescriptor) ? variance : Variance.IN_OUT,
        seen);
  }

  private static boolean isUnboundWildcardWithRecursiveDeclaration(
      TypeDescriptor typeDescriptor, @Nullable TypeVariable typeParameter) {
    if (typeDescriptor instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) typeDescriptor;
      return typeParameter != null
          && typeParameter.hasRecursiveDefinition()
          && typeVariable.isWildcardOrCapture()
          && typeVariable
              .getUpperBoundTypeDescriptor()
              .toNullable()
              .equals(typeParameter.getUpperBoundTypeDescriptor().toNullable());
    }
    return false;
  }

  private enum Variance {
    IN,
    OUT,
    IN_OUT
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
