/*
 * Copyright 2016 Google Inc.
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/** Some JavaScript related utility functions. */
public final class JsUtils {
  private static final String VALID_JS_IDENTIFIER_START_CHARS = "a-zA-Z_$";
  private static final String VALID_JS_IDENTIFIER_PART_CHARS = "\\w_$";
  private static final String VALID_JS_NAME_REGEX =
      "[" + VALID_JS_IDENTIFIER_START_CHARS + "][" + VALID_JS_IDENTIFIER_PART_CHARS + "]*";
  private static final String JAVASCRIPT_VALID_QUALIFIED_NAME_REGEX =
      VALID_JS_NAME_REGEX + "(\\." + VALID_JS_NAME_REGEX + ")*";

  /**
   * A JavaScript identifier contains only letters, numbers, _, $ and does not begin with a number.
   * There are actually other valid identifiers, such as ones that contain escaped Unicode
   * characters but we disallow those for the time being.
   */
  public static boolean isValidJsIdentifier(String name) {
    return name.matches(VALID_JS_NAME_REGEX);
  }

  /**
   * Sanitizes a string to be a valid JavaScript identifier name.
   *
   * <p>This method replaces any characters that are not valid with an underscore (`_`).
   */
  public static String sanitizeJsIdentifier(String name) {
    checkNotNull(name);
    checkState(name.length() > 0);

    if (isValidJsIdentifier(name)) {
      return name;
    }

    return name.replaceFirst("^[^" + VALID_JS_IDENTIFIER_START_CHARS + "]", "_")
        .replaceAll("[^" + VALID_JS_IDENTIFIER_PART_CHARS + "]", "_");
  }

  public static boolean isValidJsQualifiedName(String name) {
    return name.matches(JAVASCRIPT_VALID_QUALIFIED_NAME_REGEX);
  }

  public static final String JS_PACKAGE_GLOBAL = "<global>";

  /** Returns whether a type should be considered global based on its namespace. */
  public static boolean isGlobal(String jsNamespace) {
    return JS_PACKAGE_GLOBAL.equals(jsNamespace) || "<window>".equals(jsNamespace);
  }

  private JsUtils() {}
}
