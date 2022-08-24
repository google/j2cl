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

import javaemul.internal.ktstubs.ProgressionIteratorsKt.CharProgressionIterator;
import javaemul.internal.ktstubs.ProgressionIteratorsKt.IntProgressionIterator;
import javaemul.internal.ktstubs.ProgressionIteratorsKt.LongProgressionIterator;
import jsinterop.annotations.JsMethod;

/**
 * Implementations of Kotlin progressions.
 *
 * <p>Adapted from kotlin.ranges.CharProgression, etc.
 */
// TODO(b/234498715): After correcting primitive/boxed bridging, implement Iterable<>.
// TODO(b/234777938): Remove placeholder implementations once RangesKt is fully supported.
public final class ProgressionsKt {
  /** A progression of values of type `Char`. */
  public static class CharProgression {
    private final char first;
    private final char last;
    private final int step;

    CharProgression(char start, char endInclusive, int step) {
      this.first = start;
      this.last = (char) getProgressionLastElement(start, endInclusive, step);
      this.step = step;
    }

    public char getFirst() {
      return first;
    }

    public char getLast() {
      return last;
    }

    public int getStep() {
      return step;
    }

    @SuppressWarnings("unusable-by-js")
    @JsMethod(name = "m_iterator__kotlin_collections_CharIterator")
    public CharProgressionIterator iterator() {
      return new CharProgressionIterator(first, last, step);
    }

    public boolean isEmpty() {
      return step > 0 ? first > last : first < last;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof CharProgression)) {
        return false;
      }
      CharProgression otherProgression = (CharProgression) other;
      return (isEmpty() && otherProgression.isEmpty())
          || (first == otherProgression.first
              && last == otherProgression.last
              && step == otherProgression.step);
    }

    @Override
    public int hashCode() {
      return isEmpty() ? -1 : 31 * (31 * (int) first + (int) last) + step;
    }
  }

  /** A progression of values of type `Int`. */
  public static class IntProgression {
    private final int first;
    private final int last;
    private final int step;

    IntProgression(int start, int endInclusive, int step) {
      this.first = start;
      this.last = getProgressionLastElement(start, endInclusive, step);
      this.step = step;
    }

    public int getFirst() {
      return first;
    }

    public int getLast() {
      return last;
    }

    public int getStep() {
      return step;
    }

    @SuppressWarnings("unusable-by-js")
    @JsMethod(name = "m_iterator__kotlin_collections_IntIterator")
    public IntProgressionIterator iterator() {
      return new IntProgressionIterator(first, last, step);
    }

    public boolean isEmpty() {
      return step > 0 ? first > last : first < last;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof IntProgression)) {
        return false;
      }
      IntProgression otherProgression = (IntProgression) other;
      return (isEmpty() && otherProgression.isEmpty())
          || (first == otherProgression.first
              && last == otherProgression.last
              && step == otherProgression.step);
    }

    @Override
    public int hashCode() {
      return isEmpty() ? -1 : 31 * (31 * first + last) + step;
    }
  }

  /** A progression of values of type `Long`. */
  public static class LongProgression {
    private final long first;
    private final long last;
    private final long step;

    LongProgression(long start, long endInclusive, long step) {
      this.first = start;
      this.last = getProgressionLastElement(start, endInclusive, step);
      this.step = step;
    }

    public long getFirst() {
      return first;
    }

    public long getLast() {
      return last;
    }

    public long getStep() {
      return step;
    }

    @SuppressWarnings("unusable-by-js")
    @JsMethod(name = "m_iterator__kotlin_collections_LongIterator")
    public LongProgressionIterator iterator() {
      return new LongProgressionIterator(first, last, step);
    }

    public boolean isEmpty() {
      return step > 0 ? first > last : first < last;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof LongProgression)) {
        return false;
      }
      LongProgression otherProgression = (LongProgression) other;
      return (isEmpty() && otherProgression.isEmpty())
          || (first == otherProgression.first
              && last == otherProgression.last
              && step == otherProgression.step);
    }

    @Override
    public int hashCode() {
      return isEmpty()
          ? -1
          : (int)
              (31 * (31 * (first ^ (first >> 32)) + (last ^ (last >> 32))) + (step ^ (step >> 32)));
    }
  }

  // Helpers adapted from
  // http://cs/github/JetBrains/kotlin/core/builtins/src/kotlin/internal/progressionUtil.kt
  private static int mod(int a, int b) {
    int mod = a % b;
    return mod >= 0 ? mod : mod + b;
  }

  private static long mod(long a, long b) {
    long mod = a % b;
    return mod >= 0 ? mod : mod + b;
  }

  private static int differenceModulo(int a, int b, int c) {
    return mod(mod(a, c) - mod(b, c), c);
  }

  private static long differenceModulo(long a, long b, long c) {
    return mod(mod(a, c) - mod(b, c), c);
  }

  private static int getProgressionLastElement(int start, int end, int step) {
    if (step > 0) {
      return (start >= end) ? end : (end - differenceModulo(end, start, step));
    }
    if (step < 0) {
      return (start <= end) ? end : (end + differenceModulo(start, end, -step));
    }
    throw new IllegalArgumentException("Step is zero");
  }

  private static long getProgressionLastElement(long start, long end, long step) {
    if (step > 0) {
      return (start >= end) ? end : (end - differenceModulo(end, start, step));
    }
    if (step < 0) {
      return (start <= end) ? end : (end + differenceModulo(start, end, -step));
    }
    throw new IllegalArgumentException("Step is zero");
  }

  private ProgressionsKt() {}
}
