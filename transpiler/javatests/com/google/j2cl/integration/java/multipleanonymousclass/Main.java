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
package multipleanonymousclass;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test multiple anonymous classes.
 */
interface AnonymousInterface {
  AnonymousInterface getSelf();
}

public class Main {
  public static void main(String[] args) {
    AnonymousInterface intf1 =
        new AnonymousInterface() {
          @Override
          public AnonymousInterface getSelf() {
            return this;
          }
        };
    AnonymousInterface intf2 =
        new AnonymousInterface() {
          @Override
          public AnonymousInterface getSelf() {
            return this;
          }
        };

    assertTrue(intf1 == intf1.getSelf());
    assertTrue(intf2 == intf2.getSelf());
    assertTrue(intf1.getClass() != intf2.getClass());
    assertTrue(
        intf1
            .getClass()
            .getName()
            .equals("multipleanonymousclass.Main$1"));
    assertTrue(
        intf2
            .getClass()
            .getName()
            .equals("multipleanonymousclass.Main$2"));
  }
}
