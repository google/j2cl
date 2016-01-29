package com.google.j2cl.transpiler.readable.nativetype;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "Array", namespace = JsPackage.GLOBAL)
public class MyTypedArray<T> {
  private int length;

  @JsOverlay
  public final int size() {
    return length;
  }

  @JsOverlay
  public final T get(int index) {
    return null;
  }

  public int publicField;
  private int privateField;
  int packageField;
  protected int protectedField;

  public native void publicMethod();

  private native void privateMethod();

  native void packageMethod();

  protected native void protectedMethod();

  @JsOverlay
  public final void useFieldsAndMethods() {
    @SuppressWarnings("unused")
    int value = publicField + privateField + packageField + protectedField;
    publicMethod();
    privateMethod();
    packageMethod();
    protectedMethod();
  }
}
