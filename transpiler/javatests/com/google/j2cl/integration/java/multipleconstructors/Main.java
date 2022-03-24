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
package multipleconstructors;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test multiple constructors.
 */
public class Main {
  private int id;
  private boolean flag;

  public Main(int id) {
    this.id = id;
    this.flag = (id == 0);
  }

  public Main(boolean flag) {
    this.id = -1;
    this.flag = flag;
  }

  public Main(int id, boolean flag) {
    this.id = id;
    this.flag = flag;
  }

  public static void main(String[] args) {
    Main m1 = new Main(1);
    assertTrue(m1.id == 1);
    assertTrue(!m1.flag);

    Main m2 = new Main(true);
    assertTrue(m2.id == -1);
    assertTrue(m2.flag);

    Main m3 = new Main(10, false);
    assertTrue(m3.id == 10);
    assertTrue(!m3.flag);
  }
}
