package com.google.j2cl.transpiler.integration.removeunusedcontructorprops;

import jsinterop.annotations.JsMethod;

/**
 * This is test is intended to fail if compiled with "--remove_unused_constructor_properties=ON".
 */
public class Main {

  private static boolean flag = false;

  static class Inner {

    /**
     * This will ensure that we go through the setter for the static field.
     */
    static final void raiseFlag() {
      flag = true;
    }
  }


  public static void main(String[] args) {
    Inner.raiseFlag();
    assert getFlag() == true;
  }

  /**
   * A native implementation that grab the underlying flag value directly.
   */
  @JsMethod
  static native boolean getFlag();
}
