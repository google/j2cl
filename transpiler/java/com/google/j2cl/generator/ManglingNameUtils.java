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
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.FieldReference;
import com.google.j2cl.ast.MethodReference;
import com.google.j2cl.ast.TypeReference;

import java.util.List;

/**
 * Utility functions for mangling names.
 */
public class ManglingNameUtils {
  /**
   * Returns the mangled name of a type.
   */
  public static String getMangledName(TypeReference typeRef) {
    //TODO(rluble): Stub implementation.
    if (typeRef.isArray()) {
      return Strings.repeat("arrayOf_", typeRef.getDimensions())
          + getMangledName(typeRef.getLeafTypeRef());
    }
    return typeRef.getSourceName().replace('.', '_');
  }

  /**
   * Returns the mangled name of a method.
   */
  public static String getMangledName(MethodReference methodRef) {
    String suffix;
    switch (methodRef.getVisibility()) {
      case PRIVATE:
        // To ensure that private methods never override each other.
        suffix = "_$p_" + getMangledName(methodRef.getEnclosingClassRef());
        break;
      case PACKAGE_PRIVATE:
        // To ensure that package private methods only override one another when
        // they are in the same package.
        suffix = "_$pp_" + methodRef.getEnclosingClassRef().getPackageName().replace('.', '_');
        break;
      default:
        suffix = "";
        break;
    }
    String parameterSignature = getMangledParameterSignature(methodRef);
    return String.format("m_%s%s%s", methodRef.getMethodName(), parameterSignature, suffix);
  }

  /**
   * Returns the mangled name of a field.
   */
  public static String getMangledName(FieldReference fieldRef) {
    return getMangledName(fieldRef, false);
  }

  /**
   * Returns the mangled name of a field.
   */
  public static String getMangledName(FieldReference fieldRef, boolean fromClinit) {
    String name = fieldRef.getFieldName();
    String typeMangledName = getMangledName(fieldRef.getEnclosingClassReference());
    String privateSuffix = fieldRef.getVisibility().isPrivate() ? "_" : "";
    String prefix = fromClinit ? "$" : "";
    return String.format("%sf_%s__%s%s", prefix, name, typeMangledName, privateSuffix);
  }

  public static String getMangledParameterSignature(MethodReference methodRef) {
    if (methodRef.getParameterTypeRefs().isEmpty()) {
      return "";
    }
    return "__" + Joiner.on("_").join(getMangledParameterTypes(methodRef));
  }
  /**
   * Returns the list of mangled name of parameters' types.
   */
  private static List<String> getMangledParameterTypes(MethodReference methodRef) {
    return Lists.transform(
        methodRef.getParameterTypeRefs(),
        new Function<TypeReference, String>() {
          @Override
          public String apply(TypeReference parameterType) {
            return getMangledName(parameterType);
          }
        });
  }
}
