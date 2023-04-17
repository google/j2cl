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
package jsmethodoverride;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {
  public static void main(String... args) {
    testJsMethodOverride();
    testJsMethodOverrideWithBridges();
  }

  static class A {
    @JsMethod(name = "y")
    public int x() {
      return 1;
    }
  }

  @JsType
  static class B extends A {
    @Override
    public int x() {
      return 2;
    }
  }

  @JsMethod
  public static native int callY(Object o);

  public static void testJsMethodOverride() {
    A a = new A();
    B b = new B();
    assertTrue(1 == callY(a));
    assertTrue(2 == callY(b));
  }

  @JsType
  static class Parent<T> {
    public String withoutOverrideRenamed(T t) {
      return "Parent.notRenamed";
    }

    public String withOverrideRenamed(T t) {
      return "Parent.toBeRenamed";
    }
  }

  @JsType
  static class Child extends Parent<String> {
    public String withoutOverrideRenamed(String s) {
      return "Child.notRenamed";
    }

    @JsMethod(name = "withNewName")
    public String withOverrideRenamed(String s) {
      return "Child.renamed";
    }
  }

  @JsType(isNative = true, name = "?", namespace = JsPackage.GLOBAL)
  interface AllMethods {
    String withoutOverrideRenamed(Object o);

    String withOverrideRenamed(Object o);

    String withNewName(Object o);
  }

  public static void testJsMethodOverrideWithBridges() {
    Parent<?> p = new Parent<Object>();
    assertEquals("Parent.notRenamed", ((Parent<Object>) p).withoutOverrideRenamed(new Object()));
    assertEquals("Parent.toBeRenamed", ((Parent<Object>) p).withOverrideRenamed(new Object()));
    p = new Child();
    assertEquals("Child.notRenamed", ((Parent<String>) p).withoutOverrideRenamed(""));
    assertEquals("Child.renamed", ((Parent<String>) p).withOverrideRenamed(""));

    Child c = new Child();
    assertEquals("Child.notRenamed", c.withoutOverrideRenamed(""));
    assertEquals("Child.renamed", c.withOverrideRenamed(""));

    AllMethods am = (AllMethods) c;
    assertEquals("Child.notRenamed", am.withoutOverrideRenamed(""));
    // Call through the bridged method
    assertEquals("Child.renamed", am.withOverrideRenamed(""));
    // Call directly through the new name
    assertEquals("Child.renamed", am.withNewName(""));
  }
}
