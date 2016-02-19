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
package com.google.j2cl.generator;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility functions for JsDoc names.
 */
public class JsDocNameUtils {
  private static final String JS_STRING_TYPE_NAME = "string";
  private static final String JS_BOOLEAN_TYPE_NAME = "boolean";
  private static final String JS_NUMBER_TYPE_NAME = "number";
  private static final String JS_NULLABLE_PREFIX = "?";

  /**
   * Mapping from unboxed type descriptor (Boolean, Double, String) to its JsDoc name.
   */
  private static Map<TypeDescriptor, String> jsDocNamesByUnboxedTypeDescriptor;
  /**
   * Mapping from super type descriptor (super classes or super interfaces) to its sub classes
   * or implementing classes that are unboxed types.
   */
  private static Multimap<TypeDescriptor, TypeDescriptor>
      unboxedTypeDescriptorsBySuperTypeDescriptor;

  public static void init() {
    initUnboxedTypeDescriptor();
    initSuperTypesOfUnboxedTypeDescriptor();
  }

  /**
   * Initialize special cases for unboxed type descriptors
   */
  private static void initUnboxedTypeDescriptor() {
    if (jsDocNamesByUnboxedTypeDescriptor != null) {
      // Already initialized.
      return;
    }
    jsDocNamesByUnboxedTypeDescriptor = new HashMap<>();
    // unboxed types: Boolean => boolean, Double => number, String => ?string.
    TypeDescriptor javaLangBoolean = TypeDescriptors.get().javaLangBoolean;
    TypeDescriptor javaLangDouble = TypeDescriptors.get().javaLangDouble;
    TypeDescriptor javaLangString = TypeDescriptors.get().javaLangString;
    jsDocNamesByUnboxedTypeDescriptor.put(
        javaLangBoolean, JS_NULLABLE_PREFIX + JS_BOOLEAN_TYPE_NAME);
    jsDocNamesByUnboxedTypeDescriptor.put(javaLangDouble, JS_NULLABLE_PREFIX + JS_NUMBER_TYPE_NAME);
    jsDocNamesByUnboxedTypeDescriptor.put(javaLangString, JS_NULLABLE_PREFIX + JS_STRING_TYPE_NAME);
  }

  /**
   * Initialize mapping from super types to its child unboxed types.
   */
  private static void initSuperTypesOfUnboxedTypeDescriptor() {
    if (unboxedTypeDescriptorsBySuperTypeDescriptor != null) {
      // Already initialized.
      return;
    }
    unboxedTypeDescriptorsBySuperTypeDescriptor = HashMultimap.create();
    TypeDescriptor rawJavaLangComparable =
        TypeDescriptors.get().javaLangComparable.getRawTypeDescriptor();
    TypeDescriptor javaLangBoolean = TypeDescriptors.get().javaLangBoolean;
    TypeDescriptor javaLangDouble = TypeDescriptors.get().javaLangDouble;
    TypeDescriptor javaLangString = TypeDescriptors.get().javaLangString;
    TypeDescriptor javaLangNumber = TypeDescriptors.get().javaLangNumber;
    TypeDescriptor javaLangCharSequence = TypeDescriptors.get().javaLangCharSequence;

    // Boolean implements Comparable
    unboxedTypeDescriptorsBySuperTypeDescriptor.put(rawJavaLangComparable, javaLangBoolean);

    // Double extends Number implements Comparable
    unboxedTypeDescriptorsBySuperTypeDescriptor.put(rawJavaLangComparable, javaLangDouble);
    unboxedTypeDescriptorsBySuperTypeDescriptor.put(javaLangNumber, javaLangDouble);

    // String implements Comparable, CharSequence
    unboxedTypeDescriptorsBySuperTypeDescriptor.put(rawJavaLangComparable, javaLangString);
    unboxedTypeDescriptorsBySuperTypeDescriptor.put(javaLangCharSequence, javaLangString);
  }

  /**
   * Returns the JsDoc type name.
   */
  static String getJsDocName(TypeDescriptor typeDescriptor, GenerationEnvironment environment) {
    return getJsDocName(typeDescriptor, false, environment);
  }

  /**
   * Returns the list of JsDoc names of a list of type descriptors separated by comma.
   */
  static String getJsDocNames(
      Iterable<TypeDescriptor> typeDescriptors, final GenerationEnvironment environment) {
    Iterable<String> typeParameterDescriptors =
        Iterables.transform(
            typeDescriptors,
            new Function<TypeDescriptor, String>() {
              @Override
              public String apply(TypeDescriptor typeDescriptor) {
                return getJsDocName(typeDescriptor, environment);
              }
            });
    return Joiner.on(", ").join(typeParameterDescriptors);
  }

  /**
   * Returns the JsDoc type name.
   * If {@code shouldUseClassName} is true (when it is used in extends or implements clause, or is
   * used in constructor), use the class name not the specialized name.
   */
  static String getJsDocName(
      TypeDescriptor typeDescriptor,
      boolean shouldUseClassName,
      GenerationEnvironment environment) {
    if (typeDescriptor.isArray()) {
      return String.format(
          "%s%s%s",
          Strings.repeat("Array<", typeDescriptor.getDimensions()),
          getJsDocName(typeDescriptor.getLeafTypeDescriptor(), environment),
          Strings.repeat(">", typeDescriptor.getDimensions()));
    }

    if (typeDescriptor.isParameterizedType()) {
      TypeDescriptor rawTypeDescriptor = typeDescriptor.getRawTypeDescriptor();
      String jsDocName =
          String.format(
              "%s<%s>",
              ExpressionTransformer.transform(rawTypeDescriptor, environment),
              getJsDocNames(typeDescriptor.getTypeArgumentDescriptors(), environment));
      if (!shouldUseClassName
          && unboxedTypeDescriptorsBySuperTypeDescriptor.containsKey(rawTypeDescriptor)) {
        // types that are extended or implemented by unboxed types
        // JsDoc type should be a union type.
        jsDocName = String.format("(%s|%s)", jsDocName, getJsDocNameOfUnionType(rawTypeDescriptor));
      }
      if (!shouldUseClassName
          && (typeDescriptor.isJsFunctionInterface()
              || typeDescriptor.isJsFunctionImplementation())) {
        // JsFunction interface and implementor should accept a real JS function.
        jsDocName = getJsDocNameForJsFunction(typeDescriptor, environment);
      }
      return jsDocName;
    }

    // Special cases.
    switch (typeDescriptor.getSourceName()) {
      case TypeDescriptor.BYTE_TYPE_NAME:
      case TypeDescriptor.SHORT_TYPE_NAME:
      case TypeDescriptor.INT_TYPE_NAME:
      case TypeDescriptor.FLOAT_TYPE_NAME:
      case TypeDescriptor.DOUBLE_TYPE_NAME:
      case TypeDescriptor.CHAR_TYPE_NAME:
        return JS_NUMBER_TYPE_NAME;
      case TypeDescriptor.LONG_TYPE_NAME:
        return "!"
            + ExpressionTransformer.transform(
                BootstrapType.NATIVE_LONG.getDescriptor(), environment);
      case "java.lang.Object":
        if (!shouldUseClassName) {
          return "*";
        }
        break;
    }

    // Special cases for unboxed types.
    if (!shouldUseClassName && jsDocNamesByUnboxedTypeDescriptor.containsKey(typeDescriptor)) {
      return jsDocNamesByUnboxedTypeDescriptor.get(typeDescriptor);
    }

    // types that are extended or implemented by unboxed types.
    if (!shouldUseClassName
        && unboxedTypeDescriptorsBySuperTypeDescriptor.containsKey(typeDescriptor)) {
      return String.format(
          "(%s|%s)",
          ExpressionTransformer.transform(typeDescriptor, environment),
          getJsDocNameOfUnionType(typeDescriptor));
    }
    if (typeDescriptor.isPrimitive()) {
      return typeDescriptor.getSimpleName();
    }

    // JsFunction interface and implementor is a real JS function.
    if (!shouldUseClassName
        && (typeDescriptor.isJsFunctionInterface()
            || typeDescriptor.isJsFunctionImplementation())) {
      return getJsDocNameForJsFunction(typeDescriptor, environment);
    }

    // Literal native js types do not refer to any concrete types.
    if (typeDescriptor.isGlobalNativeInterface()) {
      return "*";
    }

    return ExpressionTransformer.transform(typeDescriptor, environment);
  }

  /**
   * Assume {@code typeDescriptor} is extended or implemented by an unboxed type,
   * it returns the JsDoc name of the union of all its inheriting/implementing types.
   */
  private static String getJsDocNameOfUnionType(TypeDescriptor typeDescriptor) {
    Preconditions.checkArgument(
        unboxedTypeDescriptorsBySuperTypeDescriptor.containsKey(typeDescriptor));
    return Joiner.on("|")
        .join(
            Iterables.transform(
                unboxedTypeDescriptorsBySuperTypeDescriptor.get(typeDescriptor),
                new Function<TypeDescriptor, String>() {
                  @Override
                  public String apply(TypeDescriptor unboxedTypeDescriptor) {
                    Preconditions.checkArgument(
                        jsDocNamesByUnboxedTypeDescriptor.containsKey(unboxedTypeDescriptor));
                    return jsDocNamesByUnboxedTypeDescriptor.get(unboxedTypeDescriptor);
                  }
                }));
  }

  private static String getJsDocNameForJsFunction(
      TypeDescriptor typeDescriptor, GenerationEnvironment environment) {
    // Java does not do type checking on raw type, which is very similar as 'window.Function' in
    // JS compiler's type system. 'window.Function' is both a super class and a sub class of any
    // function(...): types.
    if (typeDescriptor.isRawType()) {
      return "window.Function";
    }
    MethodDescriptor jsFunctionMethodDescriptor =
        typeDescriptor.getConcreteJsFunctionMethodDescriptor();
    Preconditions.checkNotNull(jsFunctionMethodDescriptor);

    int parameterIndex = 0;
    int parameterCount = jsFunctionMethodDescriptor.getParameterTypeDescriptors().size();
    List<String> parameterTypesList = new ArrayList<>();
    for (TypeDescriptor parameterTypeDescriptor :
        jsFunctionMethodDescriptor.getParameterTypeDescriptors()) {
      String parameterTypeAnnotation;
      if (jsFunctionMethodDescriptor.isJsMethodVarargs() && parameterIndex == parameterCount - 1) {
        // variable parameters
        Preconditions.checkArgument(parameterTypeDescriptor.isArray());
        String typeName =
            JsDocNameUtils.getJsDocName(
                parameterTypeDescriptor.getComponentTypeDescriptor(), environment);
        parameterTypeAnnotation = "..." + typeName;
      } else {
        parameterTypeAnnotation = JsDocNameUtils.getJsDocName(parameterTypeDescriptor, environment);
      }
      parameterIndex++;
      parameterTypesList.add(parameterTypeAnnotation);
    }
    return String.format(
        "?function(%s):%s",
        Joiner.on(", ").join(parameterTypesList),
        getJsDocName(jsFunctionMethodDescriptor.getReturnTypeDescriptor(), environment));
  }
}
