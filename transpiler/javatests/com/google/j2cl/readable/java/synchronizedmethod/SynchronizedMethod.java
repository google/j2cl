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
package synchronizedmethod;

public class SynchronizedMethod {
  private int a;
  private int b;

  private static int sa;
  private static int sb;

  public synchronized void testNonStatic() {
    a++;
    b--;
  }

  public static synchronized void testStatic() {
    sa++;
    sb--;
  }

  public synchronized int testReturn(boolean bool) {
    if (bool) {
      return a++;
    }
    return b--;
  }

  public synchronized void testIfStatementWithNonVoidBodyWithoutElse(boolean b1, boolean b2) {
    if (b1) {
      apply("foo");
    } else if (b2) {
      apply("bar");
    }
  }

  private String apply(String string) {
    return string;
  }
}
