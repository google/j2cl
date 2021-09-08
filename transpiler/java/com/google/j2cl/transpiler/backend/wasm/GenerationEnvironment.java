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
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.HasName;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NameDeclaration;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.backend.common.UniqueNamesResolver;
import java.util.Collections;
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
    checkArgument(typeDescriptor.isPrimitive() && !TypeDescriptors.isPrimitiveVoid(typeDescriptor));
    return WASM_TYPES_BY_PRIMITIVE_TYPES.get(typeDescriptor);
  }

  private static final ImmutableMap<PrimitiveTypeDescriptor, String>
      WASM_PACKED_TYPES_BY_PRIMITIVE_TYPES =
          ImmutableMap.<PrimitiveTypeDescriptor, String>builder()
              .put(PrimitiveTypes.BOOLEAN, "i8")
              .put(PrimitiveTypes.BYTE, "i8")
              .put(PrimitiveTypes.CHAR, "i16")
              .put(PrimitiveTypes.SHORT, "i16")
              .put(PrimitiveTypes.INT, "i32")
              .put(PrimitiveTypes.LONG, "i64")
              .put(PrimitiveTypes.FLOAT, "f32")
              .put(PrimitiveTypes.DOUBLE, "f64")
              .build();

  static String getWasmPackedTypeForPrimitive(TypeDescriptor typeDescriptor) {
    checkArgument(typeDescriptor.isPrimitive() && !TypeDescriptors.isPrimitiveVoid(typeDescriptor));
    return WASM_PACKED_TYPES_BY_PRIMITIVE_TYPES.get(typeDescriptor);
  }

  static String getGetterInstruction(TypeDescriptor typeDescriptor) {
    if (TypeDescriptors.isPrimitiveChar(typeDescriptor)) {
      return "get_u";
    }
    if (typeDescriptor.isPrimitive()
        && !getWasmTypeForPrimitive(typeDescriptor)
            .equals(getWasmPackedTypeForPrimitive(typeDescriptor))) {
      return "get_s";
    }
    return "get";
  }

  /** Maps Java type declarations to the corresponding wasm type layout objects. */
  private final Map<TypeDeclaration, WasmTypeLayout> wasmTypeLayoutByTypeDeclaration;

  /** Returns the wasm type layout for a Java declared type. */
  WasmTypeLayout getWasmTypeLayout(TypeDeclaration typeDeclaration) {
    return wasmTypeLayoutByTypeDeclaration.get(typeDeclaration);
  }

  String getWasmType(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.toRawTypeDescriptor().isWasmExtern()) {
      return "externref";
    }

    if (typeDescriptor.isPrimitive()) {
      return getWasmTypeForPrimitive(typeDescriptor);
    }
    return "(ref null " + getWasmTypeName(typeDescriptor) + ")";
  }

  /**
   * Returns the type to be used in a context of Struct or Array which can be potentially a packed
   * type. (WasmGC supports packed types only in limited contexts)
   */
  String getWasmFieldType(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isPrimitive()) {
      return getWasmPackedTypeForPrimitive(typeDescriptor);
    }
    return getWasmType(typeDescriptor);
  }

  String getWasmTypeName(TypeDeclaration typeDeclaration) {
    return getWasmTypeName(typeDeclaration.toUnparameterizedTypeDescriptor());
  }

  String getWasmTypeName(TypeDescriptor typeDescriptor) {
    typeDescriptor = typeDescriptor.toRawTypeDescriptor();

    if (typeDescriptor.isWasmExtern()) {
      return "extern";
    }

    if (typeDescriptor.isArray()) {
      ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
      if (arrayTypeDescriptor.isNativeWasmArray()) {
        return getWasmTypeName(arrayTypeDescriptor.getComponentTypeDescriptor()) + ".array";
      }
      return getWasmTypeName(TypeDescriptors.getWasmArrayType(arrayTypeDescriptor));
    }

    if (typeDescriptor.isInterface()) {
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

    throw new AssertionError("Unexpected type: " + typeDescriptor.getReadableDescription());
  }

  /** Returns the name of the global containing the rtt for a Java type. */
  String getRttGlobalName(TypeDeclaration typeDeclaration) {
    return getRttGlobalName(typeDeclaration.toUnparameterizedTypeDescriptor());
  }

  /** Returns the name of the global containing the rtt for a Java type. */
  String getRttGlobalName(TypeDescriptor typeDescriptor) {
    typeDescriptor = typeDescriptor.toUnparameterizedTypeDescriptor();
    checkArgument(typeDescriptor.isClass() || typeDescriptor.isEnum() || typeDescriptor.isArray());
    return getWasmTypeName(typeDescriptor) + ".rtt";
  }

  /** Returns the name of the global that stores the itable for a Java type. */
  public String getWasmItableGlobalName(DeclaredTypeDescriptor typeDescriptor) {
    return "$" + getTypeSignature(typeDescriptor) + ".itable";
  }

  /** Returns the name of the global that stores the itable for a Java type. */
  public String getWasmItableGlobalName(TypeDeclaration typeDeclaration) {
    return getWasmItableGlobalName(typeDeclaration.toUnparameterizedTypeDescriptor());
  }

  /** Returns the name of the wasm type of the vtable for a Java type. */
  public String getWasmVtableTypeName(DeclaredTypeDescriptor typeDescriptor) {
    return "$" + getTypeSignature(typeDescriptor) + ".vtable";
  }

  /** Returns the name of the wasm type of the vtable for a Java type. */
  public String getWasmVtableTypeName(TypeDeclaration typeDeclaration) {
    return getWasmVtableTypeName(typeDeclaration.toUnparameterizedTypeDescriptor());
  }

  /** Returns the name of the global that stores the vtable for a Java type. */
  public String getWasmVtableGlobalName(DeclaredTypeDescriptor typeDescriptor) {
    return getWasmVtableGlobalName(typeDescriptor.getTypeDeclaration());
  }

  /** Returns the name of the global that stores the vtable for a Java type. */
  public String getWasmVtableGlobalName(TypeDeclaration typeDeclaration) {
    // We use the same name for the global that holds the vtable as well as for its type since
    // the type namespace and global namespace are different naming scopes.
    return getWasmVtableTypeName(typeDeclaration);
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
    return "$" + fieldDescriptor.getMangledName();
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

  private final Map<TypeDeclaration, Integer> slotByInterfaceTypeDeclaration =
      new LinkedHashMap<>();

  int getInterfaceSlot(TypeDeclaration typeDeclaration) {
    // Interfaces with no implementors will not have a slot assigned. Use -1 to signal that fact.
    return slotByInterfaceTypeDeclaration.getOrDefault(typeDeclaration, -1);
  }

  GenerationEnvironment(Library library) {
    List<CompilationUnit> compilationUnits = library.getCompilationUnits();

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
        .sorted(comparingInt(t -> t.getDeclaration().getClassHierarchyDepth()))
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

    assignInterfaceSlots(compilationUnits);
  }

  /**
   * Assigns a slot number (i.e. an index in the itable array) for each interface in the itable.
   *
   * <p>Each interfaces implemented in the same class will have a different slots, but across
   * different parts of the hierarchy slots can be reused. This algorithm heuristically minimizes
   * the size of the itable by trying to assign slots in order of most implemented interfaces. Each
   * slot in the itable will have the interface vtable for the class and can be used for both
   * dynamic interface dispatch and interface "instanceof" checks.
   *
   * <p>This is a baseline implementation of "packed encoding" based on the algorithm described in
   * section 4.3 of "Efficient type inclusion tests" by Vitek et al (OOPSLA 97). Although the ideas
   * presented in the paper are for performing "instanceof" checks, they generalize to interface
   * dispatch.
   */
  private void assignInterfaceSlots(List<CompilationUnit> compilationUnits) {
    SetMultimap<TypeDeclaration, TypeDeclaration> concreteTypesByInterface =
        LinkedHashMultimap.create();
    SetMultimap<Integer, TypeDeclaration> concreteTypesBySlot = LinkedHashMultimap.create();

    // Traverse all concrete classes collecting the interfaces they implement. Actual vtable
    // instances are only required for concrete classes, because they provide the references to the
    // methods that will be invoked on a specific instance.
    // Since all dynamic dispatch is performed by obtaining the vtables from an instance,  if there
    // are no instances for a type there is no need for instances of vtables it.
    compilationUnits.stream()
        .flatMap(c -> c.getTypes().stream())
        .filter(Predicates.not(Type::isInterface))
        .filter(Predicates.not(Type::isAbstract))
        .forEach(
            t ->
                t.getDeclaration().getAllSuperTypesIncludingSelf().stream()
                    .filter(TypeDeclaration::isInterface)
                    .forEach(i -> concreteTypesByInterface.put(i, t.getDeclaration())));

    // Traverse and assign interfaces by most implemented to least implemented so that widely
    // implemented interfaces get lower slot numbers.
    concreteTypesByInterface.keySet().stream()
        .sorted(
            comparingInt((TypeDeclaration td) -> concreteTypesByInterface.get(td).size())
                .reversed())
        .forEach(
            i -> assignFirstNonConflictingSlot(i, concreteTypesByInterface, concreteTypesBySlot));
  }

  /** Assigns the lowest non conflicting slot to {@code interfaceToAssign}. */
  private void assignFirstNonConflictingSlot(
      TypeDeclaration interfaceToAssign,
      SetMultimap<TypeDeclaration, TypeDeclaration> concreteTypesByInterface,
      SetMultimap<Integer, TypeDeclaration> concreteTypesBySlot) {
    int slot =
        getFirstNonConflictingSlot(
            interfaceToAssign, concreteTypesBySlot, concreteTypesByInterface);
    slotByInterfaceTypeDeclaration.put(interfaceToAssign, slot);
    // Add all the concrete implementors for that interface to the assigned slot, to mark
    // that slot as already used in all those types.
    concreteTypesBySlot.putAll(slot, concreteTypesByInterface.get(interfaceToAssign));
  }

  /** Finds the lowest non-conflicting slot for {@code interface}. */
  private int getFirstNonConflictingSlot(
      TypeDeclaration interfaceToAssign,
      SetMultimap<Integer, TypeDeclaration> concreteTypesBySlot,
      SetMultimap<TypeDeclaration, TypeDeclaration> concreteTypesByInterface) {
    // Assign slots by finding the first non conflicting open slot. Interfaces that are
    // implemented by the same concrete class must have unique slots but interfaces whose
    // implementers are disjoint can share the same slot.
    int numberOfSlots = concreteTypesBySlot.keySet().size();
    for (int slot = 0; slot < numberOfSlots; slot++) {
      if (Collections.disjoint(
          concreteTypesBySlot.get(slot), concreteTypesByInterface.get(interfaceToAssign))) {
        return slot;
      }
    }
    // Couldn't find an existing slot that is not conflicting, return a new slot.
    return numberOfSlots;
  }
}
