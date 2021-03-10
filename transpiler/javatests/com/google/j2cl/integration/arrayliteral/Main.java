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
package com.google.j2cl.integration.arrayliteral;

import static com.google.j2cl.integration.testing.Asserts.assertThrows;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  public static void main(String... args) {
    testSimpleArrayLiteral();
    testOneD();
    testTwoD();
    testPartial2D();
    testUnbalanced2D();
    testTerseLiteral();
  }

  private static void testSimpleArrayLiteral() {
    Object[] empty = new Object[] {};
    assertTrue(empty.length == 0);
    assertTrue(Object[].class == empty.getClass());

    Main[] emptyM = new Main[] {};
    assertTrue(emptyM.length == 0);
    assertTrue(Main[].class == emptyM.getClass());

    Object[] values = new Object[] {"sam", "bob", "charlie"};
    values[0] = "jimmy";
  }

  private static void testOneD() {
    int[] oneD = new int[] {0, 1, 2};
    assertTrue(oneD.length == 3);

    // Values read fine.
    assertTrue(oneD[0] == 0);
    assertTrue(oneD[1] == 1);
    assertTrue(oneD[2] == 2);

    // Insertion to legal indexes is fine.
    oneD[0] = 5;
    oneD[1] = 5;
    oneD[2] = 5;
  }

  private static void testPartial2D() {
    Object[][] partial2D = new Object[][] {null};
    assertTrue(partial2D.length == 1);

    // Values read fine.
    assertTrue(partial2D[0] == null);

    // When trying to insert into the uninitialized section you'll get an NPE.
    assertThrowsNullPointerException(() -> partial2D[0][0] = new Object());

    // You can replace it with a fully initialized array with the right dimensions.
    Object[][] full2D = new Object[2][2];
    assertTrue(full2D.length == 2);
    assertTrue(full2D[0].length == 2);
  }

  private static void testTwoD() {
    Main main = new Main();
    Object[][] twoD = new Main[][] {{main, main}, {main, main}};
    assertTrue(twoD.length == 2);
    assertTrue(twoD[0].length == 2);

    // Values read fine.
    assertTrue(twoD[0][0] == main);
    assertTrue(twoD[0][1] == main);
    assertTrue(twoD[1][0] == main);
    assertTrue(twoD[1][1] == main);

    // Insertion to legal indexes is fine.
    twoD[0][0] = main;
    twoD[0][1] = main;
    twoD[1][0] = main;
    twoD[1][1] = main;

    // When inserting a leaf value the type must conform.
    assertThrows(ArrayStoreException.class, () -> twoD[0][0] = new Object());

    // The object-literal partial arrays still know their depth and so for example will reject an
    // attempt to stick an object into a 1-dimensional array slot.
    assertThrows(ArrayStoreException.class, () -> ((Object[]) twoD)[1] = new Object());
  }

  private static void testUnbalanced2D() {
    Object[][] unbalanced2D = new Object[][] {{null, null}, null};
    assertTrue(unbalanced2D.length == 2);

    // The first branch is actually fully initialized.
    assertTrue(unbalanced2D[0][0] == null);
    assertTrue(unbalanced2D[0].length == 2);
    // The second branch less so.
    assertTrue(unbalanced2D[1] == null);
  }

  @SuppressWarnings("cast")
  private static void testTerseLiteral() {
    int[] terseLiteral = {0, 1, 2};
    assertTrue(terseLiteral.length == 3);
    assertTrue(terseLiteral[0] == 0);
    assertTrue(terseLiteral[1] == 1);
    assertTrue(terseLiteral[2] == 2);
  }
}
