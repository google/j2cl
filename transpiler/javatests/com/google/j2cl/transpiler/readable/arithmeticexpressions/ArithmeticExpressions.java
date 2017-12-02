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
package com.google.j2cl.transpiler.readable.arithmeticexpressions;

public class ArithmeticExpressions {
  public void main() {
    int a = 10;
    int b = a++;
    int c = a--;
    int d = ++a;
    int e = --a;
    int f = -a;
    int g = +a;
    int h = ~a;
    int i = 1 + 1 + 2 - 5;
    int j = (1 + 2) * (3 + 4);
    boolean k = !(1 + 2 + 3 == 4);
    boolean l = (1 + 2 != 4);
    boolean m = Long.MAX_VALUE != 9223372036854776833d;
    getLongArray()[0]++;
    double o = (5 / 2) - 0.0;

    Integer boxI = 3;
    a += boxI;

    a = a << 31L;
    a <<= 1L;
    a += 1L;

    ((k)) |= true;
  }

  static long[] getLongArray() {
    return new long[10];
  }
}
