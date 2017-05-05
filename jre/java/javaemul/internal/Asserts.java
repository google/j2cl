package javaemul.internal;

import jsinterop.annotations.JsType;

/** Defines helper functions used for generating assert statements. */
@JsType(namespace = "vmbootstrap")
class Asserts {
  public static void $assert(boolean condition) {
    if (!condition) {
      throw new AssertionError();
    }
  }

  public static void $assertWithMessage(boolean condition, Object message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }
}
