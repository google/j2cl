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
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.j2cl.ast.TypeDescriptor.DescriptorFactory;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/** Utility class holding type descriptors that need to be referenced directly. */
public class TypeDescriptors {
  public TypeDescriptor primitiveBoolean;
  public TypeDescriptor primitiveByte;
  public TypeDescriptor primitiveChar;
  public TypeDescriptor primitiveDouble;
  public TypeDescriptor primitiveFloat;
  public TypeDescriptor primitiveInt;
  public TypeDescriptor primitiveLong;
  public TypeDescriptor primitiveShort;
  public TypeDescriptor primitiveVoid;

  public TypeDescriptor javaLangBoolean;
  public TypeDescriptor javaLangByte;
  public TypeDescriptor javaLangCharacter;
  public TypeDescriptor javaLangDouble;
  public TypeDescriptor javaLangFloat;
  public TypeDescriptor javaLangInteger;
  public TypeDescriptor javaLangLong;
  public TypeDescriptor javaLangShort;
  public TypeDescriptor javaLangString;
  public TypeDescriptor javaLangVoid;

  public TypeDescriptor javaLangClass;
  public TypeDescriptor javaLangObject;
  public TypeDescriptor javaLangThrowable;

  public TypeDescriptor javaLangNumber;
  public TypeDescriptor javaLangComparable;
  public TypeDescriptor javaLangCharSequence;

  public static final String SHORT_TYPE_NAME = "short";
  public static final String LONG_TYPE_NAME = "long";
  public static final String FLOAT_TYPE_NAME = "float";
  public static final String DOUBLE_TYPE_NAME = "double";
  public static final String CHAR_TYPE_NAME = "char";
  public static final String BYTE_TYPE_NAME = "byte";
  public static final String BOOLEAN_TYPE_NAME = "boolean";
  public static final String INT_TYPE_NAME = "int";
  public static final String VOID_TYPE_NAME = "void";

  /** Primitive type descriptors and boxed type descriptors mapping. */
  private BiMap<TypeDescriptor, TypeDescriptor> boxedTypeByPrimitiveType = HashBiMap.create();

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

  public static TypeDescriptor getBoxTypeFromPrimitiveType(TypeDescriptor primitiveType) {
    return get().boxedTypeByPrimitiveType.get(primitiveType);
  }

  public static TypeDescriptor getPrimitiveTypeFromBoxType(TypeDescriptor boxType) {
    return get().boxedTypeByPrimitiveType.inverse().get(TypeDescriptors.toNullable(boxType));
  }

  public static boolean isBoxedType(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isTypeVariable()) {
      return false;
    }
    return get().boxedTypeByPrimitiveType.containsValue(TypeDescriptors.toNullable(typeDescriptor));
  }

  public static boolean isNonVoidPrimitiveType(TypeDescriptor typeDescriptor) {
    return get().boxedTypeByPrimitiveType.containsKey(typeDescriptor);
  }

  public static boolean isBoxedBooleanOrDouble(TypeDescriptor typeDescriptor) {
    return TypeDescriptors.isJavaLangBoolean(typeDescriptor)
        || TypeDescriptors.isJavaLangDouble(typeDescriptor);
  }

  public static boolean isPrimitiveBoolean(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().primitiveBoolean);
  }

  public static boolean isPrimitiveByte(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().primitiveByte);
  }

  public static boolean isPrimitiveChar(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().primitiveChar);
  }

  public static boolean isPrimitiveDouble(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().primitiveDouble);
  }

  public static boolean isPrimitiveFloat(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().primitiveFloat);
  }

  public static boolean isPrimitiveFloatOrDouble(TypeDescriptor typeDescriptor) {
    return isPrimitiveFloat(typeDescriptor) || isPrimitiveDouble(typeDescriptor);
  }

  public static boolean isPrimitiveInt(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().primitiveInt);
  }

  public static boolean isPrimitiveLong(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().primitiveLong);
  }

  public static boolean isPrimitiveShort(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().primitiveShort);
  }

  public static boolean isPrimitiveVoid(TypeDescriptor typeDescriptor) {
    return typeDescriptor.hasSameRawType(get().primitiveVoid);
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

  // Common browser native types.
  public final TypeDescriptor nativeFunction =
      createNative(
          JsUtils.JS_PACKAGE_GLOBAL,
          // Native type name
          "Function",
          Collections.emptyList());
  public final TypeDescriptor nativeObject =
      createNative(
          JsUtils.JS_PACKAGE_GLOBAL,
          // Native type name
          "Object",
          Collections.emptyList());
  public final TypeDescriptor nativeArray =
      createNative(
          JsUtils.JS_PACKAGE_GLOBAL,
          // Native type name
          "Array",
          Collections.emptyList());

  public final TypeDescriptor globalNamespace =
      // This is the global window references seen as a (phantom) type that will become the
      // enclosing class of native global methods and properties.
      createNative(
          JsUtils.JS_PACKAGE_GLOBAL,
          // Native type name
          "",
          Collections.emptyList());

  /** Returns TypeDescriptor that contains the devirtualized JsOverlay methods of a native type. */
  public static TypeDescriptor createOverlayImplementationClassTypeDescriptor(
      TypeDescriptor typeDescriptor) {
    checkArgument(typeDescriptor.isNative() || typeDescriptor.isInterface());

    TypeDescriptor superTypeDescriptor =
        typeDescriptor.getSuperTypeDescriptor() == null
                || !(typeDescriptor.getSuperTypeDescriptor().isNative()
                    || typeDescriptor.getSuperTypeDescriptor().isInterface())
            ? null
            : createOverlayImplementationClassTypeDescriptor(
                typeDescriptor.getSuperTypeDescriptor());

    List<String> classComponents =
        AstUtils.synthesizeClassComponents(
            typeDescriptor,
            simpleName -> simpleName + AstUtilConstants.OVERLAY_IMPLEMENTATION_CLASS_SUFFIX);

    return createExactly(
        superTypeDescriptor,
        typeDescriptor.getPackageName(),
        classComponents,
        Collections.emptyList(),
        typeDescriptor.getPackageName(),
        Joiner.on(".").join(classComponents),
        typeDescriptor.getKind(),
        false,
        false);
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

    private final TypeDescriptor typeDescriptor;

    BootstrapType(String packageName, String name) {
      this.typeDescriptor =
          createExactly(
              Kind.CLASS, packageName, Collections.singletonList(name), Collections.emptyList());
    }

    public TypeDescriptor getDescriptor() {
      return typeDescriptor;
    }

    public static final Set<TypeDescriptor> typeDescriptors;

    static {
      ImmutableSet.Builder<TypeDescriptor> setBuilder = new ImmutableSet.Builder<>();
      for (BootstrapType value : BootstrapType.values()) {
        setBuilder.add(value.getDescriptor());
      }
      typeDescriptors = setBuilder.build();
    }
  }

  // Not externally instantiable.
  private TypeDescriptors() {}

  public static TypeDescriptor createNative(
      String jsNamespace, String jsName, List<TypeDescriptor> typeArgumentDescriptors) {
    return createNative(
        null,
        Collections.singletonList((JsUtils.isGlobal(jsNamespace) ? "global_" : "") + jsName),
        jsNamespace,
        jsName,
        typeArgumentDescriptors);
  }

  public static TypeDescriptor createNative(
      String packageName,
      List<String> classComponents,
      String jsNamespace,
      String jsName,
      List<TypeDescriptor> typeArgumentDescriptors) {
    return createExactly(
        null,
        packageName,
        classComponents,
        typeArgumentDescriptors,
        jsNamespace,
        jsName,
        Kind.INTERFACE,
        true,
        false);
  }

  public static TypeDescriptor createUnion(
      List<TypeDescriptor> unionedTypeDescriptors, final TypeDescriptor superTypeDescriptor) {
    return TypeDescriptor.newBuilder()
        .setNullable(true)
        .setKind(Kind.UNION)
        .setRawTypeDescriptorFactory(typeDescriptor -> typeDescriptor)
        .setSuperTypeDescriptorFactory(() -> superTypeDescriptor)
        .setUnionedTypeDescriptors(unionedTypeDescriptors)
        .setClassComponents(createUniqueName(unionedTypeDescriptors, "|"))
        .build();
  }

  public static TypeDescriptor createIntersection(List<TypeDescriptor> intersectedTypeDescriptors) {
    TypeDescriptor defaultSuperType = get().javaLangObject;
    final TypeDescriptor superTypeDescriptor =
        intersectedTypeDescriptors
            .stream()
            .filter(typeDescriptor -> !typeDescriptor.isInterface())
            .findFirst()
            .orElse(defaultSuperType);
    final ImmutableList<TypeDescriptor> interfaceTypeDescriptors =
        intersectedTypeDescriptors
            .stream()
            .filter(TypeDescriptor::isInterface)
            .collect(toImmutableList());
    Set<TypeDescriptor> typeArguments = Sets.newLinkedHashSet();
    for (TypeDescriptor intersectedType : intersectedTypeDescriptors) {
      typeArguments.addAll(intersectedType.getAllTypeVariables());
    }
    return TypeDescriptor.newBuilder()
        .setKind(Kind.INTERSECTION)
        .setRawTypeDescriptorFactory(() -> superTypeDescriptor.getRawTypeDescriptor())
        .setTypeArgumentDescriptors(typeArguments)
        .setNullable(true)
        .setInterfaceTypeDescriptorsFactory(() -> interfaceTypeDescriptors)
        .setSuperTypeDescriptorFactory(() -> superTypeDescriptor)
        .setClassComponents(createUniqueName(intersectedTypeDescriptors, "&"))
        .build();
  }

  public static TypeDescriptor createExactly(
      Kind kind,
      String packageName,
      List<String> classComponents,
      List<TypeDescriptor> typeArgumentDescriptors) {
    checkArgument(!Iterables.getLast(classComponents).contains("<"));
    return createExactly(
        null,
        packageName,
        classComponents,
        typeArgumentDescriptors,
        null,
        null,
        kind,
        false,
        false);
  }

  static TypeDescriptor createExactly(
      final TypeDescriptor superTypeDescriptor,
      final String packageName,
      final List<String> classComponents,
      final List<TypeDescriptor> typeArgumentDescriptors,
      final String jsNamespace,
      final String jsName,
      final Kind kind,
      final boolean isNative,
      final boolean isJsType) {
    Supplier<TypeDescriptor> rawTypeDescriptorFactory =
        () ->
            createExactly(
                superTypeDescriptor != null ? superTypeDescriptor.getRawTypeDescriptor() : null,
                packageName,
                classComponents,
                Collections.emptyList(),
                jsNamespace,
                jsName,
                kind,
                isNative,
                isJsType);

    TypeDeclaration typeDeclaration =
        TypeDeclaration.createExactly(
            superTypeDescriptor,
            packageName,
            classComponents,
            // Type declaration for native descriptors do not introduce type variables.
            Collections.emptyList(),
            jsNamespace,
            jsName,
            kind,
            isNative,
            isJsType);

    return TypeDescriptor.newBuilder()
        .setClassComponents(classComponents)
        .setNullable(true)
        .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
        .setSuperTypeDescriptorFactory(() -> superTypeDescriptor)
        .setTypeDeclaration(typeDeclaration)
        .setTypeArgumentDescriptors(typeArgumentDescriptors)
        .setKind(kind)
        .build();
  }

  public static TypeDescriptor toGivenNullability(TypeDescriptor typeDescriptor, boolean nullable) {
    return nullable ? toNullable(typeDescriptor) : toNonNullable(typeDescriptor);
  }

  public static TypeDescriptor toNonNullable(TypeDescriptor originalTypeDescriptor) {
    if (originalTypeDescriptor.isTypeVariable()) {
      // Type variables are placeholders and do not impose nullability constraints.
      return originalTypeDescriptor;
    }
    if (!originalTypeDescriptor.isNullable()) {
      return originalTypeDescriptor;
    }

    return TypeDescriptor.Builder.from(originalTypeDescriptor).setNullable(false).build();
  }

  public static TypeDescriptor toNullable(TypeDescriptor originalTypeDescriptor) {
    if (originalTypeDescriptor.isPrimitive()) {
      // Primitive types are always non nullable.
      return originalTypeDescriptor;
    }
    if (originalTypeDescriptor.isNullable()) {
      return originalTypeDescriptor;
    }

    return TypeDescriptor.Builder.from(originalTypeDescriptor).setNullable(true).build();
  }

  public static TypeDescriptor getForArray(TypeDescriptor leafTypeDescriptor, int dimensions) {
    return getForArray(leafTypeDescriptor, dimensions, true);
  }

  public static TypeDescriptor getForArray(
      TypeDescriptor leafTypeDescriptor, int dimensions, boolean isNullable) {
    checkArgument(!leafTypeDescriptor.isArray());
    checkArgument(!leafTypeDescriptor.isUnion());

    if (dimensions == 0) {
      return leafTypeDescriptor;
    }
    DescriptorFactory<TypeDescriptor> rawTypeDescriptorFactory =
        selfTypeDescriptor ->
            getForArray(
                selfTypeDescriptor.getLeafTypeDescriptor().getRawTypeDescriptor(),
                selfTypeDescriptor.getDimensions(),
                selfTypeDescriptor.isNullable());

    // Compute everything else.
    TypeDescriptor componentTypeDescriptor =
        getForArray(leafTypeDescriptor, dimensions - 1, isNullable);

    List<String> classComponents =
        AstUtils.synthesizeClassComponents(
            componentTypeDescriptor, simpleName -> simpleName + "[]");

    String uniqueKey = leafTypeDescriptor.getUniqueId() + Strings.repeat("[]", dimensions);

    return TypeDescriptor.newBuilder()
        .setComponentTypeDescriptor(componentTypeDescriptor)
        .setDimensions(dimensions)
        .setKind(Kind.ARRAY)
        .setNullable(isNullable)
        .setLeafTypeDescriptor(leafTypeDescriptor)
        .setClassComponents(classComponents)
        .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
        .setUniqueKey(uniqueKey)
        .build();
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

  public static String createUniqueName(
      final List<TypeDescriptor> typeDescriptors, String separator) {
    return typeDescriptors
        .stream()
        .map(
            typeDescriptor -> {
              String binaryName = typeDescriptor.getQualifiedBinaryName();
              if (typeDescriptor.hasTypeArguments()) {
                binaryName += "_";
                binaryName += createUniqueName(typeDescriptor.getTypeArgumentDescriptors(), "_");
              }
              return binaryName.replace('.', '_');
            })
        .collect(joining(separator));
  }

  /** Builder for TypeDescriptors. */
  public static class SingletonInitializer {

    private final TypeDescriptors typeDescriptors = new TypeDescriptors();

    public void init() {
      set(typeDescriptors);
    }

    public SingletonInitializer addPrimitiveBoxedTypeDescriptorPair(
        TypeDescriptor primitiveType, TypeDescriptor boxedType) {
      addPrimitiveType(primitiveType);
      addReferenceType(boxedType);
      addBoxedTypeMapping(primitiveType, boxedType);
      return this;
    }

    public SingletonInitializer addPrimitiveType(TypeDescriptor primitiveType) {
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

    public SingletonInitializer addReferenceType(TypeDescriptor referenceType) {
      checkArgument(
          !referenceType.isPrimitive(),
          "%s is not a reference type",
          referenceType.getQualifiedSourceName());
      String name = referenceType.getQualifiedSourceName();
      switch (name) {
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
        default:
          throw new IllegalStateException("Unexpected reference type in well known set: " + name);
      }
      return this;
    }

    private TypeDescriptor addBoxedTypeMapping(
        TypeDescriptor primitiveType, TypeDescriptor boxedType) {
      return typeDescriptors.boxedTypeByPrimitiveType.put(primitiveType, boxedType);
    }
  }
}
