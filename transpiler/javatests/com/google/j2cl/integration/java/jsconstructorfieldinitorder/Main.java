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
package jsconstructorfieldinitorder;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;

import jsinterop.annotations.JsConstructor;

public class Main {

  static class Foo extends Bar {
    private String foo;
    private int bar;
    private int zoo = 5;

    @JsConstructor
    public Foo() {
      super();
    }

    @Override
    protected void initialize() {
      assertEquals(null, this.foo);
      // Fields are not initialized via JsConstructor yet so following doesn't hold (b/27484158).
      // assertEquals(0, this.bar);
      // assertEquals(0, this.zoo);
      foo = ""; // Note: "" chosen to detect potential 'falsey' mistake.
      bar = 42;
      zoo = 1000;
    }
  }

  static class Bar {

    @JsConstructor
    public Bar() {
      initialize();
    }

    protected void initialize() {}
  }

  public static void main(String... args) {
    assertEquals("", new Foo().foo);
    // Fields are initialized late via JsConstructor and the value is overridden (b/27484158).
    // assertEquals(42, new Foo().bar);
    assertEquals(5, new Foo().zoo);
  }
}
