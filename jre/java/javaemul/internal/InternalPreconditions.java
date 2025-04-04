/*
 * Copyright 2014 Google Inc.
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

import static java.lang.System.getProperty;

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 * A utility class that provides utility functions to do precondition checks inside GWT-SDK.
 * <p>Following table summarizes the grouping of the checks:
 * <pre>
 * ┌────────┬─────────────────────────────────────────────────────┬───────────────────────────────┐
 * │Group   │Description                                          │Common Exception Types         │
 * ├────────┼─────────────────────────────────────────────────────┼───────────────────────────────┤
 * │BOUNDS  │Checks related to the bound checking in collections. │IndexOutBoundsException        │
 * │        │                                                     │ArrayIndexOutOfBoundsException │
 * ├────────┼─────────────────────────────────────────────────────┼───────────────────────────────┤
 * │API     │Checks related to the correct usage of APIs.         │IllegalStateException          │
 * │        │                                                     │NoSuchElementException         │
 * │        │                                                     │NullPointerException           │
 * │        │                                                     │IllegalArgumentException       │
 * │        │                                                     │ConcurrentModificationException│
 * ├────────┼─────────────────────────────────────────────────────┼───────────────────────────────┤
 * │NUMERIC │Checks related to numeric operations.                │ArithmeticException            │
 * ├────────┼─────────────────────────────────────────────────────┼───────────────────────────────┤
 * │TYPE    │Checks related to java type system.                  │ClassCastException             │
 * │        │                                                     │ArrayStoreException            │
 * ├────────┼─────────────────────────────────────────────────────┼───────────────────────────────┤
 * │CRITICAL│Checks for cases where not failing-fast will keep    │IllegalArgumentException       │
 * │        │the object in an inconsistent state and/or degrade   │                               │
 * │        │debugging significantly. Currently disabling these   │                               │
 * │        │checks is not supported.                             │                               │
 * └────────┴─────────────────────────────────────────────────────┴───────────────────────────────┘
 * </pre>
 *
 * <p> Following table summarizes predefined check levels:
 * <pre>
 * ┌────────────────┬──────────┬─────────┬─────────┬─────────┬─────────┐
 * │Check level     │  BOUNDS  │   API   │ NUMERIC |  TYPE   │CRITICAL │
 * ├────────────────┼──────────┼─────────┼─────────┼─────────┼─────────┤
 * │Normal (default)│    X     │    X    │    X    │    X    │    X    │
 * ├────────────────┼──────────┼─────────┼─────────┼─────────┼─────────┤
 * │Optimized       │          │         │         │    X    │    X    │
 * ├────────────────┼──────────┼─────────┼─────────┼─────────┼─────────┤
 * │Minimal         │          │         │         │         │    X    │
 * ├────────────────┼──────────┼─────────┼─────────┼─────────┼─────────┤
 * │None (N/A yet)  │          │         │         │         │         │
 * └────────────────┴──────────┴─────────┴─────────┴─────────┴─────────┘
 * </pre>
 *
 * <p>Please note that, in development mode (jre.checkedMode=ENABLED), these checks will always be
 * performed regardless of configuration but will be converted to AssertionError if check is
 * disabled. This so that any reliance on related exceptions could be detected early on.
 * For this detection to work properly; it is important for apps to share the same config in
 * all environments.
 */
// Some parts adapted from Guava
public final class InternalPreconditions {

  private static final String CHECK_TYPE = getProperty("jre.checks.type");
  private static final String CHECK_NUMERIC = getProperty("jre.checks.numeric");
  private static final String CHECK_BOUNDS = getProperty("jre.checks.bounds");
  private static final String CHECK_API = getProperty("jre.checks.api");

  // Note that == used instead of equals below for comparisons as it is easier/quicker to optimize.

  // NORMAL
  private static final boolean LEVEL_NORMAL_OR_HIGHER =
      getProperty("jre.checks.checkLevel") == "NORMAL";
  // NORMAL or OPTIMIZED
  private static final boolean LEVEL_OPT_OR_HIGHER =
      getProperty("jre.checks.checkLevel") == "OPTIMIZED"
          || getProperty("jre.checks.checkLevel") == "NORMAL";
  // NORMAL or OPTIMIZED or MINIMAL
  private static final boolean LEVEL_MINIMAL_OR_HIGHER =
      getProperty("jre.checks.checkLevel") == "MINIMAL"
          || getProperty("jre.checks.checkLevel") == "OPTIMIZED"
          || getProperty("jre.checks.checkLevel") == "NORMAL";

  static {
    if (!LEVEL_MINIMAL_OR_HIGHER) {
      throw new IllegalStateException("Incorrect level: " + getProperty("jre.checks.checkLevel"));
    }
  }

  private static final boolean IS_TYPE_CHECKED =
      CHECK_TYPE == "AUTO" && LEVEL_OPT_OR_HIGHER || CHECK_TYPE == "ENABLED";
  private static final boolean IS_BOUNDS_CHECKED =
      CHECK_BOUNDS == "AUTO" && LEVEL_NORMAL_OR_HIGHER || CHECK_BOUNDS == "ENABLED";
  private static final boolean IS_API_CHECKED =
      CHECK_API == "AUTO" && LEVEL_NORMAL_OR_HIGHER || CHECK_API == "ENABLED";
  private static final boolean IS_NUMERIC_CHECKED =
      CHECK_NUMERIC == "AUTO" && LEVEL_NORMAL_OR_HIGHER || CHECK_NUMERIC == "ENABLED";

  private static final boolean IS_ASSERTED = getProperty("jre.checkedMode") == "ENABLED";

  /**
   * This method reports if the code is compiled with type checks.
   * It must be used in places where code can be replaced with a simpler one
   * when we know that no checks will occur.
   * See {@link System#arraycopy(Object, int, Object, int, int)} for example.
   * Please note that {@link #checkType(boolean)} should be preferred where feasible.
   */
  public static boolean isTypeChecked() {
    return IS_TYPE_CHECKED || IS_ASSERTED;
  }

  /**
   * This method reports if the code is compiled with api checks.
   * It must be used in places where code can be replaced with a simpler one
   * when we know that no checks will occur.
   * Please note that {@code #checkXXX(boolean)} should be preferred where feasible.
   */
  public static boolean isApiChecked() {
    return IS_API_CHECKED || IS_ASSERTED;
  }

  public static void checkType(boolean expression) {
    checkType(expression, null);
  }

  public static void checkType(boolean expression, String message) {
    if (IS_TYPE_CHECKED) {
      checkCriticalType(expression, message);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalType(expression, message);
      } catch (RuntimeException e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void checkCriticalType(boolean expression) {
    checkCriticalType(expression, null);
  }

  public static void checkCriticalType(boolean expression, String message) {
    if (!expression) {
      throw new ClassCastException(message);
    }
  }

  /**
   * Ensures the truth of an expression that verifies array type.
   */
  public static void checkArrayType(boolean expression) {
    if (IS_TYPE_CHECKED) {
      checkCriticalArrayType(expression);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalArrayType(expression);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void checkCriticalArrayType(boolean expression) {
    if (!expression) {
      throw new ArrayStoreException();
    }
  }

  /**
   * Ensures the truth of an expression that verifies array type.
   */
  public static void checkArrayType(boolean expression, Object errorMessage) {
    if (IS_TYPE_CHECKED) {
      checkCriticalArrayType(expression, errorMessage);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalArrayType(expression, errorMessage);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void checkCriticalArrayType(boolean expression, Object errorMessage) {
    if (!expression) {
      throw new ArrayStoreException(String.valueOf(errorMessage));
    }
  }

  public static void checkArithmetic(boolean expression) {
    if (IS_NUMERIC_CHECKED) {
      checkCriticalArithmetic(expression);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalArithmetic(expression);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void checkCriticalArithmetic(boolean expression) {
    if (!expression) {
      throw new ArithmeticException();
    }
  }

  /**
   * Ensures the truth of an expression involving existence of an element.
   */
  public static void checkElement(boolean expression) {
    if (IS_API_CHECKED) {
      checkCriticalElement(expression);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalElement(expression);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * Ensures the truth of an expression involving existence of an element.
   * <p>
   * For cases where failing fast is pretty important and not failing early could cause bugs that
   * are much harder to debug.
   */
  public static void checkCriticalElement(boolean expression) {
    if (!expression) {
      throw new NoSuchElementException();
    }
  }

  /**
   * Ensures the truth of an expression involving existence of an element.
   */
  public static void checkElement(boolean expression, Object errorMessage) {
    if (IS_API_CHECKED) {
      checkCriticalElement(expression, errorMessage);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalElement(expression, errorMessage);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * Ensures the truth of an expression involving existence of an element.
   * <p>
   * For cases where failing fast is pretty important and not failing early could cause bugs that
   * are much harder to debug.
   */
  public static void checkCriticalElement(boolean expression, Object errorMessage) {
    if (!expression) {
      throw new NoSuchElementException(String.valueOf(errorMessage));
    }
  }

  /**
   * Ensures the truth of an expression involving one or more parameters to the calling method.
   */
  public static void checkArgument(boolean expression) {
    if (IS_API_CHECKED) {
      checkCriticalArgument(expression);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalArgument(expression);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * Ensures the truth of an expression involving one or more parameters to the calling method.
   * <p>
   * For cases where failing fast is pretty important and not failing early could cause bugs that
   * are much harder to debug.
   */
  public static void checkCriticalArgument(boolean expression) {
    if (!expression) {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Ensures the truth of an expression involving one or more parameters to the calling method.
   */
  public static void checkArgument(boolean expression, Object errorMessage) {
    if (IS_API_CHECKED) {
      checkCriticalArgument(expression, errorMessage);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalArgument(expression, errorMessage);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * Ensures the truth of an expression involving one or more parameters to the calling method.
   * <p>
   * For cases where failing fast is pretty important and not failing early could cause bugs that
   * are much harder to debug.
   */
  public static void checkCriticalArgument(boolean expression, Object errorMessage) {
    if (!expression) {
      throw new IllegalArgumentException(String.valueOf(errorMessage));
    }
  }

  /**
   * Ensures the truth of an expression involving the state of the calling instance, but not
   * involving any parameters to the calling method.
   *
   * @param expression a boolean expression
   * @throws IllegalStateException if {@code expression} is false
   */
  public static void checkState(boolean expression) {
    if (IS_API_CHECKED) {
      checkCriticalState(expression);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalState(expression);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * Ensures the truth of an expression involving the state of the calling instance, but not
   * involving any parameters to the calling method.
   * <p>
   * For cases where failing fast is pretty important and not failing early could cause bugs that
   * are much harder to debug.
   */
  public static void checkCriticalState(boolean expression) {
    if (!expression) {
      throw new IllegalStateException();
    }
  }

  /**
   * Ensures the truth of an expression involving the state of the calling instance, but not
   * involving any parameters to the calling method.
   */
  public static void checkState(boolean expression, Object errorMessage) {
    if (IS_API_CHECKED) {
      checkCriticalState(expression, errorMessage);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalState(expression, errorMessage);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * Ensures the truth of an expression involving the state of the calling instance, but not
   * involving any parameters to the calling method.
   */
  public static void checkCriticalState(boolean expression, Object errorMessage) {
    if (!expression) {
      throw new IllegalStateException(String.valueOf(errorMessage));
    }
  }

  /**
   * Ensures that an object reference passed as a parameter to the calling method is not null.
   */
  public static <T> T checkNotNull(T reference) {
    if (IS_API_CHECKED) {
      checkCriticalNotNull(reference);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalNotNull(reference);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }

    return reference;
  }

  public static <T> T checkCriticalNotNull(T reference) {
    if (reference == null) {
      throw new NullPointerException();
    }
    return reference;
  }

  /**
   * Ensures that an object reference passed as a parameter to the calling method is not null.
   */
  public static void checkNotNull(Object reference, Object errorMessage) {
    if (IS_API_CHECKED) {
      checkCriticalNotNull(reference, errorMessage);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalNotNull(reference, errorMessage);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void checkCriticalNotNull(Object reference, Object errorMessage) {
    if (reference == null) {
      throw new NullPointerException(String.valueOf(errorMessage));
    }
  }

  /**
   * Ensures the truth of an expression. Throws a {@link NullPointerException} if the expression is
   * not true.
   */
  public static void checkNotNull(boolean expression) {
    if (IS_API_CHECKED) {
      checkCriticalNotNull(expression);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalNotNull(expression);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void checkCriticalNotNull(boolean expression) {
    if (!expression) {
      throw new NullPointerException();
    }
  }

  /**
   * Ensures that {@code size} specifies a valid array size (i.e. non-negative).
   */
  public static void checkArraySize(int size) {
    if (IS_API_CHECKED) {
      checkCriticalArraySize(size);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalArraySize(size);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void checkCriticalArraySize(int size) {
    if (size < 0) {
      throw new NegativeArraySizeException("Negative array size: " + size);
    }
  }

  /**
   * Ensures that {@code index} specifies a valid <i>element</i> in a list or string of size
   * {@code size}. An element index may range from zero, inclusive, to {@code size}, exclusive.
   */
  public static void checkElementIndex(int index, int size) {
    if (IS_BOUNDS_CHECKED) {
      checkCriticalElementIndex(index, size);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalElementIndex(index, size);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void checkCriticalElementIndex(int index, int size) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }
  }

  public static void checkStringElementIndex(int index, int size) {
    if (IS_BOUNDS_CHECKED) {
      checkCriticalStringElementIndex(index, size);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalStringElementIndex(index, size);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void checkCriticalStringElementIndex(int index, int size) {
    if (index < 0 || index >= size) {
      throw new StringIndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }
  }

  /**
   * Ensures that {@code index} specifies a valid <i>position</i> in a list of
   * size {@code size}. A position index may range from zero to {@code size}, inclusive.
   */
  public static void checkPositionIndex(int index, int size) {
    if (IS_BOUNDS_CHECKED) {
      checkCriticalPositionIndex(index, size);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalPositionIndex(index, size);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void checkCriticalPositionIndex(int index, int size) {
    if (index < 0 || index > size) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }
  }

  /**
   * Ensures that {@code start} and {@code end} specify a valid <i>positions</i> in a list
   * of size {@code size}, and are in order. A position index may range from zero to
   * {@code size}, inclusive.
   */
  public static void checkPositionIndexes(int start, int end, int size) {
    if (IS_BOUNDS_CHECKED) {
      checkCriticalPositionIndexes(start, end, size);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalPositionIndexes(start, end, size);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * Ensures that {@code start} and {@code end} specify a valid <i>positions</i> in a list
   * of size {@code size}, and are in order. A position index may range from zero to
   * {@code size}, inclusive.
   */
  public static void checkCriticalPositionIndexes(int start, int end, int size) {
    if (start < 0 || end > size) {
      throw new IndexOutOfBoundsException(
          "fromIndex: " + start + ", toIndex: " + end + ", size: " + size);
    }
    if (start > end) {
      throw new IllegalArgumentException("fromIndex: " + start + " > toIndex: " + end);
    }
  }

  /**
   * Checks that array bounds are correct.
   *
   * @throws IllegalArgumentException if {@code start > end}
   * @throws ArrayIndexOutOfBoundsException if the range is not legal
   */
  public static void checkCriticalArrayBounds(int start, int end, int length) {
    if (start > end) {
      throw new IllegalArgumentException("fromIndex: " + start + " > toIndex: " + end);
    }
    if (start < 0 || end > length) {
      throw new ArrayIndexOutOfBoundsException(
          "fromIndex: " + start + ", toIndex: " + end + ", length: " + length);
    }
  }

  public static void checkArrayCopyIndices(
      Object src, int srcOfs, Object dest, int destOfs, int len) {
    if (IS_BOUNDS_CHECKED) {
      checkCriticalArrayCopyIndices(src, srcOfs, dest, destOfs, len);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalArrayCopyIndices(src, srcOfs, dest, destOfs, len);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  /** Checks that array copy bounds are correct. */
  public static void checkCriticalArrayCopyIndices(
      Object src, int srcOfs, Object dest, int destOfs, int len) {
    int srclen = ArrayHelper.getLength(src);
    int destlen = ArrayHelper.getLength(dest);
    if (srcOfs < 0 || destOfs < 0 || len < 0 || srcOfs + len > srclen || destOfs + len > destlen) {
      throw new IndexOutOfBoundsException();
    }
  }

  /**
   * Checks that string bounds are correct.
   *
   * @throws StringIndexOutOfBoundsException if the range is not legal
   */
  public static void checkStringBounds(int start, int end, int length) {
    if (IS_BOUNDS_CHECKED) {
      checkCriticalStringBounds(start, end, length);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalStringBounds(start, end, length);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * Checks that string bounds are correct.
   *
   * @throws StringIndexOutOfBoundsException if the range is not legal
   */
  public static void checkCriticalStringBounds(int start, int end, int length) {
    if (start < 0 || end > length || end < start) {
      throw new StringIndexOutOfBoundsException(
          "fromIndex: " + start + ", toIndex: " + end + ", length: " + length);
    }
  }

  public static void checkConcurrentModification(int currentModCount, int recordedModCount) {
    if (IS_API_CHECKED) {
      checkCriticalConcurrentModification(currentModCount, recordedModCount);
    } else if (IS_ASSERTED) {
      try {
        checkCriticalConcurrentModification(currentModCount, recordedModCount);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void checkCriticalConcurrentModification(
      double currentModCount, double recordedModCount) {
    if (currentModCount != recordedModCount) {
      throw new ConcurrentModificationException();
    }
  }

  /**
   * Throws a MatchException for exhaustive switch expressions on unexpected values.
   *
   * @throws MatchException
   */
  public static void checkCriticalExhaustive() {
    throw new MatchException();
  }

  public static void checkExhaustive() {
    if (IS_API_CHECKED) {
      checkCriticalExhaustive();
    } else if (IS_ASSERTED) {
      try {
        checkCriticalExhaustive();
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  // Hides the constructor for this static utility class.
  private InternalPreconditions() { }
}
