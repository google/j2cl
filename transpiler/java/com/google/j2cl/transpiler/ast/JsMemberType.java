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
package com.google.j2cl.transpiler.ast;

import java.beans.Introspector;
import javax.annotation.Nullable;

/**
 * Indicates type of a JS member. A method is some flavor of JsMember if it is a JsProperty, or a
 * regular JsMethod.
 */
public enum JsMemberType {
  /** A regular method. */
  NONE,
  /**
   * A directly annotated JsConstructor, (either by JsConstructor annotation on the method, or
   * JsType annotation on the enclosing class).
   */
  CONSTRUCTOR,
  /** A regular Js method. (not a JsProperty method). */
  METHOD,
  /** A JsProperty. */
  PROPERTY,
  /** A JsProperty getter method. Usually in the form of getX()/isX(). */
  GETTER {
    /** Returns the property name from getter method that follows Java Bean naming conventions. */
    @Nullable
    @Override
    public String computeJsName(MemberDescriptor memberDescriptor) {
      String methodName = memberDescriptor.getName();
      if (startsWithCamelCase(methodName, "get")) {
        return Introspector.decapitalize(methodName.substring(3));
      }
      if (startsWithCamelCase(methodName, "is")) {
        return Introspector.decapitalize(methodName.substring(2));
      }
      return null;
    }
  },
  /** A JsProperty setter method. Usually in the form of setX(x). */
  SETTER {
    /** Returns the property name from setter method that follows Java Bean naming conventions. */
    @Nullable
    @Override
    public String computeJsName(MemberDescriptor memberDescriptor) {
      String methodName = memberDescriptor.getName();
      if (startsWithCamelCase(methodName, "set")) {
        return Introspector.decapitalize(methodName.substring(3));
      }
      return null;
    }
  },
  /** An accessor with incorrect naming convention. */
  UNDEFINED_ACCESSOR,
  /** Kotlin fields are not part of the ABI, thus it's invalid for them to have @JsProperty. */
  INVALID_KOTLIN_FIELD_JS_PROPERTY,
  /** Kotlin fields are not part of the ABI, thus it's invalid for them to have @JsIgnore. */
  INVALID_KOTLIN_FIELD_JS_IGNORE;

  public boolean isPropertyAccessor() {
    return this == SETTER || this == GETTER;
  }

  public String computeJsName(MemberDescriptor memberDescriptor) {
    return memberDescriptor.getName();
  }

  private static boolean startsWithCamelCase(String string, String prefix) {
    return string.length() > prefix.length()
        && string.startsWith(prefix)
        && Character.isUpperCase(string.charAt(prefix.length()));
  }
}
