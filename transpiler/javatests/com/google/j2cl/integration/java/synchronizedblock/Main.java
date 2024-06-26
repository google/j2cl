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
package synchronizedblock;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  private static boolean expressionCalled;
  private static boolean blockExecuted;

  public static void main(String... args) {
    synchronized (getX()) {
      blockExecuted = true;
    }
    assertTrue(expressionCalled);
    assertTrue(blockExecuted);
  }

  private static X getX() {
    expressionCalled = true;
    return new X();
  }

  static class X {
    // Make sure the class gets an implied monitor.
    synchronized void foo() {}
  }
}
