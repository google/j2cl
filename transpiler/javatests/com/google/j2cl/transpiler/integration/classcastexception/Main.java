package com.google.j2cl.transpiler.integration.classcastexception;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

class Foo {}

class Bar {}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
class Baz {}

@JsFunction
interface Qux {
  String m(String s);
}

/** Test cast to class type. */
public class Main {

  @SuppressWarnings("unused")
  public static void main(String[] args) {
    Object object = new Foo();

    // Failed cast.
    try {
      Bar bar = (Bar) object;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      assert e.getMessage()
          .equals(
              "com.google.j2cl.transpiler.integration.classcastexception.Foo"
                  + " cannot be cast to"
                  + " com.google.j2cl.transpiler.integration.classcastexception.Bar");
    }

    // Failed cast to array type.
    try {
      Bar[] bars = (Bar[]) object;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
      assert e.getMessage()
          .equals(
              "com.google.j2cl.transpiler.integration.classcastexception.Foo"
                  + " cannot be cast to"
                  + " [Lcom.google.j2cl.transpiler.integration.classcastexception.Bar;");
    }

    // Failed cast to Java String.
    try {
      String string = (String) object;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
      assert e.getMessage()
          .equals(
              "com.google.j2cl.transpiler.integration.classcastexception.Foo"
                  + " cannot be cast to java.lang.String");
    }

    // Failed cast to native String.
    try {
      Baz baz = (Baz) object;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
      assert e.getMessage()
          .equals(
              "com.google.j2cl.transpiler.integration.classcastexception.Foo"
                  + " cannot be cast to String");
    }

    // Failed cast to native JsFunction.
    try {
      Qux qux = (Qux) object;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
      assert e.getMessage()
          .equals(
              "com.google.j2cl.transpiler.integration.classcastexception.Foo"
                  + " cannot be cast to Function");
    }
  }
}
