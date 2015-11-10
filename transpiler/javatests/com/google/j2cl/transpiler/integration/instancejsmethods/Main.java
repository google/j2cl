package com.google.j2cl.transpiler.integration.instancejsmethods;

import jsinterop.annotations.JsMethod;

public class Main {
  public static void main(String... args) {
    testCallByConcreteType();
    testCallBySuperParent();
    testCallByJS();
  }

  public static void testCallBySuperParent() {
    SuperParent sp = new SuperParent();
    SuperParent p = new Parent();
    SuperParent c = new Child();
    Parent pp = new Parent();
    Parent cc = new Child();
    MyInterface intf = new Child();

    assert (sp.fun(12, 35) == 158);
    assert (sp.bar(6, 7) == 264);
    assert (p.fun(12, 35) == 47);
    assert (p.bar(6, 7) == 42);
    assert (c.fun(12, 35) == 48);
    assert (c.bar(6, 7) == 43);
    assert (pp.foo(10) == 10);
    assert (cc.foo(10) == 11);
    assert (intf.intfFoo(5) == 5);
  }

  public static void testCallByConcreteType() {
    SuperParent sp = new SuperParent();
    Parent p = new Parent();
    Child c = new Child();

    assert (sp.fun(12, 35) == 158);
    assert (sp.bar(6, 7) == 264);
    assert (p.fun(12, 35) == 47);
    assert (p.bar(6, 7) == 42);
    assert (c.fun(12, 35) == 48);
    assert (c.bar(6, 7) == 43);
    assert (p.foo(10) == 10);
    assert (c.foo(10) == 11);
    assert (c.intfFoo(5) == 5);
  }

  public static void testCallByJS() {
    Parent p = new Parent();
    Child c = new Child();
    assert (callParentFun(p, 12, 35) == 47);
    assert (callParentBar(p, 6, 7) == 42);
    assert (callParentFoo(p, 10) == 10);
    assert (callChildFun(c, 12, 35) == 48);
    assert (callChildBar(c, 6, 7) == 43);
    assert (callChildFoo(c, 10) == 11);
    assert (callChildIntfFoo(c, 5) == 5);
  }

  @JsMethod
  public static native int callParentFun(Parent p, int a, int b);

  @JsMethod
  public static native int callParentBar(Parent p, int a, int b);

  @JsMethod
  public static native int callParentFoo(Parent p, int a);

  @JsMethod
  public static native int callChildFun(Child c, int a, int b);

  @JsMethod
  public static native int callChildBar(Child c, int a, int b);

  @JsMethod
  public static native int callChildFoo(Child c, int a);

  @JsMethod
  public static native int callChildIntfFoo(Child c, int a);
}
