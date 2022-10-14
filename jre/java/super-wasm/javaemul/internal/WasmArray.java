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
import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;

/**
 * A common base abstraction for the arrays in Wasm.
 *
 * <p>NOTE: All arrays declared as fields, parameters, etc in the subclasses of WasmArray will be
 * considered native WASM arrays, whereas all arrays declared in the WasmArray class itself will be
 * rewritten to be instances of one of the subclasses.
 */
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

  abstract void copyFrom(int offset, WasmArray values, int valueOffset, int len);

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

    OfObject(Object[] initialValues) {
      super(initialValues.length);
      this.elements = initialValues;
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
        copy(elements, 0, original, 0, original.length);
      }
    }

    public void insertFrom(int insertIndex, WasmArray.OfObject values) {
      Object[] original = elements;
      int newLength = length + values.length;

      // Ensure enough capacity.
      if (newLength > elements.length) {
        elements = new Object[getNewCapacity(elements.length, newLength)];
        // Copy only up to index since the other will be moved anyway.
        copy(this.elements, 0, original, 0, insertIndex);
      }

      // Make room for the values that will be inserted by moving the existing elements to the
      // end so that they are not overwritten.
      int insertEndIndex = insertIndex + values.length;
      copy(this.elements, insertEndIndex, original, insertIndex, newLength - insertEndIndex);

      // Copy new values into the insert location.
      copy(this.elements, insertIndex, values.elements, 0, values.length);

      // Adjust the final size to cover all copied items
      length = newLength;
    }

    @Override
    void copyFrom(int offset, WasmArray values, int valueOffset, int len) {
      copy(elements, offset, ((WasmArray.OfObject) values).elements, valueOffset, len);
    }
  }

  static class OfByte extends WasmArray {

    private byte[] elements;

    OfByte(int length) {
      super(length);
      elements = new byte[length];
    }

    OfByte(byte[] initialValues) {
      super(initialValues.length);
      this.elements = initialValues;
    }

    @Override
    public void setLength(int newLength) {
      ensureCapacity(newLength);
      if (newLength < length) {
        // Clear to outside contents
        for (int i = newLength; i < length; i++) {
          elements[i] = 0;
        }
      }
      length = newLength;
    }

    public void push(byte o) {
      int newLength = length + 1;
      ensureCapacity(newLength);
      elements[length] = o;
      length = newLength;
    }

    private void ensureCapacity(int newLength) {
      if (newLength > elements.length) {
        // Not enough capacity, increase it.
        byte[] original = elements;
        elements = new byte[getNewCapacity(length, newLength)];
        copy(elements, 0, original, 0, original.length);
      }
    }

    @Override
    void copyFrom(int offset, WasmArray values, int valueOffset, int len) {
      copy(elements, offset, ((WasmArray.OfByte) values).elements, valueOffset, len);
    }
  }

  static class OfShort extends WasmArray {

    private short[] elements;

    OfShort(int length) {
      super(length);
      elements = new short[length];
    }

    OfShort(short[] initialValues) {
      super(initialValues.length);
      this.elements = initialValues;
    }

    @Override
    void copyFrom(int offset, WasmArray values, int valueOffset, int len) {
      copy(elements, offset, ((WasmArray.OfShort) values).elements, valueOffset, len);
    }
  }

  static class OfChar extends WasmArray {

    private char[] elements;

    OfChar(int length) {
      super(length);
      elements = new char[length];
    }

    OfChar(char[] initialValues) {
      super(initialValues.length);
      this.elements = initialValues;
    }

    @Override
    public void setLength(int newLength) {
      ensureCapacity(newLength);
      if (newLength < length) {
        // Clear to outside contents
        for (int i = newLength; i < length; i++) {
          elements[i] = 0;
        }
      }
      length = newLength;
    }

    private void ensureCapacity(int newLength) {
      if (newLength > elements.length) {
        // Not enough capacity, increase it.
        char[] original = elements;
        elements = new char[getNewCapacity(length, newLength)];
        copy(elements, 0, original, 0, original.length);
      }
    }

    @Override
    void copyFrom(int offset, WasmArray values, int valueOffset, int len) {
      copy(elements, offset, ((WasmArray.OfChar) values).elements, valueOffset, len);
    }
  }

  static class OfInt extends WasmArray {

    private int[] elements;

    OfInt(int length) {
      super(length);
      elements = new int[length];
    }

    OfInt(int[] initialValues) {
      super(initialValues.length);
      this.elements = initialValues;
    }

    public void push(int o) {
      int newLength = length + 1;
      ensureCapacity(newLength);
      elements[length] = o;
      length = newLength;
    }

    private void ensureCapacity(int newLength) {
      if (newLength > elements.length) {
        // Not enough capacity, increase it.
        int[] original = elements;
        elements = new int[getNewCapacity(length, newLength)];
        copy(elements, 0, original, 0, original.length);
      }
    }

    @Override
    void copyFrom(int offset, WasmArray values, int valueOffset, int len) {
      copy(elements, offset, ((WasmArray.OfInt) values).elements, valueOffset, len);
    }
  }

  static class OfLong extends WasmArray {

    private long[] elements;

    OfLong(int length) {
      super(length);
      elements = new long[length];
    }

    OfLong(long[] initialValues) {
      super(initialValues.length);
      this.elements = initialValues;
    }

    public void push(long o) {
      int newLength = length + 1;
      ensureCapacity(newLength);
      elements[length] = o;
      length = newLength;
    }

    private void ensureCapacity(int newLength) {
      if (newLength > elements.length) {
        // Not enough capacity, increase it.
        long[] original = elements;
        elements = new long[getNewCapacity(length, newLength)];
        copy(elements, 0, original, 0, original.length);
      }
    }

    @Override
    void copyFrom(int offset, WasmArray values, int valueOffset, int len) {
      copy(elements, offset, ((WasmArray.OfLong) values).elements, valueOffset, len);
    }
  }

  static class OfFloat extends WasmArray {

    private float[] elements;

    OfFloat(int length) {
      super(length);
      elements = new float[length];
    }

    OfFloat(float[] initialValues) {
      super(initialValues.length);
      this.elements = initialValues;
    }

    @Override
    void copyFrom(int offset, WasmArray values, int valueOffset, int len) {
      copy(elements, offset, ((WasmArray.OfFloat) values).elements, valueOffset, len);
    }
  }

  static class OfDouble extends WasmArray {

    private double[] elements;

    OfDouble(int length) {
      super(length);
      elements = new double[length];
    }

    OfDouble(double[] initialValues) {
      super(initialValues.length);
      this.elements = initialValues;
    }

    public void push(double o) {
      int newLength = length + 1;
      ensureCapacity(newLength);
      elements[length] = o;
      length = newLength;
    }

    private void ensureCapacity(int newLength) {
      if (newLength > elements.length) {
        // Not enough capacity, increase it.
        double[] original = elements;
        elements = new double[getNewCapacity(length, newLength)];
        copy(elements, 0, original, 0, original.length);
      }
    }

    @Override
    void copyFrom(int offset, WasmArray values, int valueOffset, int len) {
      copy(elements, offset, ((WasmArray.OfDouble) values).elements, valueOffset, len);
    }
  }

  static class OfBoolean extends WasmArray {

    private boolean[] elements;

    OfBoolean(int length) {
      super(length);
      elements = new boolean[length];
    }

    OfBoolean(boolean[] initialValues) {
      super(initialValues.length);
      this.elements = initialValues;
    }

    @Override
    void copyFrom(int offset, WasmArray values, int valueOffset, int len) {
      copy(elements, offset, ((WasmArray.OfBoolean) values).elements, valueOffset, len);
    }
  }

  private static int getNewCapacity(int originalCapacity, int requestedCapacity) {
    // Grow roughly with 1.5x rate at minimum.
    int minCapacity = originalCapacity + (originalCapacity >> 1) + 1;
    return Math.max(minCapacity, requestedCapacity);
  }

  @JsMethod // Keep JsInteropRestrictionsChecker happy.
  @Wasm("array.copy typeof(0) typeof(2)")
  private static native void copy(Object dest, int destOfs, Object src, int srcOfs, int len);
}
