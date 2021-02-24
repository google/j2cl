/*
 * Copyright 2021 Google Inc.
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
package javaemul.internal;

/** Array helper class used by wasm backend. */
public class WasmArrayHelper {

  public static Object[] createArray(int[] dimensionLengths, int leafType) {
    return (Object[]) createRecursiveArray(dimensionLengths, 0, leafType);
  }

  private static Object createRecursiveArray(int[] dimensions, int dimensionIndex, int leafType) {
    int length = dimensions[dimensionIndex];
    if (length == -1) {
      // We reach a sub array that doesn't have a dimension (e.g. 3rd dimension in int[3][2][][])
      return null;
    }

    if (dimensionIndex == dimensions.length - 1) {
      // We have reached the leaf dimension.
      return createLeafArray(length, leafType);
    } else {
      Object[] array = new Object[length];
      for (int i = 0; i < length; i++) {
        array[i] = createRecursiveArray(dimensions, dimensionIndex + 1, leafType);
      }
      return array;
    }
  }

  private static Object createLeafArray(int length, int leafType) {
    switch (leafType) {
      case 1:
        return new boolean[length];
      case 2:
        return new byte[length];
      case 3:
        return new short[length];
      case 4:
        return new char[length];
      case 5:
        return new int[length];
      case 6:
        return new long[length];
      case 7:
        return new float[length];
      case 8:
        return new double[length];
      default:
        return new Object[length];
    }
  }
}
