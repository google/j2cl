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
package j2ktnotpassing;

import org.jspecify.nullness.NullMarked;
import org.jspecify.nullness.Nullable;

@NullMarked
public class BoxOverloads {
  public static void boxedOverload(double d) {}

  public static void boxedOverload(Double d) {}

  public static void testBoxedOverload() {
    // Dispatches boxedOverload(double)
    boxedOverload(1.25);

    // Dispatches boxedOverload(Double)
    boxedOverload(Double.valueOf(1.25));

    // Dispatches boxedOverload(Double)
    boxedOverload(new Double(1.25));

    // Dispatches boxedOverload(double)
    boxedOverload(1);
  }

  public interface Generic<T extends @Nullable Object> {
    void overload(double d);

    void overload(T t);
  }

  public static void testGenericBoxedOverload(Generic<Double> generic) {
    // Dispatches overload(double)
    generic.overload(1.25);

    // Dispatches overload(T)
    generic.overload(Double.valueOf(1.25));

    // Dispatches overload(double)
    generic.overload(1);
  }
}
