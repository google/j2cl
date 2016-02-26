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

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.ArrayList;
import java.util.Arrays;
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
    typeDescriptors.primitiveBoolean = create(ast, TypeDescriptor.BOOLEAN_TYPE_NAME);
    typeDescriptors.primitiveByte = create(ast, TypeDescriptor.BYTE_TYPE_NAME);
    typeDescriptors.primitiveChar = create(ast, TypeDescriptor.CHAR_TYPE_NAME);
    typeDescriptors.primitiveDouble = create(ast, TypeDescriptor.DOUBLE_TYPE_NAME);
    typeDescriptors.primitiveFloat = create(ast, TypeDescriptor.FLOAT_TYPE_NAME);
    typeDescriptors.primitiveInt = create(ast, TypeDescriptor.INT_TYPE_NAME);
    typeDescriptors.primitiveLong = create(ast, TypeDescriptor.LONG_TYPE_NAME);
    typeDescriptors.primitiveShort = create(ast, TypeDescriptor.SHORT_TYPE_NAME);
    typeDescriptors.primitiveVoid = create(ast, TypeDescriptor.VOID_TYPE_NAME);

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
        TypeDescriptors.isBoxedOrPrimitiveType(typeDescriptor)
            || typeDescriptor == TypeDescriptors.get().javaLangString);
    if (TypeDescriptors.isBoxedType(typeDescriptor)) {
      return TypeDescriptors.getPrimitiveTypeFromBoxType(typeDescriptor);
    }
    return typeDescriptor;
  }

  public static TypeDescriptor getBoxTypeFromPrimitiveType(TypeDescriptor primitiveType) {
    return TypeDescriptors.get().boxedTypeByPrimitiveType.get(primitiveType);
  }

  public static TypeDescriptor getPrimitiveTypeFromBoxType(TypeDescriptor boxType) {
    return TypeDescriptors.get().boxedTypeByPrimitiveType.inverse().get(boxType);
  }

  public static boolean isBoxedType(TypeDescriptor typeDescriptor) {
    return TypeDescriptors.get().boxedTypeByPrimitiveType.containsValue(typeDescriptor);
  }

  public static boolean isNonVoidPrimitiveType(TypeDescriptor typeDescriptor) {
    return TypeDescriptors.get().boxedTypeByPrimitiveType.containsKey(typeDescriptor);
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

    TypeDescriptors typeDescriptors = TypeDescriptors.get();
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
      RegularTypeDescriptor.createSyntheticNativeTypeDescriptor(
          new ArrayList<String>(),
          // Import alias.
          Lists.newArrayList("NativeString"),
          ImmutableList.<TypeDescriptor>of(),
          // Browser global
          JsInteropUtils.JS_GLOBAL,
          // Native type name
          "String");
  public static final TypeDescriptor NATIVE_FUNCTION =
      RegularTypeDescriptor.createSyntheticNativeTypeDescriptor(
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
      this.typeDescriptor = TypeDescriptor.createRaw(pathComponents, name);
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
}
