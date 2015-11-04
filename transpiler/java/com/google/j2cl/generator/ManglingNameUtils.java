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
import com.google.common.collect.Lists;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.TypeDescriptor;

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
    TypeDescriptor erasureTypeDescriptor = typeDescriptor.getRawTypeDescriptor();
    if (erasureTypeDescriptor.isArray()) {
      return Strings.repeat("arrayOf_", erasureTypeDescriptor.getDimensions())
          + getMangledName(erasureTypeDescriptor.getLeafTypeDescriptor());
    }
    return erasureTypeDescriptor.getBinaryName().replace('.', '_');
  }

  /**
   * Returns the mangled name of a method.
   */
  public static String getMangledName(MethodDescriptor methodDescriptor) {
    if (methodDescriptor.isRaw()) {
      return methodDescriptor.getJsMethodName() == null
          ? methodDescriptor.getMethodName()
          : methodDescriptor.getJsMethodName();
    }
    String suffix;
    switch (methodDescriptor.getVisibility()) {
      case PRIVATE:
        // To ensure that private methods never override each other.
        suffix = "_$p_" + getMangledName(methodDescriptor.getEnclosingClassTypeDescriptor());
        break;
      case PACKAGE_PRIVATE:
        // To ensure that package private methods only override one another when
        // they are in the same package.
        suffix =
            "_$pp_"
                + methodDescriptor
                    .getEnclosingClassTypeDescriptor()
                    .getPackageName()
                    .replace('.', '_');
        break;
      default:
        suffix = "";
        break;
    }
    String parameterSignature = getMangledParameterSignature(methodDescriptor);
    return String.format("m_%s%s%s", methodDescriptor.getMethodName(), parameterSignature, suffix);
  }

  /**
   * Returns the mangled name of the constructor factory method $create.
   */
  public static String getConstructorMangledName(MethodDescriptor methodDescriptor) {
    return "$create" + getMangledParameterSignature(methodDescriptor);
  }

  /**
   * Returns the mangled name of $ctor method for a particular constructor.
   */
  public static String getCtorMangledName(MethodDescriptor methodDescriptor) {
    return "$ctor__"
        + getMangledName(methodDescriptor.getEnclosingClassTypeDescriptor())
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
  public static String getMangledName(FieldDescriptor fieldDescriptor, boolean fromClinit) {
    if (fieldDescriptor.isRaw()) {
      return fieldDescriptor.getFieldName();
    }

    Preconditions.checkArgument(!fieldDescriptor.getEnclosingClassTypeDescriptor().isArray());
    String name = fieldDescriptor.getFieldName();
    String typeMangledName = getMangledName(fieldDescriptor.getEnclosingClassTypeDescriptor());
    String privateSuffix = fieldDescriptor.getVisibility().isPrivate() ? "_" : "";
    String prefix = fromClinit ? "$" : "";
    return String.format("%sf_%s__%s%s", prefix, name, typeMangledName, privateSuffix);
  }

  private static String getMangledParameterSignature(MethodDescriptor methodDescriptor) {
    if (methodDescriptor.getParameterTypeDescriptors().isEmpty()) {
      return "";
    }
    return "__" + Joiner.on("__").join(getMangledParameterTypes(methodDescriptor));
  }

  /**
   * Returns the list of mangled name of parameters' types.
   */
  private static List<String> getMangledParameterTypes(MethodDescriptor methodDescriptor) {
    return Lists.transform(
        methodDescriptor.getParameterTypeDescriptors(),
        new Function<TypeDescriptor, String>() {
          @Override
          public String apply(TypeDescriptor parameterTypeDescriptor) {
            return getMangledName(parameterTypeDescriptor);
          }
        });
  }
}
