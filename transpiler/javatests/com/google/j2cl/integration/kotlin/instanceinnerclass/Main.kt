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
package instanceinnerclass

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue

/** Test instance inner class. */
open class OuterClass(var fieldInOuter: Int) {
  open inner class InnerClass(var fieldInInner: Int = fieldInOuter * 2)
}

class EnclosingClass(var fieldInEnclosingClass: Int) {
  fun funInEnclosingClass(a: Int): Int {
    return fieldInEnclosingClass * a
  }

  /** Basic instance inner class. */
  inner class A {
    var fieldInA = fieldInEnclosingClass * 2
    var enclosingInstance = this@EnclosingClass

    fun f(): Int {
      return fieldInA * 3
    }
  }

  /** Instance inner class calls outer class's method. */
  open inner class X {
    open fun funInX(a: Int): Int {
      var result = funInEnclosingClass(a)
      result += this@EnclosingClass.funInEnclosingClass(a)
      return result
    }
  }

  /** Instance inner class with constructors and this() call. */
  open inner class B(var fieldInB: Int, var fieldEnclosingClass: EnclosingClass) {
    constructor() : this(fieldInEnclosingClass, this@EnclosingClass)

    fun getBOuter(): EnclosingClass {
      return this@EnclosingClass
    }
  }

  /** Two level nested inner class, with calls to outer class's functions. */
  open inner class C {
    fun funInC(a: Int): Int {
      return a + 11
    }

    inner class CC {
      fun funInCC(a: Int): Int {
        return a + 22
      }

      fun test(a: Int): Int {
        var result = funInEnclosingClass(a)
        result += this@EnclosingClass.funInEnclosingClass(a)
        result += funInC(a)
        result += this@C.funInC(a)
        result += funInCC(a)
        result += this.funInCC(a)
        return result
      }

      var fieldOfC = this@C
      var fieldOfEnclosingClass = this@EnclosingClass
    }
  }

  open inner class W : X() {
    override fun funInX(a: Int): Int {
      return a + 222
    }

    /** Inner class has different super class as outer class. */
    inner class W1 : C() {
      fun test(a: Int): Int {
        var result = super@W.funInX(a)
        result += this@W.funInX(a)
        return result
      }
    }

    /** Inner class has the same super class as outer class. */
    inner class W2 : X() {
      override fun funInX(a: Int): Int {
        return a + 333
      }

      fun test(a: Int): Int {
        var result = super@W.funInX(a) // X.funInX()
        result += this@W.funInX(a) // W.funInX()
        result += funInX(a) // W2.funInX()
        return result
      }
    }

    /** Inner class has its outer class as its super class. */
    inner class W3 : W() {
      override fun funInX(a: Int): Int {
        return a + 444
      }

      fun test(a: Int): Int {
        var result = super@W.funInX(a) // X.funInX
        result += this@W.funInX(a) // W.funInX
        result += funInX(a) // funInX
        val b = super.funInX(a) // W.funInX
        return result + b
      }
    }
  }

  /**
   * Two level nested inner class, with calls to outer class's functions and inherited functions.
   */
  inner class Y : X() {
    override fun funInX(a: Int): Int {
      return a + 44
    }

    fun funInY(a: Int): Int {
      return a + 55
    }

    inner class YY : X() {
      fun test(a: Int): Int {
        var result = funInEnclosingClass(a) // this.outer.outer.funInEnclosingClass()
        result += funInX(a) // this.funInX()
        result += this.funInX(a) // this.funInX()
        result += this@Y.funInX(a) // this.outer.funInX()
        return result
      }
    }
  }

  /** Test inner class that extends enclosing class */
  open inner class Z {
    open fun funInZ(a: Int): Int {
      return a + 66
    }

    inner class ZZ : Z() {
      override fun funInZ(a: Int): Int {
        return a + 77
      }

      fun test(a: Int): Int {
        var result = funInZ(a) // this.funInZ(), a + 77
        result += this.funInZ(a) // this.funInZ(), a + 77
        result += this@Z.funInZ(a) // this.outer.funInZ(), a + 66
        return result
      }
    }
  }

  /**
   * Test inner class whose parent is also an inner class, and shares the same enclosing instance.
   */
  inner class Child : B {
    var fieldInChild: Int

    constructor(x: Int, y: Int) : super(x, EnclosingClass(y)) {
      fieldInChild = fieldInB + fieldEnclosingClass.fieldInEnclosingClass // x + y
    }

    constructor() {
      // fieldInEnclosingClass * 2;
      fieldInChild = fieldInB + fieldEnclosingClass.fieldInEnclosingClass
    }
  }

  /**
   * Test inner class whose parent is also an inner class, and shares the same enclosing instance,
   * but the super() call is called by another instance.
   */
  inner class Child2 : B() {
    // In kotlin'super' is not an expression, it can only be used on the left-hand side of a dot
    // public Child2() {
    //   new EnclosingClass(30).super();
    // }
    fun getChild2Outer(): EnclosingClass {
      return this@EnclosingClass
    }
  }

  /**
   * Test inner class without any constructors, and whose parent is also an inner class, and they
   * share the same enclosing instance.
   */
  inner class D : B() {
    var fieldInD: Int = fieldEnclosingClass.fieldInEnclosingClass
  }

  // In kotlin 'super' is not an expression, it can only be used on the left-hand side of a dot
  // The instance from outer class can be different from the instance of child instance,
  // and super can be dispatched to the constructor of either
  // /**
  //  * Test inner class whose parent is also an inner class, and does not the same enclosing
  //  * instance.
  //  */
  // public class AnotherChild extends OuterClass.InnerClass {
  //   public int fieldInChild;
  //   public AnotherChild() {
  //     new OuterClass(10).super();
  //     fieldInChild = fieldInInner * 3; //10 * 2 * 3 = 60;
  //   }
  // }
}

interface X {
  fun m(): Int = 999
}

internal interface InterfaceWithDefault {
  fun m() = "InterfaceWithDefault.m from " + origin()

  fun origin(): String
}

open class ASuperClass(open var s: String) {
  open fun m() = "SuperClass.m from $s"
}

open class AnOuterClass(override var s: String) : ASuperClass(s) {
  override fun m() = "OuterClass.m from $s"

  inner class InnerClass(override var s: String) : AnOuterClass(s), InterfaceWithDefault {
    override fun origin() = s

    override fun m() = "InnerClass.m"

    fun superM() = super<AnOuterClass>.m()

    fun superMFromOuterClass() = super@AnOuterClass.m()

    fun superDefaultMethodFromInnerClass() = super<InterfaceWithDefault>.m()

    fun mFromOuterClass() = this@AnOuterClass.m()
  }
}

open class AnOuterClassExtendingInterfaceWithDefault(override var s: String) :
  ASuperClass(s), InterfaceWithDefault {
  override fun m() = "OuterClass.m from $s"

  override fun origin() = s

  inner class InnerClass(override var s: String) : ASuperClass(s), InterfaceWithDefault {
    override fun origin() = s

    override fun m() = "InnerClass.m"

    fun superM() = super<ASuperClass>.m()

    fun superMFromOuterClass() = super<ASuperClass>@AnOuterClassExtendingInterfaceWithDefault.m()

    fun superDefaultMethodFromInnerClass() = super<InterfaceWithDefault>.m()

    fun superDefaultMethodFromOuterClass() =
      super<InterfaceWithDefault>@AnOuterClassExtendingInterfaceWithDefault.m()
  }
}

fun main(vararg unused: String) {
  val m = EnclosingClass(2)
  assertTrue(m.A().f() == 12)
  assertTrue(m.A().enclosingInstance === m)

  val mm = EnclosingClass(20)
  val b = mm.B()
  val bb = mm.B(1, m)
  assertTrue(b.fieldInB == 20)
  assertTrue(b.fieldEnclosingClass === mm)
  assertTrue(bb.fieldInB == 1)
  assertTrue(bb.fieldEnclosingClass === m)

  val c = m.C()
  assertTrue(c.CC().fieldOfC === c)
  assertTrue(c.CC().fieldOfEnclosingClass === m)

  assertTrue(m.D().fieldInD == 2)
  assertTrue(m.Child().fieldInChild == 4)
  assertTrue(m.Child(10, 20).fieldInChild == 30)
  // In kotlin 'super' is not an expression, it can only be used on the left-hand side of a dot
  // assertTrue(m.AnotherChild().fieldInChild == 60)

  val c2 = m.Child2()
  assertTrue(c2.getChild2Outer().fieldInEnclosingClass == 2)
  // In kotlin 'super' is not an expression, it can only be used on the left-hand side of a dot
  // assertTrue(c2.getBOuter().fieldInEnclosingClass == 30)

  assertTrue(m.X().funInX(2) == 8)
  assertTrue(m.C().CC().test(8) == 130)
  assertTrue(m.Y().YY().test(8) == 132)
  assertTrue(m.Z().ZZ().test(8) == 244)
  assertTrue(m.W().W1().test(8) == 262)
  assertTrue(m.W().W2().test(8) == 603)
  assertTrue(m.W().W3().test(8) == 944)

  val outerClass = AnOuterClass("Outer")
  val innerClass = outerClass.InnerClass("Super")
  assertEquals(innerClass.m(), "InnerClass.m")
  assertEquals(innerClass.superM(), "OuterClass.m from Super")
  assertEquals(innerClass.superMFromOuterClass(), "SuperClass.m from Outer")
  assertEquals(innerClass.superDefaultMethodFromInnerClass(), "InterfaceWithDefault.m from Super")
  assertEquals(innerClass.mFromOuterClass(), "OuterClass.m from Outer")

  val outerClassExtendingInterfaceWithDefault = AnOuterClassExtendingInterfaceWithDefault("Outer")
  val innerClass2 = outerClassExtendingInterfaceWithDefault.InnerClass("Super")
  assertEquals(innerClass2.m(), "InnerClass.m")
  assertEquals(innerClass2.superM(), "SuperClass.m from Super")
  assertEquals(innerClass2.superMFromOuterClass(), "SuperClass.m from Outer")
  assertEquals(innerClass2.superDefaultMethodFromInnerClass(), "InterfaceWithDefault.m from Super")
  assertEquals(innerClass2.superDefaultMethodFromOuterClass(), "InterfaceWithDefault.m from Outer")
}
