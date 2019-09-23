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
    TypeDescriptor rawTypeDescriptor = typeDescriptor.toRawTypeDescriptor();
    if (rawTypeDescriptor.isArray()) {
      ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) rawTypeDescriptor;
      return Strings.repeat("arrayOf_", arrayTypeDescriptor.getDimensions())
          + getMangledName(arrayTypeDescriptor.getLeafTypeDescriptor());
    }
    if (rawTypeDescriptor.isPrimitive()) {
      return ((PrimitiveTypeDescriptor) rawTypeDescriptor).getSimpleSourceName();
    }
    return ((DeclaredTypeDescriptor) rawTypeDescriptor).getQualifiedSourceName().replace('.', '_');
  }

  /** Returns the mangled name of a member. */
  public static String getMangledName(MemberDescriptor memberDescriptor) {
    if (memberDescriptor instanceof MethodDescriptor) {
      return getMangledName((MethodDescriptor) memberDescriptor);
    }
    return getMangledName((FieldDescriptor) memberDescriptor);
  }

  /** Returns the mangled name of a method. */
  public static String getMangledName(MethodDescriptor methodDescriptor) {
    if (methodDescriptor.isConstructor()) {
      return "constructor";
    }

    if (methodDescriptor.isPropertyGetter()) {
      return "get " + getPropertyMangledName(methodDescriptor);
    }

    if (methodDescriptor.isPropertySetter()) {
      return "set " + getPropertyMangledName(methodDescriptor);
    }

    if (methodDescriptor.isJsMethod()) {
      return methodDescriptor.getSimpleJsName();
    }

    // All special cases have been handled. Go ahead and construct the mangled name for a plain
    // Java method.
    String suffix = "";
    if (!methodDescriptor.isStatic()) {
      // Only use suffixes for instance methods. Static methods are always called through the
      // right constructor, no need to add a suffix to avoid collisions.
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
                  + methodDescriptor
                      .getEnclosingTypeDescriptor()
                      .getTypeDeclaration()
                      .getPackageName()
                      .replace('.', '_');
          break;
        default:
          break;
      }
    }
    String parameterSignature = getMangledParameterSignature(methodDescriptor);
    String prefix = "m_";
    if (methodDescriptor.getName().startsWith("$")) {
      // This is an internal method so we render the actual name
      prefix = "";
    }

    return String.format(
        "%s%s%s%s", prefix, methodDescriptor.getName(), parameterSignature, suffix);
  }

  /**
   * Returns the mangled name of a field.
   */
  public static String getMangledName(FieldDescriptor fieldDescriptor) {
    return getPropertyMangledName(fieldDescriptor);
  }

  /** Returns the mangled name of a property. */
  public static String getPropertyMangledName(MemberDescriptor memberDescriptor) {
    if (memberDescriptor.isJsMember()) {
      return memberDescriptor.getSimpleJsName();
    }

    String prefix = memberDescriptor.getOrigin().getPrefix();

    TypeDescriptor enclosingTypeDescriptor = memberDescriptor.getEnclosingTypeDescriptor();
    checkArgument(!enclosingTypeDescriptor.isArray());
    String name = memberDescriptor.getName();
    String typeMangledName = getMangledName(enclosingTypeDescriptor);
    String privateSuffix = memberDescriptor.getVisibility().isPrivate() ? "_" : "";
    return String.format("%sf_%s__%s%s", prefix, name, typeMangledName, privateSuffix);
  }

  private static String getMangledParameterSignature(MethodDescriptor methodDescriptor) {
    return "__" + Joiner.on("__").join(getMangledParameterTypes(methodDescriptor));
  }

  /**
   * Returns the list of mangled name of parameters' types.
   */
  private static List<String> getMangledParameterTypes(MethodDescriptor methodDescriptor) {
    return Lists.transform(
        methodDescriptor.getDeclarationDescriptor().getParameterTypeDescriptors(),
        parameterTypeDescriptor -> getMangledName(parameterTypeDescriptor.toRawTypeDescriptor()));
  }
}
