/*
 * Copyright 2024 Google Inc.
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
package j2kt;

import javaemul.lang.J2ktMonitor;

public class LambdaReturnFromBlock {

  public void testDoWhile() {
    Function<Void> fn =
        () -> {
          do {
            return null;
          } while (false);
        };
  }

  private static final J2ktMonitor lock = new J2ktMonitor();

  public void testSynchronized() {
    Function<Void> fn =
        () -> {
          synchronized (lock) {
            return null;
          }
        };
  }

  int x = 1;

  public void testSwitchInLambda() {
    Function<Void> fn =
        () -> {
          switch (x) {
            case 1:
              return null;
            default:
              return null;
          }
        };
  }

  interface Function<O> {
    O apply();
  }
}
