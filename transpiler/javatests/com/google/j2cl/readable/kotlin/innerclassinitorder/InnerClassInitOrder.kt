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
package innerclassinitorder

/** Smoke test for inner classes, copied from GWT. */
class InnerClassInitOrder {
  var number = 0

  open class OuterRefFromSuperCtorBase(o: Any) {
    init {
      o.hashCode()
    }
  }

  inner class OuterRefFromSuperCtorCall :
    OuterRefFromSuperCtorBase(
      object : Any() {
        override fun hashCode(): Int {
          number += 100
          return 0
        }
      }
    )

  inner class OuterRefFromThisCtorCall(o: Any) : OuterRefFromSuperCtorBase(o) {
    constructor() :
      this(
        object : Any() {
          override fun hashCode(): Int {
            number += 1000
            return 0
          }
        }
      )
  }

  fun testOuterThisFromSuperCall() {
    OuterRefFromSuperCtorCall()
    val a = number == 100
  }

  fun testOuterThisFromThisCall() {
    OuterRefFromThisCtorCall()
    val a = number == 1100
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
    val a = 1 == o.ESInner().value
    val b = 2 == o.ESInner(2).value
    val a2 = 1 == o.newESInner().value
    val b2 = 2 == o.newESInner(2).value
  }

  /** Test for Issue 7789 */
  fun testExtendsNestedWithGenerics() {
    val o = ESWGOuter<String>()
    val a = 1 == o.ESWGInner().value
    val b = 2 == o.ESWGInner(2).value
    val a2 = 1 == o.newESWGInner().value
    val b2 = 2 == o.newESWGInner(2).value
  }

  fun testInnerClassCtors() {
    val p1: P1<*> = P1<Any>()
    val a = 1 == p1.value
    val b = 2 == P1<Any>(2).value
    // P2 and P3 class is commented out above.
    // val p2: P2<*> = p1.P2<Any>()
    // val c = 1 == p2.value
    val d = 2 == p1.P2<Any>(2).value
    // val e = 1 == p2.P3<Any>().value
    // val f = 2 == p2.P3<Any>(2).value
  }

  fun testInnerClassInitialization() {
    InnerClass()
    val a = number == 1111
  }

  fun testInnerClassLoop() {
    abstract class AddNumber(var num: Int) {
      abstract fun act()
    }

    val results = arrayOfNulls<AddNumber>(10)
    for (i in 0..9) {
      val ap: AddNumber =
        object : AddNumber(i) {
          override fun act() {
            number += num
          }
        }
      results[i] = ap
    }
    for (theAp in results) {
      theAp!!.act()
    }
    val a = number == 1156
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
    val a = 2 == x.getValue()
  }

  fun testSuperDispatch() {
    val a = 1 == outerIsSuper.checkDispatchFromSub1()
    val b = 1 == outerIsSuper.checkDispatchFromSub2()
  }

  fun testUnqualifiedAlloc() {
    val x = outerIsSuper.unqualifiedAlloc()
    val a = 2 == x.getValue()
  }

  fun testUnqualifiedSuperCall() {
    val x = outerIsSuper.TestUnqualifiedSuperCall()
    val a = 2 == x.getValue()
  }
}

fun main(args: Array<String>) {
  val m = InnerClassInitOrder()
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
