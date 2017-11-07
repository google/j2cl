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
  public PrimitiveTypeDescriptor primitiveBoolean;
  public PrimitiveTypeDescriptor primitiveByte;
  public PrimitiveTypeDescriptor primitiveChar;
  public PrimitiveTypeDescriptor primitiveDouble;
  public PrimitiveTypeDescriptor primitiveFloat;
  public PrimitiveTypeDescriptor primitiveInt;
  public PrimitiveTypeDescriptor primitiveLong;
  public PrimitiveTypeDescriptor primitiveShort;
  public PrimitiveTypeDescriptor primitiveVoid;

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

  public static final String SHORT_TYPE_NAME = "short";
  public static final String LONG_TYPE_NAME = "long";
  public static final String FLOAT_TYPE_NAME = "float";
  public static final String DOUBLE_TYPE_NAME = "double";
  public static final String CHAR_TYPE_NAME = "char";
  public static final String BYTE_TYPE_NAME = "byte";
  public static final String BOOLEAN_TYPE_NAME = "boolean";
  public static final String INT_TYPE_NAME = "int";
  public static final String VOID_TYPE_NAME = "void";

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
        .containsValue(typeDescriptor.getRawTypeDescriptor().toNullable());
  }

  public static boolean isNonVoidPrimitiveType(TypeDescriptor typeDescriptor) {
    return get().boxedTypeByPrimitiveType.containsKey(typeDescriptor.getRawTypeDescriptor());
  }

  public static boolean isBoxedBooleanOrDouble(TypeDescriptor typeDescriptor) {
    return TypeDescriptors.isJavaLangBoolean(typeDescriptor)
        || TypeDescriptors.isJavaLangDouble(typeDescriptor);
  }

  public static boolean isPrimitiveBoolean(TypeDescriptor typeDescriptor) {
    return get().primitiveBoolean.equals(typeDescriptor);
  }

  public static boolean isPrimitiveByte(TypeDescriptor typeDescriptor) {
    return get().primitiveByte.equals(typeDescriptor);
  }

  public static boolean isPrimitiveChar(TypeDescriptor typeDescriptor) {
    return get().primitiveChar.equals(typeDescriptor);
  }

  public static boolean isPrimitiveDouble(TypeDescriptor typeDescriptor) {
    return get().primitiveDouble.equals(typeDescriptor);
  }

  public static boolean isPrimitiveFloat(TypeDescriptor typeDescriptor) {
    return get().primitiveFloat.equals(typeDescriptor);
  }

  public static boolean isPrimitiveFloatOrDouble(TypeDescriptor typeDescriptor) {
    return isPrimitiveFloat(typeDescriptor) || isPrimitiveDouble(typeDescriptor);
  }

  public static boolean isPrimitiveInt(TypeDescriptor typeDescriptor) {
    return get().primitiveInt.equals(typeDescriptor);
  }

  public static boolean isPrimitiveLong(TypeDescriptor typeDescriptor) {
    return get().primitiveLong.equals(typeDescriptor);
  }

  public static boolean isPrimitiveShort(TypeDescriptor typeDescriptor) {
    return get().primitiveShort.equals(typeDescriptor);
  }

  public static boolean isPrimitiveVoid(TypeDescriptor typeDescriptor) {
    return get().primitiveVoid.equals(typeDescriptor);
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

  /**
   * Returns an idea of the "width" of a numeric primitive type to help with deciding when a
   * conversion would be a narrowing and when it would be a widening.
   *
   * <p>Even though the floating point types are only 4 and 8 bytes respectively they are considered
   * very wide because of the magnitude of the maximum values they can encode.
   */
  public static int getWidth(TypeDescriptor typeDescriptor) {
    checkArgument(isNumericPrimitive(typeDescriptor));

    if (isPrimitiveByte(typeDescriptor)) {
      return 1;
    } else if (isPrimitiveShort(typeDescriptor) || isPrimitiveChar(typeDescriptor)) {
      return 2;
    } else if (isPrimitiveInt(typeDescriptor)) {
      return 4;
    } else if (isPrimitiveLong(typeDescriptor)) {
      return 8;
    } else if (isPrimitiveFloat(typeDescriptor)) {
      return 4 + 100;
    } else {
      checkArgument(isPrimitiveDouble(typeDescriptor));
      return 8 + 100;
    }
  }

  /** Returns the TypeDeclaration for the Overlay implementation type. */
  private static TypeDeclaration createOverlayImplementationTypeDeclaration(
      DeclaredTypeDescriptor typeDescriptor) {

    DeclaredTypeDescriptor unparameterizedTypeDescriptor =
        typeDescriptor.unparameterizedTypeDescriptor();

    List<String> classComponents =
        AstUtils.synthesizeInnerClassComponents(
            unparameterizedTypeDescriptor, AstUtilConstants.OVERLAY_IMPLEMENTATION_CLASS_SUFFIX);

    return TypeDeclaration.newBuilder()
        .setEnclosingTypeDeclaration(unparameterizedTypeDescriptor.getTypeDeclaration())
        .setClassComponents(classComponents)
        .setRawTypeDescriptorFactory(
            () ->
                createOverlayImplementationTypeDescriptor(
                    unparameterizedTypeDescriptor.getRawTypeDescriptor()))
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
    checkArgument(typeDescriptor.hasTypeDeclaration());

    TypeDeclaration overlayImplementationTypeDeclaration =
        createOverlayImplementationTypeDeclaration(typeDescriptor);

    return DeclaredTypeDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(typeDescriptor)
        .setTypeDeclaration(overlayImplementationTypeDeclaration)
        .setClassComponents(overlayImplementationTypeDeclaration.getClassComponents())
        .setRawTypeDescriptorFactory(td -> td.getTypeDeclaration().getRawTypeDescriptor())
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

  public static TypeDescriptor withNullability(TypeDescriptor typeDescriptor, boolean nullable) {
    return nullable ? typeDescriptor.toNullable() : typeDescriptor.toNonNullable();
  }

  /** Returns the unparameterized version of {@code typeDescriptors}. */
  @SuppressWarnings("unchecked")
  public static <T extends TypeDescriptor> ImmutableList<T> toUnparameterizedTypeDescriptors(
      List<T> typeDescriptors) {
    return typeDescriptors
        .stream()
        .map(TypeDescriptor::unparameterizedTypeDescriptor)
        .map(typeDescriptor -> (T) typeDescriptor)
        .collect(ImmutableList.toImmutableList());
  }

  public static Expression getDefaultValue(TypeDescriptor typeDescriptor) {
    checkArgument(!typeDescriptor.isUnion());

    // Primitives.
    switch (typeDescriptor.getQualifiedSourceName()) {
      case BOOLEAN_TYPE_NAME:
        return BooleanLiteral.get(false);
      case BYTE_TYPE_NAME:
      case SHORT_TYPE_NAME:
      case INT_TYPE_NAME:
      case FLOAT_TYPE_NAME:
      case DOUBLE_TYPE_NAME:
      case CHAR_TYPE_NAME:
        return new NumberLiteral(typeDescriptor, 0);
      case LONG_TYPE_NAME:
        return new NumberLiteral(typeDescriptor, 0L);
      default:
        return NullLiteral.get();
    }
  }

  /** Builder for TypeDescriptors. */
  public static class SingletonInitializer {

    private final TypeDescriptors typeDescriptors = new TypeDescriptors();

    public void init() {
      set(typeDescriptors);
      typeDescriptors.javaLangObjectArray =
          ArrayTypeDescriptor.newBuilder()
              .setComponentTypeDescriptor(typeDescriptors.javaLangObject)
              .build();
    }

    public SingletonInitializer addPrimitiveBoxedTypeDescriptorPair(
        PrimitiveTypeDescriptor primitiveType, DeclaredTypeDescriptor boxedType) {
      addPrimitiveType(primitiveType);
      addReferenceType(boxedType);
      addBoxedTypeMapping(primitiveType, boxedType);
      return this;
    }

    public SingletonInitializer addPrimitiveType(PrimitiveTypeDescriptor primitiveType) {
      checkArgument(
          primitiveType.isPrimitive(),
          "%s is not a primitive type",
          primitiveType.getQualifiedSourceName());
      String name = primitiveType.getQualifiedSourceName();
      switch (name) {
        case TypeDescriptors.BOOLEAN_TYPE_NAME:
          typeDescriptors.primitiveBoolean = primitiveType;
          break;
        case TypeDescriptors.BYTE_TYPE_NAME:
          typeDescriptors.primitiveByte = primitiveType;
          break;
        case TypeDescriptors.CHAR_TYPE_NAME:
          typeDescriptors.primitiveChar = primitiveType;
          break;
        case TypeDescriptors.DOUBLE_TYPE_NAME:
          typeDescriptors.primitiveDouble = primitiveType;
          break;
        case TypeDescriptors.FLOAT_TYPE_NAME:
          typeDescriptors.primitiveFloat = primitiveType;
          break;
        case TypeDescriptors.INT_TYPE_NAME:
          typeDescriptors.primitiveInt = primitiveType;
          break;
        case TypeDescriptors.LONG_TYPE_NAME:
          typeDescriptors.primitiveLong = primitiveType;
          break;
        case TypeDescriptors.SHORT_TYPE_NAME:
          typeDescriptors.primitiveShort = primitiveType;
          break;
        case TypeDescriptors.VOID_TYPE_NAME:
          typeDescriptors.primitiveVoid = primitiveType;
          break;
        default:
          throw new IllegalStateException("Unexpected primitive type in well known set: " + name);
      }
      return this;
    }

    public SingletonInitializer addReferenceType(DeclaredTypeDescriptor referenceType) {
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
