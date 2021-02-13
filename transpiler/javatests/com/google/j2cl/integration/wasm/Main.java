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
package com.google.j2cl.integration.wasm;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.fail;

/**
 * Incrementally tests wasm features as they are being added.
 *
 * <p>This test will be removed when all WASM features are implemented and all integration tests are
 * enabled for WASM.
 */
public class Main {

  public static void main(String... args) {
    testDynamicClassMethodDispatch();
    testSwitch();
  }

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

  private static void testDynamicClassMethodDispatch() {
    A a = new A();
    B b = new B();
    assertEquals(1, a.m());
    assertEquals(2, b.m());
    a = b;
    assertEquals(2, b.m());
  }

  private static int next = 0;

  private static int returnsNext() {
    return next++;
  }

  private static void testSwitch() {
    // returnsNext = 0;
    switch (returnsNext()) {
      case 1:
        fail();
      case 0:
        break;
      default:
        fail();
    }

    // returnsNext() = 1
    switch (returnsNext()) {
      case 2:
        fail();
      default:
        break;
      case 0:
        fail();
    }
  }
}
