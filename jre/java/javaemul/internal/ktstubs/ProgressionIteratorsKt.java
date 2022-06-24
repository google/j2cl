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

import java.util.NoSuchElementException;

/**
 * Implementations of Kotlin progression iterators.
 *
 * <p>Adapted from kotlin.ranges.CharProgressionIterator, etc.
 */
// TODO(b/234498715): After correcting primitive/boxed bridging, implement Iterator<>, IntIterator,
// etc.
// TODO(b/234777938): Remove placeholder implementations once RangesKt is fully supported.
public final class ProgressionIteratorsKt {

  /** An iterator over a progression of values of type `Char`. */
  public static final class CharProgressionIterator {
    private final int finalElement;
    private boolean hasNext;
    private int next;
    private int step;

    CharProgressionIterator(char first, char last, int step) {
      this.finalElement = (int) last;
      this.hasNext = (step > 0) ? first <= last : first >= last;
      this.next = hasNext ? (int) first : finalElement;
      this.step = step;
    }

    public boolean hasNext() {
      return hasNext;
    }

    public char next() {
      int value = next;
      if (value == finalElement) {
        if (!hasNext) {
          throw new NoSuchElementException();
        }
        hasNext = false;
      } else {
        next += step;
      }
      return (char) value;
    }
  }

  /** An iterator over a progression of values of type `Int`. */
  public static final class IntProgressionIterator {
    private final int finalElement;
    private boolean hasNext;
    private int next;
    private int step;

    IntProgressionIterator(int first, int last, int step) {
      this.finalElement = last;
      this.hasNext = (step > 0) ? first <= last : first >= last;
      this.next = hasNext ? first : finalElement;
      this.step = step;
    }

    public boolean hasNext() {
      return hasNext;
    }

    public int next() {
      int value = next;
      if (value == finalElement) {
        if (!hasNext) {
          throw new NoSuchElementException();
        }
        hasNext = false;
      } else {
        next += step;
      }
      return value;
    }
  }

  /** An iterator over a progression of values of type `Long`. */
  public static final class LongProgressionIterator {
    private final long finalElement;
    private boolean hasNext;
    private long next;
    private long step;

    LongProgressionIterator(long first, long last, long step) {
      this.finalElement = last;
      this.hasNext = (step > 0) ? first <= last : first >= last;
      this.next = hasNext ? first : finalElement;
      this.step = step;
    }

    public boolean hasNext() {
      return hasNext;
    }

    public long next() {
      long value = next;
      if (value == finalElement) {
        if (!hasNext) {
          throw new NoSuchElementException();
        }
        hasNext = false;
      } else {
        next += step;
      }
      return value;
    }
  }

  private ProgressionIteratorsKt() {}
}
