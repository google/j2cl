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
package interfacejsbridge;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  public static void main(String... args) {
    MyJsInterface a = new InterfaceImpl();
    MyInterface b = new InterfaceImpl();
    SubInterface c = new InterfaceImpl();
    InterfaceImpl d = new InterfaceImpl();
    assertTrue((a.foo(1) == 1));
    assertTrue((b.foo(2) == 2));
    assertTrue((c.foo(3) == 3));
    assertTrue((d.foo(4) == 4));
  }
}
