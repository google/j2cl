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
package subclassgenericclass;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

class Parent<T> {
  public T foo(T t) {
    return t;
  }
}

class Child extends Parent<Child> {}

class GenericChild<T> extends Parent<T> {}

public class Main {
  public static void main(String... args) {
    Child c = new Child();
    Child b = c.foo(c);
    assertTrue(b == c);

    GenericChild<Child> gc = new GenericChild<>();
    Child cc = gc.foo(c);
    assertTrue(cc == c);
  }
}
