/*
 * Copyright 2022 Google Inc.
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
package wasmjsinterop;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/** Tests J2WASM jsinterop features. */
public final class Main {
  public static void main(String... args) throws Exception {
    testJsString();
    testGlobalJsType();
    testNonglobalJsType();
  }

  public static void testJsString() {
    assertEquals(null, String.fromJsString(null));

    String empty = "";
    assertEquals(empty, String.fromJsString(empty.toJsString()));

    String foo = "String with special chars like $'%$\"^";
    assertEquals(foo, String.fromJsString(foo.toJsString()));
  }

  private static void testGlobalJsType() {
    RegExp regExp = new RegExp("test", "g");
    assertEquals(true, regExp.test("test"));
    assertEquals(4, regExp.lastIndex);
    assertEquals(4, regExp.getLastIndex());

    regExp.lastIndex = 0;
    assertEquals(0, regExp.lastIndex);
    assertEquals(0, regExp.getLastIndex());

    regExp.setLastIndex(1);
    assertEquals(1, regExp.lastIndex);
    assertEquals(1, regExp.getLastIndex());

    assertEquals(false, regExp.test("rest"));
  }

  private static void testNonglobalJsType() {
    Foo f = new Foo();
    assertEquals(3, f.sum(1, 2));
    assertEquals(6, Foo.mult(2, 3));
  }

  @JsType(isNative = true, name = "RegExp", namespace = JsPackage.GLOBAL)
  public static class RegExp {
    @JsProperty public int lastIndex;

    public RegExp(String pattern, String flags) {}

    public native boolean test(String value);

    // JsProperty methods.
    @JsProperty
    public native int getLastIndex();

    @JsProperty
    public native void setLastIndex(int value);
  }

  @JsType(isNative = true, name = "Foo", namespace = "test")
  public static class Foo {
    public native int sum(int a, int b);

    public static native int mult(int a, int b);
  }
}
