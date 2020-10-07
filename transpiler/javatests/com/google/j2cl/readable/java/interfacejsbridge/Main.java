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

public class Main {
  public void test() {
    MyJsInterface a = new InterfaceImpl();
    MyInterface b = new InterfaceImpl();
    SubInterface c = new InterfaceImpl();
    InterfaceImpl d = new InterfaceImpl();
    a.foo(1);
    b.foo(1);
    c.foo(1);
    d.foo(1);
  }
}
