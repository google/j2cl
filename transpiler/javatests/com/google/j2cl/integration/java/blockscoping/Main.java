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
package blockscoping;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  @SuppressWarnings("unused")
  public static void main(String... args) {
    // If 'i' is not block scoped then Closure compile will fail and if the 'i' created in the
    // 'for' header is not available in the 'for' body then runtime will fail.
    for (int i = 11; i < 12; i++) {
      assertTrue(i == 11);
    }

    // If 'i' is not block scoped then Closure compile will fail.
    boolean firstTime = true;
    while (firstTime) {
      int i = 22;
      assertTrue(i == 22);
      firstTime = false;
    }

    // If 'i' is not block scoped then Closure compile will fail.
    if (!firstTime) {
      int i = 33;
      assertTrue(i == 33);
    }

    // If 'i' is not block scoped then Closure compile will fail.
    {
      int i = 44;
      assertTrue(i == 44);
    }

    int x;
    {
      x = 1;
    }
    x = x + 1;
    assertTrue(x == 2);
  }
}
