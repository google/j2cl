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
package synchronizedstatement;

import javaemul.internal.annotations.KtDisabled;

public class SynchronizedStatement {
  private int a;
  private int b;

  public void main() {
    synchronized (this) {
      a++;
      b--;
    }
  }

  public int testReturn() {
    synchronized (this) {
      if (a < 10) {
        return a++;
      }
    }
    return b--;
  }

  // It should be possible since Kotlin 2.1: https://youtrack.jetbrains.com/issue/KT-1436
  @KtDisabled
  public synchronized void testBreakAndContinue() {
    while (true) {
      synchronized (this) {
        if (a < 10) {
          a++;
          continue;
        }
        break;
      }
    }
  }

  public int testInitialization() {
    int a;
    synchronized (this) {
      a = 0;
    }
    // variable `a` should be initialized at this point
    return a;
  }

  public void testIfStatementWithNonVoidBodyWithoutElse() {
    synchronized (this) {
      if (a < 10) {
        intMethod();
      } else if (b < 10) {
        intMethod();
      }
    }
  }

  private int intMethod() {
    return 0;
  }

  public static class ExtendsSynchronized extends SynchronizedStatement {
    synchronized void foo() {}
  }
}
