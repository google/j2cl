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

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/** Tests J2WASM jsinterop features. */
public final class Main {
  public static void main(String... args) throws Exception {
    testJsString();
    testJsType();
    // TODO(b/264466634): After generating imports and enabling integration/jsoverlay tests, this
    // can be removed.
    testJsOverlay();
  }

  public static void testJsString() {
    assertEquals(null, String.fromJsString(null));

    String empty = "";
    assertEquals(empty, String.fromJsString(empty.toJsString()));

    String foo = "String with special chars like $'%$\"^";
    assertEquals(foo, String.fromJsString(foo.toJsString()));
  }

  private static void testJsType() {
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

  // TODO(b/264466634): After generating imports and enabling integration/jsoverlay tests, this
  // can be removed.
  private static void testJsOverlay() {
    // Static overlay field.
    assertEquals(2000, Date.staticOverlayValue.getFullYear());
    Date.staticOverlayValue = new Date(2001, 1);
    assertEquals(2001, Date.staticOverlayValue.getFullYear());

    // Static overlay method.
    Date date2 = Date.createMultiplyByTwo(1011, 2);
    assertEquals(2022, date2.getFullYear());
    assertEquals(4, date2.getMonth());

    // Instance overlay method.
    Date date = new Date(2023, 3);
    assertEquals(2026, date.getYearPlusMonth());
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

  // TODO(b/264466634): After generating imports and enabling integration/jsoverlay tests, this
  // can be removed.
  @JsType(isNative = true, name = "Date", namespace = JsPackage.GLOBAL)
  public static class Date {
    // Static overlay field.
    @JsOverlay public static Date staticOverlayValue = new Date(2000, 1);

    // Static overlay method.
    @JsOverlay
    public static Date createMultiplyByTwo(int year, int month) {
      return new Date(year * 2, month * 2);
    }

    public Date(int year, int month) {}

    public native int getFullYear();

    public native int getMonth();

    // Instance overlay method.
    @JsOverlay
    public int getYearPlusMonth() {
      return getFullYear() + getMonth();
    }
  }

  // TODO(b/264466634): Test when JS imports are generated.
  // @JsType(isNative = true, name = "Foo", namespace = "test")
  // public static class Foo {
  //   public native int sum(int a, int b);
  // }
}
