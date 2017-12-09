/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/** Utility class holding type descriptors that need to be referenced directly. */
public class TypeDescriptors {
  public DeclaredTypeDescriptor javaLangBoolean;
  public DeclaredTypeDescriptor javaLangByte;
  public DeclaredTypeDescriptor javaLangCharacter;
  public DeclaredTypeDescriptor javaLangDouble;
  public DeclaredTypeDescriptor javaLangFloat;
  public DeclaredTypeDescriptor javaLangInteger;
  public DeclaredTypeDescriptor javaLangLong;
  public DeclaredTypeDescriptor javaLangShort;
  public DeclaredTypeDescriptor javaLangString;
  public DeclaredTypeDescriptor javaLangVoid;

  public DeclaredTypeDescriptor javaLangClass;
  public DeclaredTypeDescriptor javaLangObject;
  public DeclaredTypeDescriptor javaLangThrowable;

  public DeclaredTypeDescriptor javaLangNumber;
  public DeclaredTypeDescriptor javaLangComparable;
  public DeclaredTypeDescriptor javaLangCharSequence;

  public DeclaredTypeDescriptor javaLangCloneable;
  public DeclaredTypeDescriptor javaIoSerializable;

  public ArrayTypeDescriptor javaLangObjectArray;

  // Common browser native types.
  public final DeclaredTypeDescriptor nativeFunction = createGlobalNativeTypeDescriptor("Function");
  public final DeclaredTypeDescriptor nativeObject = createGlobalNativeTypeDescriptor("Object");
  public final DeclaredTypeDescriptor nativeArray = createGlobalNativeTypeDescriptor("Array");

  /**
   * Global window reference that is the enclosing class of native global methods and properties.
   */
  public final DeclaredTypeDescriptor globalNamespace = createGlobalNativeTypeDescriptor("");

  /** Primitive type descriptors and boxed type descriptors mapping. */
  private final BiMap<PrimitiveTypeDescriptor, DeclaredTypeDescriptor> boxedTypeByPrimitiveType =
      HashBiMap.create();

  private static final ThreadLocal<TypeDescriptors> typeDescriptors = new ThreadLocal<>();

  private static void set(TypeDescriptors typeDescriptors) {
    checkState(
        TypeDescriptors.typeDescriptors.get() == null,
        "TypeDescriptors has already been initialized and cannot be reassigned.");
    TypeDescriptors.typeDescriptors.set(typeDescriptors);
  }

  public static TypeDescriptors get() {
    checkState(isInitialized(), "TypeDescriptors must be initialized before access.");
    return typeDescriptors.get();
  }

  public static boolean isInitialized() {
    return typeDescriptors.get() != null;
  }

  public static DeclaredTypeDescriptor getBoxTypeFromPrimitiveType(
      PrimitiveTypeDescriptor primitiveType) {
    return get().boxedTypeByPrimitiveType.get(primitiveType);
  }

  public static PrimitiveTypeDescriptor getPrimitiveTypeFromBoxType(TypeDescriptor boxType) {
    return get().boxedTypeByPrimitiveType.inverse().get(boxType.toNullable());
  }

  public static boolean isBoxedType(TypeDescriptor typeDescriptor) {
    return get()
        .boxedTypeByPrimitiveType
        .containsValue(typeDescriptor.toRawTypeDescriptor().toNullable());
  }

  public static boolean isNonVoidPrimitiveType(TypeDescriptor typeDescriptor) {
    return get().boxedTypeByPrimitiveType.containsKey(typeDescriptor.toRawTypeDescriptor());
  }

  public static boolean isBoxedBooleanOrDouble(TypeDescriptor typeDescriptor) {
    return TypeDescriptors.isJavaLangBoolean(typeDescriptor)
        || TypeDescriptors.isJavaLangDouble(typeDescriptor);
  }

  public static boolean isPrimitiveBoolean(TypeDescriptor typeDescriptor) {
    return PrimitiveTypes.BOOLEAN.equals(typeDescriptor);
  }

  public static boolean isPrimitiveByte(TypeDescriptor typeDescriptor) {
    return PrimitiveTypes.BYTE.equals(typeDescriptor);
  }

  public static boolean isPrimitiveChar(TypeDescriptor typeDescriptor) {
    return PrimitiveTypes.CHAR.equals(typeDescriptor);
  }

  public static boolean isPrimitiveDouble(TypeDescriptor typeDescriptor) {
    return PrimitiveTypes.DOUBLE.equals(typeDescriptor);
  }

  public static boolean isPrimitiveFloat(TypeDescriptor typeDescriptor) {
    return PrimitiveTypes.FLOAT.equals(typeDescriptor);
  }

  public static boolean isPrimitiveFloatOrDouble(TypeDescriptor typeDescriptor) {
    return isPrimitiveFloat(typeDescriptor) || isPrimitiveDouble(typeDescriptor);
  }

  public static boolean isPrimitiveInt(TypeDescriptor typeDescriptor) {
    return PrimitiveTypes.INT.equals(typeDescriptor);
  }

  public static boolean isPrimitiveLong(TypeDescriptor typeDescriptor) {
    return PrimitiveTypes.LONG.equals(typeDescriptor);
  }

  public static boolean isPrimitiveShort(TypeDescriptor typeDescriptor) {
    return PrimitiveTypes.SHORT.equals(typeDescriptor);
  }

  public static boolean isPrimitiveVoid(TypeDescriptor typeDescriptor) {
    return PrimitiveTypes.VOID.equals(typeDescriptor);
  }

  public static boolean isPrimitiveBooleanOrDouble(TypeDescriptor typeDescriptor) {
    return isPrimitiveBoolean(typeDescriptor) || isPrimitiveDouble(typeDescriptor);
  }

  public static boolean isJavaLangObject(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().javaLangObject);
  }

  public static boolean isJavaLangString(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().javaLangString);
  }

  public static boolean isJavaLangDouble(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().javaLangDouble);
  }

  public static boolean isJavaLangBoolean(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().javaLangBoolean);
  }

  public static boolean isJavaLangVoid(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().javaLangVoid);
  }

  public static boolean isJavaLangComparable(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().javaLangComparable);
  }

  public static boolean isJavaLangCharSequence(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().javaLangCharSequence);
  }

  public static boolean isJavaLangNumber(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().javaLangNumber);
  }

  public static boolean isJavaIoSerializable(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().javaIoSerializable);
  }

  public static boolean isJavaLangCloneable(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().javaLangCloneable);
  }

  public static boolean isJavaLangClass(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().javaLangClass);
  }

  public static boolean isNumericPrimitive(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isPrimitive()
        && !isPrimitiveBoolean(typeDescriptor)
        && !isPrimitiveVoid(typeDescriptor);
  }

  public static boolean isIntegralPrimitiveType(TypeDescriptor typeDescriptor) {
    return isPrimitiveShort(typeDescriptor)
        || isPrimitiveByte(typeDescriptor)
        || isPrimitiveChar(typeDescriptor)
        || isPrimitiveInt(typeDescriptor);
  }

  public static boolean isBoxedOrPrimitiveType(TypeDescriptor typeDescriptor) {
    return isBoxedType(typeDescriptor) || isNonVoidPrimitiveType(typeDescriptor);
  }

  public static boolean isBoxedTypeAsJsPrimitives(TypeDescriptor typeDescriptor) {
    return isBoxedBooleanOrDouble(typeDescriptor)
        || isJavaLangString(typeDescriptor)
        || isJavaLangVoid(typeDescriptor);
  }

  public static boolean isNonBoxedReferenceType(TypeDescriptor typeDescriptor) {
    return !typeDescriptor.isPrimitive() && !isBoxedType(typeDescriptor);
  }

  /** Returns the TypeDeclaration for the Overlay implementation type. */
  public static TypeDeclaration createOverlayImplementationTypeDeclaration(
      DeclaredTypeDescriptor typeDescriptor) {

    DeclaredTypeDescriptor unparameterizedTypeDescriptor =
        typeDescriptor.toUnparameterizedTypeDescriptor();

    List<String> classComponents =
        AstUtils.synthesizeInnerClassComponents(
            unparameterizedTypeDescriptor, AstUtilConstants.OVERLAY_IMPLEMENTATION_CLASS_SUFFIX);

    return TypeDeclaration.newBuilder()
        .setEnclosingTypeDeclaration(unparameterizedTypeDescriptor.getTypeDeclaration())
        .setClassComponents(classComponents)
        .setRawTypeDescriptorFactory(
            () ->
                createOverlayImplementationTypeDescriptor(
                    unparameterizedTypeDescriptor.toRawTypeDescriptor()))
        .setUnparameterizedTypeDescriptorFactory(
            () -> createOverlayImplementationTypeDescriptor(unparameterizedTypeDescriptor))
        .setVisibility(Visibility.PUBLIC)
        .setKind(unparameterizedTypeDescriptor.getTypeDeclaration().getKind())
        .build();
  }

  /** Returns TypeDescriptor that contains the devirtualized JsOverlay methods of a native type. */
  public static DeclaredTypeDescriptor createOverlayImplementationTypeDescriptor(
      DeclaredTypeDescriptor typeDescriptor) {
    checkArgument(typeDescriptor.isNative() || typeDescriptor.isInterface());
    checkArgument(!typeDescriptor.isTypeVariable() && !typeDescriptor.isWildCardOrCapture());

    TypeDeclaration overlayImplementationTypeDeclaration =
        createOverlayImplementationTypeDeclaration(typeDescriptor);

    return DeclaredTypeDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(typeDescriptor)
        .setTypeDeclaration(overlayImplementationTypeDeclaration)
        .setClassComponents(overlayImplementationTypeDeclaration.getClassComponents())
        .setRawTypeDescriptorFactory(td -> td.getTypeDeclaration().toRawTypeDescriptor())
        .setKind(overlayImplementationTypeDeclaration.getKind())
        .build();
  }

  public static Function<TypeDescriptor, TypeDescriptor> mappingFunctionFromMap(
      Map<TypeDescriptor, TypeDescriptor> replacingTypeDescriptorByTypeVariable) {
    return replacingTypeDescriptorByTypeVariable.isEmpty()
        ? Function.identity()
        : typeDescriptor ->
            replacingTypeDescriptorByTypeVariable.getOrDefault(typeDescriptor, typeDescriptor);
  }

  /** Holds the bootstrap types. */
  @SuppressWarnings("ImmutableEnumChecker")
  public enum BootstrapType {
    OBJECTS("vmbootstrap", "Objects"),
    COMPARABLES("vmbootstrap", "Comparables"),
    CHAR_SEQUENCES("vmbootstrap", "CharSequences"),
    NUMBERS("vmbootstrap", "Numbers"),
    ASSERTS("vmbootstrap", "Asserts"),
    ARRAYS("vmbootstrap", "Arrays"),
    CASTS("vmbootstrap", "Casts"),
    PRIMITIVES("vmbootstrap", "Primitives"),
    ENUMS("vmbootstrap", "Enums"),
    EXCEPTIONS("vmbootstrap", "Exceptions"),
    LONG_UTILS("vmbootstrap", "LongUtils"),
    JAVA_SCRIPT_OBJECT("vmbootstrap", "JavaScriptObject"),
    JAVA_SCRIPT_FUNCTION("vmbootstrap", "JavaScriptFunction"),
    NATIVE_EQUALITY("nativebootstrap", "Equality"),
    NATIVE_UTIL("nativebootstrap", "Util"),
    NATIVE_LONG("nativebootstrap", "Long");

    private final DeclaredTypeDescriptor typeDescriptor;

    BootstrapType(String packageName, String name) {
      this.typeDescriptor = createBoostrapTypeDescriptor(Kind.CLASS, packageName, name);
    }

    public DeclaredTypeDescriptor getDescriptor() {
      return typeDescriptor;
    }

    public TypeDeclaration getDeclaration() {
      return typeDescriptor.getTypeDeclaration();
    }

    public static final Set<DeclaredTypeDescriptor> typeDescriptors;

    static {
      ImmutableSet.Builder<DeclaredTypeDescriptor> setBuilder = new ImmutableSet.Builder<>();
      for (BootstrapType value : BootstrapType.values()) {
        setBuilder.add(value.getDescriptor());
      }
      typeDescriptors = setBuilder.build();
    }
  }

  // Not externally instantiable.
  private TypeDescriptors() {}

  public static DeclaredTypeDescriptor createPrimitiveMetadataTypeDescriptor(
      PrimitiveTypeDescriptor primitiveTypeDescriptor) {
    // Prepend "$" so that internal aliases start with "$".
    return createSyntheticTypeDescriptor(
        "vmbootstrap.primitives",
        "$" + primitiveTypeDescriptor.getSimpleSourceName(),
        ImmutableList.of(),
        null,
        Kind.CLASS,
        false);
  }

  /** Returns a TypeDescriptor to a Bootstrap type; used to synthesize calls to the runtime. */
  private static DeclaredTypeDescriptor createBoostrapTypeDescriptor(
      Kind kind, String packageName, String bootstrapClassName) {
    checkArgument(!bootstrapClassName.contains("<"));
    return createSyntheticTypeDescriptor(
        packageName, bootstrapClassName, ImmutableList.of(), null, kind, false);
  }

  public static DeclaredTypeDescriptor createGlobalNativeTypeDescriptor(
      String jsName, TypeDescriptor... typeArgumentDescriptors) {
    return createNativeTypeDescriptor(
        null, jsName, JsUtils.JS_PACKAGE_GLOBAL, typeArgumentDescriptors);
  }

  static DeclaredTypeDescriptor createNativeTypeDescriptor(
      String packageName,
      String className,
      String jsNamespace,
      TypeDescriptor... typeArgumentDescriptors) {
    return createSyntheticTypeDescriptor(
        packageName,
        className,
        Arrays.asList(typeArgumentDescriptors),
        jsNamespace,
        Kind.INTERFACE,
        true);
  }

  /**
   * Returns a TypeDescriptor that is not related to Java classes.
   *
   * <p>Used to synthesize type descriptors to Bootstrap types and native JS types.
   */
  private static DeclaredTypeDescriptor createSyntheticTypeDescriptor(
      final String packageName,
      final String className,
      final List<TypeDescriptor> typeArgumentDescriptors,
      final String jsNamespace,
      final Kind kind,
      final boolean isNative) {
    Supplier<DeclaredTypeDescriptor> rawTypeDescriptorFactory =
        () ->
            createSyntheticTypeDescriptor(
                packageName, className, ImmutableList.of(), jsNamespace, kind, isNative);

    TypeDeclaration typeDeclaration =
        TypeDeclaration.newBuilder()
            .setClassComponents(ImmutableList.of(className))
            .setNative(isNative)
            .setJsNamespace(jsNamespace)
            .setPackageName(packageName)
            .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
            .setUnparameterizedTypeDescriptorFactory(
                () ->
                    createSyntheticTypeDescriptor(
                        packageName, className, ImmutableList.of(), jsNamespace, kind, isNative))
            // Synthetic type declarations do not need to have type variables.
            // TODO(b/63118697): Make sure declaratations are consistent with descriptor w.r.t
            // type parameters.
            .setTypeParameterDescriptors(ImmutableList.of())
            .setVisibility(Visibility.PUBLIC)
            .setKind(kind)
            .build();

    return DeclaredTypeDescriptor.newBuilder()
        .setClassComponents(ImmutableList.of(className))
        .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
        .setTypeDeclaration(typeDeclaration)
        .setTypeArgumentDescriptors(typeArgumentDescriptors)
        .setKind(kind)
        .build();
  }

  /** Returns the unparameterized version of {@code typeDescriptors}. */
  @SuppressWarnings("unchecked")
  public static <T extends TypeDescriptor> ImmutableList<T> toUnparameterizedTypeDescriptors(
      List<T> typeDescriptors) {
    return typeDescriptors
        .stream()
        .map(TypeDescriptor::toUnparameterizedTypeDescriptor)
        .map(typeDescriptor -> (T) typeDescriptor)
        .collect(ImmutableList.toImmutableList());
  }

  /** Builder for TypeDescriptors. */
  public static class Builder {

    private final TypeDescriptors typeDescriptors = new TypeDescriptors();

    public void init() {
      set(typeDescriptors);
      typeDescriptors.javaLangObjectArray =
          ArrayTypeDescriptor.newBuilder()
              .setComponentTypeDescriptor(typeDescriptors.javaLangObject)
              .build();
    }

    public Builder addPrimitiveBoxedTypeDescriptorPair(
        PrimitiveTypeDescriptor primitiveType, DeclaredTypeDescriptor boxedType) {
      addReferenceType(boxedType);
      addBoxedTypeMapping(primitiveType, boxedType);
      return this;
    }

    public Builder addReferenceType(DeclaredTypeDescriptor referenceType) {
      checkArgument(
          !referenceType.isPrimitive(),
          "%s is not a reference type",
          referenceType.getQualifiedSourceName());
      String name = referenceType.getQualifiedSourceName();
      switch (name) {
        case "java.io.Serializable":
          typeDescriptors.javaIoSerializable = referenceType;
          break;
        case "java.lang.Boolean":
          typeDescriptors.javaLangBoolean = referenceType;
          break;
        case "java.lang.Byte":
          typeDescriptors.javaLangByte = referenceType;
          break;
        case "java.lang.Character":
          typeDescriptors.javaLangCharacter = referenceType;
          break;
        case "java.lang.Double":
          typeDescriptors.javaLangDouble = referenceType;
          break;
        case "java.lang.Float":
          typeDescriptors.javaLangFloat = referenceType;
          break;
        case "java.lang.Integer":
          typeDescriptors.javaLangInteger = referenceType;
          break;
        case "java.lang.Long":
          typeDescriptors.javaLangLong = referenceType;
          break;
        case "java.lang.Short":
          typeDescriptors.javaLangShort = referenceType;
          break;
        case "java.lang.String":
          typeDescriptors.javaLangString = referenceType;
          break;
        case "java.lang.Void":
          typeDescriptors.javaLangVoid = referenceType;
          break;
        case "java.lang.Class":
          typeDescriptors.javaLangClass = referenceType;
          break;
        case "java.lang.Object":
          typeDescriptors.javaLangObject = referenceType;
          break;
        case "java.lang.Throwable":
          typeDescriptors.javaLangThrowable = referenceType;
          break;
        case "java.lang.Number":
          typeDescriptors.javaLangNumber = referenceType;
          break;
        case "java.lang.Comparable":
          typeDescriptors.javaLangComparable = referenceType;
          break;
        case "java.lang.CharSequence":
          typeDescriptors.javaLangCharSequence = referenceType;
          break;
        case "java.lang.Cloneable":
          typeDescriptors.javaLangCloneable = referenceType;
          break;
        default:
          throw new IllegalStateException("Unexpected reference type in well known set: " + name);
      }
      return this;
    }

    private TypeDescriptor addBoxedTypeMapping(
        PrimitiveTypeDescriptor primitiveType, DeclaredTypeDescriptor boxedType) {
      return typeDescriptors.boxedTypeByPrimitiveType.put(primitiveType, boxedType);
    }
  }
}
