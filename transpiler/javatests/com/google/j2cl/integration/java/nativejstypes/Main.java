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
package nativejstypes;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  public static void testNativeJsTypeWithNamespace() {
    Foo foo = new Foo();
    assertTrue(foo.sum() == 42);
    foo.x = 50;
    foo.y = 5;
    assertTrue(foo.sum() == 55);
  }

  public static void testNativeJsTypeWithoutNamespace() {
    Bar bar = new Bar(6, 7);
    assertTrue(bar.product() == 42);
    bar.x = 50;
    bar.y = 5;
    assertTrue(bar.product() == 250);
    Bar.f = 10;
    assertTrue(Bar.f == 10);
  }

  public static void testGlobalNativeJsType() {
    Number number22 = new Number(2.2);
    Double number10base2 = Number.parseInt("10", 2);
    assertTrue(number22.toFixed().equals(number10base2.toString()));
  }

  public static void main(String... args) {
    testNativeJsTypeWithNamespace();
    testNativeJsTypeWithoutNamespace();
    testGlobalNativeJsType();
  }
}
