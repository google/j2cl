package com.google.j2cl.transpiler.integration.devirtualizedsupermethodcall;

/**
 * Test non-constructor super method calls.
 */
public class Main {
  public static void main(String[] args) {
    FooCallsSuperObjectMethods fooCallsSuperObjectMethods = new FooCallsSuperObjectMethods();

    assert fooCallsSuperObjectMethods.equals(fooCallsSuperObjectMethods);
    assert fooCallsSuperObjectMethods.hashCode() == fooCallsSuperObjectMethods.hashCode();
    assert fooCallsSuperObjectMethods.toString() == fooCallsSuperObjectMethods.toString();
    // Would verify for getClass() as well but it's final and can't be overridden.
  }
}
