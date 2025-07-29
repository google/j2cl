/*
 * Copyright 2019 Google Inc.
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
package jsenum

import java.util.Optional
import jsinterop.annotations.JsEnum
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsProperty

interface Supplier<T> {
  fun get(): T
}

interface Consumer<T> {
  fun accept(t: T)
}

@JsEnum
enum class ComparableJsEnum {
  ZERO,
  ONE,
  TWO;

  fun getValue() = ordinal
}

@JsEnum(hasCustomValue = true)
enum class IntJsEnum(private val value: Int) {
  MINUSONE(-1),
  TWENTY(2 * 10),
  ELEVEN(11);

  fun getValue(): Int = value
}

@JsEnum(hasCustomValue = true)
enum class StringJsEnum(private val value: String?) {
  ONE("ONE"),
  THREE("THREE");

  fun getValue(): String? = value
}

@JsEnum(hasCustomValue = true)
enum class NonNullableStringJsEnum(private val value: String) {
  ONE("ONE"),
  THREE("THREE");

  fun getValue(): String = value
}

@JsEnum(isNative = true, namespace = "jsenum", name = "NonNullableStringJsEnum")
enum class NativeStringEnum {
  ONE,
  THREE,
}

fun testJsEnumSwitch() {
  var comparableJsEnum = if (ComparableJsEnum.ONE.getValue() == 1) ComparableJsEnum.TWO else null
  when (comparableJsEnum) {
    ComparableJsEnum.TWO -> {}
    else -> {}
  }

  val comparable: Comparable<ComparableJsEnum>? = comparableJsEnum
  comparableJsEnum = comparable as ComparableJsEnum

  var intJsEnum = if (IntJsEnum.ELEVEN.getValue() == 10) IntJsEnum.ELEVEN else null
  when (intJsEnum) {
    IntJsEnum.TWENTY -> {}
    else -> {}
  }

  val o: Any? = intJsEnum
  intJsEnum = o as IntJsEnum

  // No boxing here.
  val equal = intJsEnum == IntJsEnum.TWENTY
  var isInstance = intJsEnum is IntJsEnum

  isInstance = intJsEnum is Comparable<*>

  val stringJsEnum = if (StringJsEnum.ONE.getValue() == "10") StringJsEnum.THREE else null
  when (stringJsEnum) {
    StringJsEnum.ONE -> {}
    else -> {}
  }

  NativeStringEnum.ONE.compareTo(NativeStringEnum.THREE)
  NativeStringEnum.ONE.equals(NativeStringEnum.THREE)
  ComparableJsEnum.ONE.compareTo(ComparableJsEnum.ZERO)
  ComparableJsEnum.ONE.equals(ComparableJsEnum.ZERO)

  val supplier: () -> ComparableJsEnum = { ComparableJsEnum.ONE }
  val consummer: (ComparableJsEnum) -> Unit = { e -> e.ordinal }

  acceptsJsFunctionSupplier { ComparableJsEnum.ONE }
  acceptsSupplierOfSupplier { { ComparableJsEnum.ONE } }
  acceptsJsFunctionParameterizedByJsEnum { e: ComparableJsEnum -> e }
}

private fun testExhaustiveJsEnumSwitchExpression() {
  val comparableJsEnum: ComparableJsEnum =
    if (ComparableJsEnum.ONE.getValue() == 1) ComparableJsEnum.TWO else ComparableJsEnum.ZERO
  val i =
    when (comparableJsEnum) {
      ComparableJsEnum.TWO -> 2
      ComparableJsEnum.ONE -> 1
      ComparableJsEnum.ZERO -> 0
    }
}

fun testJsEnumAutoboxingSpecialMethods() {
  val stringJsEnum = StringJsEnum.ONE
  val nullStringJsEnum: StringJsEnum? = null
  val jsEnum = ComparableJsEnum.ONE
  val nullJsEnum: ComparableJsEnum? = null
  val o: Any? = ComparableJsEnum.ONE

  StringJsEnum.ONE.equals(StringJsEnum.THREE)
  StringJsEnum.ONE.equals(stringJsEnum)
  StringJsEnum.ONE.equals(nullStringJsEnum)
  StringJsEnum.ONE.equals(null)
  StringJsEnum.ONE.equals(o)
  o?.equals(StringJsEnum.THREE)

  ComparableJsEnum.ONE.compareTo(ComparableJsEnum.ZERO)
  ComparableJsEnum.ONE.equals(ComparableJsEnum.ZERO)
  ComparableJsEnum.ONE.equals(jsEnum)
  ComparableJsEnum.ONE.equals(nullJsEnum)
  ComparableJsEnum.ONE.equals(null)
  ComparableJsEnum.ONE.equals(o)
  o?.equals(ComparableJsEnum.ZERO)

  StringJsEnum.ONE.equals(jsEnum)

  boxingPassthrough(ComparableJsEnum.ONE).equals(boxingPassthrough(ComparableJsEnum.ONE))
  boxingPassthrough(ComparableJsEnum.ONE).equals(boxingPassthrough(StringJsEnum.ONE))
}

@JsFunction
fun interface JsFunctionSuppiler<T> {
  fun get(): T
}

@JsFunction
fun interface SpecializedJsFunction<T> {
  fun get(value: T): T
}

private fun acceptsJsFunctionSupplier(supplier: JsFunctionSuppiler<ComparableJsEnum>) {}

private fun acceptsSupplierOfSupplier(supplier: () -> (() -> ComparableJsEnum)) {}

private fun acceptsJsFunctionParameterizedByJsEnum(
  supplier: SpecializedJsFunction<ComparableJsEnum>
) {}

fun testReturnsAndParameters() {
  val returnedValue = returnsJsEnum()
  val returnedNullValue = returnsNullJsEnum()
  takesJsEnum(ComparableJsEnum.ONE)
}

fun returnsJsEnum(): ComparableJsEnum = ComparableJsEnum.ONE

fun returnsNullJsEnum(): ComparableJsEnum? = null

fun takesJsEnum(value: ComparableJsEnum) {}

private fun testBoxUnboxWithTypeInference() {
  // Make sure the enum is boxed even when assigned to a field that is inferred to be JsEnum.
  val templatedField = TemplatedField(ComparableJsEnum.ONE)
  var unboxed = templatedField.getValue()
  unboxed = templatedField.value
  templatedField.value = ComparableJsEnum.ONE
  listOf(ComparableJsEnum.ONE)
  templatedField.getValue().ordinal
  val b = ComparableJsEnum.ONE == boxingPassthrough(ComparableJsEnum.ONE)
}

private class TemplatedField<T>(@JvmField var value: T) {
  fun getValue(): T = value
}

private fun <T> boxingPassthrough(t: T): T = t

fun boxingWithGenerics() {
  Foo<Any?>(Optional.of(IntJsEnum.MINUSONE))
}

class Foo<T>(c: Optional<IntJsEnum>)

@JsEnum
enum class JsEnumWithRenamedProperties {
  @JsProperty(name = "NAUGHT") ZERO,
  ONE,
  TWO;

  fun getValue(): Int = ordinal
}

open class SupplierConsumerImpl<T> : Supplier<T>, Consumer<T> {
  override fun accept(t: T) {}

  override fun get(): T = Any() as T
}

interface ComparableJsEnumSupplierConsumer :
  Supplier<ComparableJsEnum>, Consumer<ComparableJsEnum> {
  @JsMethod override fun get(): ComparableJsEnum

  override fun accept(e: ComparableJsEnum)
}

class ComparableJsEnumSupplierConsumerImpl :
  SupplierConsumerImpl<ComparableJsEnum>(), ComparableJsEnumSupplierConsumer

class ComparableJsEnumSupplierConsumerImplWithOverrides :
  SupplierConsumerImpl<ComparableJsEnum>(), ComparableJsEnumSupplierConsumer {
  override fun accept(t: ComparableJsEnum) {}

  @Override override fun get(): ComparableJsEnum = Any() as ComparableJsEnum
}

@JsEnum
enum class SomeJsEnum {
  A
}

private open class BaseVarargs<T>(vararg args: T)

private class SubtypeVarargs : BaseVarargs<SomeJsEnum>(SomeJsEnum.A, SomeJsEnum.A)

// This case doesn't apply in Kotlin there must be an explicit super ctor call.
// private class SubtypeImplicitVarargs : BaseVarargs<SomeJsEnum>

private fun <T> varargsConsumer(vararg args: T): T = args[0]

private fun testVarargs() {
  varargsConsumer(SomeJsEnum.A, SomeJsEnum.A)
  // Kotlin cannot infer the types of the method reference and explicit types are not currently
  // supported: https://youtrack.jetbrains.com/issue/KT-12140
  // val consumer: Consumer<SomeJsEnum> = ::varargsConsumer
}

fun testNonNativeJsEnumArrays() {
  val arr = arrayOf(IntJsEnum.MINUSONE, IntJsEnum.TWENTY)
  val b1 = arr[0] == IntJsEnum.MINUSONE
  val b2 = arr[1] == IntJsEnum.TWENTY
  var obj: Any? = arr[0]
  val v: IntJsEnum = arr[0]

  val arr2 = arrayOfNulls<IntJsEnum?>(2)
  arr2[0] = IntJsEnum.MINUSONE
  arr2[1] = IntJsEnum.TWENTY

  val nestedArr = arrayOf(arrayOf(IntJsEnum.MINUSONE))
  nestedArr[0] = arrayOf(IntJsEnum.TWENTY)

  val arrayWithNull = arrayOf<IntJsEnum?>(null)
  arrayWithNull[0] = null

  val list = arrayListOf<IntJsEnum>()
  obj = list.toArray()

  nonNativeJsEnumVarargs(IntJsEnum.MINUSONE, IntJsEnum.TWENTY)
  nonNativeJsEnumArrayVarargs(arrayOf(IntJsEnum.MINUSONE), arrayOf(IntJsEnum.TWENTY))

  tVarargs(IntJsEnum.MINUSONE, IntJsEnum.TWENTY)
}

fun nonNativeJsEnumVarargs(vararg values: IntJsEnum) {
  val v = values[0]
}

fun nonNativeJsEnumArrayVarargs(vararg values: Array<IntJsEnum>) {
  val v = values[0]
}

fun <T> tVarargs(vararg values: T) {
  val v = values[0]
}

fun testNonNativeStringJsEnumArrays() {
  val arr = arrayOf(StringJsEnum.ONE, StringJsEnum.THREE)
  val b1 = arr[0] == StringJsEnum.ONE
  var obj: Any? = arr[0]
  val v: StringJsEnum = arr[0]

  val arr2 = arrayOfNulls<StringJsEnum?>(2)
  arr2[0] = StringJsEnum.ONE

  val nestedArr = arrayOf(arrayOf(StringJsEnum.ONE))

  val arrayWithNull = arrayOf<StringJsEnum?>(null)
  arrayWithNull[0] = null
}

fun testNativeJsEnumArrays() {
  val arr = arrayOf(NativeStringEnum.ONE, NativeStringEnum.THREE)
  val b1 = arr[0] == NativeStringEnum.ONE

  val arr2 = arrayOfNulls<NativeStringEnum?>(2)
  arr2[0] = NativeStringEnum.ONE

  val nestedArr = arrayOf(arrayOf(NativeStringEnum.ONE))
  nestedArr[0] = arrayOf(NativeStringEnum.THREE)

  val arrayWithNull = arrayOf<NativeStringEnum?>(null)
  arrayWithNull[0] = null
}

var TRUE = true

fun testNonNullOnJsEnum() {
  // Make sure this gets translated as a multiexpression that introduces a variable.
  val x = (if (TRUE) NativeStringEnum.ONE else null)!!
}
