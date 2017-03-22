package com.google.j2cl.transpiler.integration.classcastexception;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/** Test cast to class type. */
public class Main {

  static class Foo {}

  static class Bar {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
  static class Baz {}

  @JsType(isNative = true, namespace = "goog.math", name = "Long")
  static class Zoo {}

  @JsFunction
  interface Qux {
    String m(String s);
  }

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
                  "com.google.j2cl.transpiler.integration.classcastexception.Main$Foo"
                      + " cannot be cast to"
                      + " com.google.j2cl.transpiler.integration.classcastexception.Main$Bar")
          : "Got unexpected message " + e.getMessage();
    }

    // Failed cast to array type.
    try {
      Bar[] bars = (Bar[]) object;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
      assert e.getMessage()
              .equals(
                  "com.google.j2cl.transpiler.integration.classcastexception.Main$Foo"
                      + " cannot be cast to"
                      + " [Lcom.google.j2cl.transpiler.integration.classcastexception.Main$Bar;")
          : "Got unexpected message " + e.getMessage();
    }

    // Failed cast to Java String.
    try {
      String string = (String) object;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
      assert e.getMessage()
              .equals(
                  "com.google.j2cl.transpiler.integration.classcastexception.Main$Foo"
                      + " cannot be cast to java.lang.String")
          : "Got unexpected message " + e.getMessage();
    }

    // Failed cast to Java Void.
    try {
      Void aVoid = (Void) object;
      assert false;
    } catch (ClassCastException e) {
      // expected
      assert e.getMessage()
              .equals(
                  "com.google.j2cl.transpiler.integration.classcastexception.Main$Foo"
                      + " cannot be cast to java.lang.Void")
          : "Got unexpected message " + e.getMessage();
    }

    // Failed cast to native extern String.
    try {
      Baz baz = (Baz) object;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
      assert e.getMessage()
              .equals(
                  "com.google.j2cl.transpiler.integration.classcastexception.Main$Foo"
                      + " cannot be cast to String")
          : "Got unexpected message " + e.getMessage();
    }

    // Failed cast to native non-extern goog.math.Long.
    try {
      Zoo baz = (Zoo) object;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
      assert e.getMessage()
              .equals(
                  "com.google.j2cl.transpiler.integration.classcastexception.Main$Foo"
                      + " cannot be cast to goog.math.Long")
          : "Got unexpected message " + e.getMessage();
    }

    // Failed cast to native JsFunction.
    try {
      Qux qux = (Qux) object;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
      assert e.getMessage()
              .equals(
                  "com.google.j2cl.transpiler.integration.classcastexception.Main$Foo"
                      + " cannot be cast to Function")
          : "Got unexpected message " + e.getMessage();
    }
  }
}
