/*
 * Copyright 2024 Google Inc.
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
package sourcemap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Supplier;
import jsinterop.annotations.JsConstructor;

abstract class SourceMap<T extends Number> implements Comparator<T> {

  private int uninitializedInstanceField;

  // Instance initializer block
  {
    uninitializedInstanceField = 1000;
  }

  private String uninitializedInstanceField2;

  // Another instance initializer block
  {
    if (uninitializedInstanceField == 1000) {
      uninitializedInstanceField2 = "Hello!";
    } else {
      uninitializedInstanceField2 = "World!";
    }
  }

  @JsConstructor
  SourceMap(int i) {}

  private SourceMap(int uninitializedInstanceField, String uninitializedInstanceField2) {
    this(2);
  }

  private int initializedInstanceField = 2;

  static double uninitializedStaticField;

  // Initializer in a static block
  static {
    uninitializedStaticField = 10.0;
  }

  static String initializedStaticField = "Hello";

  static {
    if (uninitializedStaticField == 10.0) {
      initializedStaticField = "World";
    }
  }

  private int testStatements(int a, int b, int times, int number) {
    int value = 0;
    for (int i = 0; i < times; i++) {
      value++;
    }

    if (number % 2 == 0) {
      value += 1;
    } else {
      value += 2;
    }

    int b2 = b;
    while (b2 > 0 && b2 < 100) {
      b2 -= 10;
    }

    value += a + b2;

    outerLoop: // Label for the outer loop
    for (int i = 0; i <= 2; i++) {
      switch (number) {
        case 1:
          value += 5;
          break;
        case 2:
          value += 2;
          break outerLoop;
        case 3:
          break outerLoop;
      }
      if (i == 1) {
        break;
      }
    }
    return value;
  }

  private void testLambdaAndMethodReference(int n) {
    Function<Integer, Integer> f = i -> i + 1;
    Supplier<Integer> f2 = new ArrayList()::size;
    Supplier<Integer> f3 = this::simpleMethod;
  }

  private int simpleMethod() {
    return 1;
  }

  private void testLocalClass() {
    class LocalClass {}
    new LocalClass();
  }

  enum Enum1 {
    VALUE1,
    VALUE2(),
    VALUE3(1);

    Enum1() {}

    Enum1(int i) {}
  }
}
