/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Copyright 2022 Google Inc.
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
package javaemul.internal.ktstubs;

import javaemul.internal.ktstubs.ProgressionsKt.CharProgression;
import javaemul.internal.ktstubs.ProgressionsKt.IntProgression;
import javaemul.internal.ktstubs.ProgressionsKt.LongProgression;

/**
 * Implementations of Kotlin ranges.
 *
 * <p>Adapted from kotlin.ranges.CharRange, etc.
 */
// TODO(b/234498715): After correcting primitive/boxed bridging, implement ClosedRange<>.
// TODO(b/234777938): Remove placeholder implementations once RangesKt is fully supported.
public final class RangesKt {
  /** A range of values of type `Char`. */
  public static final class CharRange extends CharProgression {
    public CharRange(char start, char endInclusive) {
      super(start, endInclusive, 1);
    }

    public char getStart() {
      return getFirst();
    }

    public char getEndInclusive() {
      return getLast();
    }

    public boolean contains(char value) {
      return getFirst() <= value && value <= getLast();
    }

    @Override
    public boolean isEmpty() {
      return getFirst() > getLast();
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof CharRange)) {
        return false;
      }
      CharRange otherRange = (CharRange) other;
      return isEmpty() && otherRange.isEmpty()
          || getFirst() == otherRange.getFirst() && getLast() == otherRange.getLast();
    }

    @Override
    public int hashCode() {
      return isEmpty() ? -1 : (31 * (int) getFirst() + (int) getLast());
    }
  }

  /** A range of values of type `Int`. */
  public static final class IntRange extends IntProgression {
    public IntRange(int start, int endInclusive) {
      super(start, endInclusive, 1);
    }

    public int getStart() {
      return getFirst();
    }

    public int getEndInclusive() {
      return getLast();
    }

    public boolean contains(int value) {
      return getFirst() <= value && value <= getLast();
    }

    @Override
    public boolean isEmpty() {
      return getFirst() > getLast();
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof IntRange)) {
        return false;
      }
      IntRange otherRange = (IntRange) other;
      return isEmpty() && otherRange.isEmpty()
          || getFirst() == otherRange.getFirst() && getLast() == otherRange.getLast();
    }

    @Override
    public int hashCode() {
      return isEmpty() ? -1 : (31 * getFirst() + getLast());
    }
  }

  /** A range of values of type `Long`. */
  public static final class LongRange extends LongProgression {
    public LongRange(long start, long endInclusive) {
      super(start, endInclusive, 1);
    }

    public long getStart() {
      return getFirst();
    }

    public long getEndInclusive() {
      return getLast();
    }

    public boolean contains(long value) {
      return getFirst() <= value && value <= getLast();
    }

    @Override
    public boolean isEmpty() {
      return getFirst() > getLast();
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof LongRange)) {
        return false;
      }
      LongRange otherRange = (LongRange) other;
      return isEmpty() && otherRange.isEmpty()
          || getFirst() == otherRange.getFirst() && getLast() == otherRange.getLast();
    }

    @Override
    public int hashCode() {
      return isEmpty()
          ? -1
          : (int) (31 * (getFirst() ^ (getFirst() >> 32)) + (getLast() ^ (getLast() >> 32)));
    }
  }

  private RangesKt() {}
}
