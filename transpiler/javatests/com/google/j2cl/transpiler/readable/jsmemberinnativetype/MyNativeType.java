package com.google.j2cl.transpiler.readable.jsmemberinnativetype;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = "test.foo")
public class MyNativeType {
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
