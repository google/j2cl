package com.google.j2cl.transpiler.integration.overloadedmethods;

/**
 * Test method overloading
 */
public class Main {
  public static class Parent {}

  public static class Child extends Parent {}

  public String foo(int a) {
    return "signature int";
  }

  public String foo(double a) {
    return "signature double";
  }

  public String foo(int a, double b) {
    return "signature int double";
  }

  public String foo(double a, int b) {
    return "signature double int";
  }

  public String foo(int a, int b) {
    return "signature int int";
  }

  public String foo(int a, double b, double c) {
    return "signature int double double";
  }

  public String foo(Child a) {
    return "signature child";
  }

  public String foo(Parent a) {
    return "signature parent";
  }

  public String foo(Object a) {
    return "signature object";
  }

  public String foo(Parent a, Parent b) {
    return "signature parent parent";
  }

  public String foo(Object a, Object b) {
    return "signature object object";
  }

  public static void main(String... args) {
    Main m = new Main();

    assert m.foo(1).equals("signature int");
    assert m.foo(1.0).equals("signature double");
    assert m.foo(1, 1.0).equals("signature int double");
    assert m.foo(1.0, 1).equals("signature double int");
    assert m.foo(1, 1).equals("signature int int");
    assert m.foo(1, 1, 1).equals("signature int double double");

    Parent parent = new Parent();
    Child child = new Child();

    Object objectParent = new Parent();
    Object objectChild = new Child();

    assert m.foo(child).equals("signature child");
    assert m.foo(parent).equals("signature parent");
    assert m.foo(objectChild).equals("signature object");
    assert m.foo(objectParent).equals("signature object");
    assert m.foo(child, parent).equals("signature parent parent");
    assert m.foo(objectChild, objectParent).equals("signature object object");
  }
}
