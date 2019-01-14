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
package com.google.j2cl.transpiler.integration.unaryexpressions;

import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

/** Test unary operations. */
public class Main {
  public static void main(String[] args) {
    testBasicUnaryExpressions();
  }

  public static void testBasicUnaryExpressions() {
    int a = 10;
    assertTrue(a == 10);

    int b = a++;
    assertTrue(a == 11);
    assertTrue(b == 10);

    int c = a--;
    assertTrue(a == 10);
    assertTrue(c == 11);

    int d = ++a;
    assertTrue(a == 11);
    assertTrue(d == 11);

    int e = --a;
    assertTrue(a == 10);
    assertTrue(e == 10);

    int f = -a;
    assertTrue(f == -10);

    int g = +a;
    assertTrue(g == 10);

    int h = ~a;
    assertTrue(h == ~10);

    boolean i = (a == 100);
    assertTrue(!i);
  }
}
