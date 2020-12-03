/*
 * Copyright 2020 Google Inc.
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
package com.google.j2cl.transpiler.backend.wasm;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import java.util.HashMap;
import java.util.Map;

/** Allows mapping of middle end constructors to the backend. */
class GenerationEnvironment {

  private static final ImmutableMap<PrimitiveTypeDescriptor, String> WASM_TYPES_BY_PRIMITIVE_TYPES =
      ImmutableMap.<PrimitiveTypeDescriptor, String>builder()
          .put(PrimitiveTypes.BOOLEAN, "i32")
          .put(PrimitiveTypes.BYTE, "i32")
          .put(PrimitiveTypes.CHAR, "i32")
          .put(PrimitiveTypes.SHORT, "i32")
          .put(PrimitiveTypes.INT, "i32")
          .put(PrimitiveTypes.LONG, "i64")
          .put(PrimitiveTypes.FLOAT, "f32")
          .put(PrimitiveTypes.DOUBLE, "f64")
          .build();

  static String getWasmTypeForPrimitive(TypeDescriptor typeDescriptor) {
    checkArgument(typeDescriptor.isPrimitive());
    return WASM_TYPES_BY_PRIMITIVE_TYPES.get(typeDescriptor);
  }

  String getWasmType(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isPrimitive()) {
      return getWasmTypeForPrimitive(typeDescriptor);
    }
    return "(ref null " + getWasmTypeName(typeDescriptor) + ")";
  }

  String getWasmTypeName(TypeDeclaration typeDeclaration) {
    return getWasmTypeName(typeDeclaration.toUnparameterizedTypeDescriptor());
  }

  String getWasmTypeName(TypeDescriptor typeDescriptor) {
    // TODO(rluble): remove j.l.O as a placeholder for arrays once arrays are implemented.
    if (typeDescriptor.isArray()) {
      return getWasmTypeName(TypeDescriptors.get().javaLangObject);
    }

    return "$" + getTypeSignature(typeDescriptor);
  }

  private static String getTypeSignature(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isPrimitive()) {
      return typeDescriptor.getReadableDescription();
    }
    typeDescriptor = typeDescriptor.toRawTypeDescriptor();
    if (typeDescriptor instanceof DeclaredTypeDescriptor) {
      return ((DeclaredTypeDescriptor) typeDescriptor).getQualifiedSourceName();
    }

    if (typeDescriptor.isArray()) {
      ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
      return getTypeSignature(arrayTypeDescriptor.getLeafTypeDescriptor())
          + Strings.repeat("<>", arrayTypeDescriptor.getDimensions());
    }
    throw new AssertionError("Unexpected type: " + typeDescriptor.getReadableDescription());
  }

  /** Returns the name of the global containing the rtt for {@code typeDeclaration}. */
  String getRttGlobalName(TypeDeclaration typeDeclaration) {
    return getWasmTypeName(typeDeclaration.toUnparameterizedTypeDescriptor()) + ".rtt";
  }

  /**
   * Returns the name of the global function that implements the method.
   *
   * <p>Note that these names need to be globally unique and are different than the names of the
   * slots in the vtable which maps nicely to our concept of mangled names.
   */
  String getMethodImplementationName(MethodDescriptor methodDescriptor) {
    methodDescriptor = methodDescriptor.getDeclarationDescriptor();
    return "$"
        + methodDescriptor.getName()
        + methodDescriptor.getDeclarationDescriptor().getParameterTypeDescriptors().stream()
            .map(GenerationEnvironment::getTypeSignature)
            .collect(joining("|", "<", ">:"))
        + getTypeSignature(methodDescriptor.getReturnTypeDescriptor())
        + "@"
        + methodDescriptor.getEnclosingTypeDescriptor().getQualifiedSourceName();
  }

  String getFieldName(Field field) {
    FieldDescriptor fieldDescriptor = field.getDescriptor();
    return "$"
        + fieldDescriptor.getName()
        + "@"
        + fieldDescriptor.getEnclosingTypeDescriptor().getQualifiedSourceName();
  }

  private final Map<Variable, String> variableNameByVariable = new HashMap<>();
  private final Multiset<String> variableNameFrequency = HashMultiset.create();

  String getVariableName(Variable variable) {
    // TODO(rluble): add a proper variable name collision resolver.
    return variableNameByVariable.computeIfAbsent(
        variable, v -> "$" + v.getName() + "." + variableNameFrequency.add(v.getName(), 1));
  }
}
