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

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/** Helpers for dealing with growing containers for primitives. */
public final class PrimitiveLists {

  public static Byte createForByte() {
    return JsUtils.uncheckedCast(new byte[0]);
  }

  /** Primtive byte list. */
  @JsType(isNative = true, name = "Array", namespace = JsPackage.GLOBAL)
  public static class Byte {
    public native void push(byte element);

    @JsProperty(name = "length")
    public native int size();

    @JsOverlay
    public final byte[] internalArray() {
      return JsUtils.uncheckedCast(this);
    }

    /**
     * Returns the internal array or copy of it (based on the platform and the gap between size and
     * actual internal array length).
     */
    @JsOverlay
    public final byte[] toArray() {
      return JsUtils.uncheckedCast(this);
    }
  }

  public static Int createForInt() {
    return JsUtils.uncheckedCast(new int[0]);
  }

  /** Primtive long list. */
  @JsType(isNative = true, name = "Array", namespace = JsPackage.GLOBAL)
  public static class Int {
    public native void push(int element);

    @JsProperty(name = "length")
    public native int size();

    @JsOverlay
    public final int[] internalArray() {
      return JsUtils.uncheckedCast(this);
    }

    /**
     * Returns the internal array or copy of it (based on the platform and the gap between size and
     * actual internal array length).
     */
    @JsOverlay
    public final int[] toArray() {
      return JsUtils.uncheckedCast(this);
    }
  }

  public static Long createForLong() {
    return JsUtils.uncheckedCast(new long[0]);
  }

  /** Primtive long list. */
  @JsType(isNative = true, name = "Array", namespace = JsPackage.GLOBAL)
  public static class Long {
    public native void push(long element);

    @JsProperty(name = "length")
    public native int size();

    @JsOverlay
    public final long[] internalArray() {
      return JsUtils.uncheckedCast(this);
    }

    /**
     * Returns the internal array or copy of it (based on the platform and the gap between size and
     * actual internal array length).
     */
    @JsOverlay
    public final long[] toArray() {
      return JsUtils.uncheckedCast(this);
    }
  }

  public static Double createForDouble() {
    return JsUtils.uncheckedCast(new double[0]);
  }

  /** Primtive long list. */
  @JsType(isNative = true, name = "Array", namespace = JsPackage.GLOBAL)
  public static class Double {
    public native void push(double element);

    @JsProperty(name = "length")
    public native int size();

    @JsOverlay
    public final double[] internalArray() {
      return JsUtils.uncheckedCast(this);
    }

    /**
     * Returns the internal array or copy of it (based on the platform and the gap between size and
     * actual internal array length).
     */
    @JsOverlay
    public final double[] toArray() {
      return JsUtils.uncheckedCast(this);
    }
  }

  private PrimitiveLists() {}
}
