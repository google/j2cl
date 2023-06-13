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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;

/** Utility class holding type descriptors that need to be referenced directly. */
public class TypeDescriptors {
  // Boxed types.
  public DeclaredTypeDescriptor javaLangBoolean;
  public DeclaredTypeDescriptor javaLangByte;
  public DeclaredTypeDescriptor javaLangCharacter;
  public DeclaredTypeDescriptor javaLangDouble;
  public DeclaredTypeDescriptor javaLangFloat;
  public DeclaredTypeDescriptor javaLangInteger;
  public DeclaredTypeDescriptor javaLangLong;
  public DeclaredTypeDescriptor javaLangShort;
  public DeclaredTypeDescriptor javaLangVoid;

  @QualifiedBinaryName("java.lang.CharSequence")
  public DeclaredTypeDescriptor javaLangCharSequence;

  public DeclaredTypeDescriptor javaLangClass;
  public DeclaredTypeDescriptor javaLangCloneable;
  public DeclaredTypeDescriptor javaLangComparable;
  public DeclaredTypeDescriptor javaLangEnum;
  public DeclaredTypeDescriptor javaLangIterable;

  @QualifiedBinaryName("java.lang.NullPointerException")
  public DeclaredTypeDescriptor javaLangNullPointerException;

  public DeclaredTypeDescriptor javaLangNumber;
  public DeclaredTypeDescriptor javaLangObject;
  public DeclaredTypeDescriptor javaLangRunnable;
  public DeclaredTypeDescriptor javaLangString;

  @QualifiedBinaryName("java.lang.StringBuilder")
  public DeclaredTypeDescriptor javaLangStringBuilder;

  public DeclaredTypeDescriptor javaLangThrowable;

  public DeclaredTypeDescriptor javaUtilArrays;
  public DeclaredTypeDescriptor javaUtilCollection;
  public DeclaredTypeDescriptor javaUtilIterator;
  public DeclaredTypeDescriptor javaUtilMap;
  public DeclaredTypeDescriptor javaUtilObjects;

  public DeclaredTypeDescriptor javaIoSerializable;

  @Nullable
  @QualifiedBinaryName("javaemul.internal.WasmArray")
  public DeclaredTypeDescriptor javaemulInternalWasmArray;

  @Nullable
  @QualifiedBinaryName("javaemul.internal.WasmArray$OfByte")
  public DeclaredTypeDescriptor javaemulInternalWasmArrayOfByte;

  @Nullable
  @QualifiedBinaryName("javaemul.internal.WasmArray$OfShort")
  public DeclaredTypeDescriptor javaemulInternalWasmArrayOfShort;

  @Nullable
  @QualifiedBinaryName("javaemul.internal.WasmArray$OfChar")
  public DeclaredTypeDescriptor javaemulInternalWasmArrayOfChar;

  @Nullable
  @QualifiedBinaryName("javaemul.internal.WasmArray$OfInt")
  public DeclaredTypeDescriptor javaemulInternalWasmArrayOfInt;

  @Nullable
  @QualifiedBinaryName("javaemul.internal.WasmArray$OfLong")
  public DeclaredTypeDescriptor javaemulInternalWasmArrayOfLong;

  @Nullable
  @QualifiedBinaryName("javaemul.internal.WasmArray$OfFloat")
  public DeclaredTypeDescriptor javaemulInternalWasmArrayOfFloat;

  @Nullable
  @QualifiedBinaryName("javaemul.internal.WasmArray$OfDouble")
  public DeclaredTypeDescriptor javaemulInternalWasmArrayOfDouble;

  @Nullable
  @QualifiedBinaryName("javaemul.internal.WasmArray$OfBoolean")
  public DeclaredTypeDescriptor javaemulInternalWasmArrayOfBoolean;

  @Nullable
  @QualifiedBinaryName("javaemul.internal.WasmArray$OfObject")
  public DeclaredTypeDescriptor javaemulInternalWasmArrayOfObject;

  @Nullable public DeclaredTypeDescriptor javaemulInternalAsserts;

  @Nullable
  @QualifiedBinaryName("javaemul.internal.ValueType")
  public DeclaredTypeDescriptor javaemulInternalValueType;

  @Nullable
  @QualifiedBinaryName("javaemul.internal.InternalPreconditions")
  public DeclaredTypeDescriptor javaemulInternalPreconditions;

  @Nullable public DeclaredTypeDescriptor javaemulInternalPrimitives;
  @Nullable public DeclaredTypeDescriptor javaemulInternalEnums;

  @Nullable
  @QualifiedBinaryName("javaemul.internal.Enums$BoxedLightEnum")
  public DeclaredTypeDescriptor javaemulInternalBoxedLightEnum;

  @Nullable
  @QualifiedBinaryName("javaemul.internal.Enums$BoxedComparableLightEnum")
  public DeclaredTypeDescriptor javaemulInternalBoxedComparableLightEnum;

  @Nullable public DeclaredTypeDescriptor javaemulInternalConstructor;
  @Nullable public DeclaredTypeDescriptor javaemulInternalPlatform;
  public DeclaredTypeDescriptor javaemulInternalExceptions;

  public ArrayTypeDescriptor javaLangObjectArray;

  // Common browser native types.
  public final DeclaredTypeDescriptor nativeFunction = createGlobalNativeTypeDescriptor("Function");
  public final DeclaredTypeDescriptor nativeObject = createGlobalNativeTypeDescriptor("Object");
  public final DeclaredTypeDescriptor nativeArray = createGlobalNativeTypeDescriptor("Array");
  public final DeclaredTypeDescriptor nativeError = createGlobalNativeTypeDescriptor("Error");
  public final DeclaredTypeDescriptor nativeTypeError =
      createGlobalNativeTypeDescriptor("TypeError");

  // Kotlin-specific types
  @Nullable
  @QualifiedBinaryName("kotlin.jvm.internal.NothingStub")
  public DeclaredTypeDescriptor kotlinNothing;

  @Nullable public DeclaredTypeDescriptor kotlinJvmInternalIntrinsics;

  @Nullable
  @QualifiedBinaryName("kotlin.jvm.internal.MutableKProperty0Impl")
  public DeclaredTypeDescriptor kotlinJvmInternalMutableKProperty0Impl;

  @Nullable
  @QualifiedBinaryName("kotlin.jvm.internal.MutableKProperty1Impl")
  public DeclaredTypeDescriptor kotlinJvmInternalMutableKProperty1Impl;

  @Nullable
  @QualifiedBinaryName("kotlin.jvm.internal.ReflectionFactory")
  public DeclaredTypeDescriptor kotlinJvmInternalReflectionFactory;

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

  public static TypeVariable getUnknownType() {
    return TypeVariable.createWildcard();
  }

  static DeclaredTypeDescriptor getBoxTypeFromPrimitiveType(PrimitiveTypeDescriptor primitiveType) {
    return get().boxedTypeByPrimitiveType.get(primitiveType);
  }

  static PrimitiveTypeDescriptor getPrimitiveTypeFromBoxType(TypeDescriptor boxType) {
    return get().boxedTypeByPrimitiveType.inverse().get(boxType.toNullable());
  }

  public static boolean isBoxedType(TypeDescriptor typeDescriptor) {
    return get()
            .boxedTypeByPrimitiveType
            .containsValue(typeDescriptor.toRawTypeDescriptor().toNullable())
        && !isJavaLangVoid(typeDescriptor);
  }

  public static boolean isNonVoidPrimitiveType(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isPrimitive() && !isPrimitiveVoid(typeDescriptor);
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
    return typeDescriptor.isSameBaseType(get().javaLangObject);
  }

  public static boolean isJavaLangString(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(get().javaLangString);
  }

  public static boolean isJavaLangByte(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(get().javaLangByte);
  }

  public static boolean isJavaLangShort(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(get().javaLangShort);
  }

  public static boolean isJavaLangInteger(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(get().javaLangInteger);
  }

  public static boolean isJavaLangCharacter(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(get().javaLangCharacter);
  }

  public static boolean isJavaLangLong(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(get().javaLangLong);
  }

  public static boolean isJavaLangFloat(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(get().javaLangFloat);
  }

  public static boolean isJavaLangDouble(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(get().javaLangDouble);
  }

  public static boolean isJavaLangBoolean(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(get().javaLangBoolean);
  }

  public static boolean isJavaLangVoid(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(get().javaLangVoid);
  }

  public static boolean isJavaLangComparable(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(get().javaLangComparable);
  }

  public static boolean isJavaLangCharSequence(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(get().javaLangCharSequence);
  }

  public static boolean isJavaLangNumber(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(get().javaLangNumber);
  }

  public static boolean isJavaIoSerializable(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(get().javaIoSerializable);
  }

  public static boolean isJavaLangCloneable(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(get().javaLangCloneable);
  }

  public static boolean isJavaLangEnum(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(get().javaLangEnum);
  }

  public static boolean isJavaLangThrowable(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(get().javaLangThrowable);
  }

  public static boolean isKotlinNothing(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(get().kotlinNothing);
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
        || isPrimitiveInt(typeDescriptor)
        || isPrimitiveLong(typeDescriptor);
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

  public static boolean isBoxedEnum(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isSameBaseType(TypeDescriptors.get().javaemulInternalBoxedLightEnum)
        || typeDescriptor.isSameBaseType(
            TypeDescriptors.get().javaemulInternalBoxedComparableLightEnum);
  }

  public static boolean isWasmArraySubtype(TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof DeclaredTypeDescriptor) {
      DeclaredTypeDescriptor superTypeDescriptor =
          ((DeclaredTypeDescriptor) typeDescriptor).getSuperTypeDescriptor();
      return get().javaemulInternalWasmArray.isSameBaseType(superTypeDescriptor);
    }
    return false;
  }

  public static TypeDescriptor getEnumBoxType(TypeDescriptor typeDescriptor) {
    checkState(AstUtils.isNonNativeJsEnum(typeDescriptor));
    TypeDescriptor boxType =
        typeDescriptor.getJsEnumInfo().supportsComparable()
            ? TypeDescriptors.get().javaemulInternalBoxedComparableLightEnum
            : TypeDescriptors.get().javaemulInternalBoxedLightEnum;
    TypeDescriptor specializedType =
        boxType.specializeTypeVariables(
            ImmutableMap.of(
                Iterables.getOnlyElement(boxType.getAllTypeVariables()), typeDescriptor));
    return typeDescriptor.isNullable() ? specializedType : specializedType.toNonNullable();
  }

  /** Gets the type descriptor representing a native string. */
  // TODO(b/257574608): This can be refactored along with improvements to well-known types.
  public static TypeDescriptor getNativeStringType() {
    // NativeString is not visible on all frontends so get it from a String method instead of
    // returning a cached value.
    return RuntimeMethods.getJsStringFromStringMethod().getReturnTypeDescriptor();
  }

  static Function<TypeVariable, ? extends TypeDescriptor> mappingFunctionFromMap(
      Map<TypeVariable, TypeDescriptor> replacingTypeDescriptorByTypeVariable) {
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
    ARRAYS("vmbootstrap", "Arrays"),
    CASTS("vmbootstrap", "Casts"),
    LONG_UTILS("vmbootstrap", "LongUtils"),
    JAVA_SCRIPT_OBJECT("vmbootstrap", "JavaScriptObject"),
    JAVA_SCRIPT_INTERFACE("vmbootstrap", "JavaScriptInterface"),
    JAVA_SCRIPT_FUNCTION("vmbootstrap", "JavaScriptFunction"),
    NATIVE_EQUALITY("nativebootstrap", "Equality"),
    NATIVE_UTIL("nativebootstrap", "Util"),
    NATIVE_LONG("nativebootstrap", "Long");

    private final DeclaredTypeDescriptor typeDescriptor;

    BootstrapType(String namespace, String name) {
      this.typeDescriptor = createSyntheticTypeDescriptor(Kind.CLASS, namespace, name);
    }

    public DeclaredTypeDescriptor getDescriptor() {
      return typeDescriptor;
    }

    public TypeDeclaration getDeclaration() {
      return typeDescriptor.getTypeDeclaration();
    }
  }

  // Not externally instantiable.
  private TypeDescriptors() {}

  public static DeclaredTypeDescriptor createPrimitiveMetadataTypeDescriptor(
      PrimitiveTypeDescriptor primitiveTypeDescriptor) {
    // Prepend "$" so that internal aliases start with "$".
    return createSyntheticTypeDescriptor(
        Kind.CLASS, "vmbootstrap.primitives", "$" + primitiveTypeDescriptor.getSimpleSourceName());
  }

  public static DeclaredTypeDescriptor createGlobalNativeTypeDescriptor(
      String jsName, TypeDescriptor... typeArgumentDescriptors) {
    return createNativeTypeDescriptor(JsUtils.JS_PACKAGE_GLOBAL, jsName, typeArgumentDescriptors);
  }

  static DeclaredTypeDescriptor createNativeTypeDescriptor(
      String jsNamespace, String className, TypeDescriptor... typeArgumentDescriptors) {
    return createSyntheticTypeDescriptor(
        Kind.INTERFACE, jsNamespace, className, typeArgumentDescriptors);
  }

  /**
   * Returns a TypeDescriptor that is not related to Java classes.
   *
   * <p>Used to synthesize type descriptors to Bootstrap types and native JS types.
   */
  private static DeclaredTypeDescriptor createSyntheticTypeDescriptor(
      Kind kind, String jsNamespace, String className, TypeDescriptor... typeArgumentDescriptors) {

    TypeDeclaration typeDeclaration =
        TypeDeclaration.newBuilder()
            .setClassComponents(ImmutableList.of(className))
            // Mark bootstrap classes as non native so that the goog.require doesn't reference
            // overlay.
            .setNative(!isBootstrapNamespace(jsNamespace))
            .setCustomizedJsNamespace(jsNamespace)
            .setPackageName(getSyntheticPackageName(jsNamespace))
            .setUnparameterizedTypeDescriptorFactory(
                () -> createSyntheticTypeDescriptor(kind, jsNamespace, className))
            // Synthetic type declarations do not need to have type variables.
            // TODO(b/63118697): Make sure declarations are consistent with descriptor w.r.t
            // type parameters.
            .setTypeParameterDescriptors(ImmutableList.of())
            .setVisibility(Visibility.PUBLIC)
            .setKind(kind)
            .build();

    return DeclaredTypeDescriptor.newBuilder()
        .setTypeDeclaration(typeDeclaration)
        .setTypeArgumentDescriptors(Arrays.asList(typeArgumentDescriptors))
        .build();
  }

  private static boolean isBootstrapNamespace(String jsNamespace) {
    String topNamespace = Iterables.get(Splitter.on('.').split(jsNamespace), 0);
    return topNamespace.equals("vmbootstrap")
        || topNamespace.equals("nativebootstrap")
        || topNamespace.equals("javaemul");
  }

  public static boolean isBootstrapNamespace(DeclaredTypeDescriptor typeDescriptor) {
    return isBootstrapNamespace(typeDescriptor.getQualifiedJsName());
  }

  /**
   * Synthesize a package name with an explicit prefix to avoid colliding with existing types. The
   * package name is part of the key for TypeDeclaration, and if two keys collide they will resolve
   * to the same TypeDeclaration creating potential for inconsistencies. The prefix "$synthetic" was
   * chosen to avoid collision with packages in the actual source code.
   */
  private static String getSyntheticPackageName(String jsNamespace) {
    if (isBootstrapNamespace(jsNamespace)) {
      // Avoid prepending synthetic to our runtime types. Those are not really synthetic. Bootstrap
      // types are handwritten non native types.
      return jsNamespace;
    }
    return "$synthetic." + jsNamespace;
  }

  /** Returns the unparameterized version of {@code typeDescriptors}. */
  @SuppressWarnings("unchecked")
  public static <T extends TypeDescriptor> ImmutableList<T> toUnparameterizedTypeDescriptors(
      List<T> typeDescriptors) {
    return typeDescriptors.stream()
        .map(TypeDescriptor::toUnparameterizedTypeDescriptor)
        .map(typeDescriptor -> (T) typeDescriptor)
        .collect(toImmutableList());
  }

  /** Return java implementation class for an array */
  public static DeclaredTypeDescriptor getWasmArrayType(ArrayTypeDescriptor arrayTypeDescriptor) {
    TypeDescriptor componentTypeDescriptor = arrayTypeDescriptor.getComponentTypeDescriptor();

    if (!componentTypeDescriptor.isPrimitive()) {
      return TypeDescriptors.get().javaemulInternalWasmArrayOfObject;
    }

    switch (((PrimitiveTypeDescriptor) componentTypeDescriptor).getSimpleSourceName()) {
      case "boolean":
        return TypeDescriptors.get().javaemulInternalWasmArrayOfBoolean;
      case "short":
        return TypeDescriptors.get().javaemulInternalWasmArrayOfShort;
      case "char":
        return TypeDescriptors.get().javaemulInternalWasmArrayOfChar;
      case "byte":
        return TypeDescriptors.get().javaemulInternalWasmArrayOfByte;
      case "int":
        return TypeDescriptors.get().javaemulInternalWasmArrayOfInt;
      case "long":
        return TypeDescriptors.get().javaemulInternalWasmArrayOfLong;
      case "float":
        return TypeDescriptors.get().javaemulInternalWasmArrayOfFloat;
      case "double":
        return TypeDescriptors.get().javaemulInternalWasmArrayOfDouble;
      default:
        throw new AssertionError("Unsupported primitive type: " + componentTypeDescriptor);
    }
  }

  /** Builder for TypeDescriptors. */
  public static class SingletonBuilder {

    private final TypeDescriptors typeDescriptors = new TypeDescriptors();
    private Map<String, DeclaredTypeDescriptor> knownTypesByQualifiedName = new HashMap<>();
    private final Set<String> requiredTypes = new HashSet<>(requiredWellKnownTypes);

    public void buildSingleton() {
      // All the wellknown reference types are loaded, add the primitive <-> boxed types mapping.
      for (PrimitiveTypeDescriptor primitiveTypeDescriptor : PrimitiveTypes.TYPES) {
        addBoxedTypeMapping(
            primitiveTypeDescriptor,
            knownTypesByQualifiedName.get(primitiveTypeDescriptor.getBoxedClassName()));
      }

      if (!requiredTypes.isEmpty()) {
        throw new InternalCompilerError(format("Missing well known types %s.", requiredTypes));
      }
      set(typeDescriptors);
      typeDescriptors.javaLangObjectArray =
          ArrayTypeDescriptor.newBuilder()
              .setComponentTypeDescriptor(typeDescriptors.javaLangObject)
              .build();
    }

    public SingletonBuilder addReferenceType(DeclaredTypeDescriptor referenceType) {
      checkArgument(
          !referenceType.isPrimitive(),
          "%s is not a reference type",
          referenceType.getQualifiedSourceName());
      String name = referenceType.getQualifiedBinaryName();
      knownTypesByQualifiedName.put(name, referenceType);
      Field field = checkNotNull(wellKnownTypeFieldsByQualifiedName.get(name));
      try {
        field.set(typeDescriptors, referenceType);
        requiredTypes.remove(name);
      } catch (IllegalAccessException e) {
        throw new InternalCompilerError(
            e, format("Could not set field for well known type '%s'.", name));
      }
      return this;
    }

    private TypeDescriptor addBoxedTypeMapping(
        PrimitiveTypeDescriptor primitiveType, DeclaredTypeDescriptor boxedType) {
      return typeDescriptors.boxedTypeByPrimitiveType.put(primitiveType, boxedType);
    }
  }

  static final Map<String, Field> wellKnownTypeFieldsByQualifiedName = new LinkedHashMap<>();
  static final Set<String> requiredWellKnownTypes = new HashSet<>();

  public static Set<String> getWellKnownTypeNames() {
    return wellKnownTypeFieldsByQualifiedName.keySet();
  }

  static {
    // Iterate over the public non final fields.
    stream(TypeDescriptors.class.getDeclaredFields())
        .sorted(Comparator.comparing(Field::getName))
        .filter(f -> (f.getModifiers() & Modifier.FINAL) == 0)
        .filter(f -> f.getType().equals(DeclaredTypeDescriptor.class))
        .forEach(
            f -> {
              String name = getClassBinaryName(f);
              checkState(wellKnownTypeFieldsByQualifiedName.put(name, f) == null);
              if (f.getDeclaredAnnotation(Nullable.class) == null) {
                requiredWellKnownTypes.add(name);
              }
            });
  }

  /**
   * Constructs the class name from the field name, assumes that capital letter split components and
   * that all but the last component is part of the package.
   *
   * <p>This works for most fields, except multiword class names and nested classes.
   */
  private static String getClassBinaryName(Field field) {
    QualifiedBinaryName qualifiedNameAnnotation =
        field.getDeclaredAnnotation(QualifiedBinaryName.class);
    if (qualifiedNameAnnotation != null) {
      return qualifiedNameAnnotation.value();
    }
    List<String> components = Arrays.asList(field.getName().split("(?=\\p{Upper})"));
    return components.subList(0, components.size() - 1).stream()
            .map(String::toLowerCase)
            .collect(joining(".", "", "."))
        + Iterables.getLast(components);
  }

  /** Annotation to declare the actual binary name for the well known type field. */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  @interface QualifiedBinaryName {
    String value();
  }
}

