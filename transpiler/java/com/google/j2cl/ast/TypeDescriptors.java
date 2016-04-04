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

import org.eclipse.jdt.core.dom.AST;
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

  /**
   * Converts a type into the correct type that should result from an operation on it.
   * Returned values are always primitive or String.
   */
  public static TypeDescriptor asOperatorReturnType(TypeDescriptor typeDescriptor) {
    Preconditions.checkArgument(
        isBoxedOrPrimitiveType(typeDescriptor) || typeDescriptor == get().javaLangString);
    if (isBoxedType(typeDescriptor)) {
      return getPrimitiveTypeFromBoxType(typeDescriptor);
    }
    return typeDescriptor;
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
   * <p>Even though the floating point types are only 4 and 8 bytes respectively they are considered
   * very wide because of the magnitude of the maximum values they can encode.
   */
  public static int getWidth(TypeDescriptor typeDescriptor) {
    Preconditions.checkArgument(typeDescriptor.isPrimitive());

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
    } else { // typeDescriptor == typeDescriptors.primitiveDouble
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
    Preconditions.checkNotNull(javaLangInteger);
    return TypeProxyUtils.createTypeDescriptor(javaLangInteger.getSuperclass());
  }

  /**
   * Create TypeDescriptor for java.lang.Comparable, which is not a well known type by JDT.
   */
  private static TypeDescriptor createJavaLangComparable(AST ast) {
    ITypeBinding javaLangInteger = ast.resolveWellKnownType("java.lang.Integer");
    Preconditions.checkNotNull(javaLangInteger);
    ITypeBinding[] interfaces = javaLangInteger.getInterfaces();
    Preconditions.checkArgument(interfaces.length == 1);
    return TypeProxyUtils.createTypeDescriptor(interfaces[0].getErasure());
  }

  /**
   * Create TypeDescriptor for java.lang.CharSequence, which is not a well known type by JDT.
   */
  private static TypeDescriptor createJavaLangCharSequence(AST ast) {
    ITypeBinding javaLangString = ast.resolveWellKnownType("java.lang.String");
    Preconditions.checkNotNull(javaLangString);
    ITypeBinding[] interfaces = javaLangString.getInterfaces();
    Preconditions.checkArgument(interfaces.length == 3);
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
          Lists.newArrayList("NativeString"),
          ImmutableList.<TypeDescriptor>of(),
          // Browser global
          JsInteropUtils.JS_GLOBAL,
          // Native type name
          "String");
  public static final TypeDescriptor NATIVE_FUNCTION =
      createSyntheticNativeTypeDescriptor(
          new ArrayList<String>(),
          // Import alias.
          Lists.newArrayList("NativeFunction"),
          ImmutableList.<TypeDescriptor>of(),
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
      Iterable<String> packageComponents,
      Iterable<String> classComponents,
      boolean isRaw,
      Iterable<TypeDescriptor> typeArgumentDescriptors) {
    Preconditions.checkArgument(!Iterables.getLast(classComponents).contains("<"));
    return getInterner()
        .intern(
            new SyntheticRegularTypeDescriptor(
                ImmutableList.copyOf(packageComponents),
                ImmutableList.copyOf(classComponents),
                isRaw,
                ImmutableList.copyOf(typeArgumentDescriptors)));
  }

  public static TypeDescriptor createSyntheticNativeTypeDescriptor(
      Iterable<String> packageComponents,
      Iterable<String> classComponents,
      Iterable<TypeDescriptor> typeArgumentDescriptors,
      String jsTypeNamespace,
      String jsTypeName) {
    return getInterner()
        .intern(
            new SyntheticRegularTypeDescriptor(
                ImmutableList.copyOf(packageComponents),
                ImmutableList.copyOf(classComponents),
                ImmutableList.copyOf(typeArgumentDescriptors),
                jsTypeNamespace,
                jsTypeName));
  }

  public static TypeDescriptor createSyntheticParametricTypeDescriptor(
      TypeDescriptor originalTypeDescriptor, Iterable<TypeDescriptor> typeArgumentTypeDescriptors) {
    return getInterner()
        .intern(
            new SyntheticParametricTypeDescriptor(
                originalTypeDescriptor, typeArgumentTypeDescriptors));
  }

  public static TypeDescriptor createLambdaTypeDescriptor(
      TypeDescriptor enclosingClassTypeDescriptor,
      String lambdaBinaryName,
      ITypeBinding lambdaInterfaceBinding) {
    return getInterner()
        .intern(
            new LambdaTypeDescriptor(
                enclosingClassTypeDescriptor, lambdaBinaryName, lambdaInterfaceBinding));
  }

  // TODO(stalcup): examine whether createRaw() uses should be turned into createNative() uses,
  // since accessing native bootstrap classes is so conceptually similar to accessing native JsType
  // classes.
  public static TypeDescriptor createRaw(Iterable<String> nameSpaceComponents, String className) {
    return createSyntheticRegularTypeDescriptor(
        nameSpaceComponents, Arrays.asList(className), true, ImmutableList.<TypeDescriptor>of());
  }

  /**
   * Creates a native TypeDescriptor from a qualified name.
   */
  public static TypeDescriptor createNative(String qualifiedName) {
    if (JsInteropUtils.isGlobal(qualifiedName)) {
      return createSyntheticNativeTypeDescriptor(
          Arrays.asList(JsInteropUtils.JS_GLOBAL),
          Arrays.asList(""),
          ImmutableList.<TypeDescriptor>of(),
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
        ImmutableList.<TypeDescriptor>of(),
        Joiner.on(".").join(namespaceComponents),
        nameComponents.get(size - 1));
  }

  static Interner<TypeDescriptor> getInterner() {
    if (interner == null) {
      interner = Interners.newWeakInterner();
    }
    return interner;
  }

  // This is only used by TypeProxyUtils, and cannot be used elsewhere. Because to create a
  // TypeDescriptor from a TypeBinding, it should go through the path to check array type.
  static TypeDescriptor create(ITypeBinding typeBinding) {
    Preconditions.checkArgument(!typeBinding.isArray());
    return getInterner().intern(new RegularTypeDescriptor(typeBinding));
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
      Preconditions.checkArgument(
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

  public static TypeDescriptor getForArray(TypeDescriptor typeDescriptor, int dimensions) {
    if (dimensions == 0) {
      return typeDescriptor;
    }
    return getInterner().intern(new AutoValue_ArrayTypeDescriptor(dimensions, typeDescriptor));
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
      return String.format(
          "<%s>",
          Joiner.on(", ")
              .join(
                  Lists.transform(
                      typeDescriptor.getTypeArgumentDescriptors(),
                      new Function<TypeDescriptor, String>() {
                        @Override
                        public String apply(TypeDescriptor typeDescriptor) {
                          return typeDescriptor.getUniqueId();
                        }
                      })));
    }
    return "";
  }
}
