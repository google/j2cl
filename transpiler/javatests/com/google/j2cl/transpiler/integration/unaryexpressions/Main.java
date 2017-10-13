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

/**
 * Test unary operations.
 */
public class Main {
  public static void main(String[] args) {
    {
      int a = 10;
      assert a == 10;

      int b = a++;
      assert a == 11;
      assert b == 10;

      int c = a--;
      assert a == 10;
      assert c == 11;

      int d = ++a;
      assert a == 11;
      assert d == 11;

      int e = --a;
      assert a == 10;
      assert e == 10;

      int f = -a;
      assert f == -10;

      int g = +a;
      assert g == 10;

      int h = ~a;
      assert h == ~10;

      boolean i = (a == 100);
      assert !i;
    }

    {
      Double d = null;
      try {
        d = +d;
        assert false : "Should have thrown NPE";
      } catch (NullPointerException expected) {
      }
    }

    {
      Boolean b = null;
      try {
        b = !b;
        assert false : "Should have thrown NPE";
      } catch (NullPointerException expected) {
      }
    }

    {
      Integer n = null;
      try {
        n = -n;
        assert false : "Should have thrown NPE";
      } catch (NullPointerException expected) {
      }
    }
  }
}
