package com.google.j2cl.transpiler.integration.nativejstypes;

public class Main {
  public static void testNativeJsTypeWithNamespace() {
    Foo foo = new Foo();
    assert foo.sum() == 42;
    foo.x = 50;
    foo.y = 5;
    assert foo.sum() == 55;
  }

  public static void testNativeJsTypeWithoutNamespace() {
    Bar bar = new Bar(6, 7);
    assert bar.product() == 42;
    bar.x = 50;
    bar.y = 5;
    assert bar.product() == 250;
  }

  public static void testGlobalNativeJsType() {
    Headers header = new Headers();
    header.append("Content-Type", "text/xml");
    assert header.get("Content-Type").equals("text/xml");
  }

  public static void main(String... args) {
    testNativeJsTypeWithNamespace();
    testNativeJsTypeWithoutNamespace();
    testGlobalNativeJsType();
  }
}
