package javaemul.internal;

import jsinterop.annotations.JsType;

/** Defines helper functions used for generating assert statements. */
@JsType(namespace = "vmbootstrap")
class Asserts {
  public static void $assert(boolean condition) {
    if (!condition) {
      throw new AssertionError("Assertion failed.", null);
    }
  }

  public static void $assertWithMessage(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message, null);
    }
  }

  public static boolean $enabled() {
    return !"false".equals(System.getProperty("ASSERTIONS_ENABLED_"));
  }
}
