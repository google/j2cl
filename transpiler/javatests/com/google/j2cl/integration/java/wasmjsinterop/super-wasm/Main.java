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
import jsinterop.annotations.JsType;

/** Tests J2WASM jsinterop features. */
public final class Main {
  public static void main(String... args) throws Exception {
    testJsString();
    testJsType();
  }

  public static void testJsString() {
    assertEquals(null, String.fromJsString(null));

    String empty = "";
    assertEquals(empty, String.fromJsString(empty.toJsString()));

    String foo = "String with special chars like $'%$\"^";
    assertEquals(foo, String.fromJsString(foo.toJsString()));
  }

  private static void testJsType() {
    RegExp regExp = new RegExp("test");
    assertEquals(true, regExp.test("test"));
    assertEquals(false, regExp.test("rest"));
  }

  @JsType(isNative = true, name = "RegExp", namespace = JsPackage.GLOBAL)
  public static class RegExp {
    public RegExp(String pattern) {}

    public native boolean test(String value);
  }
}
