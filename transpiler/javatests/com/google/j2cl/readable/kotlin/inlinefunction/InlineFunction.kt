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
package inlinefunction

class MyClass(var f: Int) {
  fun addAndReturn(i: Int): Int {
    f += i
    return f
  }
}

inline fun topLevelInlineFunction(myClass: MyClass, action: (Int) -> Int): Int {
  return action(myClass.f)
}

inline fun <T> doSomethingOn(target: T, block: (T) -> Unit) = block(target)

class ClassWithInlineFun(var e: Int) {
  inline fun inlineFun(action: (Int) -> Int): Int {
    var sum = e
    while (e > 0) {
      sum += action(e--)
      if (sum > 10) {
        return sum
      }
    }

    return sum
  }

  inline fun inlineFunWithAnonymousObject(captureParam: Int): Int {
    val capture = 5
    val delegate =
      object {
        fun compute(): Int {
          return (e + capture + captureParam) * 2
        }
      }
    return delegate.compute()
  }

  inline fun <T> inlineFunWithAnonymousObjectAndTypeParam(t: T): T {
    val delegate =
      object {
        fun compute(): T {
          return t
        }
      }
    return delegate.compute()
  }
}

inline fun MyClass.extensionInlineFunction(action: (Int) -> Int): Int {
  var sum = f
  while (f > 0) {
    sum += action(f--)
  }

  return sum
}

inline fun MyClass.extensionInlineFunctionNoReturn(action: (Int) -> Int) {
  f = action(f)
}

fun testInlining() {
  val a = topLevelInlineFunction(MyClass(2)) { it * 2 }
  val b = ClassWithInlineFun(5).inlineFun { it * 3 }
  val c = ClassWithInlineFun(5).inlineFunWithAnonymousObject(5)
  val d = ClassWithInlineFun(5).inlineFunWithAnonymousObjectAndTypeParam(5)
  val e = MyClass(5).extensionInlineFunction { it * 4 }
  MyClass(5).extensionInlineFunctionNoReturn { it * 5 }
  MyClass(5).extensionInlineFunction { if (it < 10) return else it * 4 }
  MyClass(5).extensionInlineFunction { if (it < 10) return@extensionInlineFunction 0 else it * 4 }
}

fun testInliningWithPrivateMemberCall() {
  ExternalClassWithInlineFun(2).inlineFun { it }
}

fun testInliningWithLocalClass() {
  val d =
    topLevelInlineFunction(MyClass(2)) {
      class Foo(val f: Int) {
        fun mulByTwo() = f * 2
      }
      Foo(it).mulByTwo()
    }
}

fun testFunctionRef() {
  val foo = MyClass(2)
  topLevelInlineFunction(foo, foo::addAndReturn)

  // TODO(b/405183980): Uncomment when this doesn't crash the frontend.
  // doSomethingOn(MyClass(2)) {
  //   topLevelInlineFunction(it, it::addAndReturn)
  // }
}

inline fun <reified T> castTo(obj: Any?): T = obj as T

inline fun <reified T> instanceOf(obj: Any?): Boolean = obj is T

inline fun <reified T> instanceOf(i: Int): Boolean = i is T

fun testReifiedParameter() {
  var aStringAsAny: Any = "foo"
  var someInt: Int = 1

  val a = castTo<String>(aStringAsAny)
  val b = castTo<Error>(aStringAsAny)
  val c = instanceOf<Double>(1)
  val d = instanceOf<Double>(++someInt)
  val e = instanceOf<Double>(aStringAsAny)
  val f = reifiedInlineFunInAnotherLib<String>("string")
}

inline fun inlineFunWithNoInlineParam(noinline notInlined: () -> Int): Any {
  return notInlined
}

fun testNoInline() {
  val intProducer = inlineFunWithNoInlineParam { 5 } as () -> Int
  val a = intProducer.invoke()
}

fun acceptMyClass(myClass: MyClass) {}

inline var myClass: MyClass
  get() = MyClass(12)
  set(v) = acceptMyClass(v)

fun testInlineProperty() {
  val a = myClass
  myClass = MyClass(5)
}

fun testInlineFunctionFromAnotherLibrary() {
  val a = inlineFun(null, "foo")
  val b = inlineFun("gar", "foo")
  val c = callTopLevelInlineFunctionOnlyReferencedButDeclaredInAnotherFile()

  val i = HolderInAnotherLibrary(42).getPlusOne()
}

fun testInlineFunctionWithLambda() {
  val a = funWithLambda(null) { "foo" }
  val b = funWithLambda("bar") { "foo" }
  val c = funWithNestedInlineFunAndLambda(null)
  val d = funWithNestedInlineFunAndLambda("buzz")
  val e = funWithNestedInlineFunAndLambdaWithInlineFunctionCall(null) { "foo" }
}

fun testNestedInlineFunction() {
  val a = inlineFunWithNestedInlineFun(null)
  val b = inlineFunWithNestedInlineFun("bar")
}

fun testMixingInlineAndNonInline() {
  val a = callsFunThatIsNeverDirectlyReferenced()
  val b = callsAccessorThatIsNeverDirectlyReferenced()
  val c = callAnotherCompanionInlineFunction()
  val d = callAnotherInstanceInlineFunction()
  val e = callNestedInlineFunctions()
  val f = ClassWithCompanion(Any()).instanceInlineFunDirectlyInvoked()
  val g = callInstanceInlineFunctionFromAnotherFile()
  val h = callNotReferencedExtensionProperty()
  val i = callAmbiguousFunction()
}

// Tests a pattern that is used in the library to make a non functional interface appear as if it
// is a functional interface
class Container<T>(val t: T)

interface NonFun<T> {
  fun container(): Container<T>
}

inline fun <T> NonFun(crossinline container: () -> Container<T>): NonFun<T> =
  object : NonFun<T> {
    override fun container() = container()
  }

private fun useAdaptor() {
  var capturedValue = "Initial"

  val nf = NonFun {
    capturedValue = "Modified by lambda in NonFun"
    Container(MyClass(12))
  }

  val s = Sequence { arrayOf(1).iterator() }
}

private fun testNonReifiedIntTypeParameterErasureInInlineFun(): Int = castIntToObj<Int>() + 1

private inline fun <R> castIntToObj(): R = 1 as R

inline fun inlineFunWithDefaults(a: Int, b: Int = 2): Int = a + b

inline fun inlineFunWithDefaultsAndVarargs(a: Int, b: Int = 2, vararg c: Int): Int = a + b + c.sum()

inline fun inlineFunWithDefaultsAndDefaultVarargs(
  a: Int,
  b: Int = 2,
  vararg c: Int = intArrayOf(3, 4, 5),
) = a + b + c.sum()

fun testDefaultParams() {
  inlineFunWithDefaults(1)
  inlineFunWithDefaults(1, 20)

  inlineFunWithDefaultsAndVarargs(1)
  inlineFunWithDefaultsAndVarargs(1, 20)
  inlineFunWithDefaultsAndVarargs(1, 20, 3, 4, 5)

  inlineFunWithDefaultsAndDefaultVarargs(1)
  inlineFunWithDefaultsAndDefaultVarargs(1, 20)
  inlineFunWithDefaultsAndDefaultVarargs(1, 20, 30, 40, 50)
}

// This example reproduces a J2CL bug where a local variable declared inside an inlined
// generic function is incorrectly typed with a generic type parameter (`N`) instead of its
// concrete type argument (`String`).
internal abstract class Repro<N> {
  inline fun get(): N? {
    var value: N? = null
    return value
  }
}

internal abstract class StringRepro : Repro<String>()

internal abstract class StringReproChild : StringRepro()

internal abstract class ParametrizedReproChild<T> : Repro<T>()

internal abstract class StringParametrizedReproChild : ParametrizedReproChild<String>()

internal abstract class ParametrizedContainerReproChild<T> : Repro<Container<T>>()

internal fun <S : Repro<String>> S.find0(): String? {
  return get()
}

internal fun StringRepro.find1(): String? {
  return get()
}

internal fun StringReproChild.find2(): String? {
  return get()
}

internal fun <S : StringReproChild> S.find3(): String? {
  return get()
}

internal fun <S : ParametrizedReproChild<String>> S.find4(): String? {
  return get()
}

internal fun <S : ParametrizedContainerReproChild<String>> S.find5(): String? {
  return get()?.t
}

internal fun <S : StringParametrizedReproChild> S.find6(): String? {
  return get()
}

internal fun <T : ParametrizedReproChild<String>, S : T> S.find7(t: T): String? {
  return get()
}
