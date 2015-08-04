package com.google.j2cl.transpiler.integration.subclassgenericclass;

class Parent<T> {
  public T foo(T t) {
    return t;
  }
}

class Child extends Parent<Child> {}

class GenericChild<T> extends Parent<T> {}

public class Main {
  public static void main(String[] args) {
    Child c = new Child();
    Child b = c.foo(c);
    assert b == c;

    GenericChild<Child> gc = new GenericChild<>();
    Child cc = gc.foo(c);
    assert cc == c;
  }
}
