/*
 * Copyright 2021 Google Inc.
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
package com.google.j2cl.integration.dispatch;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;

/** Tests method dispatch. */
public class Main {
  static class A {
    public int m() {
      return 1;
    }
  }

  static class B extends A {
    @Override
    public int m() {
      return 2;
    }
  }

  public static void main(String... args) {
    A a = new A();
    B b = new B();
    assertEquals(1, a.m());
    assertEquals(2, b.m());
    a = b;
    assertEquals(2, b.m());
  }
}
