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
package arrayoutofbounds;

public class Main {
  public static void main(String... args) {
    testArrayLiteral();
    testArray();
  }

  private static void testArrayLiteral() {
    Object[] empty = new Object[] {};
    // We don't throw exception on out of bounds index.
    empty[100] = "ted";

    int[] oneD = new int[] {0, 1, 2};
    // We don't throw exception on out of bounds index.
    oneD[3] = 5;

    int[][] twoD = new int[][] {{0, 1}, {2, 3}};
    // We don't throw exception on out of bounds index.
    twoD[0][2] = 4;
  }

  private static void testArray() {
    Object[] empty = new Object[0];
    // We don't throw exception on out of bounds index.
    empty[100] = "ted";

    int[] oneD = new int[3];
    // We don't throw exception on out of bounds index.
    oneD[3] = 5;

    int[][] twoD = new int[2][2];
    // We don't throw exception on out of bounds index.
    twoD[0][2] = 4;
  }
}
