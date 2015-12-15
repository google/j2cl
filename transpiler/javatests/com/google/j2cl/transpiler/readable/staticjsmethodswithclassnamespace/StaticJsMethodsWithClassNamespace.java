package com.google.j2cl.transpiler.readable.staticjsmethodswithclassnamespace;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

@JsType(namespace = "woo")
public class StaticJsMethodsWithClassNamespace {
  @JsMethod(name = "replacedName")
  public static void originalName() {}

  public void test() {
    StaticJsMethodsWithClassNamespace.originalName();
    originalName();
  }
}
