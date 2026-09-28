/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package jsmethod;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {
  public static void main(String... args) {
    testJsMethodWithDifferentVisiblities();
    testInheritName();
  }

  static class NonPublicJsMethodClass {
    @JsMethod
    private String foo() {
      return "foo";
    }

    @JsMethod
    String bar() {
      return "bar";
    }
  }

  private static void testJsMethodWithDifferentVisiblities() {
    NonPublicJsMethodClass instance = new NonPublicJsMethodClass();
    assertEquals("foo", instance.foo());
    assertEquals("bar", instance.bar());
    assertEquals("foo", callFoo(instance, null));
    assertEquals("bar", callBar(instance, null));
  }

  @JsMethod(namespace = "jsmethod.JsMethodTestHelper")
  public static native Object callFoo(Object obj, Object param);

  @JsMethod(namespace = "jsmethod.JsMethodTestHelper")
  public static native Object callBar(Object obj, Object param);

  static class ClassWithJsMethod {
    @JsMethod(name = "name")
    public String className() {
      return ClassWithJsMethod.class.getName();
    }
  }

  static class ClassWithJsMethodInheritingName extends ClassWithJsMethod {
    @JsMethod
    public String className() {
      return ClassWithJsMethodInheritingName.class.getName();
    }
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  private interface HasName {
    String name();
  }

  private static String callName(Object o) {
    return ((HasName) o).name();
  }

  private static void testInheritName() {
    ClassWithJsMethod object = new ClassWithJsMethod();
    assertEquals(ClassWithJsMethod.class.getName(), object.className());
    assertEquals(ClassWithJsMethod.class.getName(), callName(object));

    object = new ClassWithJsMethodInheritingName();
    assertEquals(ClassWithJsMethodInheritingName.class.getName(), object.className());
    assertEquals(ClassWithJsMethodInheritingName.class.getName(), callName(object));
  }
}
