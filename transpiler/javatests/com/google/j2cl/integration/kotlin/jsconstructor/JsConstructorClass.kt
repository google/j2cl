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
package jsconstructor

import jsinterop.annotations.JsConstructor

class JsConstructorClass {
  /** A regular class (no JsConstructor), with two constructors. */
  open class A {
    var fA: Int = 1

    constructor(x: Int) {
      this.fA = x
    }

    constructor() {}
  }

  /** A class with JsConstructor, which extends a regular class. */
  open class B @JsConstructor constructor(x: Int) : A(x + 1) {
    var fB: Int = 2

    init {
      fB += 3
    }

    constructor() : this(10) { // must call this(int).
      fB += 4
    }

    constructor(x: Int, y: Int) : this(x + y) {
      fB += x * y
    }
  }

  /** A class with JsConstructor, which extends a JsConstructor class. */
  open class C @JsConstructor constructor(x: Int) :
    B(x * 2) { // must call super(int), cannot call super().
    var fC: Int = 1

    init {
      fC += 6
    }

    constructor(x: Int, y: Int) : this(x + y) { // must call this(int).
      fC += 7
    }
  }
}
