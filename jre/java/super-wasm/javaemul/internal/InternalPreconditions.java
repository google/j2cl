/*
 * Copyright 2021 Google Inc.
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

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 * Mostly noop temporary replacement for Wasm to disable optional checking since j2wasm is not
 * capable of optimizing them yet.
 */
public final class InternalPreconditions {

  public static boolean isTypeChecked() {
    return false;
  }

  public static boolean isApiChecked() {
    return false;
  }

  public static void checkType(boolean expression) {
    checkType(expression, null);
  }

  public static void checkType(boolean expression, String message) {}

  public static void checkCriticalType(boolean expression) {
    checkCriticalType(expression, null);
  }

  public static void checkCriticalType(boolean expression, String message) {
    if (!expression) {
      throw new ClassCastException(message);
    }
  }

  public static void checkArrayType(boolean expression) {}

  public static void checkCriticalArrayType(boolean expression) {
    if (!expression) {
      throw new ArrayStoreException();
    }
  }

  public static void checkArrayType(boolean expression, Object errorMessage) {}

  public static void checkCriticalArrayType(boolean expression, Object errorMessage) {
    if (!expression) {
      throw new ArrayStoreException(String.valueOf(errorMessage));
    }
  }

  public static void checkArithmetic(boolean expression) {}

  public static void checkCriticalArithmetic(boolean expression) {
    if (!expression) {
      throw new ArithmeticException();
    }
  }

  public static void checkElement(boolean expression) {}

  public static void checkCriticalElement(boolean expression) {
    if (!expression) {
      throw new NoSuchElementException();
    }
  }

  public static void checkElement(boolean expression, Object errorMessage) {}

  public static void checkCriticalElement(boolean expression, Object errorMessage) {
    if (!expression) {
      throw new NoSuchElementException(String.valueOf(errorMessage));
    }
  }

  public static void checkArgument(boolean expression) {}

  public static void checkCriticalArgument(boolean expression) {
    if (!expression) {
      throw new IllegalArgumentException();
    }
  }

  public static void checkArgument(boolean expression, Object errorMessage) {}

  public static void checkCriticalArgument(boolean expression, Object errorMessage) {
    if (!expression) {
      throw new IllegalArgumentException(String.valueOf(errorMessage));
    }
  }

  public static void checkState(boolean expression) {}

  public static void checkCriticalState(boolean expression) {
    if (!expression) {
      throw new IllegalStateException();
    }
  }

  public static void checkState(boolean expression, Object errorMessage) {}

  public static void checkCriticalState(boolean expression, Object errorMessage) {
    if (!expression) {
      throw new IllegalStateException(String.valueOf(errorMessage));
    }
  }

  public static <T> T checkNotNull(T reference) {
    return reference;
  }

  public static <T> T checkCriticalNotNull(T reference) {
    if (reference == null) {
      throw new NullPointerException();
    }
    return reference;
  }

  public static void checkNotNull(Object reference, Object errorMessage) {}

  public static void checkCriticalNotNull(Object reference, Object errorMessage) {
    if (reference == null) {
      throw new NullPointerException(String.valueOf(errorMessage));
    }
  }

  public static void checkArraySize(int size) {}

  public static void checkCriticalArraySize(int size) {
    if (size < 0) {
      throw new NegativeArraySizeException("Negative array size: " + size);
    }
  }

  public static void checkElementIndex(int index, int size) {}

  public static void checkCriticalElementIndex(int index, int size) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }
  }

  public static void checkStringElementIndex(int index, int size) {}

  public static void checkCriticalStringElementIndex(int index, int size) {
    if (index < 0 || index >= size) {
      throw new StringIndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }
  }

  public static void checkPositionIndex(int index, int size) {}

  public static void checkCriticalPositionIndex(int index, int size) {
    if (index < 0 || index > size) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }
  }

  public static void checkPositionIndexes(int start, int end, int size) {}

  public static void checkCriticalPositionIndexes(int start, int end, int size) {
    if (start < 0 || end > size) {
      throw new IndexOutOfBoundsException(
          "fromIndex: " + start + ", toIndex: " + end + ", size: " + size);
    }
    if (start > end) {
      throw new IllegalArgumentException("fromIndex: " + start + " > toIndex: " + end);
    }
  }

  public static void checkCriticalArrayBounds(int start, int end, int length) {
    if (start > end) {
      throw new IllegalArgumentException("fromIndex: " + start + " > toIndex: " + end);
    }
    if (start < 0 || end > length) {
      throw new ArrayIndexOutOfBoundsException(
          "fromIndex: " + start + ", toIndex: " + end + ", length: " + length);
    }
  }

  public static void checkStringBounds(int start, int end, int length) {}

  public static void checkCriticalStringBounds(int start, int end, int length) {
    if (start < 0 || end > length || end < start) {
      throw new StringIndexOutOfBoundsException(
          "fromIndex: " + start + ", toIndex: " + end + ", length: " + length);
    }
  }

  public static void checkConcurrentModification(int currentModCount, int recordedModCount) {}

  public static void checkCriticalConcurrentModification(
      double currentModCount, double recordedModCount) {
    if (currentModCount != recordedModCount) {
      throw new ConcurrentModificationException();
    }
  }
  // Hides the constructor for this static utility class.
  private InternalPreconditions() {}
}
