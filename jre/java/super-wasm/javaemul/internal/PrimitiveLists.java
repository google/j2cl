/*
 * Copyright 2023 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package javaemul.internal;

/** Helpers for dealing with growing containers for primitives. */
public final class PrimitiveLists {

  public static Int createForInt() {
    return new PrimitiveLists.Int();
  }

  /** Primitive int list. */
  public static final class Int {

    private static final int START_SIZE = 10;

    private int[] array = new int[START_SIZE];
    private int size;

    public void push(int element) {
      int[] array = this.array;
      if (size == array.length) {
        array = ArrayHelper.resize(array, size + 1);
        this.array = array;
      }
      array[size++] = element;
    }

    public int size() {
      return size;
    }

    public int[] internalArray() {
      return this.array;
    }

    public int[] toArray() {
      return ArrayHelper.clone(this.array, 0, size);
    }
  }

  public static Long createForLong() {
    return new PrimitiveLists.Long();
  }

  /** Primitive long list. */
  public static final class Long {

    private static final int START_SIZE = 10;

    private long[] array = new long[START_SIZE];
    private int size;

    public void push(long element) {
      long[] array = this.array;
      if (size == array.length) {
        array = ArrayHelper.resize(array, size + 1);
        this.array = array;
      }
      array[size++] = element;
    }

    public int size() {
      return size;
    }

    public long[] internalArray() {
      return this.array;
    }

    public long[] toArray() {
      return ArrayHelper.clone(this.array, 0, size);
    }
  }

  public static Double createForDouble() {
    return new PrimitiveLists.Double();
  }

  /** Primitive double list. */
  public static final class Double {

    private static final int START_SIZE = 10;

    private double[] array = new double[START_SIZE];
    private int size;

    public void push(double element) {
      double[] array = this.array;
      if (size == array.length) {
        array = ArrayHelper.resize(array, size + 1);
        this.array = array;
      }
      array[size++] = element;
    }

    public int size() {
      return size;
    }

    public double[] internalArray() {
      return this.array;
    }

    public double[] toArray() {
      return ArrayHelper.clone(this.array, 0, size);
    }
  }

  private PrimitiveLists() {}
}
