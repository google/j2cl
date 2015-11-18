package com.google.j2cl.transpiler.readable.jsproperties;

public class Main {
  public void testNativeJsProperty() {
    new NativeFoo().getA();
    NativeFoo.getB();
  }

  public void testStaticJsProperty() {
    Foo.getA();
    Foo.setA(10);
    Foo.getB();
    Foo.setB(10);
  }

  public void testInstanceJsProperty() {
    Bar bar = new Bar();
    bar.getA();
    bar.setA(10);
    bar.getB();
    bar.setB(10);
  }
}
