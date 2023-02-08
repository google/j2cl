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
package bridgemethods;

import jsinterop.annotations.JsMethod;

class BridgeMethodDefault {
  interface I<T> {
    void m(T t);
  }

  interface II extends I<String> {
    @Override
    default void m(String s) {}
  }

  static class A implements II {}

  interface JJ extends I<Object> {
    @JsMethod
    @Override
    default void m(Object o) {}
  }

  static class B implements JJ {}

  static class C extends B {
    @Override
    public void m(Object o) {}
  }

  {
    JJ jj = new C();
    jj.m(new Object());
    I<Object> i = jj;
    i.m(new Object());
  }
}
