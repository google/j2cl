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
import jsinterop.annotations.JsType;

/**
 * Restrictions on JsConstructor:
 *
 * <p>1. A class can have only one JsConstructor.
 * <p>2. Other constructors must delegate to the JsConstructor.
 *
 * <p>Restrictions on subclass that extends a class with JsConstructor:
 *
 * <p>(Including the implicit default constructor and default super call)
 * <p>1. There is one and only one constructor that does not delegate to any other constructor.
 * <p>2. The super() call in the delegated constructor must invoke the delegated constructor in its
 * super class.
 * <p>3. Other constructors must delegate to the delegated constructor.
 *
 * <p>Test scenarios:
 * ChildClass\ParentClass   RegularClass    JsConstructorSubClass       JsConstructorClass
 * RegularClass                normal case      N/A                         N/A
 * JsConstructorSubClass        N/A             class E                     class C
 * JsConstructorClass           class B         class F                     class D
 */
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
      this.fB = 3;
    }

    public B() {
      this(10); // must call this(int).
      this.fB = 4;
    }

    public B(int x, int y) {
      this(x + y);
      this.fB = x * y;
    }
  }

  /**
   * A regular class (no JsConstructor), which extends a JsConstructor class.
   */
  public static class C extends B {
    public int fC = 1;

    @JsConstructor
    public C(int x) {
      super(x * 2); // must call super(int), cannot call super().
      this.fC = 6;
    }

    public C(int x, int y) {
      this(x + y); // must call this(int);
      this.fC = 7;
    }
  }

  /**
   * A class with JsConstructor, which extends a JsConstructor class.
   */
  public static class D extends B {
    public int fD = 8;

    @JsConstructor
    public D() {
      super(9); // must call super(int), cannot call super().
      this.fD = 10;
    }

    public D(int x) {
      this(); // must call this().
      this.fD = x;
    }
  }

  /**
   * A regular class (no JsConstructor), which extends a subclass of a JsConstructor class.
   */
  public static class E extends C {
    public int fE = 11;

    @JsConstructor
    public E() {
      super(10); // must call super(int), cannot call super(int, int).
      this.fE = 12;
    }
  }

  /**
   * A JsConstructor class, which extends a subclass of a JsConstructor class.
   */
  public static class F extends C {
    public int fF = 13;

    @JsConstructor
    public F(int x) {
      super(x + 2); // must call super(int), cannot call super(int, int).
      this.fF = x + 3;
    }
  }

  /**
   * JsType class with default constructor.
   */
  @JsType
  public static class G {}

  /** Subclass of a JsType class with default constructor. */
  public static class H extends G {
    @JsConstructor
    public H() {}
  }

  public static class Varargs extends A {
    @JsConstructor
    public Varargs(int... args) {
      super(args[1]);
    }
  }

  public static class SubVarargs extends Varargs {
    @JsConstructor
    public SubVarargs(Object i, int... args) {
      super(args);
    }

    public SubVarargs(int j) {
      this(new Object(), j);
    }

    static void subNativeInvocation() {
      SubVarargs unusedS1 = new SubVarargs(2);
      SubVarargs unusedS2 = new SubVarargs(new Object(), 1, 2, 3);
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  public class InstanceVarargs extends A {
    @JsConstructor
    public InstanceVarargs(int... args) {
      super(args[1]);
    }
  }

  public static class RegularType {
    public RegularType(Object b) {}
  }

  public static class JsConstructorSubtypeOfRegularType extends RegularType {
    @JsConstructor
    public JsConstructorSubtypeOfRegularType(Object object) {
      super(object);
    }

    public JsConstructorSubtypeOfRegularType() {
      this(new Object());
    }
  }
}
