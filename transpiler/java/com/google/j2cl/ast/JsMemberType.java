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

import com.google.j2cl.common.J2clUtils;

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
  /** A JsFunction method. */
  JS_FUNCTION,
  /** A regular Js method. (not a JsProperty method). */
  METHOD,
  /** A JsProperty. */
  PROPERTY,
  /** A JsProperty getter method. Usually in the form of getX()/isX(). */
  GETTER {
    /** Returns the property name from getter method that follows Java Bean naming conversions. */
    @Override
    public String computeJsName(MemberDescriptor memberDescriptor) {
      String methodName = memberDescriptor.getName();
      if (startsWithCamelCase(methodName, "get")) {
        return J2clUtils.decapitalize(methodName.substring(3));
      }
      if (startsWithCamelCase(methodName, "is")) {
        return J2clUtils.decapitalize(methodName.substring(2));
      }
      return null;
    }
  },
  /** A JsProperty setter method. Usually in the form of setX(x). */
  SETTER {
    /** Returns the property name from setter method that follows Java Bean naming conversions. */
    @Override
    public String computeJsName(MemberDescriptor memberDescriptor) {
      String methodName = memberDescriptor.getName();
      if (startsWithCamelCase(methodName, "set")) {
        return J2clUtils.decapitalize(methodName.substring(3));
      }
      return null;
    }
  };

  public String computeJsName(MemberDescriptor memberDescriptor) {
    return memberDescriptor.getName();
  }

  private static boolean startsWithCamelCase(String string, String prefix) {
    return string.length() > prefix.length()
        && string.startsWith(prefix)
        && Character.isUpperCase(string.charAt(prefix.length()));
  }
}
