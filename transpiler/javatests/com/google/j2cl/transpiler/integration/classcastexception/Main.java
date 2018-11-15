/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    // Failed cast to native JsFunction.
    try {
      Qux qux = (Qux) object;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
      assert e.getMessage()
              .equals(
                  "com.google.j2cl.transpiler.integration.classcastexception.Main$Foo"
                      + " cannot be cast to <native function>")
          : "Got unexpected message " + e.getMessage();
    }
  }
}
