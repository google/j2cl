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
package com.google.j2cl.transpiler.integration.arrayliteral;

public class Main {
  public static void main(String... args) {
    testSimpleArrayLiteral();
    testOneD();
    testTwoD();
    testPartial2D();
    testUnbalanced2D();
    testTerseLiteral();
  }

  public static void testSimpleArrayLiteral() {
    Object[] empty = new Object[] {};
    assert empty.length == 0;
    assert Object[].class == empty.getClass();

    Main[] emptyM = new Main[] {};
    assert emptyM.length == 0;
    assert Main[].class == emptyM.getClass();

    Object[] values = new Object[] {"sam", "bob", "charlie"};
    values[0] = "jimmy";
    // We don't throw exception on out of bounds index.
    values[100] = "ted";
  }

  private static void testOneD() {
    int[] oneD = new int[] {0, 1, 2};
    assert oneD.length == 3;

    // Values read fine.
    assert oneD[0] == 0;
    assert oneD[1] == 1;
    assert oneD[2] == 2;

    // Insertion to legal indexes is fine.
    oneD[0] = 5;
    oneD[1] = 5;
    oneD[2] = 5;

    // We don't throw exception on out of bounds index.
    oneD[3] = 5;
  }

  private static void testPartial2D() {
    Object[][] partial2D = new Object[][] {null};
    assert partial2D.length == 1;

    // Values read fine.
    assert partial2D[0] == null;

    // When trying to insert into the uninitialized section you'll get an NPE.
    try {
      partial2D[0][0] = new Object();
      assert false : "An expected failure did not occur.";
    } catch (NullPointerException e) {
      // expected
    }

    // You can replace it with a fully initialized array with the right dimensions.
    partial2D = new Object[2][2];
    assert partial2D.length == 2;
    assert partial2D[0].length == 2;
  }

  private static void testTwoD() {
    Main main = new Main();
    Object[][] twoD = new Main[][] {{main, main}, {main, main}};
    assert twoD.length == 2;
    assert twoD[0].length == 2;

    // Values read fine.
    assert twoD[0][0] == main;
    assert twoD[0][1] == main;
    assert twoD[1][0] == main;
    assert twoD[1][1] == main;

    // Insertion to legal indexes is fine.
    twoD[0][0] = main;
    twoD[0][1] = main;
    twoD[1][0] = main;
    twoD[1][1] = main;

    // We don't throw exception on out of bounds index.
    twoD[0][2] = main;

    // When inserting a leaf value the type must conform.
    try {
      twoD[0][0] = new Object();
      assert false : "An expected failure did not occur.";
    } catch (ArrayStoreException e) {
      // expected
    }

    // The object-literal partial arrays still know their depth and so for example will reject an
    // attempt to stick an object into a 1-dimensional array slot.
    try {
      ((Object[]) twoD)[1] = new Object();
      assert false : "An expected failure did not occur.";
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  private static void testUnbalanced2D() {
    Object[][] unbalanced2D = new Object[][] {{null, null}, null};
    assert unbalanced2D.length == 2;

    // The first branch is actually fully initialized.
    assert unbalanced2D[0][0] == null;
    assert unbalanced2D[0].length == 2;
    // The second branch less so.
    assert unbalanced2D[1] == null;
  }

  @SuppressWarnings("cast")
  private static void testTerseLiteral() {
    int[] terseLiteral = {0, 1, 2};
    assert terseLiteral.length == 3;
    assert terseLiteral instanceof int[];
  }
}
