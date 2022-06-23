/*
 * Copyright 2017 Google Inc.
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
package arithmeticexception;

import static com.google.j2cl.integration.testing.Asserts.assertThrowsArithmeticException;

public class Main {
  public static void main(String... args) {
    testDivideByZero();
    testModByZero();
  }

  private static void testDivideByZero() {
    assertThrowsArithmeticException(
        () -> {
          int a = 10;
          int b = 0;
          int unused = a / b;
        });

    assertThrowsArithmeticException(
        () -> {
          int unused = 10;
          int b = 0;
          unused /= b;
        });

    assertThrowsArithmeticException(
        () -> {
          int a = 10;
          int b = 0;
          int unused = 1 + a / b;
        });

    assertThrowsArithmeticException(
        () -> {
          short a = 10;
          short b = 0;
          short unused = (short) (a / b);
        });

    assertThrowsArithmeticException(
        () -> {
          char a = 10;
          char b = 0;
          char unused = (char) (a / b);
        });

    assertThrowsArithmeticException(
        () -> {
          byte a = 10;
          byte b = 0;
          @SuppressWarnings("unused")
          byte unused = (byte) (a / b);
        });

    assertThrowsArithmeticException(
        () -> {
          long a = 10;
          long b = 0;
          long unused = (a / b);
        });

    assertThrowsArithmeticException(
        () -> {
          int a = 10;
          int b = 0;
          long unused = 10L + (a / b);
        });

    assertThrowsArithmeticException(
        () -> {
          short a = 10;
          short b = 0;
          long unused = 10L + (a / b);
        });
  }

  private static void testModByZero() {
    assertThrowsArithmeticException(
        () -> {
          int a = 10;
          int b = 0;
          int unused = a % b;
        });

    assertThrowsArithmeticException(
        () -> {
          int unused = 10;
          int b = 0;
          unused %= b;
        });

    assertThrowsArithmeticException(
        () -> {
          short a = 10;
          short b = 0;
          short unused = (short) (a % b);
        });

    assertThrowsArithmeticException(
        () -> {
          char a = 10;
          char b = 0;
          char unused = (char) (a % b);
        });

    assertThrowsArithmeticException(
        () -> {
          byte a = 10;
          byte b = 0;
          byte unused = (byte) (a % b);
        });

    assertThrowsArithmeticException(
        () -> {
          long a = 10;
          long b = 0;
          long unused = (a % b);
        });

    assertThrowsArithmeticException(
        () -> {
          int a = 10;
          int b = 0;
          long unused = 10L + (a % b);
        });

    assertThrowsArithmeticException(
        () -> {
          short a = 10;
          short b = 0;
          double unused = 10d + (a % b);
        });
  }
}
