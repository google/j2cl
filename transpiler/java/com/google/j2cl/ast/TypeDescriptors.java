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
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.TypeDescriptor.MethodDescriptorFactory;
import com.google.j2cl.ast.TypeDescriptor.TypeDescriptorFactory;
import com.google.j2cl.ast.TypeDescriptor.TypeDescriptorsFactory;
import com.google.j2cl.common.JsInteropAnnotationUtils;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

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

  public static Interner<TypeDescriptor> interner;

  /**
   * Primitive type descriptors and boxed type descriptors mapping.
   */
  private BiMap<TypeDescriptor, TypeDescriptor> boxedTypeByPrimitiveType = HashBiMap.create();

  private static ThreadLocal<TypeDescriptors> typeDescriptorsStorage = new ThreadLocal<>();

  public static void init(AST ast) {
    if (typeDescriptorsStorage.get() != null) {
      // Already initialized.
      return;
    }
    TypeDescriptors typeDescriptors = new TypeDescriptors();

    // initialize primitive types.
    typeDescriptors.primitiveBoolean =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType(BOOLEAN_TYPE_NAME));
    typeDescriptors.primitiveByte =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType(BYTE_TYPE_NAME));
    typeDescriptors.primitiveChar =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType(CHAR_TYPE_NAME));
    typeDescriptors.primitiveDouble =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType(DOUBLE_TYPE_NAME));
    typeDescriptors.primitiveFloat =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType(FLOAT_TYPE_NAME));
    typeDescriptors.primitiveInt =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType(INT_TYPE_NAME));
    typeDescriptors.primitiveLong =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType(LONG_TYPE_NAME));
    typeDescriptors.primitiveShort =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType(SHORT_TYPE_NAME));
    typeDescriptors.primitiveVoid =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType(VOID_TYPE_NAME));

    // initialize boxed types.
    typeDescriptors.javaLangBoolean =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Boolean"));
    typeDescriptors.javaLangByte =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Byte"));
    typeDescriptors.javaLangCharacter =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Character"));
    typeDescriptors.javaLangDouble =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Double"));
    typeDescriptors.javaLangFloat =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Float"));
    typeDescriptors.javaLangInteger =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Integer"));
    typeDescriptors.javaLangLong =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Long"));
    typeDescriptors.javaLangShort =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Short"));
    typeDescriptors.javaLangString =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.String"));

    typeDescriptors.javaLangClass =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Class"));
    typeDescriptors.javaLangObject =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Object"));
    typeDescriptors.javaLangThrowable =
        TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Throwable"));

    typeDescriptors.javaLangNumber = createJavaLangNumber(ast);
    typeDescriptors.javaLangComparable = createJavaLangComparable(ast);
    typeDescriptors.javaLangCharSequence = createJavaLangCharSequence(ast);

    typeDescriptors.unknownType = TypeDescriptors.createExactly(
        Collections.emptyList(), Lists.newArrayList("$$unknown$$"), false,
        Collections.emptyList());

    initBoxedPrimitiveTypeMapping(typeDescriptors);

    typeDescriptorsStorage.set(typeDescriptors);
  }

  private static void initBoxedPrimitiveTypeMapping(TypeDescriptors typeDescriptors) {
    BiMap<TypeDescriptor, TypeDescriptor> boxedTypeByPrimitiveType = HashBiMap.create();
    boxedTypeByPrimitiveType.put(typeDescriptors.primitiveBoolean, typeDescriptors.javaLangBoolean);
    boxedTypeByPrimitiveType.put(typeDescriptors.primitiveByte, typeDescriptors.javaLangByte);
    boxedTypeByPrimitiveType.put(typeDescriptors.primitiveChar, typeDescriptors.javaLangCharacter);
    boxedTypeByPrimitiveType.put(typeDescriptors.primitiveDouble, typeDescriptors.javaLangDouble);
    boxedTypeByPrimitiveType.put(typeDescriptors.primitiveFloat, typeDescriptors.javaLangFloat);
    boxedTypeByPrimitiveType.put(typeDescriptors.primitiveInt, typeDescriptors.javaLangInteger);
    boxedTypeByPrimitiveType.put(typeDescriptors.primitiveLong, typeDescriptors.javaLangLong);
    boxedTypeByPrimitiveType.put(typeDescriptors.primitiveShort, typeDescriptors.javaLangShort);
    typeDescriptors.boxedTypeByPrimitiveType = ImmutableBiMap.copyOf(boxedTypeByPrimitiveType);
  }

  public static TypeDescriptors get() {
    return typeDescriptorsStorage.get();
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

  /**
   * Create TypeDescriptor for java.lang.Number, which is not a well known type by JDT.
   */
  private static TypeDescriptor createJavaLangNumber(AST ast) {
    ITypeBinding javaLangInteger = ast.resolveWellKnownType("java.lang.Integer");
    checkNotNull(javaLangInteger);
    return TypeProxyUtils.createTypeDescriptor(javaLangInteger.getSuperclass());
  }

  /**
   * Create TypeDescriptor for java.lang.Comparable, which is not a well known type by JDT.
   */
  private static TypeDescriptor createJavaLangComparable(AST ast) {
    ITypeBinding javaLangInteger = ast.resolveWellKnownType("java.lang.Integer");
    checkNotNull(javaLangInteger);
    ITypeBinding[] interfaces = javaLangInteger.getInterfaces();
    checkArgument(interfaces.length == 1);
    return TypeProxyUtils.createTypeDescriptor(interfaces[0].getErasure());
  }

  /**
   * Create TypeDescriptor for java.lang.CharSequence, which is not a well known type by JDT.
   */
  private static TypeDescriptor createJavaLangCharSequence(AST ast) {
    ITypeBinding javaLangString = ast.resolveWellKnownType("java.lang.String");
    checkNotNull(javaLangString);
    ITypeBinding[] interfaces = javaLangString.getInterfaces();
    checkArgument(interfaces.length == 3);
    for (ITypeBinding i : interfaces) {
      if (i.getBinaryName().equals("java.lang.CharSequence")) {
        return TypeProxyUtils.createTypeDescriptor(i);
      }
    }
    return null;
  }

  // Common browser native types.
  public static final TypeDescriptor NATIVE_STRING =
      createNative(
          new ArrayList<String>(),
          // Import alias.
          Arrays.asList("NativeString"),
          Collections.emptyList(),
          // Browser global
          JsInteropUtils.JS_GLOBAL,
          // Native type name
          "String");
  public static final TypeDescriptor NATIVE_FUNCTION =
      createNative(
          Collections.emptyList(),
          // Import alias.
          Arrays.asList("NativeFunction"),
          Collections.emptyList(),
          // Browser global
          JsInteropUtils.JS_GLOBAL,
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
            .setUniqueId(
                createUniqueId(
                    true, false, null, 0, true, unionedTypeDescriptors, null, null, false, null))
            .build();
    self[0] = newTypeDescriptor;

    return intern(newTypeDescriptor);
  }

  private static TypeDescriptor createExactly(
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
    String binaryClassName = createBinaryClassName(classComponents, simpleName, false, false);
    String packageName = Joiner.on(".").join(packageComponents);

    return intern(
        new TypeDescriptor.Builder()
            .setBinaryName(binaryName)
            .setClassComponents(classComponents)
            .setBinaryClassName(binaryClassName)
            .setIsExtern(isNative && JsInteropUtils.isGlobal(jsNamespace))
            .setIsGlobal(
                JsInteropUtils.JS_GLOBAL.equals(jsNamespace) && Strings.isNullOrEmpty(jsName))
            .setIsJsType(isJsType)
            .setIsNative(isNative)
            .setIsNullable(true)
            .setIsRaw(isRaw)
            .setJsName(jsName)
            .setJsNamespace(jsNamespace)
            .setPackageComponents(packageComponents)
            .setPackageName(packageName)
            .setQualifiedName(
                createQualifiedName(packageName, binaryClassName, simpleName, jsNamespace, jsName))
            .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
            .setSimpleName(simpleName)
            .setSourceName(
                Joiner.on(".").join(Iterables.concat(packageComponents, classComponents)))
            .setTypeArgumentDescriptors(typeArgumentDescriptors)
            .setUniqueId(
                createUniqueId(
                    true,
                    false,
                    null,
                    0,
                    false,
                    null,
                    binaryName,
                    typeArgumentDescriptors,
                    false,
                    null))
            .setVisibility(Visibility.PUBLIC)
            .build());
  }

  public static TypeDescriptor replaceTypeArgumentDescriptors(
      TypeDescriptor originalTypeDescriptor, Iterable<TypeDescriptor> typeArgumentTypeDescriptors) {
    Preconditions.checkArgument(!originalTypeDescriptor.isArray());
    checkArgument(!originalTypeDescriptor.isTypeVariable());

    List<TypeDescriptor> typeArgumentDescriptors = Lists.newArrayList(typeArgumentTypeDescriptors);

    TypeDescriptor newTypeDescriptor =
        TypeDescriptor.Builder.from(originalTypeDescriptor)
            .setTypeArgumentDescriptors(typeArgumentDescriptors)
            .setUniqueId(
                createUniqueId(
                    originalTypeDescriptor.isNullable(),
                    originalTypeDescriptor.isArray(),
                    originalTypeDescriptor.getLeafTypeDescriptor(),
                    originalTypeDescriptor.getDimensions(),
                    originalTypeDescriptor.isUnion(),
                    originalTypeDescriptor.getUnionedTypeDescriptors(),
                    originalTypeDescriptor.getBinaryName(),
                    typeArgumentDescriptors,
                    false,
                    null))
            .build();

    return intern(newTypeDescriptor);
  }

  /**
   * Returns the type in the hierarchy of {@code type} that matches (excluding nullability and
   * generics) with {@code typeToMatch}.
   * If there is no match, returns null.
   */
  public static TypeDescriptor getMatchingTypeInHierarchy(
      TypeDescriptor subjectTypeDescriptor, TypeDescriptor toMatchTypeDescriptor) {
    if (subjectTypeDescriptor.getRawTypeDescriptor().equalsIgnoreNullability(
        toMatchTypeDescriptor.getRawTypeDescriptor())) {
      return subjectTypeDescriptor;
    }

    // Check superclasses.
    if (subjectTypeDescriptor.getSuperTypeDescriptor() != null) {
      TypeDescriptor match =
          getMatchingTypeInHierarchy(
              subjectTypeDescriptor.getSuperTypeDescriptor(),
              toMatchTypeDescriptor);
      if (match != null) {
        return match;
      }
    }

    // Check implemented interfaces.
    for (TypeDescriptor interfaceDescriptor : subjectTypeDescriptor.getInterfacesTypeDescriptors()) {
      TypeDescriptor match =
          getMatchingTypeInHierarchy(interfaceDescriptor, toMatchTypeDescriptor);
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
    Preconditions.checkArgument(!originalTypeDescriptor.isTypeVariable());

    if (!originalTypeDescriptor.isNullable()) {
      return originalTypeDescriptor;
    }

    TypeDescriptor newTypeDescriptor =
        TypeDescriptor.Builder.from(originalTypeDescriptor)
            .setIsNullable(false)
            .setUniqueId(
                createUniqueId(
                    false,
                    originalTypeDescriptor.isArray(),
                    originalTypeDescriptor.getLeafTypeDescriptor(),
                    originalTypeDescriptor.getDimensions(),
                    originalTypeDescriptor.isUnion(),
                    originalTypeDescriptor.getUnionedTypeDescriptors(),
                    originalTypeDescriptor.getBinaryName(),
                    originalTypeDescriptor.getTypeArgumentDescriptors(),
                    false,
                    null))
            .build();

    return intern(newTypeDescriptor);
  }

  public static TypeDescriptor toNullable(TypeDescriptor originalTypeDescriptor) {
    Preconditions.checkArgument(!originalTypeDescriptor.isTypeVariable());

    if (originalTypeDescriptor.isNullable()) {
      return originalTypeDescriptor;
    }

    TypeDescriptor newTypeDescriptor =
        TypeDescriptor.Builder.from(originalTypeDescriptor)
            .setIsNullable(true)
            .setUniqueId(
                createUniqueId(
                    true,
                    originalTypeDescriptor.isArray(),
                    originalTypeDescriptor.getLeafTypeDescriptor(),
                    originalTypeDescriptor.getDimensions(),
                    originalTypeDescriptor.isUnion(),
                    originalTypeDescriptor.getUnionedTypeDescriptors(),
                    originalTypeDescriptor.getBinaryName(),
                    originalTypeDescriptor.getTypeArgumentDescriptors(),
                    false,
                    null))
            .build();

    return intern(newTypeDescriptor);
  }

  public static TypeDescriptor createLambda(
      TypeDescriptor enclosingClassTypeDescriptor,
      String lambdaBinaryName,
      final ITypeBinding lambdaInterfaceBinding) {
    final TypeDescriptor[] self = new TypeDescriptor[1];

    MethodDescriptorFactory concreteJsFunctionMethodDescriptorFactory =
        new MethodDescriptorFactory() {
          @Override
          public MethodDescriptor create() {
            return TypeProxyUtils.getConcreteJsFunctionMethodDescriptor(lambdaInterfaceBinding);
          }
        };
    MethodDescriptorFactory jsFunctionMethodDescriptorFactory =
        new MethodDescriptorFactory() {
          @Override
          public MethodDescriptor create() {
            return TypeProxyUtils.getJsFunctionMethodDescriptor(lambdaInterfaceBinding);
          }
        };
    TypeDescriptorFactory rawTypeDescriptorFactory =
        new TypeDescriptorFactory() {
          @Override
          public TypeDescriptor create() {
            return self[0];
          }
        };
    TypeDescriptorFactory superTypeDescriptorFactory =
        new TypeDescriptorFactory() {
          @Override
          public TypeDescriptor create() {
            return TypeDescriptors.get().javaLangObject;
          }
        };

    // Compute these first since they're reused in other calculations.
    List<String> classComponents =
        ImmutableList.copyOf(
            Iterables.concat(
                enclosingClassTypeDescriptor.getClassComponents(),
                Arrays.asList(lambdaBinaryName)));
    List<String> packageComponents = enclosingClassTypeDescriptor.getPackageComponents();
    String simpleName = Iterables.getLast(classComponents);

    // Compute everything else.
    String binaryName =
        Joiner.on(".")
            .join(
                Iterables.concat(
                    packageComponents,
                    Collections.singleton(Joiner.on("$").join(classComponents))));
    String binaryClassName = createBinaryClassName(classComponents, simpleName, false, false);
    String packageName = Joiner.on(".").join(packageComponents);
    String qualifiedName =
        createQualifiedName(packageName, binaryClassName, simpleName, null, null);
    String sourceName = Joiner.on(".").join(Iterables.concat(packageComponents, classComponents));

    TypeDescriptor typeDescriptor =
        new TypeDescriptor.Builder()
            .setBinaryName(binaryName)
            .setClassComponents(classComponents)
            .setBinaryClassName(binaryClassName)
            .setConcreteJsFunctionMethodDescriptorFactory(concreteJsFunctionMethodDescriptorFactory)
            .setIsInstanceNestedClass(true)
            .setIsJsFunctionImplementation(JsInteropUtils.isJsFunction(lambdaInterfaceBinding))
            .setIsLocal(true)
            .setIsNullable(true)
            .setJsFunctionMethodDescriptorFactory(jsFunctionMethodDescriptorFactory)
            .setPackageComponents(packageComponents)
            .setPackageName(packageName)
            .setQualifiedName(qualifiedName)
            .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
            .setSimpleName(simpleName)
            .setSourceName(sourceName)
            .setSuperTypeDescriptorFactory(superTypeDescriptorFactory)
            .setUniqueId(
                createUniqueId(true, false, null, 0, false, null, binaryName, null, false, null))
            .setVisibility(Visibility.PRIVATE)
            .build();
    self[0] = typeDescriptor;

    return intern(typeDescriptor);
  }

  // This is only used by TypeProxyUtils, and cannot be used elsewhere. Because to create a
  // TypeDescriptor from a TypeBinding, it should go through the path to check array type.
  static TypeDescriptor createForType(
      final ITypeBinding typeBinding, List<TypeDescriptor> overrideTypeArgumentDescriptors) {
    checkArgument(!typeBinding.isArray());

    MethodDescriptorFactory concreteJsFunctionMethodDescriptorFactory =
        new MethodDescriptorFactory() {
          @Override
          public MethodDescriptor create() {
            return TypeProxyUtils.getConcreteJsFunctionMethodDescriptor(typeBinding);
          }
        };
    TypeDescriptorFactory enclosingTypeDescriptorFactory =
        new TypeDescriptorFactory() {
          @Override
          public TypeDescriptor create() {
            return TypeProxyUtils.createTypeDescriptor(typeBinding.getDeclaringClass());
          }
        };
    MethodDescriptorFactory jsFunctionMethodDescriptorFactory =
        new MethodDescriptorFactory() {
          @Override
          public MethodDescriptor create() {
            return TypeProxyUtils.getJsFunctionMethodDescriptor(typeBinding);
          }
        };
    TypeDescriptorFactory rawTypeDescriptorFactory =
        new TypeDescriptorFactory() {
          @Override
          public TypeDescriptor create() {
            TypeDescriptor rawTypeDescriptor =
                TypeProxyUtils.createTypeDescriptor(typeBinding.getErasure());
            if (rawTypeDescriptor.isParameterizedType()) {
              return TypeDescriptors.replaceTypeArgumentDescriptors(
                  rawTypeDescriptor, ImmutableList.<TypeDescriptor>of());
            }
            return rawTypeDescriptor;
          }
        };
    TypeDescriptorFactory superTypeDescriptorFactory =
        new TypeDescriptorFactory() {
          @Override
          public TypeDescriptor create() {
            return TypeProxyUtils.createTypeDescriptor(typeBinding.getSuperclass());
          }
        };
    TypeDescriptorsFactory interfacesDescriptorsFactory =
        new TypeDescriptorsFactory() {
          @Override
          public ImmutableList<TypeDescriptor> create() {
            ImmutableList.Builder<TypeDescriptor> typeDescriptors = ImmutableList.builder();
            for (ITypeBinding binding : typeBinding.getInterfaces()) {
              typeDescriptors.add(TypeProxyUtils.createTypeDescriptor(binding));
            }
            return typeDescriptors.build();
          }
        };

    // Compute these first since they're reused in other calculations.
    List<String> classComponents = TypeProxyUtils.getClassComponents(typeBinding);
    List<String> packageComponents = TypeProxyUtils.getPackageComponents(typeBinding);
    boolean isPrimitive = typeBinding.isPrimitive();
    boolean isTypeVariable = typeBinding.isTypeVariable();
    IAnnotationBinding jsTypeAnnotation = JsInteropAnnotationUtils.getJsTypeAnnotation(typeBinding);
    String simpleName = Iterables.getLast(classComponents);

    // Compute everything else.
    String binaryName =
        Joiner.on(".")
            .join(
                Iterables.concat(
                    packageComponents,
                    Collections.singleton(Joiner.on("$").join(classComponents))));
    String binaryClassName =
        createBinaryClassName(classComponents, simpleName, isPrimitive, isTypeVariable);
    boolean isNative = JsInteropAnnotationUtils.isNative(jsTypeAnnotation);
    boolean isNullable = !typeBinding.isPrimitive();
    String jsName = JsInteropAnnotationUtils.getJsName(jsTypeAnnotation);
    String jsNamespace = JsInteropAnnotationUtils.getJsNamespace(jsTypeAnnotation);
    String packageName = Joiner.on(".").join(packageComponents);
    String sourceName = Joiner.on(".").join(Iterables.concat(packageComponents, classComponents));
    List<TypeDescriptor> typeArgumentDescriptors =
        overrideTypeArgumentDescriptors != null
            ? overrideTypeArgumentDescriptors
            : TypeProxyUtils.getTypeArgumentDescriptors(typeBinding);

    // Compute these even later
    boolean isExtern = isNative && JsInteropUtils.isGlobal(jsNamespace);

    return intern(
        new TypeDescriptor.Builder()
            .setBinaryName(binaryName)
            .setClassComponents(classComponents)
            .setBinaryClassName(binaryClassName)
            .setConcreteJsFunctionMethodDescriptorFactory(concreteJsFunctionMethodDescriptorFactory)
            .setEnclosingTypeDescriptorFactory(enclosingTypeDescriptorFactory)
            .setInterfacesTypeDescriptorsFactory(interfacesDescriptorsFactory)
            .setIsEnumOrSubclass(TypeProxyUtils.isEnumOrSubclass(typeBinding))
            .setIsExtern(isExtern)
            .setIsInstanceMemberClass(TypeProxyUtils.isInstanceMemberClass(typeBinding))
            .setIsInstanceNestedClass(TypeProxyUtils.isInstanceNestedClass(typeBinding))
            .setIsInterface(typeBinding.isInterface())
            .setIsJsFunction(JsInteropUtils.isJsFunction(typeBinding))
            .setIsJsFunctionImplementation(TypeProxyUtils.isJsFunctionImplementation(typeBinding))
            .setIsJsType(jsTypeAnnotation != null)
            .setIsLocal(TypeProxyUtils.isLocal(typeBinding))
            .setIsNative(isNative)
            .setIsNullable(isNullable)
            .setIsPrimitive(isPrimitive)
            .setIsRawType(typeBinding.isRawType())
            .setIsTypeVariable(isTypeVariable)
            .setIsWildCard(typeBinding.isWildcardType() || typeBinding.isCapture())
            .setJsFunctionMethodDescriptorFactory(jsFunctionMethodDescriptorFactory)
            .setJsName(jsName)
            .setJsNamespace(jsNamespace)
            .setPackageComponents(packageComponents)
            .setPackageName(packageName)
            .setQualifiedName(
                createQualifiedName(packageName, binaryClassName, simpleName, jsNamespace, jsName))
            .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
            .setSimpleName(simpleName)
            .setSourceName(sourceName)
            .setSubclassesJsConstructorClass(
                TypeProxyUtils.subclassesJsConstructorClass(typeBinding))
            .setSuperTypeDescriptorFactory(superTypeDescriptorFactory)
            .setTypeArgumentDescriptors(typeArgumentDescriptors)
            .setUniqueId(
                createUniqueId(
                    isNullable,
                    false,
                    null,
                    0,
                    false,
                    null,
                    binaryName,
                    typeArgumentDescriptors,
                    isTypeVariable,
                    typeBinding.getErasure().getBinaryName()))
            .setVisibility(TypeProxyUtils.getVisibility(typeBinding.getModifiers()))
            .build());
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
    String binaryClassName = leafTypeDescriptor.getBinaryClassName() + arraySuffix;
    TypeDescriptor componentTypeDescriptor =
        getForArray(leafTypeDescriptor, dimensions - 1, isNullable);
    String packageName = leafTypeDescriptor.getPackageName();
    String qualifiedName =
        createQualifiedName(packageName, binaryClassName, simpleName, null, null);
    String sourceName = leafTypeDescriptor.getSourceName() + arraySuffix;
    List<TypeDescriptor> typeArgumentDescriptors = Collections.emptyList();

    TypeDescriptor typeDescriptor =
        intern(
            new TypeDescriptor.Builder()
                .setBinaryName(binaryName)
                .setBinaryClassName(binaryClassName)
                .setComponentTypeDescriptor(componentTypeDescriptor)
                .setDimensions(dimensions)
                .setIsArray(true)
                .setIsNullable(isNullable)
                .setIsRaw(leafTypeDescriptor.isRaw())
                .setLeafTypeDescriptor(leafTypeDescriptor)
                .setPackageName(packageName)
                .setQualifiedName(qualifiedName)
                .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
                .setSimpleName(simpleName)
                .setSourceName(sourceName)
                .setTypeArgumentDescriptors(typeArgumentDescriptors)
                .setUniqueId(
                    createUniqueId(
                        isNullable,
                        true,
                        leafTypeDescriptor,
                        dimensions,
                        false,
                        null,
                        binaryName,
                        typeArgumentDescriptors,
                        false,
                        null))
                .build());

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

  private static String createUniqueId(
      boolean isNullable,
      boolean isArray,
      TypeDescriptor leafTypeDescriptor,
      int dimensions,
      boolean isUnion,
      List<TypeDescriptor> unionedTypeDescriptors,
      String binaryName,
      List<TypeDescriptor> typeArgumentDescriptors,
      boolean isTypeVariable,
      String erasureBinaryName) {
    String uniqueId;

    if (isArray) {
      uniqueId = "(" + leafTypeDescriptor.getUniqueId() + ")" + Strings.repeat("[]", dimensions);
    } else if (isUnion) {
      uniqueId = createUnionBinaryName(unionedTypeDescriptors);
    } else if (isTypeVariable) {
      uniqueId = binaryName + ":" + erasureBinaryName;
    } else {
      uniqueId = binaryName + TypeDescriptors.createTypeArgumentsUniqueId(typeArgumentDescriptors);
    }

    return (isNullable ? "?" : "!") + uniqueId;
  }

  private static String createTypeArgumentsUniqueId(List<TypeDescriptor> typeArgumentDescriptors) {
    if (typeArgumentDescriptors == null || typeArgumentDescriptors.isEmpty()) {
      return "";
    }
    return String.format(
        "<%s>",
        Joiner.on(", ")
            .join(
                Lists.transform(
                    typeArgumentDescriptors,
                    new Function<TypeDescriptor, String>() {
                      @Override
                      public String apply(TypeDescriptor typeDescriptor) {
                        return typeDescriptor.getUniqueId();
                      }
                    })));
  }

  private static String createQualifiedName(
      String packageName,
      String binaryClassName,
      String simpleName,
      String jsNamespace,
      String jsName) {
    String qualifiedName;
    {
      String localNamespace = packageName;
      String localBinaryClassName = binaryClassName;

      // If a custom js namespace was specified.
      if (jsNamespace != null) {
        // The effect is to replace both the package and the class's enclosing class prefixes.
        localNamespace = jsNamespace;
        localBinaryClassName = simpleName;
      }

      // If the JS namespace the user specified was JsPackage.GLOBAL then consider that to be top
      // level.
      if (JsInteropUtils.isGlobal(localNamespace)) {
        localNamespace = "";
      }

      // If a custom JS name was specified.
      if (jsName != null) {
        // Then use it instead of the (potentially enclosing class qualified) class name.
        localBinaryClassName = jsName;
      }

      qualifiedName =
          Joiner.on(".")
              .skipNulls()
              .join(Strings.emptyToNull(localNamespace), Strings.emptyToNull(localBinaryClassName));
    }
    return qualifiedName;
  }

  private static String createBinaryClassName(
      List<String> classComponents,
      String simpleName,
      boolean isPrimitive,
      boolean isTypeVariable) {
    String binaryClassName;
    {
      if (isPrimitive) {
        binaryClassName = "$" + simpleName;
      } else if (simpleName.equals("?")) {
        binaryClassName = "?";
      } else if (isTypeVariable) {
        // skip the top level class component for better output readability.
        List<String> nameComponents =
            new ArrayList<>(classComponents.subList(1, classComponents.size()));

        // move the prefix in the simple name to the class name to avoid collisions between method-
        // level and class-level type variable and avoid variable name starts with a number.
        // concat class components to avoid collisions between type variables in inner/outer class.
        // use '_' instead of '$' because '$' is not allowed in template variable name in closure.
        nameComponents.set(
            nameComponents.size() - 1, simpleName.substring(simpleName.indexOf('_') + 1));
        String prefix = simpleName.substring(0, simpleName.indexOf('_') + 1);

        binaryClassName = prefix + Joiner.on('_').join(nameComponents);
      } else {
        binaryClassName = Joiner.on('$').join(classComponents);
      }
    }
    return binaryClassName;
  }

  private static String createUnionBinaryName(final List<TypeDescriptor> unionedTypeDescriptors) {
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

  private static Interner<TypeDescriptor> getInterner() {
    if (interner == null) {
      interner = Interners.newWeakInterner();
    }
    return interner;
  }

  private static TypeDescriptor intern(TypeDescriptor typeDescriptor) {
    // Run interning through a central function so that debugging has an opportunity to inspect
    // all of them.
    TypeDescriptor internedTypeDescriptor = getInterner().intern(typeDescriptor);
    return internedTypeDescriptor;
  }
}
