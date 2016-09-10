package com.google.j2cl.transpiler.readable.arraysimple;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class ArraySimple {

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
  private static class NativeType {}

  public void main() {
    // Creation
    Object[] objects = new Object[100];
    Object[][] objects2d = new Object[5][10];
    Object[][] objectsPartial = new Object[20][];

    NativeType[] nativeObjects = new NativeType[100];
    NativeType[][] nativeObjects2d = new NativeType[5][10];
    NativeType[][] nativeObjectsPartial = new NativeType[20][];

    // Access
    Object object = objects[0];
    object = objects2d[0][1];

    // Assignment
    objects[0] = null;
    objects2d[0][1] = null;

    // There's no compound assignment for Object types.
  }
}
