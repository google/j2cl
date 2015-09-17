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

import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;

import java.util.HashMap;
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
  private static final Map<TypeDescriptor, String> jsDocNamesByUnboxedTypeDescriptor =
      new HashMap<>();
  /**
   * Mapping from super type descriptor (super classes or super interfaces) to its sub classes
   * or implementing classes that are unboxed types.
   */
  private static final Multimap<TypeDescriptor, TypeDescriptor>
      unboxedTypeDescriptorsBySuperTypeDescriptor = HashMultimap.create();

  // initialize special cases for unboxed type descriptors
  static {
    // unboxed types: Boolean => boolean, Double => number, String => ?string.
    TypeDescriptor javaLangBoolean =
        TypeDescriptors.boxedTypeByPrimitiveType.get(TypeDescriptors.BOOLEAN_TYPE_DESCRIPTOR);
    TypeDescriptor javaLangDouble =
        TypeDescriptors.boxedTypeByPrimitiveType.get(TypeDescriptors.DOUBLE_TYPE_DESCRIPTOR);
    TypeDescriptor javaLangString = TypeDescriptors.STRING_TYPE_DESCRIPTOR;
    jsDocNamesByUnboxedTypeDescriptor.put(
        javaLangBoolean, JS_NULLABLE_PREFIX + JS_BOOLEAN_TYPE_NAME);
    jsDocNamesByUnboxedTypeDescriptor.put(javaLangDouble, JS_NULLABLE_PREFIX + JS_NUMBER_TYPE_NAME);
    jsDocNamesByUnboxedTypeDescriptor.put(javaLangString, JS_NULLABLE_PREFIX + JS_STRING_TYPE_NAME);
  }

  // initialize mapping from super types to its child unboxed types.
  static {
    TypeDescriptor javaLangComparable = TypeDescriptors.COMPARABLE_TYPE_DESCRIPTOR;
    TypeDescriptor javaLangBoolean =
        TypeDescriptors.boxedTypeByPrimitiveType.get(TypeDescriptors.BOOLEAN_TYPE_DESCRIPTOR);
    TypeDescriptor javaLangDouble =
        TypeDescriptors.boxedTypeByPrimitiveType.get(TypeDescriptors.DOUBLE_TYPE_DESCRIPTOR);
    TypeDescriptor javaLangString = TypeDescriptors.STRING_TYPE_DESCRIPTOR;
    TypeDescriptor javaLangNumber = TypeDescriptors.NUMBER_TYPE_DESCRIPTOR;

    // Boolean implements Comparable
    unboxedTypeDescriptorsBySuperTypeDescriptor.put(javaLangComparable, javaLangBoolean);

    // Double extends Number implements Comparable
    unboxedTypeDescriptorsBySuperTypeDescriptor.put(javaLangComparable, javaLangDouble);
    unboxedTypeDescriptorsBySuperTypeDescriptor.put(javaLangNumber, javaLangDouble);

    // TODO: register String implements CharSequence
    unboxedTypeDescriptorsBySuperTypeDescriptor.put(javaLangComparable, javaLangString);
  }

  /**
   * Returns the JsDoc type name.
   */
  static String getJsDocName(
      TypeDescriptor typeDescriptor, StatementSourceGenerator statementSourceGenerator) {
    return getJsDocName(typeDescriptor, false, statementSourceGenerator);
  }

  /**
   * Returns the list of JsDoc names of a list of type descriptors separated by comma.
   */
  static String getJsDocNames(
      Iterable<TypeDescriptor> typeDescriptors,
      final StatementSourceGenerator statementSourceGenerator) {
    Iterable<String> typeParameterDescriptors =
        Iterables.transform(
            typeDescriptors,
            new Function<TypeDescriptor, String>() {
              @Override
              public String apply(TypeDescriptor typeDescriptor) {
                return getJsDocName(typeDescriptor, statementSourceGenerator);
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
      StatementSourceGenerator statementSourceGenerator) {
    if (typeDescriptor.isArray()) {
      return String.format(
          "%s%s%s",
          Strings.repeat("Array<", typeDescriptor.getDimensions()),
          getJsDocName(typeDescriptor.getLeafTypeDescriptor(), statementSourceGenerator),
          Strings.repeat(">", typeDescriptor.getDimensions()));
    }

    if (typeDescriptor.isParameterizedType()) {
      TypeDescriptor rawTypeDescriptor = typeDescriptor.getRawTypeDescriptor();
      String jsDocName =
          String.format(
              "%s<%s>",
              statementSourceGenerator.toSource(rawTypeDescriptor),
              getJsDocNames(typeDescriptor.getTypeArgumentDescriptors(), statementSourceGenerator));
      if (!shouldUseClassName
          && unboxedTypeDescriptorsBySuperTypeDescriptor.containsKey(rawTypeDescriptor)) {
        // types that are extended or implemented by unboxed types
        // JsDoc type should be a union type.
        jsDocName = String.format("(%s|%s)", jsDocName, getJsDocNameOfUnionType(rawTypeDescriptor));
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
        return "!" + statementSourceGenerator.toSource(TypeDescriptors.NATIVE_LONG_TYPE_DESCRIPTOR);
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
          statementSourceGenerator.toSource(typeDescriptor),
          getJsDocNameOfUnionType(typeDescriptor));
    }
    if (typeDescriptor.isPrimitive()) {
      return typeDescriptor.getSimpleName();
    }
    return statementSourceGenerator.toSource(typeDescriptor);
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
}
