package com.google.j2cl.transpiler.readable.nativejstypes;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {
  public static int testNativeJsTypeWithNamespace() {
    Foo foo = new Foo();
    foo.x = 50;
    foo.y = 5;
    return foo.sum();
  }

  public static int testNativeJsTypeWithoutNamespace() {
    Bar bar = new Bar(6, 7);
    bar.x = 50;
    bar.y = 5;
    Bar.f = 10;
    return bar.product();
  }

  public static void testInnerNativeJsType() {
    Bar.Inner unusedFoo = new Bar.Inner() {};
    Another.Inner unusedZoo = new Another.Inner() {};
  }

  public static void testGlobalNativeJsType() {
    Headers header = new Headers();
    header.append("Content-Type", "text/xml");
  }

  @SuppressWarnings("unused")
  public static void testNativeTypeClassLiteral() {
    Object o1 = Bar.class;
    o1 = Bar[][].class;
  }

  public static void testNativeTypeObjectMethods() {
    Bar bar = new Bar(6, 7);
    bar.toString();
    bar.hashCode();
    bar.equals(new Object());
  }

  @JsType(isNative = true, name = "?", namespace = JsPackage.GLOBAL)
  interface Wildcard<T> {}

  Wildcard<String> get() {
    return null;
  }
}
