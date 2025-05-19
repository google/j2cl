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
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;

/** Inserts casts in places where necessary due to nullability differences in type arguments. */
// TODO(b/392084555): Clean-up this pass, as right now it's quite messy.
public final class InsertCastsOnNullabilityMismatch extends AbstractJ2ktNormalizationPass {
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

                if (isNullabilityAssignableTo(
                    fromTypeDescriptor, inferredTypeDescriptor, ImmutableSet.of())) {
                  return expression;
                }

                TypeDescriptor castTypeDescriptor = projectCaptures(inferredTypeDescriptor);
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

                debug(
                    getSourcePosition(),
                    "Inserted nullability mismatch cast to '%s' because of assignment from '%s'"
                        + " to '%s'",
                    getDescription(castTypeDescriptor),
                    getDescription(expression.getTypeDescriptor()),
                    getDescription(inferredTypeDescriptor));

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
    } else if (from instanceof ArrayTypeDescriptor fromArray) {
      TypeDescriptor toTarget = getAssignableTarget(to);

      if (toTarget instanceof ArrayTypeDescriptor toArray) {
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
    } else if (from instanceof DeclaredTypeDescriptor fromDeclared) {
      TypeDescriptor toTarget = getAssignableTarget(to);
      if (toTarget instanceof DeclaredTypeDescriptor toDeclared) {
        TypeDeclaration toDeclaration = toDeclared.getTypeDeclaration();
        if (!isNullableAssignableTo(fromDeclared.isNullable(), toDeclared.isNullable())) {
          return false;
        }

        // Use conservative approach and always require a cast from/to RAW types.
        if (fromDeclared.isRaw() || toDeclared.isRaw()) {
          return false;
        }

        DeclaredTypeDescriptor fromDeclaredBase = fromDeclared.findSupertype(toDeclaration);
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
    } else if (from instanceof TypeVariable fromVariable) {
      if (to instanceof TypeVariable toVariable) {
        // TypeVariable -> TypeVariable
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
    } else if (from instanceof IntersectionTypeDescriptor fromIntersection) {
      TypeDescriptor toTarget = getAssignableTarget(to);
      return fromIntersection.getIntersectionTypeDescriptors().stream()
          .anyMatch(it -> isNullabilityAssignableTo(it, toTarget, seenTo));
    } else if (from instanceof UnionTypeDescriptor fromUnion) {
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

    if (!toTypeDescriptor.isWildcardOrCapture()) {
      return fromTypeDescriptor.equals(toTypeDescriptor);
    } else {
      TypeVariable toVariable = (TypeVariable) toTypeDescriptor;
      if (seenTo.contains(toVariable)) {
        return true;
      }

      TypeDescriptor toLowerBound = getNormalizedLowerBoundTypeDescriptor(toVariable);
      if (toLowerBound != null) {
        if (!fromTypeDescriptor.isWildcardOrCapture()) {
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

        if (!fromTypeDescriptor.isWildcardOrCapture()) {
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
    } else if (typeDescriptor instanceof TypeVariable typeVariable) {
      if (typeVariable.isWildcardOrCapture()) {
        return getAssignableTarget(getNormalizedUpperBoundTypeDescriptor(typeVariable));
      } else {
        return typeVariable;
      }
    } else if (typeDescriptor instanceof IntersectionTypeDescriptor intersectionTypeDescriptor) {
      return getAssignableTarget(intersectionTypeDescriptor.getFirstType());
    } else if (typeDescriptor instanceof UnionTypeDescriptor unionTypeDescriptor) {
      return getAssignableTarget(unionTypeDescriptor.getClosestCommonSuperClass());
    } else {
      throw new AssertionError();
    }
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
}
