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

import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;
import static com.google.j2cl.transpiler.utils.Asserts.fail;

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
      fail("An expected failure did not occur.");
    } catch (ClassCastException e) {
      assertTrue(
          "Got unexpected message " + e.getMessage(),
          e.getMessage()
              .equals(
                  "com.google.j2cl.transpiler.integration.classcastexception.Main$Foo"
                      + " cannot be cast to"
                      + " com.google.j2cl.transpiler.integration.classcastexception.Main$Bar"));
    }

    // Failed cast to array type.
    try {
      Bar[] bars = (Bar[]) object;
      fail("An expected failure did not occur.");
    } catch (ClassCastException e) {
      // expected
      assertTrue(
          "Got unexpected message " + e.getMessage(),
          e.getMessage()
              .equals(
                  "com.google.j2cl.transpiler.integration.classcastexception.Main$Foo"
                      + " cannot be cast to"
                      + " [Lcom.google.j2cl.transpiler.integration.classcastexception.Main$Bar;"));
    }

    // Failed cast to Java String.
    try {
      String string = (String) object;
      fail("An expected failure did not occur.");
    } catch (ClassCastException e) {
      // expected
      assertTrue(
          "Got unexpected message " + e.getMessage(),
          e.getMessage()
              .equals(
                  "com.google.j2cl.transpiler.integration.classcastexception.Main$Foo"
                      + " cannot be cast to java.lang.String"));
    }

    // Failed cast to Java Void.
    try {
      Void aVoid = (Void) object;
      assertTrue(false);
    } catch (ClassCastException e) {
      // expected
      assertTrue(
          "Got unexpected message " + e.getMessage(),
          e.getMessage()
              .equals(
                  "com.google.j2cl.transpiler.integration.classcastexception.Main$Foo"
                      + " cannot be cast to java.lang.Void"));
    }

    // Failed cast to native extern String.
    try {
      Baz baz = (Baz) object;
      fail("An expected failure did not occur.");
    } catch (ClassCastException e) {
      // expected
      assertTrue(
          "Got unexpected message " + e.getMessage(),
          e.getMessage()
              .equals(
                  "com.google.j2cl.transpiler.integration.classcastexception.Main$Foo"
                      + " cannot be cast to String"));
    }

    // Failed cast to native JsFunction.
    try {
      Qux qux = (Qux) object;
      fail("An expected failure did not occur.");
    } catch (ClassCastException e) {
      // expected
      assertTrue(
          "Got unexpected message " + e.getMessage(),
          e.getMessage()
              .equals(
                  "com.google.j2cl.transpiler.integration.classcastexception.Main$Foo"
                      + " cannot be cast to <native function>"));
    }
  }
}
