package com.google.j2cl.transpiler.readable.arrayliteral;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class ArrayLiteral {

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
  private static class NativeType {}

  @SuppressWarnings("unused")
  public void main() {
    Object object = new Object();

    int[] ints = new int[] {0, 1, 2};
    Object[][] objects2d = new Object[][] {{object, object}, {object, object}};
    int[][] partial = new int[][] {};
    Object[][] unbalanced = new Object[][] {{object, object}, null};

    NativeType nativeObject = new NativeType();
    NativeType[][] array = new NativeType[][] {new NativeType[] {nativeObject, nativeObject}};
  }
}
