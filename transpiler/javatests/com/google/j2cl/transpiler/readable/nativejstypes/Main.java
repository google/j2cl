package com.google.j2cl.transpiler.readable.nativejstypes;

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
    return bar.product();
  }

  public static void testGlobalNativeJsType() {
    Headers header = new Headers();
    header.append("Content-Type", "text/xml");
  }
}
