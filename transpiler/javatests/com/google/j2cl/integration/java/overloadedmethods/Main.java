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
package overloadedmethods;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test method overloading
 */
public class Main {
  public static class Parent {}

  public static class Child extends Parent {}

  public String foo(int a) {
    return "signature int";
  }

  public String foo(long a) {
    return "signature long";
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

    assertTrue(m.foo(1).equals("signature int"));
    assertTrue(m.foo(1.0).equals("signature double"));
    assertTrue(m.foo(1, 1.0).equals("signature int double"));
    assertTrue(m.foo(1.0, 1).equals("signature double int"));
    assertTrue(m.foo(1, 1).equals("signature int int"));
    assertTrue(m.foo(1, 1, 1).equals("signature int double double"));

    Parent parent = new Parent();
    Child child = new Child();

    Object objectParent = new Parent();
    Object objectChild = new Child();

    assertTrue(m.foo(child).equals("signature child"));
    assertTrue(m.foo(parent).equals("signature parent"));
    assertTrue(m.foo(objectChild).equals("signature object"));
    assertTrue(m.foo(objectParent).equals("signature object"));
    assertTrue(m.foo(child, parent).equals("signature parent parent"));
    assertTrue(m.foo(objectChild, objectParent).equals("signature object object"));

    assertTrue(m.foo(0x80000000).equals("signature int"));
    assertTrue(m.foo(-0x80000000).equals("signature int"));
    assertTrue(m.foo(-(0x80000000)).equals("signature int"));
    assertTrue(m.foo(0x80000000 - 1).equals("signature int"));
    assertTrue(m.foo(0x7fffffff + 1).equals("signature int"));
    assertTrue(m.foo(0x7fffffff * 2).equals("signature int"));
    assertTrue(m.foo(0x7fffffff << 2).equals("signature int"));

    assertTrue(m.foo(0x80000000 - 1 + 1).equals("signature int"));
    assertTrue(m.foo(2147483648L - 1 + 1).equals("signature long"));
  }
}
