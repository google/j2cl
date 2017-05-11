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

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.j2cl.common.J2clUtils;
import java.util.List;

/**
 * Utility functions for mangling names.
 */
public class ManglingNameUtils {
  /**
   * Returns the mangled name of a type.
   */
  public static String getMangledName(TypeDescriptor typeDescriptor) {
    // Method signature should be decided by the erasure type.
    TypeDescriptor rawTypeDescriptor = typeDescriptor.getRawTypeDescriptor();
    if (rawTypeDescriptor.isArray()) {
      return Strings.repeat("arrayOf_", rawTypeDescriptor.getDimensions())
          + getMangledName(rawTypeDescriptor.getLeafTypeDescriptor());
    }
    return rawTypeDescriptor.getQualifiedSourceName().replace('.', '_');
  }

  /**
   * Returns the mangled name of a method.
   */
  public static String getMangledName(MethodDescriptor methodDescriptor) {
    if (methodDescriptor.isConstructor()) {
      return "constructor";
    }
    if (methodDescriptor.isJsPropertyGetter()) {
      return "get" + " " + methodDescriptor.getSimpleJsName();
    }
    if (methodDescriptor.isJsPropertySetter()) {
      return "set" + " " + methodDescriptor.getSimpleJsName();
    }
    if (methodDescriptor.isJsMethod()) {
      return methodDescriptor.getSimpleJsName();
    }
    String suffix;
    switch (methodDescriptor.getVisibility()) {
      case PRIVATE:
        // To ensure that private methods never override each other.
        suffix = "_$p_" + getMangledName(methodDescriptor.getEnclosingTypeDescriptor());
        break;
      case PACKAGE_PRIVATE:
        // To ensure that package private methods only override one another when
        // they are in the same package.
        suffix =
            "_$pp_"
                + methodDescriptor.getEnclosingTypeDescriptor().getPackageName().replace('.', '_');
        break;
      default:
        suffix = "";
        break;
    }
    String parameterSignature = getMangledParameterSignature(methodDescriptor);
    String prefix = "m_";
    if (methodDescriptor.getName().startsWith("$")) {
      // This is an internal method so we render the actual name
      prefix = "";
    }
    // TODO(tdeegan): We can remove this check and just use the regular method naming pattern.
    if (methodDescriptor.getName().startsWith("$ctor")) {
      return methodDescriptor.getName();
    }
    return J2clUtils.format(
        "%s%s%s%s", prefix, methodDescriptor.getName(), parameterSignature, suffix);
  }

  /**
   * Returns the mangled name of the constructor factory method $create.
   */
  public static String getFactoryMethodMangledName(MethodDescriptor methodDescriptor) {
    return "$create" + getMangledParameterSignature(methodDescriptor);
  }

  /**
   * Returns the mangled name of $ctor method for a particular constructor.
   */
  public static String getCtorMangledName(MethodDescriptor methodDescriptor) {
    return "$ctor__"
        + getMangledName(methodDescriptor.getEnclosingTypeDescriptor())
        + getMangledParameterSignature(methodDescriptor);
  }

  /**
   * Returns the mangled name of $init method for a type.
   */
  public static String getInitMangledName(TypeDescriptor typeDescriptor) {
    return "$init__" + getMangledName(typeDescriptor);
  }

  /**
   * Returns the mangled name of a field.
   */
  public static String getMangledName(FieldDescriptor fieldDescriptor) {
    return getMangledName(fieldDescriptor, false);
  }

  /**
   * Returns the mangled name of a field.
   */
  public static String getMangledName(
      FieldDescriptor fieldDescriptor, boolean accessStaticsDirectly) {
    if (fieldDescriptor.isJsProperty() && !accessStaticsDirectly) {
      return fieldDescriptor.getSimpleJsName();
    }

    String prefix = accessStaticsDirectly ? "$" : "";
    checkArgument(!fieldDescriptor.getEnclosingTypeDescriptor().isArray());
    String name = fieldDescriptor.getName();
    String typeMangledName = getMangledName(fieldDescriptor.getEnclosingTypeDescriptor());
    String privateSuffix = fieldDescriptor.getVisibility().isPrivate() ? "_" : "";
    return J2clUtils.format("%sf_%s__%s%s", prefix, name, typeMangledName, privateSuffix);
  }

  private static String getMangledParameterSignature(MethodDescriptor methodDescriptor) {
    return "__" + Joiner.on("__").join(getMangledParameterTypes(methodDescriptor));
  }

  /**
   * Returns the list of mangled name of parameters' types.
   */
  private static List<String> getMangledParameterTypes(MethodDescriptor methodDescriptor) {
    return Lists.transform(
        methodDescriptor.getDeclarationMethodDescriptor().getParameterTypeDescriptors(),
        parameterTypeDescriptor -> getMangledName(parameterTypeDescriptor.getRawTypeDescriptor()));
  }
}
