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
package com.google.j2cl.transpiler.ast;

import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Produces a comprehensive descriptor of descriptors, containing information about resolved
 * nullability and captures with unique IDs assigned within an instance of this class.
 *
 * <p>Nullability annotations in descriptions represent resolved nullability as if the type occurred
 * within {@code @NullMarked} scope.
 */
// TODO(b/382500942): Remove when no longer needed for debugging / development.
public final class DebugDescriber {
  public static DebugDescriber newDebugDescriber() {
    return new DebugDescriber();
  }

  public String getTypeParameterDescription(TypeVariable typeVariable) {
    return typeVariable.getName()
        + " extends "
        + getDescription(typeVariable.getUpperBoundTypeDescriptor());
  }

  public String getDescription(TypeDescriptor typeDescriptor) {
    return getDescription(typeDescriptor, ImmutableList.of());
  }

  public String getDescription(TypeDeclaration typeDeclaration) {
    return typeDeclaration.getReadableDescription()
        + getTypeParametersDescription(typeDeclaration.getTypeParameterDescriptors());
  }

  public String getDescription(MethodDescriptor methodDescriptor) {
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

  private final List<TypeVariable> seenCaptures = new ArrayList<>();

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
    if (typeDescriptor instanceof PrimitiveTypeDescriptor descriptor) {
      return descriptor.getSimpleSourceName();
    } else if (typeDescriptor instanceof ArrayTypeDescriptor descriptor) {
      return getDescription(descriptor.getComponentTypeDescriptor(), enclosingWildcardOrCaptures)
          + getDescriptionInfix(getNullabilityAnnotation(typeDescriptor))
          + "[]";
    } else if (typeDescriptor instanceof DeclaredTypeDescriptor descriptor) {
      return getDescriptionPrefix(getNullabilityAnnotation(typeDescriptor))
          + descriptor.getTypeDeclaration().getReadableDescription()
          + getTypeArgumentsDescription(descriptor, enclosingWildcardOrCaptures);
    } else if (typeDescriptor instanceof TypeVariable typeVariable) {
      return getDescriptionPrefix(getNullabilityAnnotation(typeDescriptor))
          + getDescriptionWithoutNullabilityAnnotation(typeVariable, enclosingWildcardOrCaptures);
    } else if (typeDescriptor instanceof IntersectionTypeDescriptor descriptor) {
      return descriptor.getIntersectionTypeDescriptors().stream()
          .map(it -> getDescription(it, enclosingWildcardOrCaptures))
          .collect(joining(" & ", "(", ")"));
    } else if (typeDescriptor instanceof UnionTypeDescriptor descriptor) {
      return descriptor.getUnionTypeDescriptors().stream()
          .map(it -> getDescription(it, enclosingWildcardOrCaptures))
          .collect(joining(" | ", "(", ")"));
    } else {
      throw new AssertionError();
    }
  }

  private String getParametersDescription(ImmutableList<ParameterDescriptor> parameterDescriptors) {
    return parameterDescriptors.stream().map(this::getDescription).collect(joining(", ", "(", ")"));
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
          + getDescription(typeVariable.getUpperBoundTypeDescriptor(), enclosingWildcardOrCaptures);
    }
  }

  private static NullabilityAnnotation getNullabilityAnnotation(TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof TypeVariable typeVariable) {
      return typeVariable.getNullabilityAnnotation();
    } else {
      return typeDescriptor.isNullable()
          ? NullabilityAnnotation.NULLABLE
          : NullabilityAnnotation.NONE;
    }
  }

  @Nullable
  private static String getDescription(NullabilityAnnotation nullabilityAnnotation) {
    return switch (nullabilityAnnotation) {
      case NULLABLE -> "@Nullable";
      case NONE -> null;
      case NOT_NULLABLE -> "@NonNull";
    };
  }

  private static String getDescriptionPrefix(NullabilityAnnotation nullabilityAnnotation) {
    String description = getDescription(nullabilityAnnotation);
    return description == null ? "" : description + " ";
  }

  private static String getDescriptionInfix(NullabilityAnnotation nullabilityAnnotation) {
    String description = getDescription(nullabilityAnnotation);
    return description == null ? "" : " " + description + " ";
  }

  private static String appendSpaceIfNotEmpty(String string) {
    return string.isEmpty() ? string : string + " ";
  }

  private static String inAngleBracketsIfNotEmpty(String string) {
    return string.isEmpty() ? string : "<" + string + ">";
  }

  private DebugDescriber() {}
}
