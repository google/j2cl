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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.lang.String.format;
import static java.util.Comparator.comparingInt;

import com.google.common.base.Predicates;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.HasName;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NameDeclaration;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.backend.common.UniqueNamesResolver;
import com.google.j2cl.transpiler.backend.wasm.JsImportsGenerator.Imports;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;

/** Allows mapping of middle end constructors to the backend. */
public class WasmGenerationEnvironment {

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
    return checkNotNull(wasmTypeLayoutByTypeDeclaration.get(typeDeclaration));
  }

  String getWasmType(TypeDescriptor typeDescriptor) {
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
    return getWasmTypeName(typeDeclaration.toDescriptor());
  }

  String getWasmTypeName(TypeDescriptor typeDescriptor) {
    typeDescriptor = typeDescriptor.toRawTypeDescriptor();

    if (typeDescriptor instanceof DeclaredTypeDescriptor
        && ((DeclaredTypeDescriptor) typeDescriptor).getTypeDeclaration().getWasmInfo() != null) {
      return ((DeclaredTypeDescriptor) typeDescriptor).getTypeDeclaration().getWasmInfo();
    }

    if (typeDescriptor.isNative()) {
      return "extern";
    }

    if (typeDescriptor.isArray()) {
      ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
      if (arrayTypeDescriptor.isNativeWasmArray()) {
        String wasmTypeName = getWasmTypeName(arrayTypeDescriptor.getComponentTypeDescriptor());
        // Make sure the resulting type name always has $ prefix.
        String prefix = wasmTypeName.startsWith("$") ? "" : "$";
        return format("%s%s.array", prefix, wasmTypeName);
      }
      return getWasmTypeName(TypeDescriptors.getWasmArrayType(arrayTypeDescriptor));
    }

    if (typeDescriptor.isInterface()) {
      // Interfaces are modeled as java.lang.Object at runtime.
      return getWasmTypeName(TypeDescriptors.get().javaLangObject);
    }

    return getTypeSignature(typeDescriptor);
  }

  public String getTypeSignature(TypeDeclaration typeDeclaration) {
    return getTypeSignature(typeDeclaration.toDescriptor());
  }

  public String getTypeSignature(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isPrimitive()) {
      return "$" + typeDescriptor.getReadableDescription();
    }
    typeDescriptor = typeDescriptor.toRawTypeDescriptor();
    if (typeDescriptor instanceof DeclaredTypeDescriptor) {
      return "$" + ((DeclaredTypeDescriptor) typeDescriptor).getQualifiedSourceName();
    }

    throw new AssertionError("Unexpected type: " + typeDescriptor.getReadableDescription());
  }

  public String getWasmEmptyArrayGlobalName(ArrayTypeDescriptor arrayTypeDescriptor) {
    return "$__emptyArray_" + getWasmTypeName(arrayTypeDescriptor);
  }
  /** Returns the name of the global that stores the itable for a Java type. */
  public String getWasmItableGlobalName(DeclaredTypeDescriptor typeDescriptor) {
    if (!typeDescriptor.getTypeDeclaration().implementsInterfaces()) {
      return "$itable.empty";
    }
    return getTypeSignature(typeDescriptor) + ".itable";
  }

  /** Returns the name of the global that stores the itable for a Java type. */
  public String getWasmItableGlobalName(TypeDeclaration typeDeclaration) {
    return getWasmItableGlobalName(typeDeclaration.toDescriptor());
  }

  /** Returns the name of the wasm type of the vtable for a Java type. */
  public String getWasmVtableTypeName(DeclaredTypeDescriptor typeDescriptor) {
    return getTypeSignature(typeDescriptor) + ".vtable";
  }

  /** Returns the name of the wasm type of the vtable for a Java type. */
  public String getWasmVtableTypeName(TypeDeclaration typeDeclaration) {
    return getWasmVtableTypeName(typeDeclaration.toDescriptor());
  }

  /** Returns the name of the wasm type of the itable for a Java type. */
  public String getWasmItableTypeName(TypeDeclaration typeDeclaration) {
    if (typeDeclaration == null || !typeDeclaration.implementsInterfaces()) {
      return "$itable";
    }

    return getTypeSignature(typeDeclaration) + ".itable";
  }

  /** Returns the name of the itable interface getter. */
  public String getWasmItableInterfaceGetter(TypeDeclaration typeDeclaration) {
    return getWasmItableInterfaceGetter(getTypeSignature(typeDeclaration));
  }

  /** Returns the name of the itable interface getter. */
  public String getWasmItableInterfaceGetter(String fieldName) {
    return format("$get.itable.%s", fieldName);
  }

  /** Returns the name of the global that stores the vtable for a Java type. */
  public String getWasmInterfaceVtableGlobalName(TypeDeclaration ifce, TypeDeclaration inClass) {
    return format("%s@%s", getWasmVtableTypeName(ifce), getWasmTypeName(inClass));
  }

  /** Returns the name of the global that stores the vtable for a Java type. */
  public String getWasmVtableGlobalName(DeclaredTypeDescriptor typeDescriptor) {
    return getWasmVtableGlobalName(typeDescriptor.getTypeDeclaration());
  }

  /** Returns the name of the global that stores the vtable for a Java type. */
  public String getWasmVtableGlobalName(TypeDeclaration typeDeclaration) {
    // For classes we use the same name for the global that holds the vtable as well as for its type
    // since the type namespace and global namespace are different naming scopes.
    return getWasmVtableTypeName(typeDeclaration);
  }

  /** Returns the name of the field in the vtable that corresponds to {@code methodDescriptor}. */
  public String getVtableFieldName(MethodDescriptor methodDescriptor) {
    return "$" + methodDescriptor.getMangledName();
  }

  /**
   * Returns the name of the global function that implements the method.
   *
   * <p>Note that these names need to be globally unique and are different than the names of the
   * slots in the vtable which maps nicely to our concept of mangled names.
   */
  public String getMethodImplementationName(MethodDescriptor methodDescriptor) {
    methodDescriptor = methodDescriptor.getDeclarationDescriptor();
    return "$"
        // TODO(b/315893220): Improve method names to avoid repetition of the enclosing type.
        + methodDescriptor.getMangledName()
        + (methodDescriptor.getOrigin().isOnceMethod() ? "_<once>_" : "")
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

  /** Returns true if the field is the WasmArray.OfNNN.elements. */
  boolean isWasmArrayElementsField(FieldDescriptor descriptor) {
    return TypeDescriptors.isWasmArrayOrSubtype(descriptor.getEnclosingTypeDescriptor())
        && descriptor.getName().equals("elements");
  }

  private final Map<HasName, String> nameByDeclaration = new HashMap<>();

  String getDeclarationName(NameDeclaration declaration) {
    return "$" + checkNotNull(nameByDeclaration.get(declaration));
  }

  /**
   * Returns the name for the type associated with a function signature.
   *
   * <p>In Wasm in order to use function references as structure fields (e.g. in the vtable), their
   * types needs to be declared.
   */
  String getFunctionTypeName(MethodDescriptor methodDescriptor) {
    return generateFunctionTypeName("$function", methodDescriptor);
  }

  String getNoSideEffectWrapperFunctionName(MethodDescriptor methodDescriptor) {
    return generateFunctionTypeName("$function.no.side.effects", methodDescriptor);
  }

  private String generateFunctionTypeName(String prefix, MethodDescriptor methodDescriptor) {
    return prefix + "." + methodDescriptor.getMangledName();
  }

  /** Returns the methods that need intrinsic declaration indexed by the name of the import. */
  ImmutableMap<String, MethodDescriptor> collectMethodsNeedingIntrinsicDeclarations() {
    return library
        .streamTypes()
        .flatMap(t -> t.getMethods().stream())
        .map(Method::getDescriptor)
        .filter(MethodDescriptor::isSideEffectFree)
        .collect(
            toImmutableMap(
                this::getNoSideEffectWrapperFunctionName, Function.identity(), (a, b) -> a));
  }

  /** Returns methods that need a wasm function type declaration indexed by the name of the type. */
  ImmutableMap<String, MethodDescriptor> collectMethodsThatNeedTypeDeclarations() {
    return library
        .streamTypes()
        .flatMap(t -> t.getMethods().stream())
        .map(Method::getDescriptor)
        .filter(MethodDescriptor::isPolymorphic)
        .collect(toImmutableMap(this::getFunctionTypeName, Function.identity(), (a, b) -> a));
  }

  /** The data index for the array literals that can be emitted as data. */
  private final Map<ArrayLiteral, String> dataNameByLiteral = new HashMap<>();

  /**
   * Data elements for array literal will be given a name relative to the type they appear in.
   *
   * <p>Keep track how many literals have been given for a given type.
   */
  private final Multiset<String> lastIndexByName = HashMultiset.create();

  /** Registers the ArrayLiteral as a data segment and returns true if it was not present. */
  public boolean registerDataSegmentLiteral(ArrayLiteral arrayLiteral, String typeQualifiedName) {
    if (dataNameByLiteral.containsKey(arrayLiteral)) {
      return false;
    }

    // Create names that are relative to the type they are first created in.
    lastIndexByName.add(typeQualifiedName);
    var name =
        "$arrayliteral@" + typeQualifiedName + "-" + lastIndexByName.count(typeQualifiedName);

    dataNameByLiteral.put(arrayLiteral, name);
    return true;
  }

  /** Returns the data segment index for this literal and null if it does not have one. */
  public String getDataElementNameForLiteral(ArrayLiteral arrayLiteral) {
    return dataNameByLiteral.get(arrayLiteral);
  }

  int getItableIndexForInterface(TypeDeclaration typeDeclaration) {
    return itableAllocator.getItableFieldIndex(typeDeclaration);
  }

  int getItableSize() {
    if (isModular) {
      throw new UnsupportedOperationException();
    }
    return itableAllocator.getItableSize();
  }

  public JsImportsGenerator.Imports getJsImports() {
    return jsImports;
  }

  public JsMethodImport getJsMethodImport(MethodDescriptor methodDescriptor) {
    return jsImports.getMethodImports().get(methodDescriptor);
  }

  public boolean isJsImport(Method method) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    return jsImports.getMethodImports().get(methodDescriptor) != null;
  }

  String getSourceMappingPathPrefix() {
    return sourceMappingPathPrefix;
  }

  private final boolean isModular;
  private final Library library;
  private final JsImportsGenerator.Imports jsImports;
  private final ItableAllocator<TypeDeclaration> itableAllocator;
  private final String sourceMappingPathPrefix;

  WasmGenerationEnvironment(Library library, Imports jsImports) {
    this(library, jsImports, /* sourceMappingPathPrefix= */ null, /* isModular= */ false);
  }

  WasmGenerationEnvironment(
      Library library, Imports jsImports, String sourceMappingPathPrefix, boolean isModular) {
    this.isModular = isModular;
    this.library = library;
    this.sourceMappingPathPrefix = sourceMappingPathPrefix;

    // Resolve variable names into unique wasm identifiers.
    library
        .streamTypes()
        .forEach(
            t ->
                nameByDeclaration.putAll(
                    UniqueNamesResolver.computeUniqueNames(ImmutableSet.of(), t)));

    // Create a representation for Java types that is useful to lay out the structs and
    // vtables needed in the wasm output.
    wasmTypeLayoutByTypeDeclaration = new LinkedHashMap<>();
    library
        .streamTypes()
        // Traverse superclasses before subclasses to ensure that the layout for the superclass
        // is already available to build the layout for the subclass.
        .sorted(comparingInt(t -> t.getDeclaration().getTypeHierarchyDepth()))
        .forEach(
            t -> {
              TypeDeclaration typeDeclaration = t.getDeclaration();
              // Force creation of layouts for all superinterfaces.
              typeDeclaration.getAllSuperInterfaces().forEach(this::getOrCreateWasmTypeLayout);
              WasmTypeLayout superWasmLayout =
                  getOrCreateWasmTypeLayout(getTypeLayoutSuperTypeDeclaration(typeDeclaration));
              var previous =
                  wasmTypeLayoutByTypeDeclaration.put(
                      typeDeclaration, WasmTypeLayout.createFromType(t, superWasmLayout));
              // Since the layout is for a type in the AST, it is expected that the
              // layout was not already created from the descriptor.
              checkState(previous == null);
            });

    this.itableAllocator = createItableAllocator(library);

    this.jsImports = jsImports;
  }

  private ItableAllocator<TypeDeclaration> createItableAllocator(Library library) {
    if (isModular) {
      // Itable allocation happens in the bundler for modular compilation.
      return null;
    }
    return new ItableAllocator<>(
        library
            .streamTypes()
            .filter(Predicates.not(Type::isInterface))
            .map(Type::getDeclaration)
            .collect(toImmutableList()),
        TypeDeclaration::getAllSuperInterfaces,
        WasmGenerationEnvironment::getTypeLayoutSuperTypeDeclaration);
  }

  /** Returns a wasm layout creating it from a type declaration if it wasn't created before. */
  @Nullable
  @CanIgnoreReturnValue
  private WasmTypeLayout getOrCreateWasmTypeLayout(TypeDeclaration typeDeclaration) {
    if (typeDeclaration == null) {
      return null;
    }
    if (!wasmTypeLayoutByTypeDeclaration.containsKey(typeDeclaration)) {
      // Get the wasm layout for the supertype. Note that the supertype might be or not in the
      // current library, so its layout might need to be created from a declaration. This is
      // accomplished by calling recursively "getOrCreateWasmTypeLayout" rather than assuming it
      // was already created and would be returned by "getWasmTypeLayout'.
      WasmTypeLayout superTypeLayout =
          getOrCreateWasmTypeLayout(getTypeLayoutSuperTypeDeclaration(typeDeclaration));
      WasmTypeLayout typeLayout =
          WasmTypeLayout.createFromTypeDeclaration(typeDeclaration, superTypeLayout);
      // If the supertype layout was not created by the type it is requested here,
      // it means that the type is from a different library and is ok to
      // create its layout from the type model.
      wasmTypeLayoutByTypeDeclaration.put(typeDeclaration, typeLayout);
      return typeLayout;
    }
    return wasmTypeLayoutByTypeDeclaration.get(typeDeclaration);
  }

  /** Gets a supertype declaration for the specified type to be used in generating the Wasm type. */
  @Nullable
  static TypeDeclaration getTypeLayoutSuperTypeDeclaration(TypeDeclaration typeDeclaration) {
    if (typeDeclaration.isInterface()) {
      // For interfaces, choose a suitable "superinterface". Java interfaces can inherit multiple
      // parent interfaces, which cannot be fully expressed in Wasm.
      // Here, we choose the immediate superinterface with the most methods as a heuristic to
      // minimize the number of conversions needed when calling superinterface methods.
      return typeDeclaration.getInterfaceTypeDescriptors().stream()
          .max(Comparator.comparingInt(i -> i.getPolymorphicMethods().size()))
          .map(DeclaredTypeDescriptor::getTypeDeclaration)
          .orElse(null);
    }

    return typeDeclaration.getSuperTypeDeclaration();
  }
}
