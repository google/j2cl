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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.common.J2clUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility functions for JsDoc names.
 */
public class JsDocNameUtils {
  private static final String JS_STRING_TYPE_NAME = "string";
  private static final String JS_BOOLEAN_TYPE_NAME = "boolean";
  private static final String JS_NUMBER_TYPE_NAME = "number";
  private static final String JS_VOID_TYPE_NAME = "void";
  private static final String JS_NULLABLE_PREFIX = "?";

  /** Mapping from devirtualized type descriptor (Boolean, Double, String) to its JsDoc name. */
  private static ThreadLocal<Map<TypeDescriptor, String>> jsDocNamesByDevirtualizedTypeDescriptor =
      ThreadLocal.withInitial(
          () ->
              // Devirtualized types:
              //     Boolean => boolean, Double => number, String => ?string, Void => ?void.
              ImmutableMap.of(
                  TypeDescriptors.get().javaLangBoolean, JS_NULLABLE_PREFIX + JS_BOOLEAN_TYPE_NAME,
                  TypeDescriptors.get().javaLangDouble, JS_NULLABLE_PREFIX + JS_NUMBER_TYPE_NAME,
                  TypeDescriptors.get().javaLangString, JS_NULLABLE_PREFIX + JS_STRING_TYPE_NAME,
                  TypeDescriptors.get().javaLangVoid, JS_NULLABLE_PREFIX + JS_VOID_TYPE_NAME));
  /**
   * Mapping from super type descriptor (super classes or super interfaces) to its sub classes or
   * implementing classes that are unboxed types.
   */
  private static ThreadLocal<Multimap<TypeDescriptor, TypeDescriptor>>
      devirtualizedTypeDescriptorsBySuperTypeDescriptor =
          ThreadLocal.withInitial(
              () -> {
                TypeDescriptor rawJavaLangComparable =
                    TypeDescriptors.get().javaLangComparable.getRawTypeDescriptor();
                TypeDescriptor javaLangBoolean = TypeDescriptors.get().javaLangBoolean;
                TypeDescriptor javaLangDouble = TypeDescriptors.get().javaLangDouble;
                TypeDescriptor javaLangString = TypeDescriptors.get().javaLangString;
                TypeDescriptor javaLangNumber = TypeDescriptors.get().javaLangNumber;
                TypeDescriptor javaLangCharSequence = TypeDescriptors.get().javaLangCharSequence;

                return ImmutableMultimap.of(
                    // Boolean implements Comparable
                    rawJavaLangComparable, javaLangBoolean,
                    // Double extends Number implements Comparable
                    rawJavaLangComparable, javaLangDouble,
                    javaLangNumber, javaLangDouble,
                    // String implements Comparable, CharSequence
                    rawJavaLangComparable, javaLangString,
                    javaLangCharSequence, javaLangString);
              });

  /** Returns the list of JsDoc names of a list of type descriptors separated by comma. */
  static String getCommaSeparatedJsDocNames(
      Collection<TypeDescriptor> typeDescriptors, final GenerationEnvironment environment) {
    return typeDescriptors
        .stream()
        .map(typeDescriptor -> getJsDocName(typeDescriptor, environment))
        .collect(Collectors.joining(", "));
  }

  /** Returns the JsDoc type name. */
  static String getJsDocName(TypeDescriptor typeDescriptor, GenerationEnvironment environment) {
    checkArgument(!typeDescriptor.isIntersection());

    if (typeDescriptor.isStarOrUnknown()) {
      return typeDescriptor.getSimpleJsName();
    }
    // TODO(stalcup): this looks like a hack to me, really the TypeDescriptor for JsFunctions should
    // be created in the first place to know they themselves are nullable.

    // JsFunction interface and implementor is a real JS function.
    // Unlike other js types, functions have to be explicitly set to nullable.
    if ((typeDescriptor.isJsFunctionInterface() || typeDescriptor.isJsFunctionImplementation())) {
      return getJsDocNameWithNullability(
          getJsDocNameForJsFunction(typeDescriptor, environment), typeDescriptor.isNullable());
    }

    if (!typeDescriptor.isNullable() && !typeDescriptor.isPrimitive()) {
      return getJsDocNameWithNullability(
          getJsDocName(TypeDescriptors.toNullable(typeDescriptor), environment),
          false /* nullable */);
    }

    // Everything below is nullable.
    checkArgument(typeDescriptor.isNullable() || typeDescriptor.isPrimitive());

    switch (typeDescriptor.getQualifiedSourceName()) {
      case TypeDescriptors.BYTE_TYPE_NAME:
      case TypeDescriptors.SHORT_TYPE_NAME:
      case TypeDescriptors.INT_TYPE_NAME:
      case TypeDescriptors.FLOAT_TYPE_NAME:
      case TypeDescriptors.DOUBLE_TYPE_NAME:
      case TypeDescriptors.CHAR_TYPE_NAME:
        return JS_NUMBER_TYPE_NAME;
      case TypeDescriptors.VOID_TYPE_NAME:
      case TypeDescriptors.BOOLEAN_TYPE_NAME:
        return typeDescriptor.getQualifiedSourceName();
      case TypeDescriptors.LONG_TYPE_NAME:
        return "!" + environment.aliasForType(BootstrapType.NATIVE_LONG.getDescriptor());
      case "java.lang.Object":
        return "*";
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

    if (typeDescriptor.hasTypeArguments()) {
      TypeDescriptor rawTypeDescriptor = typeDescriptor.getRawTypeDescriptor();
      List<TypeDescriptor> typeArgumentDescriptors = typeDescriptor.getTypeArgumentDescriptors();
      String typeParametersJsDoc =
          getCommaSeparatedJsDocNames(typeArgumentDescriptors, environment);

      // If the type is the native 'Object' class and is being given exactly one type parameter
      // then it is being used as a map. Expand it to two type parameters where the first one
      // (the implicit key type parameter) is a 'string'. This special case exists to replicate the
      // same special case that already exists in the JSCompiler optimizing backend, and to
      // generalize it to work everywhere (including when types are referenced via an alias).
      String typeQualifiedName = rawTypeDescriptor.getQualifiedJsName();
      if (typeQualifiedName.equals(TypeDescriptors.get().nativeObject.getQualifiedJsName())
          && rawTypeDescriptor.isJsType()
          && typeArgumentDescriptors.size() == 1) {
        typeParametersJsDoc = "string, " + typeParametersJsDoc;
      }

      String jsDocName =
          String.format("%s<%s>", environment.aliasForType(rawTypeDescriptor), typeParametersJsDoc);
      if (hasDevirtualizedSubtype(rawTypeDescriptor)) {
        // types that are extended or implemented by unboxed types
        // JsDoc type should be a union type.
        jsDocName =
            String.format(
                "(%s|%s)",
                jsDocName, getJsDocNameAsUnionOfDevirtualizedSubtypes(rawTypeDescriptor));
      }
      return jsDocName;
    }

    // Special cases for unboxed types.
    if (needsJsDocNameMapping(typeDescriptor)) {
      return mapDevirtualizedJsDocName(typeDescriptor);
    }

    // types that are extended or implemented by unboxed types.
    if (hasDevirtualizedSubtype(typeDescriptor)) {
      return String.format(
          "(%s|%s)",
          environment.aliasForType(typeDescriptor),
          getJsDocNameAsUnionOfDevirtualizedSubtypes(typeDescriptor));
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
      String simpleName = typeDescriptor.getSimpleSourceName();
      nameComponents.set(
          nameComponents.size() - 1, simpleName.substring(simpleName.indexOf('_') + 1));
      String prefix = simpleName.substring(0, simpleName.indexOf('_') + 1);

      return prefix + Joiner.on('_').join(nameComponents);
    }

    if (typeDescriptor.isWildCardOrCapture()) {
      return "?";
    }

    return environment.aliasForType(typeDescriptor);
  }

  private static boolean needsJsDocNameMapping(TypeDescriptor typeDescriptor) {
    return jsDocNamesByDevirtualizedTypeDescriptor.get().containsKey(typeDescriptor);
  }

  private static boolean hasDevirtualizedSubtype(TypeDescriptor rawTypeDescriptor) {
    return devirtualizedTypeDescriptorsBySuperTypeDescriptor.get().containsKey(rawTypeDescriptor);
  }

  private static String mapDevirtualizedJsDocName(TypeDescriptor typeDescriptor) {
    return jsDocNamesByDevirtualizedTypeDescriptor.get().get(typeDescriptor);
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
   * Assume {@code setTypeDescriptor} is extended or implemented by devirtualized types, it returns
   * the JsDoc name of the union of all its inheriting/implementing types.
   */
  private static String getJsDocNameAsUnionOfDevirtualizedSubtypes(TypeDescriptor typeDescriptor) {
    checkArgument(hasDevirtualizedSubtype(typeDescriptor));
    return getDevirtualziedSubtypes(typeDescriptor)
        .stream()
        .map(
            unboxedTypeDescriptor -> {
              checkArgument(needsJsDocNameMapping(unboxedTypeDescriptor));
              return mapDevirtualizedJsDocName(unboxedTypeDescriptor);
            })
        .collect(joining("|"));
  }

  private static Collection<TypeDescriptor> getDevirtualziedSubtypes(
      TypeDescriptor typeDescriptor) {
    return devirtualizedTypeDescriptorsBySuperTypeDescriptor.get().get(typeDescriptor);
  }

  public static String getJsDocNameForJsFunction(
      TypeDescriptor typeDescriptor, GenerationEnvironment environment) {
    checkState(
        typeDescriptor.isJsFunctionImplementation() || typeDescriptor.isJsFunctionInterface(),
        "'%s' is not a JsFunction type.",
        typeDescriptor.getQualifiedBinaryName());

    MethodDescriptor jsFunctionMethodDescriptor =
        checkNotNull(typeDescriptor.getConcreteJsFunctionMethodDescriptor());

    String parameterString =
        jsFunctionMethodDescriptor
            .getParameterDescriptors()
            .stream()
            .map(parameterDescriptor -> getParameterJsDocString(environment, parameterDescriptor))
            .collect(joining(", "));

    return String.format(
        "function(%s):%s",
        parameterString,
        getJsDocName(jsFunctionMethodDescriptor.getReturnTypeDescriptor(), environment));
  }

  private static String getParameterJsDocString(
      GenerationEnvironment environment, ParameterDescriptor parameterDescriptor) {
    if (parameterDescriptor.isVarargs()) {
      return J2clUtils.format(
          "...%s",
          JsDocNameUtils.getJsDocName(
              parameterDescriptor.getTypeDescriptor().getComponentTypeDescriptor(), environment));
    }
    if (parameterDescriptor.isJsOptional()) {
      return J2clUtils.format(
          "%s=", JsDocNameUtils.getJsDocName(parameterDescriptor.getTypeDescriptor(), environment));
    }
    return J2clUtils.format(
        "%s", JsDocNameUtils.getJsDocName(parameterDescriptor.getTypeDescriptor(), environment));
  }
}

