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
package complexcascadingconstructor;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test for cascading constructors with side effect in field initialization.
 */
public class Main {
  public static int counter = 1;
  public int a = incCounter();
  public int b = incCounter();

  private Main(int a, int b) {
    this.a += a;
    this.b += b;
  }

  public Main(int a) {
    this(a, a * 2);
  }

  public static void main(String... args) {
    Main m = new Main(10);
    assertTrue(m.a == 12);
    assertTrue(m.b == 23);
    assertTrue(counter == 3);
  }

  private int incCounter() {
    counter += 1;
    return counter;
  }
}
