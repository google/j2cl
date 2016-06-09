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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.TypeDescriptor.TypeDescriptorFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Utility class holding type descriptors that need to be referenced directly.
 */
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

  public TypeDescriptor javaLangClass;
  public TypeDescriptor javaLangObject;
  public TypeDescriptor javaLangThrowable;

  public TypeDescriptor javaLangNumber;
  public TypeDescriptor javaLangComparable;
  public TypeDescriptor javaLangCharSequence;

  public TypeDescriptor unknownType;

  public static final String SHORT_TYPE_NAME = "short";
  public static final String LONG_TYPE_NAME = "long";
  public static final String FLOAT_TYPE_NAME = "float";
  public static final String DOUBLE_TYPE_NAME = "double";
  public static final String CHAR_TYPE_NAME = "char";
  public static final String BYTE_TYPE_NAME = "byte";
  public static final String BOOLEAN_TYPE_NAME = "boolean";
  public static final String INT_TYPE_NAME = "int";
  public static final String VOID_TYPE_NAME = "void";

  /**
   * Primitive type descriptors and boxed type descriptors mapping.
   */
  private BiMap<TypeDescriptor, TypeDescriptor> boxedTypeByPrimitiveType = HashBiMap.create();

  private static ThreadLocal<TypeDescriptors> typeDescriptorsStorage = new ThreadLocal<>();

  public static TypeDescriptors get() {
    if (TypeDescriptors.typeDescriptorsStorage.get() == null) {
      // Type descriptor storage is not already initialized, initialize it.
      TypeDescriptors typeDescriptors = new TypeDescriptors();
      TypeDescriptors.typeDescriptorsStorage.set(typeDescriptors);
    }
    return typeDescriptorsStorage.get();
  }

  public static TypeDescriptor addBoxedTypeMapping(
      TypeDescriptor primitiveType, TypeDescriptor boxedType) {
    return get().boxedTypeByPrimitiveType.put(primitiveType, boxedType);
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
    return typeDescriptor.equalsIgnoreNullability(get().javaLangBoolean)
        || typeDescriptor.equalsIgnoreNullability(get().javaLangDouble);
  }

  public static boolean isPrimitiveBooleanOrDouble(TypeDescriptor typeDescriptor) {
    return typeDescriptor.equalsIgnoreNullability(get().primitiveBoolean)
        || typeDescriptor.equalsIgnoreNullability(get().primitiveDouble);
  }

  public static boolean isNumericPrimitive(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isPrimitive()
        && !typeDescriptor.equalsIgnoreNullability(get().primitiveBoolean)
        && !typeDescriptor.equalsIgnoreNullability(get().primitiveVoid);
  }

  public static boolean isBoxedOrPrimitiveType(TypeDescriptor typeDescriptor) {
    return isBoxedType(typeDescriptor) || isNonVoidPrimitiveType(typeDescriptor);
  }

  public static boolean isBoxedTypeAsJsPrimitives(TypeDescriptor typeDescriptor) {
    return isBoxedBooleanOrDouble(typeDescriptor)
        || typeDescriptor.equalsIgnoreNullability(get().javaLangString);
  }

  public static boolean isNonBoxedReferenceType(TypeDescriptor typeDescriptor) {
    return !typeDescriptor.isPrimitive() && !isBoxedType(typeDescriptor);
  }

  /**
   * Returns an idea of the "width" of a numeric primitive type to help with deciding when a
   * conversion would be a narrowing and when it would be a widening.
   *
   * <p>
   * Even though the floating point types are only 4 and 8 bytes respectively they are considered
   * very wide because of the magnitude of the maximum values they can encode.
   */
  public static int getWidth(TypeDescriptor typeDescriptor) {
    checkArgument(isNumericPrimitive(typeDescriptor));

    TypeDescriptors typeDescriptors = get();
    if (typeDescriptor.equalsIgnoreNullability(typeDescriptors.primitiveByte)) {
      return 1;
    } else if (typeDescriptor.equalsIgnoreNullability(typeDescriptors.primitiveShort)) {
      return 2;
    } else if (typeDescriptor.equalsIgnoreNullability(typeDescriptors.primitiveChar)) {
      return 2;
    } else if (typeDescriptor.equalsIgnoreNullability(typeDescriptors.primitiveInt)) {
      return 4;
    } else if (typeDescriptor.equalsIgnoreNullability(typeDescriptors.primitiveLong)) {
      return 8;
    } else if (typeDescriptor.equalsIgnoreNullability(typeDescriptors.primitiveFloat)) {
      return 4 + 100;
    } else {
      checkArgument(typeDescriptor.equalsIgnoreNullability(typeDescriptors.primitiveDouble));
      return 8 + 100;
    }
  }

  // Common browser native types.
  public static final TypeDescriptor NATIVE_STRING =
      createNative(
          new ArrayList<String>(),
          // Import alias.
          Arrays.asList("NativeString"),
          Collections.emptyList(),
          // Browser global
          JsUtils.JS_GLOBAL,
          // Native type name
          "String");
  public static final TypeDescriptor NATIVE_FUNCTION =
      createNative(
          Collections.emptyList(),
          // Import alias.
          Arrays.asList("NativeFunction"),
          Collections.emptyList(),
          // Browser global
          JsUtils.JS_GLOBAL,
          // Native type name
          "Function");

  /**
   * Holds the bootstrap types.
   */
  public enum BootstrapType {
    OBJECTS(Arrays.asList("vmbootstrap"), "Objects"),
    COMPARABLES(Arrays.asList("vmbootstrap"), "Comparables"),
    CHAR_SEQUENCES(Arrays.asList("vmbootstrap"), "CharSequences"),
    NUMBERS(Arrays.asList("vmbootstrap"), "Numbers"),
    ASSERTS(Arrays.asList("vmbootstrap"), "Asserts"),
    ARRAYS(Arrays.asList("vmbootstrap"), "Arrays"),
    CASTS(Arrays.asList("vmbootstrap"), "Casts"),
    PRIMITIVES(Arrays.asList("vmbootstrap", "primitives"), "Primitives"),
    ENUMS(Arrays.asList("vmbootstrap"), "Enums"),
    LONGS(Arrays.asList("vmbootstrap"), "LongUtils"),
    JAVA_SCRIPT_OBJECT(Arrays.asList("vmbootstrap"), "JavaScriptObject"),
    JAVA_SCRIPT_FUNCTION(Arrays.asList("vmbootstrap"), "JavaScriptFunction"),
    NATIVE_EQUALITY(Arrays.asList("nativebootstrap"), "Equality"),
    NATIVE_UTIL(Arrays.asList("nativebootstrap"), "Util"),
    NATIVE_LONG(Arrays.asList("nativebootstrap"), "Long"),
    EXCEPTIONS(Arrays.asList("vmbootstrap"), "Exceptions");

    private TypeDescriptor typeDescriptor;

    private BootstrapType(List<String> pathComponents, String name) {
      this.typeDescriptor =
          createExactly(pathComponents, Arrays.asList(name), true, Collections.emptyList());
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
      List<String> packageComponents,
      List<String> classComponents,
      List<TypeDescriptor> typeArgumentDescriptors,
      String jsNamespace,
      String jsName) {

    return createExactly(
        packageComponents,
        classComponents,
        typeArgumentDescriptors,
        jsNamespace,
        jsName,
        false,
        true,
        true);
  }

  public static TypeDescriptor createUnion(final List<TypeDescriptor> unionedTypeDescriptors) {
    final TypeDescriptor[] self = new TypeDescriptor[1];

    TypeDescriptor newTypeDescriptor =
        new TypeDescriptor.Builder()
            .setBinaryName(createUnionBinaryName(unionedTypeDescriptors))
            .setIsNullable(true)
            .setIsUnion(true)
            .setRawTypeDescriptorFactory(
                new TypeDescriptorFactory() {
                  @Override
                  public TypeDescriptor create() {
                    return self[0];
                  }
                })
            .setUnionedTypeDescriptors(unionedTypeDescriptors)
            .build();
    self[0] = newTypeDescriptor;

    return newTypeDescriptor;
  }

  public static TypeDescriptor createExactly(
      List<String> packageComponents,
      List<String> classComponents,
      boolean isRaw,
      List<TypeDescriptor> typeArgumentDescriptors) {
    checkArgument(!Iterables.getLast(classComponents).contains("<"));
    return createExactly(
        packageComponents,
        classComponents,
        typeArgumentDescriptors,
        null,
        null,
        isRaw,
        false,
        false);
  }

  public static TypeDescriptor createExactly(
      final List<String> packageComponents,
      final List<String> classComponents,
      final List<TypeDescriptor> typeArgumentDescriptors,
      final String jsNamespace,
      final String jsName,
      final boolean isRaw,
      final boolean isNative,
      final boolean isJsType) {
    TypeDescriptorFactory rawTypeDescriptorFactory =
        new TypeDescriptorFactory() {
          @Override
          public TypeDescriptor create() {
            List<TypeDescriptor> emptyTypeArgumentDescriptors = Collections.emptyList();

            return createExactly(
                packageComponents,
                classComponents,
                emptyTypeArgumentDescriptors,
                jsNamespace,
                jsName,
                isRaw,
                isNative,
                isJsType);
          }
        };

    String simpleName = Iterables.getLast(classComponents);
    String binaryName =
        Joiner.on(".")
            .join(
                Iterables.concat(
                    packageComponents,
                    Collections.singleton(Joiner.on("$").join(classComponents))));
    String packageName = Joiner.on(".").join(packageComponents);

    return new TypeDescriptor.Builder()
        .setBinaryName(binaryName)
        .setClassComponents(classComponents)
        .setIsExtern(isNative && JsUtils.isGlobal(jsNamespace))
        .setIsGlobal(JsUtils.JS_GLOBAL.equals(jsNamespace) && Strings.isNullOrEmpty(jsName))
        .setIsJsType(isJsType)
        .setIsNative(isNative)
        .setIsNullable(true)
        .setIsRaw(isRaw)
        .setJsName(jsName)
        .setJsNamespace(jsNamespace)
        .setPackageComponents(packageComponents)
        .setPackageName(packageName)
        .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
        .setSimpleName(simpleName)
        .setSourceName(Joiner.on(".").join(Iterables.concat(packageComponents, classComponents)))
        .setTypeArgumentDescriptors(typeArgumentDescriptors)
        .setVisibility(Visibility.PUBLIC)
        .build();
  }

  public static TypeDescriptor replaceTypeArgumentDescriptors(
      TypeDescriptor originalTypeDescriptor, Iterable<TypeDescriptor> typeArgumentTypeDescriptors) {
    checkArgument(!originalTypeDescriptor.isArray());
    checkArgument(!originalTypeDescriptor.isTypeVariable());

    List<TypeDescriptor> typeArgumentDescriptors = Lists.newArrayList(typeArgumentTypeDescriptors);

    return TypeDescriptor.Builder.from(originalTypeDescriptor)
        .setTypeArgumentDescriptors(typeArgumentDescriptors)
        .build();
  }

  /**
   * Returns the type in the hierarchy of {@code type} that matches (excluding nullability and
   * generics) with {@code typeToMatch}. If there is no match, returns null.
   */
  public static TypeDescriptor getMatchingTypeInHierarchy(
      TypeDescriptor subjectTypeDescriptor, TypeDescriptor toMatchTypeDescriptor) {
    if (subjectTypeDescriptor
        .getRawTypeDescriptor()
        .equalsIgnoreNullability(toMatchTypeDescriptor.getRawTypeDescriptor())) {
      return subjectTypeDescriptor;
    }

    // Check superclasses.
    if (subjectTypeDescriptor.getSuperTypeDescriptor() != null) {
      TypeDescriptor match =
          getMatchingTypeInHierarchy(
              subjectTypeDescriptor.getSuperTypeDescriptor(), toMatchTypeDescriptor);
      if (match != null) {
        return match;
      }
    }

    // Check implemented interfaces.
    for (TypeDescriptor interfaceDescriptor :
        subjectTypeDescriptor.getInterfacesTypeDescriptors()) {
      TypeDescriptor match = getMatchingTypeInHierarchy(interfaceDescriptor, toMatchTypeDescriptor);
      if (match != null) {
        return match;
      }
    }

    return null;
  }

  public static TypeDescriptor toGivenNullability(
      TypeDescriptor originalTypeDescriptor, boolean nullable) {
    if (nullable) {
      return toNullable(originalTypeDescriptor);
    }
    return toNonNullable(originalTypeDescriptor);
  }

  public static TypeDescriptor toNonNullable(TypeDescriptor originalTypeDescriptor) {
    if (!originalTypeDescriptor.isNullable()) {
      return originalTypeDescriptor;
    }

    return TypeDescriptor.Builder.from(originalTypeDescriptor)
        .setIsNullable(false)
        .build();
  }

  public static TypeDescriptor toNullable(TypeDescriptor originalTypeDescriptor) {
    if (originalTypeDescriptor.isNullable()) {
      return originalTypeDescriptor;
    }

    return TypeDescriptor.Builder.from(originalTypeDescriptor)
        .setIsNullable(true)
        .build();
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
    final TypeDescriptor[] self = new TypeDescriptor[1];

    TypeDescriptorFactory rawTypeDescriptorFactory =
        new TypeDescriptorFactory() {
          @Override
          public TypeDescriptor create() {
            return self[0];
          }
        };

    // Compute these first since they're reused in other calculations.
    String arrayPrefix = Strings.repeat("[", dimensions);
    String arraySuffix = Strings.repeat("[]", dimensions);
    String simpleName = leafTypeDescriptor.getSimpleName() + arraySuffix;

    // Compute everything else.
    String binaryName = arrayPrefix + leafTypeDescriptor.getBinaryName();
    TypeDescriptor componentTypeDescriptor =
        getForArray(leafTypeDescriptor, dimensions - 1, isNullable);
    String packageName = leafTypeDescriptor.getPackageName();
    String sourceName = leafTypeDescriptor.getSourceName() + arraySuffix;
    List<TypeDescriptor> typeArgumentDescriptors = Collections.emptyList();

    TypeDescriptor typeDescriptor =
        new TypeDescriptor.Builder()
            .setBinaryName(binaryName)
            .setComponentTypeDescriptor(componentTypeDescriptor)
            .setDimensions(dimensions)
            .setIsArray(true)
            .setIsNullable(isNullable)
            .setIsRaw(leafTypeDescriptor.isRaw())
            .setLeafTypeDescriptor(leafTypeDescriptor)
            .setPackageName(packageName)
            .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
            .setSimpleName(simpleName)
            .setSourceName(sourceName)
            .setTypeArgumentDescriptors(typeArgumentDescriptors)
            .build();

    self[0] = typeDescriptor;

    return typeDescriptor;
  }

  public static Expression getDefaultValue(TypeDescriptor typeDescriptor) {
    checkArgument(!typeDescriptor.isUnion());

    // Primitives.
    switch (typeDescriptor.getSourceName()) {
      case BOOLEAN_TYPE_NAME:
        return BooleanLiteral.FALSE;
      case BYTE_TYPE_NAME:
      case SHORT_TYPE_NAME:
      case INT_TYPE_NAME:
      case FLOAT_TYPE_NAME:
      case DOUBLE_TYPE_NAME:
      case CHAR_TYPE_NAME:
        return new NumberLiteral(typeDescriptor, 0);
      case LONG_TYPE_NAME:
        return new NumberLiteral(typeDescriptor, 0L);
    }

    // Objects.
    if (typeDescriptor.isNullable()) {
      return NullLiteral.NULL;
    }
    // If the type is not nullable, we can't assign null to it, so we cast to the unknown type to
    // avoid JSCompiler errors. It's assumed that the Java code has already been checked and this
    // assignment is only temporary and it will be overwritten before the end of the constructor.
    return JsTypeAnnotation.createTypeAnnotation(
        NullLiteral.NULL,
        TypeDescriptors.get().unknownType);
  }

  static String createUnionBinaryName(final List<TypeDescriptor> unionedTypeDescriptors) {
    return Joiner.on(" | ")
        .join(
            Lists.transform(
                unionedTypeDescriptors,
                new Function<TypeDescriptor, String>() {
                  @Override
                  public String apply(TypeDescriptor typeDescriptor) {
                    return typeDescriptor.getBinaryName();
                  }
                }));
  }
}
