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
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.HasName;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NameDeclaration;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.backend.common.UniqueNamesResolver;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

  /** Maps Java type declarations to the corresponding wasm type layout objects. */
  private final Map<TypeDeclaration, WasmTypeLayout> wasmTypeLayoutByTypeDeclaration;

  /** Returns the wasm type layout for a Java declared type. */
  WasmTypeLayout getWasmTypeLayout(TypeDeclaration typeDeclaration) {
    return wasmTypeLayoutByTypeDeclaration.get(typeDeclaration);
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
    typeDescriptor = typeDescriptor.toRawTypeDescriptor();

    // TODO(rluble): remove j.l.O as a placeholder for arrays once arrays are implemented.
    if (typeDescriptor.isInterface() || typeDescriptor.isArray()) {
      // Interfaces are modeled as java.lang.Object at runtime.
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

  /** Returns the name of the global containing the rtt for {@code typeDescriptor}. */
  String getRttGlobalName(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isClass() || typeDescriptor.isEnum()) {
      return getRttGlobalName(((DeclaredTypeDescriptor) typeDescriptor).getTypeDeclaration());
    }
    throw new AssertionError("Unexpected type: " + typeDescriptor.getReadableDescription());
  }

  /** Returns the name of the wasm type of the vtable for a Java type. */
  public String getWasmVtableTypeName(DeclaredTypeDescriptor typeDescriptor) {
    return getWasmTypeName(typeDescriptor) + ".vtable";
  }

  /** Returns the name of the global that stores the vtable for {@code typeDescriptor}. */
  public String getWasmVtableGlobalName(DeclaredTypeDescriptor typeDescriptor) {
    // We use the same name for the global that holds the vtable as well as for its type since
    // the type namespace and global namespace are different naming scopes.
    return getWasmVtableTypeName(typeDescriptor);
  }

  /** Returns the name of the field in the vtable that corresponds to {@code methodDescriptor}. */
  public String getVtableSlot(MethodDescriptor methodDescriptor) {
    return "$" + methodDescriptor.getMangledName();
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
        + methodDescriptor.getMangledName()
        + "@"
        + methodDescriptor.getEnclosingTypeDescriptor().getQualifiedSourceName();
  }

  String getFieldName(Field field) {
    return getFieldName(field.getDescriptor());
  }

  String getFieldName(FieldDescriptor fieldDescriptor) {
    return "$"
        + fieldDescriptor.getName()
        + "@"
        + fieldDescriptor.getEnclosingTypeDescriptor().getQualifiedSourceName();
  }

  private final Map<HasName, String> nameByDeclaration = new HashMap<>();

  String getDeclarationName(NameDeclaration declaration) {
    return "$" + checkNotNull(nameByDeclaration.get(declaration));
  }

  /**
   * Returns the name for the type associated with a function signature.
   *
   * <p>In WASM in order to use function references as structure fields (e.g. in the vtable), their
   * types needs to be declared.
   */
  String getFunctionTypeName(MethodDescriptor methodDescriptor) {
    return String.format(
        "$function.%s__%s",
        methodDescriptor.getDispatchParameterTypeDescriptors().stream()
            .map(TypeDescriptor::toRawTypeDescriptor)
            .map(this::getWasmTypeName)
            .collect(joining("__")),
        getWasmTypeName(methodDescriptor.getDispatchReturnTypeDescriptor()));
  }

  GenerationEnvironment(List<CompilationUnit> compilationUnits) {
    // Resolve variable names into unique wasm identifiers.
    compilationUnits.stream()
        .flatMap(c -> c.getTypes().stream())
        .forEach(
            t ->
                nameByDeclaration.putAll(
                    UniqueNamesResolver.computeUniqueNames(ImmutableSet.of(), t)));

    // Create a representation for Java classes that is useful to lay out the structs and
    // vtables needed in the wasm output.
    wasmTypeLayoutByTypeDeclaration = new LinkedHashMap<>();
    compilationUnits.stream()
        .flatMap(c -> c.getTypes().stream())
        .filter(Predicates.not(Type::isInterface))
        // Traverse superclasses before subclasses to ensure that the layout for the superclass
        // is already available to build the layout for the subclass.
        .sorted(Comparator.comparingInt(t -> t.getDeclaration().getClassHierarchyDepth()))
        .forEach(
            t -> {
              WasmTypeLayout superTypeLayout = null;
              if (t.getSuperTypeDescriptor() != null) {
                superTypeLayout =
                    wasmTypeLayoutByTypeDeclaration.get(
                        t.getSuperTypeDescriptor().getTypeDeclaration());
              }
              wasmTypeLayoutByTypeDeclaration.put(
                  t.getDeclaration(), WasmTypeLayout.create(t, superTypeLayout));
            });
  }
}
