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
package com.google.j2cl.transpiler.integration.nestedanonymousclass;

import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

/**
 * Test nested anonymous class.
 */
interface AnonymousInterface {
  public void foo();
}

public class Main {
  public static void main(String... args) {
    AnonymousInterface intf1 =
        new AnonymousInterface() {
          @Override
          public void foo() {
            AnonymousInterface intf2 =
                new AnonymousInterface() {
                  @Override
                  public void foo() {}
                };
            assertTrue(intf2 instanceof AnonymousInterface);
            assertTrue(
                intf2
                    .getClass()
                    .getName()
                    .equals(
                        "com.google.j2cl.transpiler.integration.nestedanonymousclass.Main$1$1"));
          }
        };

    assertTrue(intf1 instanceof AnonymousInterface);
    assertTrue(
        intf1
            .getClass()
            .getName()
            .equals("com.google.j2cl.transpiler.integration.nestedanonymousclass.Main$1"));
    intf1.foo();
  }
}
