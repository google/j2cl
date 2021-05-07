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

import java.io.Serializable;

/** A common base abstraction for the arrays in Wasm. */
abstract class WasmArray implements Serializable, Cloneable {

  int length;

  protected WasmArray(int length) {
    this.length = length;
  }

  public final int getLength() {
    return length;
  }

  // TODO(b/185437698): convert following methods to abstract when all subtypes implement them.

  /** Create a new array of same type with given length. */
  Object newArray(int length) {
    throw new UnsupportedOperationException();
  }

  /** Change the length of the array. May resize storage if necessary. */
  void setLength(int length) {
    throw new UnsupportedOperationException();
  }

  void copyFrom(int offset, WasmArray values, int valueOffset, int len) {
    throw new UnsupportedOperationException();
  }

  public static Object[] createMultiDimensional(int[] dimensionLengths, int leafType) {
    return (Object[]) createRecursively(dimensionLengths, 0, leafType);
  }

  private static Object createRecursively(int[] dimensions, int dimensionIndex, int leafType) {
    int length = dimensions[dimensionIndex];
    if (length == -1) {
      // We reach a sub array that doesn't have a dimension (e.g. 3rd dimension in int[3][2][][])
      return null;
    }

    if (dimensionIndex == dimensions.length - 1) {
      // We have reached the leaf dimension.
      return createLeaf(length, leafType);
    } else {
      Object[] array = new Object[length];
      for (int i = 0; i < length; i++) {
        array[i] = createRecursively(dimensions, dimensionIndex + 1, leafType);
      }
      return array;
    }
  }

  private static Object createLeaf(int length, int leafType) {
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

  static class OfObject extends WasmArray {
    Object[] elements;

    OfObject(int length) {
      super(length);
      this.elements = new Object[length];
    }

    @Override
    Object newArray(int length) {
      return new WasmArray.OfObject(length);
    }

    @Override
    public void setLength(int newLength) {
      ensureCapacity(newLength);
      if (newLength < length) {
        // Clear to outside contents
        for (int i = newLength; i < length; i++) {
          elements[i] = null;
        }
      }
      length = newLength;
    }

    public void push(Object o) {
      int newLength = length + 1;
      ensureCapacity(newLength);
      elements[length] = o;
      length = newLength;
    }

    private void ensureCapacity(int newLength) {
      if (newLength > elements.length) {
        // Not enough capacity, increase it.
        Object[] original = elements;
        elements = new Object[getNewCapacity(length, newLength)];
        copy(original, 0, elements, 0, original.length);
      }
    }

    public void insertFrom(int insertIndex, WasmArray.OfObject values) {
      Object[] original = elements;
      int newLength = length + values.length;

      // Ensure enough capacity.
      if (newLength > elements.length) {
        elements = new Object[getNewCapacity(elements.length, newLength)];
        // Copy only up to index since the other will be moved anyway.
        copy(original, 0, this.elements, 0, insertIndex);
      }

      // Make room for the values that will be inserted by moving the existing elements to the
      // end so that they are not overwritten.
      int insertEndIndex = insertIndex + values.length;
      copy(original, insertIndex, this.elements, insertEndIndex, newLength - insertEndIndex);

      // Copy new values into the insert location.
      copy(values.elements, 0, this.elements, insertIndex, values.length);

      // Adjust the final size to cover all copied items
      length = newLength;
    }

    @Override
    void copyFrom(int offset, WasmArray values, int valueOffset, int len) {
      copy(((WasmArray.OfObject) values).elements, valueOffset, elements, offset, len);
    }

    private static void copy(Object[] src, int srcOfs, Object[] dest, int destOfs, int len) {
      if (src == dest && srcOfs < destOfs) {
        // Reverse copy to handle overlap that would destroy values otherwise.
        srcOfs += len;
        for (int destEnd = destOfs + len; destEnd > destOfs; ) {
          dest[--destEnd] = src[--srcOfs];
        }
      } else {
        for (int destEnd = destOfs + len; destOfs < destEnd; ) {
          dest[destOfs++] = src[srcOfs++];
        }
      }
    }
  }

  static class OfByte extends WasmArray {

    private byte[] elements;

    OfByte(int length) {
      super(length);
      elements = new byte[length];
    }
  }

  static class OfShort extends WasmArray {

    private short[] elements;

    OfShort(int length) {
      super(length);
      elements = new short[length];
    }
  }

  static class OfChar extends WasmArray {

    private char[] elements;

    OfChar(int length) {
      super(length);
      elements = new char[length];
    }

    @Override
    void copyFrom(int offset, WasmArray values, int valueOffset, int len) {
      copy(((WasmArray.OfChar) values).elements, valueOffset, elements, offset, len);
    }

    private static void copy(char[] src, int srcOfs, char[] dest, int destOfs, int len) {
      if (src == dest && srcOfs < destOfs) {
        // Reverse copy to handle overlap that would destroy values otherwise.
        srcOfs += len;
        for (int destEnd = destOfs + len; destEnd > destOfs; ) {
          dest[--destEnd] = src[--srcOfs];
        }
      } else {
        for (int destEnd = destOfs + len; destOfs < destEnd; ) {
          dest[destOfs++] = src[srcOfs++];
        }
      }
    }
  }

  static class OfInt extends WasmArray {

    private int[] elements;

    OfInt(int length) {
      super(length);
      elements = new int[length];
    }

    @Override
    void copyFrom(int offset, WasmArray values, int valueOffset, int len) {
      copy(((WasmArray.OfInt) values).elements, valueOffset, elements, offset, len);
    }

    private static void copy(int[] src, int srcOfs, int[] dest, int destOfs, int len) {
      if (src == dest && srcOfs < destOfs) {
        // Reverse copy to handle overlap that would destroy values otherwise.
        srcOfs += len;
        for (int destEnd = destOfs + len; destEnd > destOfs; ) {
          dest[--destEnd] = src[--srcOfs];
        }
      } else {
        for (int destEnd = destOfs + len; destOfs < destEnd; ) {
          dest[destOfs++] = src[srcOfs++];
        }
      }
    }
  }

  static class OfLong extends WasmArray {

    private long[] elements;

    OfLong(int length) {
      super(length);
      elements = new long[length];
    }

    @Override
    void copyFrom(int offset, WasmArray values, int valueOffset, int len) {
      copy(((WasmArray.OfLong) values).elements, valueOffset, elements, offset, len);
    }

    private static void copy(long[] src, int srcOfs, long[] dest, int destOfs, int len) {
      if (src == dest && srcOfs < destOfs) {
        // Reverse copy to handle overlap that would destroy values otherwise.
        srcOfs += len;
        for (int destEnd = destOfs + len; destEnd > destOfs; ) {
          dest[--destEnd] = src[--srcOfs];
        }
      } else {
        for (int destEnd = destOfs + len; destOfs < destEnd; ) {
          dest[destOfs++] = src[srcOfs++];
        }
      }
    }
  }

  static class OfFloat extends WasmArray {

    private float[] elements;

    OfFloat(int length) {
      super(length);
      elements = new float[length];
    }
  }

  static class OfDouble extends WasmArray {

    private double[] elements;

    OfDouble(int length) {
      super(length);
      elements = new double[length];
    }
  }

  static class OfBoolean extends WasmArray {

    private boolean[] elements;

    OfBoolean(int length) {
      super(length);
      elements = new boolean[length];
    }
  }

  private static int getNewCapacity(int originalCapacity, int requestedCapacity) {
    // Grow roughly with 1.5x rate at minimum.
    int minCapacity = originalCapacity + (originalCapacity >> 1) + 1;
    return Math.max(minCapacity, requestedCapacity);
  }
}
