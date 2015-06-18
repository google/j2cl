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
  public static String getMangledName(TypeReference typeReference) {
    //TODO(rluble): Stub implementation.
    if (typeReference.isArray()) {
      return Strings.repeat("arrayOf_", typeReference.getDimensions())
          + getMangledName(typeReference.getLeafType());
    }
    return typeReference.getSourceName().replace('.', '_');
  }

  /**
   * Returns the mangled name of a method.
   */
  public static String getMangledName(MethodReference methodReference) {
    String suffix;
    switch (methodReference.getVisibility()) {
      case PRIVATE:
        // To ensure that private methods never override each other.
        suffix = "_$p_" + getMangledName(methodReference.getEnclosingClassReference());
        break;
      case PACKAGE_PRIVATE:
        // To ensure that package private methods only override one another when
        // they are in the same package.
        suffix =
            "_$pp_"
                + methodReference.getEnclosingClassReference().getPackageName().replace('.', '_');
        break;
      default:
        suffix = "";
        break;
    }
    String parameterSignature = getMangledParameterSignature(methodReference);
    return String.format("m_%s%s%s", methodReference.getMethodName(), parameterSignature, suffix);
  }

  public static String getMangledParameterSignature(MethodReference methodReference) {
    if (methodReference.getParameterTypeReferences().isEmpty()) {
      return "";
    }
    return "__" + Joiner.on("_").join(getMangledParameterTypes(methodReference));
  }
  /**
   * Returns the list of mangled name of parameters' types.
   */
  private static List<String> getMangledParameterTypes(MethodReference methodReference) {
    return Lists.transform(
        methodReference.getParameterTypeReferences(),
        new Function<TypeReference, String>() {
          @Override
          public String apply(TypeReference parameterType) {
            return getMangledName(parameterType);
          }
        });
  }
}
