/*
 * Copyright 2022 Google Inc.
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
package j2ktjre;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.AssertsBase.assertTrue;

public class LangSystem {

  static void testSystemTime() {
    long millisTimeStart = System.currentTimeMillis();
    long millisTimeEnd = 0;
    for (int i = 0; i < 100000; i++) {
      millisTimeEnd = System.currentTimeMillis();
    }
    assertTrue(millisTimeStart > 0 && millisTimeEnd > millisTimeStart);

    long nanoTimeStart = System.nanoTime();
    long nanoTimeEnd = 0;
    for (int i = 0; i < 100000; i++) {
      nanoTimeEnd = System.nanoTime();
    }
    assertTrue(nanoTimeStart > 0 && nanoTimeEnd > nanoTimeStart);
  }

  static void testArrayCopy() {
    int[] intArraySrc = {1, 2, 3, 4, 5};
    int[] intArrayDst = {0, 0, 0, 0, 0};
    System.arraycopy(intArraySrc, 0, intArrayDst, 1, 2);
    assertEquals(intArrayDst[1], 1);

    String[] strArraySrc = {"1", "2", "3", "4", "5"};
    String[] strArrayDst = {"0", "0", "0", "0", "0"};
    System.arraycopy(strArraySrc, 0, strArrayDst, 1, 2);
    assertEquals(strArrayDst[1], "1");

    ExampleObject[] objArraySrc = {new ExampleObject(1), new ExampleObject(2)};
    ExampleObject[] objArrayDst = new ExampleObject[2];
    System.arraycopy(objArraySrc, 0, objArrayDst, 0, 2);
    assertEquals(1, objArrayDst[0].val);
  }

  static class ExampleObject {
    public int val;

    ExampleObject(int val) {
      this.val = val;
    }
  }

  static void testHashCode() {
    ExampleObject obj = new ExampleObject(1);
    assertEquals(System.identityHashCode(obj), System.identityHashCode(obj));
  }
}
