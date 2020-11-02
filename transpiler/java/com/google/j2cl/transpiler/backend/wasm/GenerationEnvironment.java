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

import static java.util.stream.Collectors.joining;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

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

  String getWasmType(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isPrimitive()) {
      return WASM_TYPES_BY_PRIMITIVE_TYPES.get(typeDescriptor);
    }
    return "(ref null " + getWasmTypeName(typeDescriptor) + ")";
  }

  String getWasmTypeName(TypeDescriptor typeDescriptor) {
    // TODO(rluble): remove j.l.O as a placeholder for arrays once arrays are implemented.
    if (typeDescriptor.isArray()) {
      return getWasmTypeName(TypeDescriptors.get().javaLangObject);
    }

    return "$" + getTypeSignature(typeDescriptor);
  }

  String getTypeSignature(TypeDescriptor typeDescriptor) {
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

  /**
   * Returns the name of the global function that implements the method.
   *
   * <p>Note that these names need to be globally unique and are different than the names of the
   * slots in the vtable which maps nicely to our concept of mangled names.
   */
  String getMethodImplementationName(Method method) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    return "$"
        + methodDescriptor.getName()
        + method.getParameters().stream()
            .map(p -> getTypeSignature(p.getTypeDescriptor()))
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
}
