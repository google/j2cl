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
package devirtualizedsupermethodcall;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test non-constructor super method calls.
 */
public class Main {
  public static void main(String... args) {
    FooCallsSuperObjectMethods fooCallsSuperObjectMethods = new FooCallsSuperObjectMethods();

    assertTrue(fooCallsSuperObjectMethods.equals(fooCallsSuperObjectMethods));
    assertTrue(fooCallsSuperObjectMethods.hashCode() == fooCallsSuperObjectMethods.hashCode());
    assertTrue(fooCallsSuperObjectMethods.toString().equals(fooCallsSuperObjectMethods.toString()));
    // Would verify for getClass() as well but it's final and can't be overridden.
  }
}
