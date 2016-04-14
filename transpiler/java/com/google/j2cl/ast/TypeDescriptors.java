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
import com.google.common.base.Splitter;
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
    typeDescriptors.primitiveBoolean = create(ast, BOOLEAN_TYPE_NAME);
    typeDescriptors.primitiveByte = create(ast, BYTE_TYPE_NAME);
    typeDescriptors.primitiveChar = create(ast, CHAR_TYPE_NAME);
    typeDescriptors.primitiveDouble = create(ast, DOUBLE_TYPE_NAME);
    typeDescriptors.primitiveFloat = create(ast, FLOAT_TYPE_NAME);
    typeDescriptors.primitiveInt = create(ast, INT_TYPE_NAME);
    typeDescriptors.primitiveLong = create(ast, LONG_TYPE_NAME);
    typeDescriptors.primitiveShort = create(ast, SHORT_TYPE_NAME);
    typeDescriptors.primitiveVoid = create(ast, VOID_TYPE_NAME);

    // initialize boxed types.
    typeDescriptors.javaLangBoolean = create(ast, "java.lang.Boolean");
    typeDescriptors.javaLangByte = create(ast, "java.lang.Byte");
    typeDescriptors.javaLangCharacter = create(ast, "java.lang.Character");
    typeDescriptors.javaLangDouble = create(ast, "java.lang.Double");
    typeDescriptors.javaLangFloat = create(ast, "java.lang.Float");
    typeDescriptors.javaLangInteger = create(ast, "java.lang.Integer");
    typeDescriptors.javaLangLong = create(ast, "java.lang.Long");
    typeDescriptors.javaLangShort = create(ast, "java.lang.Short");
    typeDescriptors.javaLangString = create(ast, "java.lang.String");

    typeDescriptors.javaLangClass = create(ast, "java.lang.Class");
    typeDescriptors.javaLangObject = create(ast, "java.lang.Object");
    typeDescriptors.javaLangThrowable = create(ast, "java.lang.Throwable");

    typeDescriptors.javaLangNumber = createJavaLangNumber(ast);
    typeDescriptors.javaLangComparable = createJavaLangComparable(ast);
    typeDescriptors.javaLangCharSequence = createJavaLangCharSequence(ast);

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
    return get().boxedTypeByPrimitiveType.inverse().get(boxType);
  }

  public static boolean isBoxedType(TypeDescriptor typeDescriptor) {
    return get().boxedTypeByPrimitiveType.containsValue(typeDescriptor);
  }

  public static boolean isNonVoidPrimitiveType(TypeDescriptor typeDescriptor) {
    return get().boxedTypeByPrimitiveType.containsKey(typeDescriptor);
  }

  public static boolean isBoxedBooleanOrDouble(TypeDescriptor typeDescriptor) {
    return typeDescriptor == get().javaLangBoolean || typeDescriptor == get().javaLangDouble;
  }

  public static boolean isPrimitiveBooleanOrDouble(TypeDescriptor typeDescriptor) {
    return typeDescriptor == get().primitiveBoolean || typeDescriptor == get().primitiveDouble;
  }

  public static boolean isNumericPrimitive(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isPrimitive()
        && typeDescriptor != get().primitiveBoolean
        && typeDescriptor != get().primitiveVoid;
  }

  public static boolean isBoxedOrPrimitiveType(TypeDescriptor typeDescriptor) {
    return isBoxedType(typeDescriptor) || isNonVoidPrimitiveType(typeDescriptor);
  }

  public static boolean isBoxedTypeAsJsPrimitives(TypeDescriptor typeDescriptor) {
    return isBoxedBooleanOrDouble(typeDescriptor) || typeDescriptor == get().javaLangString;
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
    if (typeDescriptor == typeDescriptors.primitiveByte) {
      return 1;
    } else if (typeDescriptor == typeDescriptors.primitiveShort) {
      return 2;
    } else if (typeDescriptor == typeDescriptors.primitiveChar) {
      return 2;
    } else if (typeDescriptor == typeDescriptors.primitiveInt) {
      return 4;
    } else if (typeDescriptor == typeDescriptors.primitiveLong) {
      return 8;
    } else if (typeDescriptor == typeDescriptors.primitiveFloat) {
      return 4 + 100;
    } else {
      checkArgument(typeDescriptor == typeDescriptors.primitiveDouble);
      return 8 + 100;
    }
  }

  private static TypeDescriptor create(AST ast, String typeName) {
    return TypeProxyUtils.createTypeDescriptor(ast.resolveWellKnownType(typeName));
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
      createSyntheticNativeTypeDescriptor(
          new ArrayList<String>(),
          // Import alias.
          Arrays.asList("NativeString"),
          Collections.emptyList(),
          // Browser global
          JsInteropUtils.JS_GLOBAL,
          // Native type name
          "String");
  public static final TypeDescriptor NATIVE_FUNCTION =
      createSyntheticNativeTypeDescriptor(
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
      this.typeDescriptor = createRaw(pathComponents, name);
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

  public static TypeDescriptor createSyntheticRegularTypeDescriptor(
      List<String> packageComponents,
      List<String> classComponents,
      boolean isRaw,
      List<TypeDescriptor> typeArgumentDescriptors) {
    checkArgument(!Iterables.getLast(classComponents).contains("<"));
    return createTypeDescriptor(
        packageComponents,
        classComponents,
        typeArgumentDescriptors,
        null,
        null,
        isRaw,
        false,
        false);
  }

  public static TypeDescriptor createSyntheticNativeTypeDescriptor(
      List<String> packageComponents,
      List<String> classComponents,
      List<TypeDescriptor> typeArgumentDescriptors,
      String jsTypeNamespace,
      String jsTypeName) {
    return createTypeDescriptor(
        packageComponents,
        classComponents,
        typeArgumentDescriptors,
        jsTypeNamespace,
        jsTypeName,
        false,
        true,
        true);
  }

  private static TypeDescriptor createTypeDescriptor(
      final List<String> packageComponents,
      final List<String> classComponents,
      final List<TypeDescriptor> typeArgumentDescriptors,
      final String jsTypeNamespace,
      final String jsTypeName,
      final boolean isRaw,
      final boolean isNative,
      final boolean isJsType) {
    // These must be lazily calculated or else there are guarranteed infinite loops of
    // TypeDescriptor creation.
    TypeDescriptorFactory rawTypeDescriptorFactory =
        new TypeDescriptorFactory() {
          @Override
          public TypeDescriptor create() {
            List<TypeDescriptor> emptyTypeArgumentDescriptors = Collections.emptyList();
            return createTypeDescriptor(
                packageComponents,
                classComponents,
                emptyTypeArgumentDescriptors,
                jsTypeNamespace,
                jsTypeName,
                isRaw,
                isNative,
                isJsType);
          }
        };

    // Compute these first since they're reused in other calculations.
    String simpleName = Iterables.getLast(classComponents);

    // Compute everything else.
    String binaryName =
        Joiner.on(".")
            .join(
                Iterables.concat(
                    packageComponents,
                    Collections.singleton(Joiner.on("$").join(classComponents))));
    String className = createClassName(classComponents, simpleName, false, false);
    boolean isGlobal =
        JsInteropUtils.JS_GLOBAL.equals(jsTypeNamespace) && Strings.isNullOrEmpty(jsTypeName);
    String packageName = Joiner.on(".").join(packageComponents);
    String qualifiedName =
        createQualifiedName(packageName, className, simpleName, jsTypeNamespace, jsTypeName);
    String sourceName = Joiner.on(".").join(Iterables.concat(packageComponents, classComponents));
    String toString = sourceName;
    String uniqueId =
        "?" + binaryName + TypeDescriptors.getTypeArgumentsUniqueId(typeArgumentDescriptors);
    Visibility visibility = Visibility.PUBLIC;

    // Compute these even later
    boolean isExtern = isNative && JsInteropUtils.isGlobal(jsTypeNamespace);

    return intern(
        new RegularTypeDescriptor.Builder()
            .setBinaryName(binaryName)
            .setClassComponents(classComponents)
            .setClassName(className)
            .setIsExtern(isExtern)
            .setIsGlobal(isGlobal)
            .setIsJsType(isJsType)
            .setIsNative(isNative)
            .setIsNullable(true)
            .setIsRaw(isRaw)
            .setJsTypeName(jsTypeName)
            .setJsTypeNamespace(jsTypeNamespace)
            .setPackageComponents(packageComponents)
            .setPackageName(packageName)
            .setQualifiedName(qualifiedName)
            .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
            .setSimpleName(simpleName)
            .setSourceName(sourceName)
            .setToString(toString)
            .setTypeArgumentDescriptors(typeArgumentDescriptors)
            .setUniqueId(uniqueId)
            .setVisibility(visibility)
            .build());
  }

  private static String createQualifiedName(
      String packageName,
      String className,
      String simpleName,
      String jsTypeNamespace,
      String jsTypeName) {
    String qualifiedName;
    {
      String localNamespace = packageName;
      String localClassName = className;

      // If a custom js namespace was specified.
      if (jsTypeNamespace != null) {
        // The effect is to replace both the package and the class's enclosing class prefixes.
        localNamespace = jsTypeNamespace;
        localClassName = simpleName;
      }

      // If the JS namespace the user specified was JsPackage.GLOBAL then consider that to be top
      // level.
      if (JsInteropUtils.isGlobal(localNamespace)) {
        localNamespace = "";
      }

      // If a custom JS name was specified.
      if (jsTypeName != null) {
        // Then use it instead of the (potentially enclosing class qualified) class name.
        localClassName = jsTypeName;
      }

      qualifiedName =
          Joiner.on(".")
              .skipNulls()
              .join(Strings.emptyToNull(localNamespace), Strings.emptyToNull(localClassName));
    }
    return qualifiedName;
  }

  private static String createClassName(
      List<String> classComponents,
      String simpleName,
      boolean isPrimitive,
      boolean isTypeVariable) {
    String className;
    {
      if (isPrimitive) {
        className = "$" + simpleName;
      } else if (simpleName.equals("?")) {
        className = "?";
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

        className = prefix + Joiner.on('_').join(nameComponents);
      } else {
        className = Joiner.on('$').join(classComponents);
      }
    }
    return className;
  }

  public static TypeDescriptor replaceTypeArgumentDescriptors(
      TypeDescriptor originalTypeDescriptor, Iterable<TypeDescriptor> typeArgumentTypeDescriptors) {
    Preconditions.checkArgument(!originalTypeDescriptor.isArray());
    checkArgument(!originalTypeDescriptor.isTypeVariable());

    String nullabilityPrefix = originalTypeDescriptor.isNullable() ? "?" : "!";
    List<TypeDescriptor> typeArgumentDescriptors = Lists.newArrayList(typeArgumentTypeDescriptors);
    String uniqueId =
        nullabilityPrefix
            + originalTypeDescriptor.getBinaryName()
            + TypeDescriptors.getTypeArgumentsUniqueId(typeArgumentDescriptors);

    RegularTypeDescriptor newTypeDescriptor =
        RegularTypeDescriptor.Builder.from(originalTypeDescriptor)
            .setTypeArgumentDescriptors(typeArgumentDescriptors)
            .setUniqueId(uniqueId)
            .build();

    return intern(newTypeDescriptor);
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

    String nullabilityPrefix = "!";
    String uniqueId;
    if (originalTypeDescriptor.isArray()) {
      uniqueId =
          "("
              + originalTypeDescriptor.getLeafTypeDescriptor().getUniqueId()
              + ")"
              + Strings.repeat("[]", originalTypeDescriptor.getDimensions());
    } else {
      uniqueId =
          originalTypeDescriptor.getBinaryName()
              + TypeDescriptors.getTypeArgumentsUniqueId(
                  originalTypeDescriptor.getTypeArgumentDescriptors());
    }
    uniqueId = nullabilityPrefix + uniqueId;

    RegularTypeDescriptor newTypeDescriptor =
        RegularTypeDescriptor.Builder.from(originalTypeDescriptor)
            .setIsNullable(false)
            .setUniqueId(uniqueId)
            .build();

    return intern(newTypeDescriptor);
  }

  public static TypeDescriptor toNullable(TypeDescriptor originalTypeDescriptor) {
    Preconditions.checkArgument(!originalTypeDescriptor.isTypeVariable());

    if (originalTypeDescriptor.isNullable()) {
      return originalTypeDescriptor;
    }

    String nullabilityPrefix = "?";
    // TODO (stalcup): pull out a reusable uniqueId calculation function once.
    String uniqueId;
    if (originalTypeDescriptor.isArray()) {
      uniqueId =
          "("
              + originalTypeDescriptor.getLeafTypeDescriptor().getUniqueId()
              + ")"
              + Strings.repeat("[]", originalTypeDescriptor.getDimensions());
    } else {
      uniqueId =
          originalTypeDescriptor.getBinaryName()
              + TypeDescriptors.getTypeArgumentsUniqueId(
                  originalTypeDescriptor.getTypeArgumentDescriptors());
    }
    uniqueId = nullabilityPrefix + uniqueId;

    RegularTypeDescriptor newTypeDescriptor =
        RegularTypeDescriptor.Builder.from(originalTypeDescriptor)
            .setIsNullable(true)
            .setUniqueId(uniqueId)
            .build();

    return intern(newTypeDescriptor);
  }

  public static TypeDescriptor createLambdaTypeDescriptor(
      TypeDescriptor enclosingClassTypeDescriptor,
      String lambdaBinaryName,
      ITypeBinding lambdaInterfaceBinding) {
    return intern(
        new LambdaTypeDescriptor(
            enclosingClassTypeDescriptor, lambdaBinaryName, lambdaInterfaceBinding));
  }

  // TODO(stalcup): examine whether createRaw() uses should be turned into createNative() uses,
  // since accessing native bootstrap classes is so conceptually similar to accessing native JsType
  // classes.
  public static TypeDescriptor createRaw(List<String> nameSpaceComponents, String className) {
    return createSyntheticRegularTypeDescriptor(
        nameSpaceComponents, Arrays.asList(className), true, Collections.emptyList());
  }

  /**
   * Creates a native TypeDescriptor from a qualified name.
   */
  public static TypeDescriptor createNative(String qualifiedName) {
    if (JsInteropUtils.isGlobal(qualifiedName)) {
      return createSyntheticNativeTypeDescriptor(
          Arrays.asList(JsInteropUtils.JS_GLOBAL),
          Arrays.asList(""),
          Collections.emptyList(),
          JsInteropUtils.JS_GLOBAL,
          "");
    }
    List<String> nameComponents = Splitter.on('.').splitToList(qualifiedName);
    int size = nameComponents.size();
    // Fill in JS_GLOBAL as the namespace if the namespace is empty.
    List<String> namespaceComponents =
        size == 1 ? Arrays.asList(JsInteropUtils.JS_GLOBAL) : nameComponents.subList(0, size - 1);
    return createSyntheticNativeTypeDescriptor(
        namespaceComponents,
        nameComponents.subList(size - 1, size),
        Collections.emptyList(),
        Joiner.on(".").join(namespaceComponents),
        nameComponents.get(size - 1));
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

  // This is only used by TypeProxyUtils, and cannot be used elsewhere. Because to create a
  // TypeDescriptor from a TypeBinding, it should go through the path to check array type.
  static TypeDescriptor createFromTypeBinding(
      final ITypeBinding typeBinding, List<TypeDescriptor> overrideTypeArgumentDescriptors) {
    checkArgument(!typeBinding.isArray());

    // These must be lazily calculated or else there are guarranteed infinite loops of
    // TypeDescriptor creation.
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
    String className = createClassName(classComponents, simpleName, isPrimitive, isTypeVariable);
    boolean isNative = JsInteropAnnotationUtils.isNative(jsTypeAnnotation);
    boolean isNullable = !typeBinding.isPrimitive();
    String jsTypeName = JsInteropAnnotationUtils.getJsName(jsTypeAnnotation);
    String jsTypeNamespace = JsInteropAnnotationUtils.getJsNamespace(jsTypeAnnotation);
    String packageName = Joiner.on(".").join(packageComponents);
    String sourceName = Joiner.on(".").join(Iterables.concat(packageComponents, classComponents));
    String toString = sourceName;
    List<TypeDescriptor> typeArgumentDescriptors =
        overrideTypeArgumentDescriptors != null
            ? overrideTypeArgumentDescriptors
            : TypeProxyUtils.getTypeArgumentDescriptors(typeBinding);

    // Compute these even later
    boolean isExtern = isNative && JsInteropUtils.isGlobal(jsTypeNamespace);

    return intern(
        new RegularTypeDescriptor.Builder()
            .setBinaryName(binaryName)
            .setClassComponents(classComponents)
            .setClassName(className)
            .setConcreteJsFunctionMethodDescriptorFactory(concreteJsFunctionMethodDescriptorFactory)
            .setEnclosingTypeDescriptorFactory(enclosingTypeDescriptorFactory)
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
            .setJsTypeName(jsTypeName)
            .setJsTypeNamespace(jsTypeNamespace)
            .setPackageComponents(packageComponents)
            .setPackageName(packageName)
            .setQualifiedName(
                createQualifiedName(
                    packageName, className, simpleName, jsTypeNamespace, jsTypeName))
            .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
            .setSimpleName(simpleName)
            .setSourceName(sourceName)
            .setSubclassesJsConstructorClass(
                TypeProxyUtils.subclassesJsConstructorClass(typeBinding))
            .setSuperTypeDescriptorFactory(superTypeDescriptorFactory)
            .setToString(toString)
            .setTypeArgumentDescriptors(typeArgumentDescriptors)
            .setUniqueId(
                (isNullable ? "?" : "!")
                    + (isTypeVariable
                        ? typeBinding.getBinaryName()
                            + ":"
                            + typeBinding.getErasure().getBinaryName()
                        : binaryName
                            + TypeDescriptors.getTypeArgumentsUniqueId(typeArgumentDescriptors)))
            .setVisibility(TypeProxyUtils.getVisibility(typeBinding.getModifiers()))
            .build());
  }

  /**
   * TODO: Currently we depends on the namespace to tell if a type is an extern type. Returns true
   * if the namespace is an empty string. It is true for most common cases, but not always true. We
   * may need to introduce a new annotation to tell if it is extern when we hit the problem.
   */
  public static boolean isExtern(TypeDescriptor typeDescriptor) {
    boolean isSynthesizedGlobalType =
        typeDescriptor.isRaw() && JsInteropUtils.isGlobal(typeDescriptor.getPackageName());
    boolean isNativeJsType =
        typeDescriptor.isNative() && JsInteropUtils.isGlobal(typeDescriptor.getJsNamespace());
    return isSynthesizedGlobalType || isNativeJsType;
  }

  public static boolean isGlobal(TypeDescriptor typeDescriptor) {
    return "".equals(typeDescriptor.getQualifiedName());
  }

  public static String getBinaryName(TypeDescriptor typeDescriptor) {
    return Joiner.on(".")
        .join(
            Iterables.concat(
                typeDescriptor.getPackageComponents(),
                Collections.singleton(Joiner.on("$").join(typeDescriptor.getClassComponents()))));
  }

  public static String getClassName(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isPrimitive()) {
      return "$" + typeDescriptor.getSimpleName();
    }
    if (typeDescriptor.getSimpleName().equals("?")) {
      return "?";
    }
    if (typeDescriptor.isTypeVariable()) {
      checkArgument(
          typeDescriptor.getClassComponents().size() > 1,
          "Type Variable (not including wild card type) should have at least two name components");

      // skip the top level class component for better output readability.
      List<String> nameComponents =
          new ArrayList<>(
              typeDescriptor
                  .getClassComponents()
                  .subList(1, typeDescriptor.getClassComponents().size()));

      // move the prefix in the simple name to the class name to avoid collisions between method-
      // level and class-level type variable and avoid variable name starts with a number.
      // concat class components to avoid collisions between type variables in inner/outer class.
      // use '_' instead of '$' because '$' is not allowed in template variable name in closure.
      String simpleName = typeDescriptor.getSimpleName();
      nameComponents.set(
          nameComponents.size() - 1, simpleName.substring(simpleName.indexOf('_') + 1));
      String prefix = simpleName.substring(0, simpleName.indexOf('_') + 1);

      return prefix + Joiner.on('_').join(nameComponents);
    }
    return Joiner.on('$').join(typeDescriptor.getClassComponents());
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
    final TypeDescriptor[] holder = new TypeDescriptor[1];

    // These must be lazily calculated or else there are guarranteed infinite loops of
    // TypeDescriptor creation.
    TypeDescriptorFactory rawTypeDescriptorFactory =
        new TypeDescriptorFactory() {
          @Override
          public TypeDescriptor create() {
            return holder[0];
          }
        };

    // Compute these first since they're reused in other calculations.
    String prefix = Strings.repeat("[", dimensions);
    String suffix = Strings.repeat("[]", dimensions);
    String simpleName = leafTypeDescriptor.getSimpleName() + suffix;

    // Compute everything else.
    String binaryName = prefix + leafTypeDescriptor.getBinaryName();
    String className = leafTypeDescriptor.getClassName() + suffix;
    TypeDescriptor componentTypeDescriptor =
        getForArray(leafTypeDescriptor, dimensions - 1, isNullable);
    String packageName = leafTypeDescriptor.getPackageName();
    String qualifiedName = createQualifiedName(packageName, className, simpleName, null, null);
    String sourceName = leafTypeDescriptor.getSourceName() + suffix;
    String toString = sourceName;
    List<TypeDescriptor> typeArgumentDescriptors = Collections.emptyList();
    String uniqueId = leafTypeDescriptor.getUniqueId() + suffix;
    uniqueId = (isNullable ? "?" : "!") + uniqueId;

    TypeDescriptor typeDescriptor =
        intern(
            new RegularTypeDescriptor.Builder()
                .setBinaryName(binaryName)
                .setClassName(className)
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
                .setToString(toString)
                .setTypeArgumentDescriptors(typeArgumentDescriptors)
                .setUniqueId(uniqueId)
                .build());

    holder[0] = typeDescriptor;

    return typeDescriptor;
  }

  public static String getQualifiedName(TypeDescriptor typeDescriptor) {
    String namespace = typeDescriptor.getPackageName();
    String className = typeDescriptor.getClassName();

    // If a custom js namespace was specified.
    if (typeDescriptor.getJsNamespace() != null) {
      // The effect is to replace both the package and the class's enclosing class prefixes.
      namespace = typeDescriptor.getJsNamespace();
      className = typeDescriptor.getSimpleName();
    }

    // If the JS namespace the user specified was JsPackage.GLOBAL then consider that to be top
    // level.
    if (JsInteropUtils.isGlobal(namespace)) {
      namespace = "";
    }

    // If a custom JS name was specified.
    if (typeDescriptor.getJsName() != null) {
      // Then use it instead of the (potentially enclosing class qualified) class name.
      className = typeDescriptor.getJsName();
    }

    return Joiner.on(".")
        .skipNulls()
        .join(Strings.emptyToNull(namespace), Strings.emptyToNull(className));
  }

  public static String getTypeArgumentsUniqueId(final TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isParameterizedType()) {
      List<TypeDescriptor> typeArgumentDescriptors = typeDescriptor.getTypeArgumentDescriptors();
      return getTypeArgumentsUniqueId(typeArgumentDescriptors);
    }
    return "";
  }

  private static String getTypeArgumentsUniqueId(List<TypeDescriptor> typeArgumentDescriptors) {
    if (typeArgumentDescriptors.isEmpty()) {
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
    return NullLiteral.NULL;
  }
}
