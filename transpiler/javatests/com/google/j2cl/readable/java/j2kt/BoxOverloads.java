/*
 * Copyright 2023 Google Inc.
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
package j2kt;

import org.jspecify.nullness.NullMarked;
import org.jspecify.nullness.Nullable;

@NullMarked
public class BoxOverloads {
  public BoxOverloads(double d) {
    // Dispatches BoxOverloads(@Nullable Double)
    this(Double.valueOf(d));
  }

  public BoxOverloads(@Nullable Double d) {}

  public static void nullableBoxedOverload(double d) {}

  public static void nullableBoxedOverload(@Nullable Double d) {}

  public static void testNullableBoxedOverload() {
    // Dispatches nullableBoxedOverload(double)
    nullableBoxedOverload(1.25);

    // Dispatches nullableBoxedOverload(@Nullable Double)
    nullableBoxedOverload(Double.valueOf(1.25));

    // Dispatches nullableBoxedOverload(@Nullable Double)
    nullableBoxedOverload(new Double(1.25));

    // Dispatches nullableBoxedOverload(double)
    nullableBoxedOverload(1);
  }

  public static void nullableNumberOverload(double d) {}

  public static void nullableNumberOverload(@Nullable Number n) {}

  public static void testNullableNumberOverload() {
    // Dispatches nullableNumberOverload(double)
    nullableNumberOverload(1.25);

    // Dispatches nullableNumberOverload(@Nullable Number)
    nullableNumberOverload(Double.valueOf(1.25));

    // Dispatches nullableNumberOverload(@Nullable Number)
    nullableNumberOverload(new Double(1.25));

    // Dispatches nullableNumberOverload(double)
    nullableNumberOverload(1);

    // Dispatches nullableNumberOverload(@Nullable Number)
    nullableNumberOverload(Integer.valueOf(1));

    // Dispatches nullableNumberOverload(@Nullable Number)
    nullableNumberOverload(new Integer(1));
  }

  public static void numberOverload(double d) {}

  public static void numberOverload(Number n) {}

  public static void testNumberOverload() {
    // Dispatches numberOverload(double)
    numberOverload(1.25);

    // Dispatches numberOverload(Number)
    numberOverload(Double.valueOf(1.25));

    // Dispatches numberOverload(Number)
    numberOverload(new Double(1.25));

    // Dispatches numberOverload(double)
    numberOverload(1);

    // Dispatches numberOverload(Number)
    numberOverload(Integer.valueOf(1));

    // Dispatches numberOverload(Number)
    numberOverload(new Integer(1));
  }

  public static void nullableObjectOverload(double d) {}

  public static void nullableObjectOverload(@Nullable Object o) {}

  public static void testNullableObjectOverload() {
    // Dispatches nullableObjectOverload(double)
    nullableObjectOverload(1.25);

    // Dispatches nullableObjectOverload(@Nullable Object)
    nullableObjectOverload(Double.valueOf(1.25));

    // Dispatches nullableObjectOverload(@Nullable Object)
    nullableObjectOverload(new Double(1.25));

    // Dispatches nullableObjectOverload(double)
    nullableObjectOverload(1);

    // Dispatches nullableObjectOverload(@Nullable Object)
    nullableObjectOverload(Integer.valueOf(1));

    // Dispatches nullableObjectOverload(@Nullable Object)
    nullableObjectOverload(new Integer(1));

    // Dispatches nullableObjectOverload(@Nullable Object)
    nullableObjectOverload("foo");
  }

  public static void objectOverload(double d) {}

  public static void objectOverload(Object o) {}

  public static void testObjectOverload() {
    // Dispatches objectOverload(double)
    objectOverload(1.25);

    // Dispatches objectOverload(Object)
    objectOverload(Double.valueOf(1.25));

    // Dispatches objectOverload(Object)
    objectOverload(new Double(1.25));

    // Dispatches objectOverload(double)
    objectOverload(1);

    // Dispatches objectOverload(Object)
    objectOverload(Integer.valueOf(1));

    // Dispatches objectOverload(Object)
    objectOverload(new Integer(1));

    // Dispatches objectOverload(@Object)
    objectOverload("foo");
  }

  // A case from com.google.common.base.MoreObjects.
  public static <T> T firstNonNull(@Nullable T t1, T t2) {
    return t1 != null ? t1 : t2;
  }

  public static void testFirstNonNull(@Nullable Integer i) {
    firstNonNull(i, 0);
  }

  public interface Generic<T extends @Nullable Object> {
    void comparableOverload(double d);

    void comparableOverload(Comparable<T> comparable);
  }

  public static void testComparableOverload(Generic<Double> generic) {
    // Dispatches overload(double)
    generic.comparableOverload(1.25);

    // Dispatches overload(Comparable<T>)
    generic.comparableOverload(Double.valueOf(1.25));
  }
}
