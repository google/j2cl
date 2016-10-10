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

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
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
    unboxedTypeDescriptorsBySuperTypeDescriptor = LinkedHashMultimap.create();
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
            typeDescriptors, typeDescriptor -> getJsDocName(typeDescriptor, environment));
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
    // TODO: this looks like a hack to me, really the TypeDescriptor for JsFunctions should be
    // created in the first place to know they themselves are nullable.

    // JsFunction interface and implementor is a real JS function.
    // Unlike other js types, functions have to be explicitly set to nullable.
    if (!shouldUseClassName
        && (typeDescriptor.isJsFunctionInterface()
            || typeDescriptor.isJsFunctionImplementation())) {
      return getJsDocNameWithNullability(
          getJsDocNameForJsFunction(typeDescriptor, environment), typeDescriptor.isNullable());
    }

    if (!typeDescriptor.isNullable() && !shouldUseClassName && !typeDescriptor.isPrimitive()) {
      return getJsDocNameWithNullability(
          getJsDocName(TypeDescriptors.toNullable(typeDescriptor), shouldUseClassName, environment),
          false /* nullable */);
    }

    if (shouldUseClassName) {
      typeDescriptor = TypeDescriptors.toNullable(typeDescriptor);
    }

    // Everything below is nullable.
    Preconditions.checkArgument(typeDescriptor.isNullable() || typeDescriptor.isPrimitive());

    switch (typeDescriptor.getSourceName()) {
      case TypeDescriptors.BYTE_TYPE_NAME:
      case TypeDescriptors.SHORT_TYPE_NAME:
      case TypeDescriptors.INT_TYPE_NAME:
      case TypeDescriptors.FLOAT_TYPE_NAME:
      case TypeDescriptors.DOUBLE_TYPE_NAME:
      case TypeDescriptors.CHAR_TYPE_NAME:
        return JS_NUMBER_TYPE_NAME;
      case TypeDescriptors.VOID_TYPE_NAME:
      case TypeDescriptors.BOOLEAN_TYPE_NAME:
        return typeDescriptor.getSourceName();
      case TypeDescriptors.LONG_TYPE_NAME:
        return "!" + environment.aliasForType(BootstrapType.NATIVE_LONG.getDescriptor());
      case "java.lang.Object":
        if (!shouldUseClassName) {
          return "*";
        }
        break;
      default:
        checkState(!typeDescriptor.isPrimitive());
    }

    if (typeDescriptor.isArray()) {
      return String.format(
          "%s%s%s",
          Strings.repeat("Array<", typeDescriptor.getDimensions()),
          getJsDocName(typeDescriptor.getLeafTypeDescriptor(), environment),
          Strings.repeat(">", typeDescriptor.getDimensions()));
    }

    if (typeDescriptor.isParameterizedType()) {
      TypeDescriptor rawTypeDescriptor = typeDescriptor.getRawTypeDescriptor();
      List<TypeDescriptor> typeArgumentDescriptors = typeDescriptor.getTypeArgumentDescriptors();
      String typeParametersJsDoc = getJsDocNames(typeArgumentDescriptors, environment);

      // If the type is the native 'Object' class and is being given exactly one type parameter
      // then it is being used as a map. Expand it to two type parameters where the first one
      // (the implicit key type parameter) is a 'string'. This special case exists to replicate the
      // same special case that already exists in the JSCompiler optimizing backend, and to
      // generalize it to work everywhere (including when types are referenced via an alias).
      String typeQualifiedName = rawTypeDescriptor.getQualifiedName();
      if (typeQualifiedName.equals(TypeDescriptors.NATIVE_OBJECT.getQualifiedName())
          && rawTypeDescriptor.isJsType()
          && typeArgumentDescriptors.size() == 1) {
        typeParametersJsDoc = "string, " + typeParametersJsDoc;
      }

      String jsDocName =
          String.format("%s<%s>", environment.aliasForType(rawTypeDescriptor), typeParametersJsDoc);
      if (!shouldUseClassName
          && unboxedTypeDescriptorsBySuperTypeDescriptor.containsKey(rawTypeDescriptor)) {
        // types that are extended or implemented by unboxed types
        // JsDoc type should be a union type.
        jsDocName = String.format("(%s|%s)", jsDocName, getJsDocNameOfUnionType(rawTypeDescriptor));
      }
      return jsDocName;
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
          environment.aliasForType(typeDescriptor),
          getJsDocNameOfUnionType(typeDescriptor));
    }

    if (typeDescriptor.isTypeVariable()) {
      // Template variable like "C_T".

      // skip the top level class component for better output readability.
      List<String> classComponents = typeDescriptor.getClassComponents();
      List<String> nameComponents =
          new ArrayList<>(classComponents.subList(1, classComponents.size()));

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

    if (typeDescriptor.isWildCard()) {
      return "?";
    }

    return environment.aliasForType(typeDescriptor);
  }

  private static String getJsDocNameWithNullability(String jsDocType, boolean nullable) {
    if (jsDocType.equals("?") || jsDocType.equals("*")) {
      return jsDocType;
    }
    if (jsDocType.startsWith("?") || jsDocType.startsWith("!")) {
      jsDocType = jsDocType.substring(1);
    }
    if (jsDocType.equals("void")) {
      return jsDocType;
    }
    if (!nullable
        && (jsDocType.equals("boolean")
            || jsDocType.equals("number")
            || jsDocType.equals("string")
            || jsDocType.startsWith("function("))) {
      // Those types are not nullable by default.
      // Use 'function(' to make sure we are not matching on some type that starts with 'function'.
      return jsDocType;
    }
    return (nullable ? "?" : "!") + jsDocType;
  }

  /**
   * Assume {@code setTypeDescriptor} is extended or implemented by an unboxed type,
   * it returns the JsDoc name of the union of all its inheriting/implementing types.
   */
  private static String getJsDocNameOfUnionType(TypeDescriptor typeDescriptor) {
    Preconditions.checkArgument(
        unboxedTypeDescriptorsBySuperTypeDescriptor.containsKey(typeDescriptor));
    return unboxedTypeDescriptorsBySuperTypeDescriptor
        .get(typeDescriptor)
        .stream()
        .map(
            unboxedTypeDescriptor -> {
              Preconditions.checkArgument(
                  jsDocNamesByUnboxedTypeDescriptor.containsKey(unboxedTypeDescriptor));
              return jsDocNamesByUnboxedTypeDescriptor.get(unboxedTypeDescriptor);
            })
        .collect(joining("|"));
  }

  public static String getJsDocNameForJsFunction(
      TypeDescriptor typeDescriptor, GenerationEnvironment environment) {
    Preconditions.checkState(
        typeDescriptor.isJsFunctionImplementation() || typeDescriptor.isJsFunctionInterface(),
        "'%s' is not a JsFunction type.",
        typeDescriptor.getBinaryName());

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
        "function(%s):%s",
        Joiner.on(", ").join(parameterTypesList),
        getJsDocName(jsFunctionMethodDescriptor.getReturnTypeDescriptor(), environment));
  }
}

