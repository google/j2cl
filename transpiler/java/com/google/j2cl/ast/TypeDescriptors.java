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
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.j2cl.ast.TypeDescriptor.DescriptorFactory;
import com.google.j2cl.ast.common.JsUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

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

  private static ThreadLocal<TypeDescriptors> typeDescriptorsStorage = new ThreadLocal<>();

  private static ThreadLocal<Boolean> isInitialized = new ThreadLocal<>();

  public static TypeDescriptors get() {
    Preconditions.checkState(
        typeDescriptorsStorage.get() != null, "TypeDescriptors must be initialized before access.");
    return typeDescriptorsStorage.get();
  }

  private static void set(TypeDescriptors typeDescriptors) {
    Preconditions.checkState(
        typeDescriptorsStorage.get() == null,
        "TypeDescriptors has already been initialized and cannot be reassigned.");
    isInitialized.set(true);
    typeDescriptorsStorage.set(typeDescriptors);
  }

  public static boolean isInitialized() {
    Boolean initialized = isInitialized.get();
    return initialized == null ? false : initialized;
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
   * <p>Even though the floating point types are only 4 and 8 bytes respectively they are considered
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
  public static final TypeDescriptor NATIVE_FUNCTION =
      createNative(
          JsUtils.JS_PACKAGE_GLOBAL,
          // Native type name
          "Function",
          Collections.emptyList());
  public static final TypeDescriptor NATIVE_OBJECT =
      createNative(
          JsUtils.JS_PACKAGE_GLOBAL,
          // Native type name
          "Object",
          Collections.emptyList());
  public static final TypeDescriptor NATIVE_ARRAY =
      createNative(
          JsUtils.JS_PACKAGE_GLOBAL,
          // Native type name
          "Array",
          Collections.emptyList());

  public static final TypeDescriptor GLOBAL_NAMESPACE =
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
    checkArgument(!typeDescriptor.isArray());
    checkArgument(!typeDescriptor.isUnion());

    TypeDescriptor superTypeDescriptor =
        typeDescriptor.getSuperTypeDescriptor() == null
                || !(typeDescriptor.getSuperTypeDescriptor().isNative()
                    || typeDescriptor.getSuperTypeDescriptor().isInterface())
            ? null
            : createOverlayImplementationClassTypeDescriptor(
                typeDescriptor.getSuperTypeDescriptor());

    List<String> classComponents =
        Stream.concat(
                typeDescriptor.getClassComponents().stream(),
                Stream.of(AstUtils.OVERLAY_IMPLEMENTATION_CLASS_SUFFIX))
            .collect(toCollection(ArrayList::new));

    return createExactly(
        superTypeDescriptor,
        typeDescriptor.getPackageComponents(),
        classComponents,
        Collections.<TypeDescriptor>emptyList(),
        Joiner.on(".").join(typeDescriptor.getPackageComponents()),
        Joiner.on(".").join(classComponents),
        false,
        false);
  }

  /** Holds the bootstrap types. */
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
    LONG_UTILS(Arrays.asList("vmbootstrap"), "LongUtils"),
    JAVA_SCRIPT_OBJECT(Arrays.asList("vmbootstrap"), "JavaScriptObject"),
    JAVA_SCRIPT_FUNCTION(Arrays.asList("vmbootstrap"), "JavaScriptFunction"),
    NATIVE_EQUALITY(Arrays.asList("nativebootstrap"), "Equality"),
    NATIVE_UTIL(Arrays.asList("nativebootstrap"), "Util"),
    NATIVE_LONG(Arrays.asList("nativebootstrap"), "Long"),
    EXCEPTIONS(Arrays.asList("vmbootstrap"), "Exceptions");

    private TypeDescriptor typeDescriptor;

    BootstrapType(List<String> pathComponents, String name) {
      this.typeDescriptor =
          createExactly(pathComponents, Arrays.asList(name), Collections.emptyList());
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
        Collections.singletonList(jsNamespace),
        Collections.singletonList((JsUtils.isGlobal(jsNamespace) ? "global_" : "") + jsName),
        jsNamespace,
        jsName,
        typeArgumentDescriptors);
  }

  public static TypeDescriptor createNative(
      List<String> packageComponents,
      List<String> classComponents,
      String jsNamespace,
      String jsName,
      List<TypeDescriptor> typeArgumentDescriptors) {
    return createExactly(
        null,
        packageComponents,
        classComponents,
        typeArgumentDescriptors,
        jsNamespace,
        jsName,
        true,
        true);
  }

  public static TypeDescriptor createUnion(
      List<TypeDescriptor> unionedTypeDescriptors, final TypeDescriptor superTypeDescriptor) {
    String joinedBinaryName = createJoinedBinaryName(unionedTypeDescriptors, " | ");
    return new TypeDescriptor.Builder()
        .setBinaryName(joinedBinaryName)
        .setIsNullable(true)
        .setIsUnion(true)
        .setRawTypeDescriptorFactory(
            new DescriptorFactory<TypeDescriptor>() {
              @Override
              public TypeDescriptor create(TypeDescriptor selfTypeDescriptor) {
                return selfTypeDescriptor;
              }
            })
        .setSuperTypeDescriptorFactory(
            new DescriptorFactory<TypeDescriptor>() {
              @Override
              public TypeDescriptor create(TypeDescriptor selfTypeDescriptor) {
                return superTypeDescriptor;
              }
            })
        .setUnionedTypeDescriptors(unionedTypeDescriptors)
        .build();
  }

  public static TypeDescriptor createIntersection(List<TypeDescriptor> intersectedTypeDescriptors) {
    TypeDescriptor defaultSuperType = get().javaLangObject;
    final TypeDescriptor superTypeDescriptor =
        Iterables.find(
            intersectedTypeDescriptors,
            typeDescriptor -> !typeDescriptor.isInterface(),
            defaultSuperType);
    final List<TypeDescriptor> interfaceTypeDescriptors =
        intersectedTypeDescriptors
            .stream()
            .filter(TypeDescriptor::isInterface)
            .collect(toImmutableList());
    String joinedBinaryName =
        TypeDescriptors.createJoinedBinaryName(intersectedTypeDescriptors, " & ");
    Set<TypeDescriptor> typeVars = Sets.newLinkedHashSet();
    for (TypeDescriptor intersectedType : intersectedTypeDescriptors) {
      typeVars.addAll(intersectedType.getAllTypeVariables());
    }
    return new TypeDescriptor.Builder()
        .setIsIntersection(true)
        .setSimpleName(joinedBinaryName)
        .setTypeArgumentDescriptors(typeVars)
        .setBinaryName(joinedBinaryName)
        .setVisibility(Visibility.PUBLIC)
        .setIsNullable(true)
        .setInterfacesTypeDescriptorsFactory(
            new DescriptorFactory<List<TypeDescriptor>>() {
              @Override
              public List<TypeDescriptor> create(TypeDescriptor selfTypeDescriptor) {
                return interfaceTypeDescriptors;
              }
            })
        .setSuperTypeDescriptorFactory(
            new DescriptorFactory<TypeDescriptor>() {
              @Override
              public TypeDescriptor create(TypeDescriptor selfTypeDescriptor) {
                return superTypeDescriptor;
              }
            })
        .build();
  }

  public static TypeDescriptor createExactly(
      List<String> packageComponents,
      List<String> classComponents,
      List<TypeDescriptor> typeArgumentDescriptors) {
    checkArgument(!Iterables.getLast(classComponents).contains("<"));
    return createExactly(
        null,
        packageComponents,
        classComponents,
        typeArgumentDescriptors,
        null,
        null,
        false,
        false);
  }

  private static TypeDescriptor createExactly(
      final TypeDescriptor superTypeDescriptor,
      final List<String> packageComponents,
      final List<String> classComponents,
      final List<TypeDescriptor> typeArgumentDescriptors,
      final String jsNamespace,
      final String jsName,
      final boolean isNative,
      final boolean isJsType) {
    DescriptorFactory<TypeDescriptor> rawTypeDescriptorFactory =
        new DescriptorFactory<TypeDescriptor>() {
          @Override
          public TypeDescriptor create(TypeDescriptor selfTypeDescriptor) {
            TypeDescriptor rawSuperTypeDescriptor =
                superTypeDescriptor != null ? superTypeDescriptor.getRawTypeDescriptor() : null;
            List<TypeDescriptor> emptyTypeArgumentDescriptors = Collections.emptyList();
            return createExactly(
                rawSuperTypeDescriptor,
                packageComponents,
                classComponents,
                emptyTypeArgumentDescriptors,
                jsNamespace,
                jsName,
                isNative,
                isJsType);
          }
        };
    DescriptorFactory<TypeDescriptor> superTypeDescriptorFactory =
        new DescriptorFactory<TypeDescriptor>() {
          @Override
          protected TypeDescriptor create(TypeDescriptor selfTypeDescriptor) {
            return superTypeDescriptor;
          }
        };

    String simpleName = Iterables.getLast(classComponents);
    String binaryName =
        Stream.concat(
                packageComponents.stream(),
                Collections.singleton(Joiner.on("$").join(classComponents)).stream())
            .collect(joining("."));
    String packageName = Joiner.on(".").join(packageComponents);
    return new TypeDescriptor.Builder()
        .setBinaryName(binaryName)
        .setClassComponents(classComponents)
        .setIsJsType(isJsType)
        .setIsNative(isNative)
        .setIsNullable(true)
        .setJsName(jsName)
        .setJsNamespace(jsNamespace)
        .setPackageComponents(packageComponents)
        .setPackageName(packageName)
        .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
        .setSimpleName(simpleName)
        .setSourceName(
            Stream.concat(packageComponents.stream(), classComponents.stream())
                .collect(joining(".")))
        .setSuperTypeDescriptorFactory(superTypeDescriptorFactory)
        .setTypeArgumentDescriptors(typeArgumentDescriptors)
        .setVisibility(Visibility.PUBLIC)
        .build();
  }

  public static TypeDescriptor replaceTypeArgumentDescriptors(
      TypeDescriptor originalTypeDescriptor, Iterable<TypeDescriptor> typeArgumentTypeDescriptors) {
    checkArgument(!originalTypeDescriptor.isArray());
    checkArgument(!originalTypeDescriptor.isTypeVariable());
    checkArgument(!originalTypeDescriptor.isUnion());

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
    if (originalTypeDescriptor.isTypeVariable()) {
      // Type variables are placeholders and do not impose nullability constraints.
      return originalTypeDescriptor;
    }
    if (!originalTypeDescriptor.isNullable()) {
      return originalTypeDescriptor;
    }

    return TypeDescriptor.Builder.from(originalTypeDescriptor).setIsNullable(false).build();
  }

  public static TypeDescriptor toNullable(TypeDescriptor originalTypeDescriptor) {
    if (originalTypeDescriptor.isPrimitive()) {
      // Primitive types are always non nullable.
      return originalTypeDescriptor;
    }
    if (originalTypeDescriptor.isNullable()) {
      return originalTypeDescriptor;
    }

    return TypeDescriptor.Builder.from(originalTypeDescriptor).setIsNullable(true).build();
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
        new DescriptorFactory<TypeDescriptor>() {
          @Override
          public TypeDescriptor create(TypeDescriptor selfTypeDescriptor) {
            return getForArray(
                selfTypeDescriptor.getLeafTypeDescriptor().getRawTypeDescriptor(),
                selfTypeDescriptor.getDimensions(),
                selfTypeDescriptor.isNullable());
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

    return new TypeDescriptor.Builder()
        .setBinaryName(binaryName)
        .setComponentTypeDescriptor(componentTypeDescriptor)
        .setDimensions(dimensions)
        .setIsArray(true)
        .setIsNullable(isNullable)
        .setLeafTypeDescriptor(leafTypeDescriptor)
        .setPackageName(packageName)
        .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
        .setSimpleName(simpleName)
        .setSourceName(sourceName)
        .setTypeArgumentDescriptors(typeArgumentDescriptors)
        .build();
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

    return NullLiteral.NULL;
  }

  public static String createJoinedBinaryName(
      final List<TypeDescriptor> typeDescriptors, String separator) {
    return typeDescriptors
        .stream()
        .map(
            typeDescriptor -> {
              String binaryName = typeDescriptor.getBinaryName();
              if (typeDescriptor.isParameterizedType()) {
                binaryName += "_";
                binaryName +=
                    createJoinedBinaryName(typeDescriptor.getTypeArgumentDescriptors(), "_");
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
          primitiveType.isPrimitive(), "%s is not a primitive type", primitiveType.getSourceName());
      String name = primitiveType.getSourceName();
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
          referenceType.getSourceName());
      String name = referenceType.getSourceName();
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
