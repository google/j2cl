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
package cast

class CastGenerics<T, E : Number?> {
  var field: T? = null

  fun method(): T? {
    return null
  }

  internal interface A {
    fun mA()
  }

  internal interface B {
    fun mB()
  }

  private abstract class BaseImplementor : A, B

  private class Implementor private constructor() : BaseImplementor() {
    override fun mA() {}

    override fun mB() {}
  }

  private class Container<T> {
    fun get(): T {
      return Any() as T
    }
  }

  private fun <T, U : T> testErasureCast() where T : A, T : B {
    var str = CastGenerics<String, Number>().field
    str = CastGenerics<String, Number>().method()

    val containerT: Container<T> = Container()
    containerT.get().mA()
    containerT.get().mB()

    val containerU: Container<U> = Container()
    containerU.get().mA()
    containerU.get().mB()

    val containerArrT: Container<Array<T>> = Container()
    val arrT = containerArrT.get()
    arrT[0].mA()
    arrT[0].mB()

    var arrA: Array<out A> = containerArrT.get()
    var arrB: Array<out B> = containerArrT.get()

    val containerArrU: Container<Array<U>> = Container()
    val arrU = containerArrU.get()
    arrU[0].mA()
    arrU[0].mB()

    arrA = containerArrU.get()
    arrB = containerArrU.get()

    val containerBase: Container<BaseImplementor> = Container()
    containerBase.get().mA()
    containerBase.get().mB()

    val containerImplementor: Container<Implementor> = Container()
    containerImplementor.get().mA()
    containerImplementor.get().mB()

    val strictlyA: Container<A> = Container()
    var oA: Any? = strictlyA.get()
    var a: A = strictlyA.get()

    val extendsA: Container<out A> = Container()
    oA = extendsA.get()
    a = extendsA.get()

    val superA: Container<in A> = Container()
    oA = superA.get()

    val strictlyString: Container<String> = Container()
    var s: String = strictlyString.get()

    val extendsString: Container<out String> = Container()
    s = extendsString.get()

    val superString: Container<in String> = Container()
    var o: Any? = superString.get()

    val nullableValue: Container<Implementor?> = Container()
    val i: Implementor = nullableValue.get()!!
  }

  fun testCastToParameterizedType() {
    val o: Any = 1
    val cc = o as CastGenerics<java.lang.Error?, Number?>
    val cc2 = o as CastGenerics<*, *>
  }

  fun testCastToTypeVariable() {
    val o: Any = 1
    val e = o as E? // cast to type variable with bound
    val t = o as T? // cast to type variable without bound
    val es = o as Array<E>? // cast to array of type variable.
    val ts = o as Array<T>?
  }

  fun <S, V : Enum<V>?> testCastToMethodTypeVariable() {
    val o: Any = 1
    val s = o as S // cast to type variable declared by the method.
    var c: Any? = o as CastGenerics<S, Number?>?
    c = o as Array<S>?
    c = o as V?
  }

  /**
   * This method tests that J2CL correctly sets the Generic to its bound inside a method since
   * closure compiler cannot handle it.
   */
  fun <TT : Error?> outerGenericMethod() {
    class Nested<SS> {
      private fun nestedGenericMethod(o: Any) {
        val t = o as TT?
      }
    }
  }

  interface Empty1

  interface Empty2<TT>

  fun <EE> method(o: Any?): EE? where EE : Empty1?, EE : Empty2<EE>? {
    if (o is Empty1) {
      return o as EE?
    } else {
      return null
    }
  }

  open class Foo<V>

  fun <T> doSomething(): Foo<T> {
    return object : Foo<T>() {}
  }

  class A1 : Empty1

  class A2 : Empty2<String>

  class C : Empty1, Empty2<String>

  fun testSafeCastToTypeVariable() {
    val a = safeCastToTypeWithPrimitiveUpperBound<Int>(1)
    val b = safeCastToTypeWithPrimitiveUpperBound<Int>(Any())

    val c = safeCastToTypeWithUpperBound<A1>(Any())
    val d = safeCastToTypeWithUpperBound<A1>(object : Empty1 {})
    val e = safeCastToTypeWithUpperBound<A1>(A1())
    val f = safeCastToTypeWithUpperBound<A1>(A2())

    val g = safeCastWithUnboundType<A1>(Any())
    val h = safeCastWithUnboundType<A1>(object : Empty1 {})
    val i = safeCastWithUnboundType<A1>(A1())

    val j = safeCastToIntersectionType<C>(A1())
    val k = safeCastToIntersectionType<C>(A2())
    val l = safeCastToIntersectionType<C>(C())
  }

  private fun <T : Int> safeCastToTypeWithPrimitiveUpperBound(a: Any) = a as? T

  private fun <T : Empty1> safeCastToTypeWithUpperBound(a: Any) = a as? T

  private fun <T> safeCastWithUnboundType(a: Any) = a as? T

  private fun <T> safeCastToIntersectionType(o: Any): T? where T : Empty1, T : Empty2<String> =
    (o as? T)
}
