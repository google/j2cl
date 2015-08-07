package com.google.j2cl.transpiler.readable.accidentaloverride;

class Parent<T extends Error> {
  @SuppressWarnings("unused")
  public void foo(T e) {}
}

interface SuperInterface<T> {
  void foo(T t);
}

/**
 * Class that subclasses a parameterized type with argument that is not the type bound of the
 * type variable declared in super class.
 */
class AnotherAccidentalOverride extends Parent<AssertionError>
    implements SuperInterface<AssertionError> {
  // there should be a bridge method foo__Object for SuperInterface.foo(T), and the bridge method
  // delegates to Parent.foo__Error().
}

/**
 * Class that subclasses a parameterized type with argument that is the bound of the type variable
 * declared in super class.
 */
public class AccidentalOverride extends Parent<Error> implements SuperInterface<Error> {
  // there should be a bridge method foo__Object for SuperInterface.foo(T), and the bridge
  // method delegates to Parent.foo__Error().
}
