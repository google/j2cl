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
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.TypeDescriptor.MethodDescriptorFactory;
import com.google.j2cl.ast.TypeDescriptor.TypeDescriptorFactory;
import com.google.j2cl.ast.TypeDescriptor.TypeDescriptorsFactory;
import com.google.j2cl.common.JsInteropAnnotationUtils;
import com.google.j2cl.common.PackageInfoCache;

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
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType(BOOLEAN_TYPE_NAME));
    typeDescriptors.primitiveByte =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType(BYTE_TYPE_NAME));
    typeDescriptors.primitiveChar =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType(CHAR_TYPE_NAME));
    typeDescriptors.primitiveDouble =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType(DOUBLE_TYPE_NAME));
    typeDescriptors.primitiveFloat =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType(FLOAT_TYPE_NAME));
    typeDescriptors.primitiveInt =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType(INT_TYPE_NAME));
    typeDescriptors.primitiveLong =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType(LONG_TYPE_NAME));
    typeDescriptors.primitiveShort =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType(SHORT_TYPE_NAME));
    typeDescriptors.primitiveVoid =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType(VOID_TYPE_NAME));

    // initialize boxed types.
    typeDescriptors.javaLangBoolean =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Boolean"));
    typeDescriptors.javaLangByte =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Byte"));
    typeDescriptors.javaLangCharacter =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Character"));
    typeDescriptors.javaLangDouble =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Double"));
    typeDescriptors.javaLangFloat =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Float"));
    typeDescriptors.javaLangInteger =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Integer"));
    typeDescriptors.javaLangLong =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Long"));
    typeDescriptors.javaLangShort =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Short"));
    typeDescriptors.javaLangString =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.String"));

    typeDescriptors.javaLangClass =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Class"));
    typeDescriptors.javaLangObject =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Object"));
    typeDescriptors.javaLangThrowable =
        JdtBindingUtils.createTypeDescriptor(ast.resolveWellKnownType("java.lang.Throwable"));

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
    return JdtBindingUtils.createTypeDescriptor(javaLangInteger.getSuperclass());
  }

  /**
   * Create TypeDescriptor for java.lang.Comparable, which is not a well known type by JDT.
   */
  private static TypeDescriptor createJavaLangComparable(AST ast) {
    ITypeBinding javaLangInteger = ast.resolveWellKnownType("java.lang.Integer");
    checkNotNull(javaLangInteger);
    ITypeBinding[] interfaces = javaLangInteger.getInterfaces();
    checkArgument(interfaces.length == 1);
    return JdtBindingUtils.createTypeDescriptor(interfaces[0].getErasure());
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
        return JdtBindingUtils.createTypeDescriptor(i);
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
        .setIsExtern(isNative && JsInteropUtils.isGlobal(jsNamespace))
        .setIsGlobal(JsInteropUtils.JS_GLOBAL.equals(jsNamespace) && Strings.isNullOrEmpty(jsName))
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

  public static TypeDescriptor createLambda(
      final TypeDescriptor enclosingClassTypeDescriptor,
      String lambdaBinaryName,
      final ITypeBinding lambdaInterfaceBinding) {
    final TypeDescriptor[] self = new TypeDescriptor[1];

    MethodDescriptorFactory concreteJsFunctionMethodDescriptorFactory =
        new MethodDescriptorFactory() {
          @Override
          public MethodDescriptor create() {
            return JdtBindingUtils.getConcreteJsFunctionMethodDescriptor(lambdaInterfaceBinding);
          }
        };
    MethodDescriptorFactory jsFunctionMethodDescriptorFactory =
        new MethodDescriptorFactory() {
          @Override
          public MethodDescriptor create() {
            return JdtBindingUtils.getJsFunctionMethodDescriptor(lambdaInterfaceBinding);
          }
        };
    TypeDescriptorFactory rawTypeDescriptorFactory =
        new TypeDescriptorFactory() {
          @Override
          public TypeDescriptor create() {
            return TypeDescriptors.replaceTypeArgumentDescriptors(
                self[0], Collections.<TypeDescriptor>emptyList());
          }
        };
    TypeDescriptorFactory superTypeDescriptorFactory =
        new TypeDescriptorFactory() {
          @Override
          public TypeDescriptor create() {
            return TypeDescriptors.get().javaLangObject;
          }
        };
    TypeDescriptorFactory enclosingTypeDescriptorFactory =
        new TypeDescriptorFactory() {
          @Override
          public TypeDescriptor create() {
            return enclosingClassTypeDescriptor;
          }
        };
    TypeDescriptorsFactory interfacesDescriptorsFactory =
        new TypeDescriptorsFactory() {
          @Override
          public ImmutableList<TypeDescriptor> create() {
            return ImmutableList.of(JdtBindingUtils.createTypeDescriptor(lambdaInterfaceBinding));
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
    String packageName = Joiner.on(".").join(packageComponents);
    String sourceName = Joiner.on(".").join(Iterables.concat(packageComponents, classComponents));

    List<TypeDescriptor> typeArgumentDescriptors =
        new ArrayList<>(
            JdtBindingUtils.createTypeDescriptor(lambdaInterfaceBinding).getAllTypeVariables());
    TypeDescriptor typeDescriptor =
        new TypeDescriptor.Builder()
            .setBinaryName(binaryName)
            .setClassComponents(classComponents)
            .setConcreteJsFunctionMethodDescriptorFactory(concreteJsFunctionMethodDescriptorFactory)
            .setEnclosingTypeDescriptorFactory(enclosingTypeDescriptorFactory)
            .setIsInstanceNestedClass(true)
            .setIsJsFunctionImplementation(JsInteropUtils.isJsFunction(lambdaInterfaceBinding))
            .setIsLocal(true)
            .setIsNullable(true)
            .setJsFunctionMethodDescriptorFactory(jsFunctionMethodDescriptorFactory)
            .setPackageComponents(packageComponents)
            .setPackageName(packageName)
            .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
            .setSimpleName(simpleName)
            .setSourceName(sourceName)
            .setInterfacesTypeDescriptorsFactory(interfacesDescriptorsFactory)
            .setSuperTypeDescriptorFactory(superTypeDescriptorFactory)
            .setTypeArgumentDescriptors(typeArgumentDescriptors)
            .setVisibility(Visibility.PRIVATE)
            .build();
    self[0] = typeDescriptor;

    return typeDescriptor;
  }

  // This is only used by TypeProxyUtils, and cannot be used elsewhere. Because to create a
  // TypeDescriptor from a TypeBinding, it should go through the path to check array type.
  static TypeDescriptor createForType(
      final ITypeBinding typeBinding, List<TypeDescriptor> overrideTypeArgumentDescriptors) {
    checkArgument(!typeBinding.isArray());

    PackageInfoCache packageInfoCache = PackageInfoCache.get();

    ITypeBinding topLevelTypeBinding = JdtBindingUtils.toTopLevelTypeBinding(typeBinding);
    if (topLevelTypeBinding.isFromSource()) {
      // Let the PackageInfoCache know that this class is Source, otherwise it would have to rummage
      // around in the class path to figure it out and it might even come up with the wrong answer
      // for example if this class has also been globbed into some other library that is a
      // dependency of this one.
      PackageInfoCache.get().markAsSource(topLevelTypeBinding.getBinaryName());
    }

    MethodDescriptorFactory concreteJsFunctionMethodDescriptorFactory =
        new MethodDescriptorFactory() {
          @Override
          public MethodDescriptor create() {
            return JdtBindingUtils.getConcreteJsFunctionMethodDescriptor(typeBinding);
          }
        };
    TypeDescriptorFactory enclosingTypeDescriptorFactory =
        new TypeDescriptorFactory() {
          @Override
          public TypeDescriptor create() {
            return JdtBindingUtils.createTypeDescriptor(typeBinding.getDeclaringClass());
          }
        };
    MethodDescriptorFactory jsFunctionMethodDescriptorFactory =
        new MethodDescriptorFactory() {
          @Override
          public MethodDescriptor create() {
            return JdtBindingUtils.getJsFunctionMethodDescriptor(typeBinding);
          }
        };
    TypeDescriptorFactory rawTypeDescriptorFactory =
        new TypeDescriptorFactory() {
          @Override
          public TypeDescriptor create() {
            TypeDescriptor rawTypeDescriptor =
                JdtBindingUtils.createTypeDescriptor(typeBinding.getErasure());
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
            return JdtBindingUtils.createTypeDescriptorWithNullability(
                typeBinding.getSuperclass(),
                new IAnnotationBinding[0],
                JdtBindingUtils.getTypeDefaultNullability(typeBinding));
          }
        };
    TypeDescriptorsFactory interfacesDescriptorsFactory =
        new TypeDescriptorsFactory() {
          @Override
          public ImmutableList<TypeDescriptor> create() {
            ImmutableList.Builder<TypeDescriptor> typeDescriptors = ImmutableList.builder();
            for (ITypeBinding interfaceBinding : typeBinding.getInterfaces()) {
              TypeDescriptor interfaceType = JdtBindingUtils.createTypeDescriptorWithNullability(
                  interfaceBinding,
                  new IAnnotationBinding[0],
                  JdtBindingUtils.getTypeDefaultNullability(typeBinding));
              typeDescriptors.add(interfaceType);
            }
            return typeDescriptors.build();
          }
        };

    // Compute these first since they're reused in other calculations.
    List<String> classComponents = JdtBindingUtils.getClassComponents(typeBinding);
    List<String> packageComponents = JdtBindingUtils.getPackageComponents(typeBinding);
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
    
    if (isTypeVariable) {
      binaryName = binaryName + ":" + typeBinding.getErasure().getBinaryName();
    }

    boolean isNative = JsInteropAnnotationUtils.isNative(jsTypeAnnotation);
    boolean isNullable = !typeBinding.isPrimitive() && !typeBinding.isTypeVariable();
    String jsName = JsInteropAnnotationUtils.getJsName(jsTypeAnnotation);
    String jsNamespace = null;

    // If a package-info file has specified a JsPackage namespace then it is sugar for setting the
    // jsNamespace of all top level types in that package.
    boolean isTopLevelType = typeBinding.getDeclaringClass() == null;
    if (isTopLevelType) {
      String jsPackageNamespace =
          packageInfoCache.getJsNamespace(
              JdtBindingUtils.toTopLevelTypeBinding(typeBinding).getBinaryName());
      if (jsPackageNamespace != null) {
        jsNamespace = jsPackageNamespace;
      }
    }

    String jsTypeNamespace = JsInteropAnnotationUtils.getJsNamespace(jsTypeAnnotation);
    if (jsTypeNamespace != null) {
      jsNamespace = jsTypeNamespace;
    }

    String packageName = Joiner.on(".").join(packageComponents);
    String sourceName = Joiner.on(".").join(Iterables.concat(packageComponents, classComponents));
    List<TypeDescriptor> typeArgumentDescriptors =
        overrideTypeArgumentDescriptors != null
            ? overrideTypeArgumentDescriptors
            : JdtBindingUtils.getTypeArgumentTypeDescriptors(typeBinding);

    // Compute these even later
    boolean isExtern = isNative && JsInteropUtils.isGlobal(jsNamespace);
    return new TypeDescriptor.Builder()
        .setBinaryName(binaryName)
        .setClassComponents(classComponents)
        .setConcreteJsFunctionMethodDescriptorFactory(concreteJsFunctionMethodDescriptorFactory)
        .setEnclosingTypeDescriptorFactory(enclosingTypeDescriptorFactory)
        .setInterfacesTypeDescriptorsFactory(interfacesDescriptorsFactory)
        .setIsEnumOrSubclass(JdtBindingUtils.isEnumOrSubclass(typeBinding))
        .setIsExtern(isExtern)
        .setIsInstanceMemberClass(JdtBindingUtils.isInstanceMemberClass(typeBinding))
        .setIsInstanceNestedClass(JdtBindingUtils.isInstanceNestedClass(typeBinding))
        .setIsInterface(typeBinding.isInterface())
        .setIsJsFunction(JsInteropUtils.isJsFunction(typeBinding))
        .setIsJsFunctionImplementation(JdtBindingUtils.isJsFunctionImplementation(typeBinding))
        .setIsJsType(jsTypeAnnotation != null)
        .setIsLocal(JdtBindingUtils.isLocal(typeBinding))
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
        .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
        .setSimpleName(simpleName)
        .setSourceName(sourceName)
        .setIsOrSubclassesJsConstructorClass(
            JdtBindingUtils.isOrSubclassesJsConstructorClass(typeBinding))
        .setSuperTypeDescriptorFactory(superTypeDescriptorFactory)
        .setTypeArgumentDescriptors(typeArgumentDescriptors)
        .setVisibility(JdtBindingUtils.getVisibility(typeBinding))
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
