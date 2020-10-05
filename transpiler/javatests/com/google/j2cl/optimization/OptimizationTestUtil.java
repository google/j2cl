/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.optimization;

import static org.junit.Assert.assertTrue;

/**
 * A utility class for helping to write optimizations tests.
 */
public final class OptimizationTestUtil {

  public static void assertFunctionMatches(Object function, String pattern) {
    String content = getFunctionContent(function.toString());
    String regex = createRegex(pattern);
    assertTrue(
        "content: \"" + content + "\" does not match pattern: " + regex, content.matches(regex));
  }

  private static String getFunctionContent(String functionDef) {
    String stripped = functionDef.replaceAll("\\s", "");
    stripped = stripped.replace('"', '\'');
    return stripped;
  }

  private static String createRegex(String pattern) {
    pattern = pattern.replace(" ", "");
    for (char toBeEscaped : ".[](){}+=?".toCharArray()) {
      pattern = pattern.replace("" + toBeEscaped, "\\" + toBeEscaped);
    }
    pattern = "function[\\w$_]*\\(.*\\){" + pattern + "}";
    pattern = pattern.replace("<obf>", "[\\w$_]+");
    return pattern;
  }

  private OptimizationTestUtil() {
    // Hides the constructor of utility class.
  }
}
