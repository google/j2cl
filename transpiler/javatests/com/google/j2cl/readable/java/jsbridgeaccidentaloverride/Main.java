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
package jsbridgeaccidentaloverride;

public class Main {
  public void test() {
    MyJsType a = new MyJsType();
    SubJsType b = new SubJsType();
    MyInterface c = new SubJsType();
    OtherInterface d = new SubJsType();
    a.foo(1);
    b.foo(1);
    c.foo(1);
    a.bar(1);
    b.bar(1);
    c.bar(1);
    a.f(1);
    b.f(1);
    d.f(1);
  }
}
