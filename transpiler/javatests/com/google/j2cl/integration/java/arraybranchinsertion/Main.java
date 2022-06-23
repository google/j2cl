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
package arraybranchinsertion;

import static com.google.j2cl.integration.testing.Asserts.assertThrowsArrayStoreException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  public static void main(String... args) {
    testFullArray();
    testPartialArray();
  }

  private static void testFullArray() {
    Object[][] array2d = new HasName[2][2];
    assertTrue(array2d[0].length == 2);
    assertTrue(array2d.length == 2);

    // You can swap out an entire array in an array slot.
    array2d[0] = new HasName[2];
    assertTrue(array2d[0].length == 2);

    // When inserting an array into an array slot you *can* change the length.
    array2d[0] = new HasName[4];
    assertTrue(array2d[0].length == 4);

    // When inserting an array into an array slot you can tighten the leaf type as a class or
    // interface.
    array2d[0] = new Person[2];
    assertTrue(array2d[0].length == 2);
    array2d[0] = new HasFullName[2];
    assertTrue(array2d[0].length == 2);

    // When inserting an array into an array slot you can NOT broaden the leaf type.
    assertThrowsArrayStoreException(() -> array2d[0] = new Object[2]);

    // You can NOT put an object in a slot that expects an array.
    assertThrowsArrayStoreException(() -> ((Object[]) array2d)[0] = new Object());

    // When inserting an array into an array slot you can not change the number of dimensions.
    assertThrowsArrayStoreException(() -> array2d[0] = new HasName[2][2]);
  }

  private static void testPartialArray() {
    // You can create a partially initialized array.
    Object[][] partialArray = new Object[1][];
    assertTrue(partialArray.length == 1);

    // You can fill the uninitialized dimensions with the same type.
    partialArray[0] = new Object[100];
    assertTrue(partialArray[0].length == 100);

    // Or with a subtype.
    partialArray[0] = new Person[100];
    assertTrue(partialArray[0].length == 100);
  }
}
