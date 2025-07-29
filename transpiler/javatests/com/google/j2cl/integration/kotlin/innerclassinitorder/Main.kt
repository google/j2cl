/*
 * Copyright 2023 Google Inc.
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
package innerclassinitorder

import com.google.j2cl.integration.testing.Asserts.assertTrue

/** Smoke test for inner classes, copied from GWT. */
class Main {
  var number = 0

  open class Base {
    open fun polymorph() {}
  }

  open class OuterRefFromSuperCtorBase(o: Base) : Base() {
    init {
      o.polymorph()
    }
  }

  inner class OuterRefFromSuperCtorCall :
    OuterRefFromSuperCtorBase(
      object : Base() {
        override fun polymorph() {
          number += 100
        }
      }
    )

  inner class OuterRefFromThisCtorCall(o: Base) : OuterRefFromSuperCtorBase(o) {
    constructor() :
      this(
        object : Base() {
          override fun polymorph() {
            number += 1000
          }
        }
      )
  }

  fun testOuterThisFromSuperCall() {
    OuterRefFromSuperCtorCall()
    assertTrue(number == 100)
  }

  fun testOuterThisFromThisCall() {
    OuterRefFromThisCtorCall()
    assertTrue(number == 1100)
  }

  inner class InnerClass {
    init {
      callInner()
    }

    fun callInner() {
      number += 1
      class ReallyInnerClass {
        init {
          callReallyInner()
        }

        fun callReallyInner() {
          number += 10
        }
      }
      ReallyInnerClass()
    }
  }

  open class P1<T1>(val value: Int) {
    open inner class P2<T2>(i: Int) : P1<T1>(i) {

      inner class P3<T3>(i: Int) : P1<T1>.P2<T2>(i) {

        // TODO(b/242791935): Enable after kotlin bug is fixed
        // constructor(): this(1)

      }

      // constructor(): this(1)

    }

    constructor() : this(1)
  }

  /** Used in test [testExtendsNested] */
  private class ESOuter {
    open inner class ESInner {
      var value: Int

      constructor() {
        value = 1
      }

      constructor(value: Int) {
        this.value = value
      }
    }

    fun newESInner(): ESInner {
      return ESInner()
    }

    fun newESInner(value: Int): ESInner {
      return ESInner(value)
    }
  }

  /** Used in test [testExtendsNestedWithGenerics] */
  private class ESWGOuter<T> {
    open inner class ESWGInner {
      var value: Int

      constructor() {
        value = 1
      }

      constructor(value: Int) {
        this.value = value
      }
    }

    fun newESWGInner(): ESWGInner {
      return ESWGInner()
    }

    fun newESWGInner(value: Int): ESWGInner {
      return ESWGInner(value)
    }
  }

  fun testExtendsNested() {
    val o = ESOuter()

    assertTrue(1 == o.ESInner().value)
    assertTrue(2 == o.ESInner(2).value)
    assertTrue(1 == o.newESInner().value)
    assertTrue(2 == o.newESInner(2).value)
  }

  /** Test for Issue 7789 */
  fun testExtendsNestedWithGenerics() {
    val o = ESWGOuter<String>()
    assertTrue(1 == o.ESWGInner().value)
    assertTrue(2 == o.ESWGInner(2).value)
    assertTrue(1 == o.newESWGInner().value)
    assertTrue(2 == o.newESWGInner(2).value)
  }

  fun testInnerClassCtors() {
    val p1: P1<*> = P1<Any>()
    assertTrue(1 == p1.value)
    assertTrue(2 == P1<Any>(2).value)
    // TODO(b/242791935): Enable after kotlin bug is fixed
    // assertTrue(P2<*> = p1.P2<Any>())
    // assertTrue(1 == p2.value)
    assertTrue(2 == p1.P2<Any>(2).value)
    // TODO(b/242791935): Enable after kotlin bug is fixed
    // assertTrue(1 == p2.P3<Any>().value)
    // assertTrue(2 == p2.P3<Any>(2).value)
  }

  fun testInnerClassInitialization() {
    InnerClass()
    assertTrue(number == 1111)
  }

  fun testInnerClassLoop() {
    abstract class AddNumber(var num: Int) {
      abstract fun act()
    }

    val results =
      Array<AddNumber>(10) {
        object : AddNumber(it) {
          override fun act() {
            number += num
          }
        }
      }
    for (r in results) {
      r.act()
    }
    assertTrue(number == 1156)
  }

  open class Outer(protected val value: Int) {
    open inner class OuterIsNotSuper {
      fun getValue(): Int {
        return value
      }
    }

    inner class OuterIsSuper(i: Int) : Outer(i) {
      override fun checkDispatch(): Int {
        return 2
      }

      fun checkDispatchFromSub1(): Int {
        return super.checkDispatch()
      }

      fun checkDispatchFromSub2(): Int {
        return object : Outer(1) {
            fun go(): Int {
              return super@OuterIsSuper.checkDispatch()
            }
          }
          .go()
      }

      fun unqualifiedAlloc(): OuterIsNotSuper {
        return OuterIsNotSuper()
      }
    }

    inner class TestUnqualifiedSuperCall : OuterIsNotSuper()

    open fun checkDispatch(): Int {
      return 1
    }
  }

  private val outer = Outer(1)

  private val outerIsSuper = outer.OuterIsSuper(2)

  fun testOuterIsNotSuper() {
    val x = outerIsSuper.OuterIsNotSuper()
    assertTrue(2 == x.getValue())
  }

  fun testSuperDispatch() {
    assertTrue(1 == outerIsSuper.checkDispatchFromSub1())
    assertTrue(1 == outerIsSuper.checkDispatchFromSub2())
  }

  fun testUnqualifiedAlloc() {
    val x = outerIsSuper.unqualifiedAlloc()
    assertTrue(2 == x.getValue())
  }

  fun testUnqualifiedSuperCall() {
    val x = outerIsSuper.TestUnqualifiedSuperCall()
    assertTrue(2 == x.getValue())
  }
}

fun main(vararg unused: String) {
  val m = Main()
  m.testOuterThisFromSuperCall()
  m.testOuterThisFromThisCall()

  m.testExtendsNested()
  m.testExtendsNestedWithGenerics()
  m.testInnerClassCtors()
  m.testInnerClassInitialization()
  m.testInnerClassLoop()

  m.testOuterIsNotSuper()
  m.testSuperDispatch()
  m.testUnqualifiedAlloc()
  m.testUnqualifiedSuperCall()
}
