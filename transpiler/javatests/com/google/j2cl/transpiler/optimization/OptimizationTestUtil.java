package com.google.j2cl.transpiler.optimization;

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
