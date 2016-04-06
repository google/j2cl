package com.google.j2cl.transpiler.readable.jstypeinnerclasswithnamespace;

import jsinterop.annotations.JsType;

public class JsTypeInnerClassWithNamespace {

  @JsType(namespace = "bar.foo.baz")
  static class InnerClass {}
}
