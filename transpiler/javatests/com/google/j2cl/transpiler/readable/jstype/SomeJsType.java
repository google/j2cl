package com.google.j2cl.transpiler.readable.jstype;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType
public class SomeJsType<T> {
  public int publicField;
  private int privateField;
  int packageField;
  protected int protectedField;

  public void publicMethod() {};

  private void privateMethod() {};

  void packageMethod() {};

  protected void protectedMethod() {};

  public void useFieldsAndMethods() {
    @SuppressWarnings("unused")
    int value = publicField + privateField + packageField + protectedField;
    publicMethod();
    privateMethod();
    packageMethod();
    protectedMethod();
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "*")
  interface Star {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  interface Wildcard {}

  public Wildcard testStarAndWildCard(Star s, Wildcard w) {
    Object object = new Object();

    Star star = (Star) (Object) 3.0;
    return (Wildcard) star;
  }
}
