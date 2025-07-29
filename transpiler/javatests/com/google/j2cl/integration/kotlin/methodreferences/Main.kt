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
package methodreferences

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException
import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testConstructorReferences()
  testInstanceMethodReferences()
  testTopLevelMethodReferences()
  testObjectMethodReferences()
  testQualifierEvaluation()
  testQualifierEvaluation_array()
  testQualifierEvaluation_observeUninitializedField()
  testQualifierEvaluation_implicitObjectMethods()
  testExtensionMethodReferences()
  testGenericMethodReferences()
  testInlineFunctionReferences()
  testVarargs()
  testLocalFunctionReferences()
}

fun interface Function<I, O> {
  fun apply(i: I): O
}

fun interface Producer<T> {
  fun produce(): T
}

fun interface Predicate<T> {
  fun apply(parameter: T): Boolean
}

fun interface Consumer<T> {
  fun accept(parameter: T)
}

fun interface Transform<T, U> {
  fun transform(parameter: T): U
}

fun interface ArrayProducer {
  fun produce(size: Int): IntArray
}

fun interface UnqualifiedSetter<I, V> {
  fun setTo(instance: I, value: V)
}

fun <T> produce(producer: Producer<T>): T = producer.produce()

fun <T> accept(consumer: Consumer<T>, value: T): Unit = consumer.accept(value)

var instanceNumber = 0

class Outer {
  var outerInstanceNumber = instanceNumber++

  class SomeObject {
    val someObjectInstanceNumber = instanceNumber++

    fun isInstance0() = someObjectInstanceNumber == 0

    fun getInstanceNumberString() = "" + someObjectInstanceNumber
  }

  inner class ObjectCapturingOuter {
    val outer: Outer
      get() = this@Outer
  }
}

fun createSomeObject() = Outer.SomeObject()

fun testConstructorReferences() {
  instanceNumber = 0
  val objectFactory = Producer<Outer>(::Outer)
  assertEquals(0, objectFactory.produce().outerInstanceNumber)
  assertEquals(1, objectFactory.produce().outerInstanceNumber)
  val objectFactory2 = Producer<Outer.SomeObject>(Outer::SomeObject)
  assertEquals(2, objectFactory2.produce().someObjectInstanceNumber)
  assertEquals(3, objectFactory2.produce().someObjectInstanceNumber)

  val outer = Outer()
  val objectCapturingOuterProducer =
    Producer<Outer.ObjectCapturingOuter>(outer::ObjectCapturingOuter)
  assertEquals(outer, objectCapturingOuterProducer.produce().outer)

  // TODO(b/228999962): Implement array constructors/access
  // val arrayProducer = ArrayProducer(::IntArray)
  // assertEquals(10, arrayProducer.produce(10).size)
  instanceNumber = 0
  val outer2 = ::Outer.invoke()
  assertTrue(outer2 is Outer)
  assertTrue(instanceNumber == 1)
}

fun testInstanceMethodReferences() {
  // Qualified instance method, make sure that the evaluation of the qualifier only happens once.
  instanceNumber = 0
  val booleanProducer = Producer<Boolean>(Outer.SomeObject()::isInstance0)
  assertTrue(booleanProducer.produce())
  assertTrue(booleanProducer.produce())
  assertFalse(Outer.SomeObject()::isInstance0.invoke())

  // Property getters
  val intProducer = Producer<Int>(Outer.SomeObject()::someObjectInstanceNumber)
  assertTrue(2 == intProducer.produce())
  assertTrue(2 == intProducer.produce())

  val intProducer2 = Producer<Int>(Outer.SomeObject()::someObjectInstanceNumber::get)
  assertTrue(3 == intProducer2.produce())
  assertTrue(4 == Outer.SomeObject()::someObjectInstanceNumber::get.invoke())

  // Property setters
  val instanceWithSetter = Outer()
  val intConsumer = Consumer(instanceWithSetter::outerInstanceNumber::set)
  intConsumer.accept(200)
  assertTrue(200 == instanceWithSetter.outerInstanceNumber)
  instanceWithSetter::outerInstanceNumber::set.invoke(300)
  assertTrue(300 == instanceWithSetter.outerInstanceNumber)

  // Unqualified SomeObject method
  instanceNumber = 0
  val objectPredicate = Predicate<Outer.SomeObject>(Outer.SomeObject::isInstance0)
  assertTrue(objectPredicate.apply(Outer.SomeObject()))
  assertFalse(objectPredicate.apply(Outer.SomeObject()))
  assertFalse(Outer.SomeObject::isInstance0.invoke(Outer.SomeObject()))

  // Unqualified property getter
  instanceNumber = 100
  val objectTransform = Transform<Outer.SomeObject, Int>(Outer.SomeObject::someObjectInstanceNumber)
  assertTrue(100 == objectTransform.transform(Outer.SomeObject()))
  val objectTransform2 =
    Transform<Outer.SomeObject, Int>(Outer.SomeObject::someObjectInstanceNumber::get)
  assertTrue(101 == objectTransform2.transform(Outer.SomeObject()))
  assertTrue(102 == Outer.SomeObject::someObjectInstanceNumber::get.invoke(Outer.SomeObject()))

  // Unqualified property setter
  val unqualifiedSetter = UnqualifiedSetter<Outer, Int>(Outer::outerInstanceNumber::set)
  val outer = Outer()
  unqualifiedSetter.setTo(outer, 2000)
  assertTrue(2000 == outer.outerInstanceNumber)
  Outer::outerInstanceNumber::set.invoke(outer, 3000)
  assertTrue(3000 == outer.outerInstanceNumber)

  // SAM conversions
  instanceNumber = 0
  val instance0 = Outer.SomeObject()
  val instance1 = Outer.SomeObject()
  assertTrue(produce(instance0::isInstance0))
  assertFalse(produce(instance1::isInstance0))
  assertTrue(0 == produce(instance0::someObjectInstanceNumber::get))
  accept(instanceWithSetter::outerInstanceNumber::set, 400)
  assertTrue(400 == instanceWithSetter.outerInstanceNumber)
}

var sequenceNumber = 0

fun returnSequenceAsInt() = sequenceNumber++

fun returnSequenceAsString() = "" + sequenceNumber++

fun testTopLevelMethodReferences() {
  sequenceNumber = 0
  val ref = Producer<Int>(::returnSequenceAsInt)
  assertTrue(0 == ref.produce())
  assertTrue(1 == ref.produce())
  assertTrue(2 == ::returnSequenceAsInt.invoke())

  // Property getters
  sequenceNumber = 123
  val ref2 = Producer<Int>(::sequenceNumber)
  assertTrue(123 == ref2.produce())
  assertTrue(123 == ref2.produce())
  val ref3 = Producer<Int>(::sequenceNumber::get)
  assertTrue(123 == ref3.produce())
  val ref4 = ::sequenceNumber::get
  assertTrue(123 == ref4())

  // Property setters
  val setterRef = Consumer<Int>(::sequenceNumber::set)
  setterRef.accept(200)
  assertTrue(200 == sequenceNumber)
  ::sequenceNumber::set.invoke(300)
  assertTrue(300 == sequenceNumber)

  // SAM conversions
  sequenceNumber = 0
  assertTrue(0 == produce(::returnSequenceAsInt))
  assertTrue(1 == produce(::sequenceNumber))
  assertTrue(1 == produce(::sequenceNumber::get))
  accept(::sequenceNumber::set, 100)
  assertTrue(100 == sequenceNumber)
}

object TestObject {
  fun getValue() = 1234
}

@SuppressWarnings("ClassShouldBeObject")
class TestClassWithCompanion {
  companion object {
    fun getValue() = 1235
  }
}

fun testObjectMethodReferences() {
  val objectRef = Producer<Int>(TestObject::getValue)
  assertTrue(1234 == objectRef.produce())
  assertTrue(1234 == TestObject::getValue.invoke())

  val companionRef = Producer(TestClassWithCompanion::getValue)
  assertTrue(1235 == companionRef.produce())
  val companionRef2 = Producer(TestClassWithCompanion.Companion::getValue)
  assertTrue(1235 == companionRef2.produce())
  // Accessing companion function reference directly from the enclosing class requires parenthesized
  // class name notation. Note this notation is deprecated.
  assertTrue(1235 == (TestClassWithCompanion)::getValue.invoke())
  assertTrue(1235 == TestClassWithCompanion.Companion::getValue.invoke())

  // Anonymous object
  val anonRef =
    Producer<Int>(
      object {
        fun getValue() = 4321
      }::getValue
    )
  assertTrue(4321 == anonRef.produce())
  assertTrue(
    4322 ==
      object {
          fun getValue() = 4322
        }::getValue
        .invoke()
  )
}

/** Tests different qualifier shapes since J2CL optimizes some of these constructs. */
fun testQualifierEvaluation() {
  lateinit var stringProducer: Producer<String>
  var variable = "Hello"
  stringProducer = Producer<String>(variable::lowercase)
  var stringFunRef: () -> String = variable::lowercase
  variable = "GoodBye"
  // Check that the evaluation of variable happens at taking the reference not at the evaluation
  // of the lambda.
  assertEquals("hello", stringProducer.produce())
  assertEquals("hello", stringFunRef.invoke())

  // Field reference.
  class Local(var field: String)
  val local = Local("Hello")
  stringProducer = Producer<String>(local.field::lowercase)
  stringFunRef = local.field::lowercase
  local.field = "GoodBye"
  // Check that the evaluation of variable happens at taking the reference not at the evaluation
  // of the lambda.
  assertEquals("hello", stringProducer.produce())
  assertEquals("hello", stringFunRef.invoke())

  // Static field reference
  assertFalse(isInitialized)
  stringProducer = Producer<String>(ClassWithStaticInitializer.f::getInstanceNumberString)
  // Class initializer should be triggered by just creating the reference.
  assertTrue(isInitialized)

  // Instantiation
  stringProducer = Producer<String>(Outer.SomeObject()::getInstanceNumberString)
  assertEquals(stringProducer.produce(), stringProducer.produce())
  stringFunRef = Outer.SomeObject()::getInstanceNumberString
  assertEquals(stringFunRef.invoke(), stringFunRef.invoke())

  // Method call
  stringProducer = Producer<String>(createSomeObject()::getInstanceNumberString)
  assertEquals(stringProducer.produce(), stringProducer.produce())
  stringFunRef = createSomeObject()::getInstanceNumberString
  assertEquals(stringFunRef.invoke(), stringFunRef.invoke())

  // Binary expression
  stringProducer =
    Producer<String>((returnSequenceAsString() + returnSequenceAsString())::lowercase)
  assertEquals(stringProducer.produce(), stringProducer.produce())
  stringFunRef = (returnSequenceAsString() + returnSequenceAsString())::lowercase
  assertEquals(stringFunRef.invoke(), stringFunRef.invoke())

  // Cast expression
  assertThrowsClassCastException {
    val foo = Producer((returnSequenceAsInt() as Any as String)::toString)
  }
  assertThrowsClassCastException {
    val foo = (returnSequenceAsInt() as Any as String)::toString
  }

  // Array access
  // TODO(b/228999962): Look into array accesses
  // stringProducer = returnsArray()[0]::lowercase;
  // assertEquals(stringProducer.produce(), stringProducer.produce());

  // Ternary conditional
  stringProducer =
    Producer(
      (if (returnSequenceAsInt() > 0) returnSequenceAsString() else returnSequenceAsString())::
        lowercase
    )
  assertEquals(stringProducer.produce(), stringProducer.produce())
  stringFunRef =
    (if (returnSequenceAsInt() > 0) returnSequenceAsString() else returnSequenceAsString())::
      lowercase
  assertEquals(stringFunRef.invoke(), stringFunRef.invoke())

  data class LocalData(val value: Int)
  // Object instantiation
  stringProducer = Producer(LocalData(returnSequenceAsInt())::toString)
  assertEquals(stringProducer.produce(), stringProducer.produce())
  stringFunRef = LocalData(returnSequenceAsInt())::toString
  assertEquals(stringFunRef.invoke(), stringFunRef.invoke())
}

interface InterfaceWithHashcode {
  override fun hashCode(): Int
}

fun testQualifierEvaluation_implicitObjectMethods() {
  val i = object {}
  val stringProducer = Producer<String>(i::toString)
  assertEquals(i.toString(), stringProducer.produce())
  assertEquals(i.toString(), i::toString.invoke())

  val integerProducer = Producer<Int>(i::hashCode)
  assertTrue(i.hashCode() == integerProducer.produce())
  assertTrue(i.hashCode() == i::hashCode.invoke())

  val equalityTester = Function(i::equals)
  assertTrue(equalityTester.apply(i))
  assertFalse(equalityTester.apply(object {}))
  assertTrue(i::equals.invoke(i))
  assertFalse(i::equals.invoke(object {}))

  val interfaceWithHashCode =
    object : InterfaceWithHashcode {
      override fun hashCode() = 0
    }
  val integerProducer2 = Producer<Int>(interfaceWithHashCode::hashCode)
  assertTrue(interfaceWithHashCode.hashCode() == integerProducer2.produce())
  assertTrue(interfaceWithHashCode.hashCode() == interfaceWithHashCode::hashCode.invoke())
}

@SuppressWarnings("ArrayHashCode")
fun testQualifierEvaluation_array() {
  sequenceNumber = 0
  val array = arrayOf(returnSequenceAsString())
  val stringProducer = Producer<String>(array::toString)
  assertEquals(array.toString(), stringProducer.produce())
  assertEquals(array.toString(), array::toString.invoke())

  val integerProducer = Producer<Int>(array::hashCode)
  assertTrue(array.hashCode() == integerProducer.produce())
  assertTrue(array.hashCode() == array::hashCode.invoke())

  val equalityTester = Function(array::equals)
  assertTrue(equalityTester.apply(array))
  assertFalse(equalityTester.apply(arrayOf<String>()))
  assertTrue(array::equals.invoke(array))
  assertFalse(array::equals.invoke(arrayOf<String>()))
}

// TODO(b/228999962): Look into array accesses
// fun returnsArray() = arrayOf(returnSequenceAsString())

var isInitialized = false

@SuppressWarnings("ClassShouldBeObject")
class ClassWithStaticInitializer {
  companion object {
    init {
      isInitialized = true
    }

    val f = Outer.SomeObject()
  }
}

fun testQualifierEvaluation_observeUninitializedField() {
  open class Super {
    init {
      // Call polymorphic init to allow referencing final subclass fields before they are
      // initialized
      initialize()
    }

    open fun initialize() {}
  }

  class Sub : Super() {
    lateinit var stringProducer: Producer<String>
    lateinit var stringRefFun: Function0<String>
    val qualifier = Outer.SomeObject()

    override fun initialize() {
      // `init` runs before property initialization. Construct the method reference before qualifier
      // is initialized.
      stringProducer = Producer<String>(qualifier::getInstanceNumberString)
      stringRefFun = qualifier::getInstanceNumberString
    }
  }

  assertTrue(Sub().stringProducer != null)
  assertThrowsNullPointerException { Sub().stringProducer.produce() }

  assertTrue(Sub().stringRefFun != null)
  assertThrowsNullPointerException { Sub().stringRefFun.invoke() }
}

fun String.testAppendX() = this + "X"

fun String.testAppend(x: String) = this + x

val String.valueWithY: String
  get() = this + "Y"

fun TestClassWithCompanion.Companion.getValueAndX() = "${getValue()}X"

fun Outer.getInstanceNumberExtension() = outerInstanceNumber

fun testExtensionMethodReferences() {
  var variable = "Hello"
  val stringProducer = Producer<String>(variable::testAppendX)
  val stringFunRef = variable::testAppendX
  variable = "GoodBye"
  // Check that the evaluation of variable happens at taking the reference not at the evaluation
  // of the lambda.
  assertEquals("HelloX", stringProducer.produce())
  assertEquals("HelloX", stringFunRef.invoke())

  // Extension method references are only evaluated once.
  instanceNumber = 0
  val intProducer = Producer<Int>(Outer()::getInstanceNumberExtension)
  assertTrue(0 == intProducer.produce())
  assertTrue(0 == intProducer.produce())
  val infFunRef = Outer()::getInstanceNumberExtension
  assertTrue(1 == infFunRef.invoke())
  assertTrue(1 == infFunRef.invoke())

  // With arguments
  val str = "123"
  val stringTransform = Transform<String, String>(str::testAppend)
  assertEquals("123123", stringTransform.transform("123"))
  assertEquals("123123", str::testAppend.invoke("123"))

  // Literals
  val stringProducer2 = Producer<String>("123"::testAppendX)
  assertEquals("123X", stringProducer2.produce())
  assertEquals("123X", "123"::testAppendX.invoke())

  val stringTransform2 = Transform<String, String>(String::testAppendX)
  assertEquals("123X", stringTransform2.transform(str))
  assertEquals("123X", String::testAppendX.invoke(str))

  // Extension properties
  val stringProducer3 = Producer<String>(str::valueWithY)
  assertEquals("123Y", stringProducer3.produce())
  assertEquals("123Y", str::valueWithY::get.invoke())

  val companionRef = Producer<String>(TestClassWithCompanion::getValueAndX)
  assertEquals("1235X", companionRef.produce())
  assertEquals("1235X", TestClassWithCompanion.Companion::getValueAndX.invoke())
}

fun <T, U> transform(transform: Transform<T, U>, value: T): U = transform.transform(value)

fun <T> prependX(value: T) = "X" + value

/** Test type inference with references to generic functions. */
fun testGenericMethodReferences() {
  assertEquals("X1", transform(::prependX, "1"))
  // There is no way to directly reference a generic function without assigning it to a function
  // type. See https://youtrack.jetbrains.com/issue/KT-13003
  // assertEquals("X1", ::prependX.invoke("1"))
}

inline fun inlineFun(): Int {
  return 5
}

inline fun <reified T> castTo(param: Any): T {
  return param as T
}

fun testInlineFunctionReferences() {
  val a = produce(::inlineFun)
  assertTrue(a == 5)
  assertTrue(::inlineFun.invoke() == 5)

  var stringAsAny: Any = "foo"
  val b: String = transform(::castTo, stringAsAny)
  assertEquals("foo", b)
  // There is no way to directly reference a generic function without assigning it to a function
  // type. See https://youtrack.jetbrains.com/issue/KT-13003
  // assertEquals("foo", ::castTo.invoke(stringAsAny))
}

private fun stringCount(vararg strings: String) = strings.size

private fun numberCount(vararg numbers: Number?): Int {
  return numbers.size
}

private fun intCount(vararg integers: Int?): Int {
  return integers.size
}

private fun interface StringStringToIntFunction {
  fun toInt(s1: String, s2: String): Int
}

private fun interface StringArrayToIntFunction {
  fun toInt(strings: Array<String>): Int
}

private fun interface ToIntFunction<T> {
  fun toInt(strings: T): Int
}

fun String.extensionFctWithVarargs(vararg v: String) = this.length + v.size

fun String.extensionFctWithVarargsAndTrailingArgument(
  vararg v: String,
  trailing: String,
  trailing2: Int,
) = this.length + v.size + trailing.length + trailing2

private fun interface StringArrayStringIntToIntFunction {
  fun toInt(strings: Array<String>, s: String, i: Int): Int
}

private fun testVarargs() {
  val fun1 = StringStringToIntFunction(::stringCount)
  assertTrue(fun1.toInt("foo", "bar") == 2)

  val fun2 = StringArrayToIntFunction(::stringCount)
  assertTrue(fun2.toInt(arrayOf("foo", "bar")) == 2)

  var fun3: (Array<out Int?>) -> Int = ::numberCount
  assertTrue(fun3.invoke(arrayOf(1, 2)) == 2)
  fun3 = ::intCount
  assertTrue(fun3.invoke(arrayOf(1, 2)) == 2)

  val fun4 = ::stringCount
  assertTrue(fun4.invoke(arrayOf("foo", "bar")) == 2)

  val str = "foo"
  val extensionFun1 = StringStringToIntFunction(str::extensionFctWithVarargs)
  assertTrue(extensionFun1.toInt("foo", "bar") == 5)

  val extensionFun2 = StringArrayToIntFunction(str::extensionFctWithVarargs)
  assertTrue(extensionFun2.toInt(arrayOf("foo", "bar", "baz")) == 6)

  val extensionFun3 = str::extensionFctWithVarargs
  assertTrue(extensionFun3.invoke(arrayOf("foo")) == 4)

  val extensionFun4 = String::extensionFctWithVarargs
  assertTrue(extensionFun4.invoke("foo", arrayOf("bar", "baz")) == 5)

  val extensionFun11 =
    StringArrayStringIntToIntFunction(str::extensionFctWithVarargsAndTrailingArgument)
  assertTrue(extensionFun11.toInt(arrayOf("foo", "bar"), "baz", 1) == 9)

  val extensionFun13 = str::extensionFctWithVarargsAndTrailingArgument
  assertTrue(extensionFun13.invoke(arrayOf("foo", "bar"), "baz", 2) == 10)

  val extensionFun14 = String::extensionFctWithVarargsAndTrailingArgument
  assertTrue(extensionFun14.invoke("foo2", arrayOf("bar", "baz"), "baz", 3) == 12)
}

fun testLocalFunctionReferences() {
  fun localFunction() = "foo"

  assertEquals("foo", Producer<String>(::localFunction).produce())

  fun String.localExtensionFunction() = this + "bar"
  assertEquals("foobar", Producer<String>("foo"::localExtensionFunction).produce())
}
