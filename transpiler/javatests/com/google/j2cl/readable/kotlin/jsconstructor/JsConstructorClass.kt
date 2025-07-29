/*
 * Copyright 2022 Google Inc.
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
import jsinterop.annotations.JsType

/**
 * Restrictions on JsConstructor:
 * 1. A class can have only one JsConstructor.
 * 2. Other constructors must delegate to the JsConstructor.
 *
 * Restrictions on subclass that extends a class with JsConstructor: (Including the implicit default
 * constructor and default super call)
 * 1. There is one and only one constructor that does not delegate to any other constructor.
 * 2. The super() call in the delegated constructor must invoke the delegated constructor in its
 *    super class.
 * 3. Other constructors must delegate to the delegated constructor.
 *
 * Test scenarios:
 * ```
 * ChildClass\ParentClass   RegularClass    JsConstructorSubClass   JsConstructorClass
 * RegularClass             normal case     N/A                     N/A
 * JsConstructorSubClass    N/A             class E                 class C
 * JsConstructorClass       class B         class F                 class D
 * ```
 */

/** A regular class (no JsConstructor), with two constructors. */
open class A() {
  var fA: Int = 1

  constructor(x: Int) : this() {
    fA = x
  }
}

/** A class with JsConstructor, which extends a regular class. */
open class B : A {
  var fB: Int = 2

  @JsConstructor
  constructor(x: Int) : super(x + 1) {
    fB = 3
  }

  constructor() : this(10) {
    fB = 4
  }

  constructor(x: Int, y: Int) : this(x + y) {
    fB = x * y
  }
}

open class C : B {
  var fC: Int = 1

  @JsConstructor
  // must call super(int), cannot call super().
  constructor(x: Int) : super(x * 2) {
    fC = 6
  }

  // must call this(int)
  constructor(x: Int, y: Int) : this(x + y) {
    fC = 7
  }
}

class E : C {
  var fE: Int = 11

  @JsConstructor
  // must call super(int), cannot call super(int, int).
  constructor() : super(10) {
    fE = 12
  }
}

/** A class with JsConstructor, which extends a JsConstructor class. */
class D : B {
  var fD: Int = 8

  @JsConstructor
  // must call super(int), cannot call super().
  constructor() : super(9) {
    fD = 10
  }

  // must call this().
  constructor(x: Int) : this() {
    fD = x
  }
}

/** A JsConstructor class, which extends a subclass of a JsConstructor class. */
class F : C {
  var fF: Int = 13

  @JsConstructor
  // must call super(int), cannot call super(int, int).
  constructor(x: Int) : super(x + 2) {
    fF = x + 3
  }
}

/** JsType class with default constructor. */
@JsType open class G

/** Subclass of a JsType class with default constructor. */
class H @JsConstructor constructor() : G()

class Outer {
  /** Subclass of a JsConstructor that captures an enclosing class. */
  inner class I @JsConstructor constructor() : G()
}

open class Varargs @JsConstructor constructor(vararg args: Int) : A(args[1])

class SubVarargs @JsConstructor constructor(i: Any, vararg args: Int) : Varargs(*args) {
  constructor(j: Int) : this(Object(), j)

  companion object {
    fun subNativeInvocation() {
      val unusedS1 = SubVarargs(2)
      val unusedS2 = SubVarargs(Object(), 1, 2, 3)
    }
  }
}

open class RegularType(b: Any)

class JsConstructorSubtypeOfRegularType @JsConstructor constructor(obj: Any) : RegularType(obj) {
  constructor() : this(Object())
}

class JsConstructorClass {
  inner class InstanceVarargs @JsConstructor constructor(vararg args: Int) : A(args[1])
}

open class JsConstructorClassWithExplicitConstructor {
  @JsConstructor constructor(i: Int) {}
}

class JsConstructorSubclass : JsConstructorClassWithExplicitConstructor {
  @JsConstructor
  constructor() : super(1) {

    // Adds a case in which the variable `i` needs to be hoisted to the top scope, and since it
    // is a constructor it might end up being defined before the call to super, which is
    // what is being tested here.
    do {
      var i = 0
    } while (i == 0)
  }
}
