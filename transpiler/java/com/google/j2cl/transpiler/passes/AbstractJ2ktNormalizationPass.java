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

import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor;
import com.google.j2cl.transpiler.ast.NullabilityAnnotation;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Abstract base class for Kotlin passes, providing shared functionality:
 *
 * <ul>
 *   <li>verbose type descriptions,
 * </ul>
 */
public abstract class AbstractJ2ktNormalizationPass extends NormalizationPass {

  /**
   * Produces readable description string for AST nodes, containing information about resolved
   * nullability and captures with unique IDs. Used in Kotlin passes for debugging / development.
   */
  // TODO(b/382500942): Remove when no longer needed for debugging / development.
  static final class Describer {
    private final List<TypeVariable> seenCaptures = new ArrayList<>();

    String getDescription(TypeDescriptor typeDescriptor) {
      return getDescription(typeDescriptor, ImmutableList.of());
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
