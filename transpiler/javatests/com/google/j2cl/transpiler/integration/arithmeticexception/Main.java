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
package com.google.j2cl.transpiler.integration.arithmeticexception;

import static com.google.j2cl.transpiler.utils.Asserts.fail;

public class Main {
  public static void main(String... args) {
    testDivideByZero();
    testModByZero();
  }

  private static void testDivideByZero() {
    try {
      int a = 10;
      int b = 0;
      @SuppressWarnings("unused")
      int c = a / b;
      fail("failed to throw ArithmeticException");
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      @SuppressWarnings("unused")
      int a = 10;
      int b = 0;
      a /= b;
      fail("failed to throw ArithmeticException");
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      @SuppressWarnings("unused")
      int a = 10;
      int b = 0;
      int c = 1 + a / b;
      fail("failed to throw ArithmeticException");
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      short a = 10;
      short b = 0;
      @SuppressWarnings("unused")
      short c = (short) (a / b);
      fail("failed to throw ArithmeticException");
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      char a = 10;
      char b = 0;
      @SuppressWarnings("unused")
      char c = (char) (a / b);
      fail("failed to throw ArithmeticException");
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      byte a = 10;
      byte b = 0;
      @SuppressWarnings("unused")
      byte c = (byte) (a / b);
      fail("failed to throw ArithmeticException");
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      long a = 10;
      long b = 0;
      @SuppressWarnings("unused")
      long c = (a / b);
      fail("failed to throw ArithmeticException");
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      int a = 10;
      int b = 0;
      @SuppressWarnings("unused")
      long c = 10L + (a / b);
      fail("failed to throw ArithmeticException");
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      short a = 10;
      short b = 0;
      @SuppressWarnings("unused")
      long c = 10L + (a / b);
      fail("failed to throw ArithmeticException");
    } catch (ArithmeticException e) {
      // do nothing.
    }
  }

  private static void testModByZero() {
    try {
      int a = 10;
      int b = 0;
      @SuppressWarnings("unused")
      int c = a % b;
      fail("failed to throw ArithmeticException");
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      @SuppressWarnings("unused")
      int a = 10;
      int b = 0;
      a %= b;
      fail("failed to throw ArithmeticException");
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      short a = 10;
      short b = 0;
      @SuppressWarnings("unused")
      short c = (short) (a % b);
      fail("failed to throw ArithmeticException");
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      char a = 10;
      char b = 0;
      @SuppressWarnings("unused")
      char c = (char) (a % b);
      fail("failed to throw ArithmeticException");
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      byte a = 10;
      byte b = 0;
      @SuppressWarnings("unused")
      byte c = (byte) (a % b);
      fail("failed to throw ArithmeticException");
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      long a = 10;
      long b = 0;
      @SuppressWarnings("unused")
      long c = (a % b);
      fail("failed to throw ArithmeticException");
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      int a = 10;
      int b = 0;
      @SuppressWarnings("unused")
      long c = 10L + (a % b);
      fail("failed to throw ArithmeticException");
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      short a = 10;
      short b = 0;
      @SuppressWarnings("unused")
      double c = 10d + (a % b);
      fail("failed to throw ArithmeticException");
    } catch (ArithmeticException e) {
      // do nothing.
    }
  }
}
