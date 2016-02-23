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
package com.google.j2cl.ast;

import java.util.regex.Pattern;

/**
 * Some JavaScript related utility functions.
 */
public class JsUtils {
  private static final String VALID_JS_NAME_REGEX = "[a-zA-Z_$][\\w_$]*";
  private static final Pattern JAVASCRIPT_VALID_QUALIFIED_NAME_PATTERN =
      Pattern.compile(VALID_JS_NAME_REGEX + "(\\." + VALID_JS_NAME_REGEX + ")*");
  private static final Pattern JAVASCRIPT_VALID_IDENTIFIER_PATTERN =
      Pattern.compile(VALID_JS_NAME_REGEX);

  /**
   * A JavaScript identifier contains only letters, numbers, _, $ and does not begin with a number.
   * There are actually other valid identifiers, such as ones that contain escaped Unicode
   * characters but we disallow those for the time being.
   */
  public static boolean isValidJsIdentifier(String name) {
    return JAVASCRIPT_VALID_IDENTIFIER_PATTERN.matcher(name).matches();
  }

  public static boolean isValidJsQualifiedName(String name) {
    return JAVASCRIPT_VALID_QUALIFIED_NAME_PATTERN.matcher(name).matches();
  }
}
