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
package jsconstructor;

import jsinterop.annotations.JsConstructor;

public class JsConstructorClass {
  /**
   * A regular class (no JsConstructor), with two constructors.
   */
  public static class A {
    public int fA = 1;

    public A(int x) {
      this.fA = x;
    }

    public A() {}
  }

  /**
   * A class with JsConstructor, which extends a regular class.
   */
  public static class B extends A {
    public int fB = 2;

    @JsConstructor
    public B(int x) {
      super(x + 1);
      this.fB += 3;
    }

    public B() {
      this(10); // must call this(int).
      this.fB += 4;
    }

    public B(int x, int y) {
      this(x + y);
      this.fB += x * y;
    }
  }

  /** A class with JsConstructor, which extends a JsConstructor class. */
  public static class C extends B {
    public int fC = 1;

    @JsConstructor
    public C(int x) {
      super(x * 2); // must call super(int), cannot call super().
      this.fC += 6;
    }

    public C(int x, int y) {
      this(x + y); // must call this(int);
      this.fC += 7;
    }
  }
}
