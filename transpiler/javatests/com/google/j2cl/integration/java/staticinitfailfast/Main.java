/*
 * Copyright 2018 Google Inc.
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
package staticinitfailfast;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsMethod;

public class Main {

  // Note that this shouldn't be insantiable from Java.
  static class Foo {}

  @JsMethod
  private static native void createFoo();

  @JsMethod
  private static native void createFooJsChild();

  public static void main(String[] args) {
    try {
      createFoo();
      assertTrue("should fail in uncompiled mode", "true".equals(System.getProperty("COMPILED")));
    } catch (Exception e) {
      assertTrue(
          "should not fail in compiled mode", "false".equals(System.getProperty("COMPILED")));
      assertTrue(e.getMessage().contains("Java class Foo is instantiated but not initialized."));
    }

    try {
      createFooJsChild();
      assertTrue("should fail in uncompiled mode", "true".equals(System.getProperty("COMPILED")));
    } catch (Exception e) {
      assertTrue(
          "should not fail in compiled mode", "false".equals(System.getProperty("COMPILED")));
      assertTrue(
          e.getMessage().contains("Java class Foo is extended by FooChild but not initialized."));
    }
  }
}
