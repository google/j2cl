/*
 * Copyright 2018 Google Inc.
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
@file:Suppress("UNCHECKED_CAST")

package jsenum

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertThrows
import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.Asserts.assertUnderlyingTypeEquals
import com.google.j2cl.integration.testing.Asserts.fail
import com.google.j2cl.integration.testing.TestUtils
import java.io.Serializable
import java.util.TreeSet
import java.util.function.Function
import java.util.function.Supplier
import javaemul.internal.annotations.DoNotAutobox
import javaemul.internal.annotations.UncheckedCast
import jsinterop.annotations.JsConstructor
import jsinterop.annotations.JsEnum
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsOverlay
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType
import kotlin.js.definedExternally

val OK_STRING: Any? = "Ok"
val HELLO_STRING: Any? = "Hello"
val ONE_DOUBLE: Any? = 1.0
val FALSE_BOOLEAN: Any? = false

fun main(vararg unused: String) {
  testNativeJsEnum()
  testNativeJsEnumWithMissingValues()
  testStringNativeJsEnum()
  testCastOnNative()
  testComparableJsEnum()
  testStringJsEnum()
  testStringJsEnumAsSeenFromJs()
  testJsEnumClassInitialization()
  testNativeEnumClassInitialization()
  testDoNotAutoboxJsEnum()
  testUnckeckedCastJsEnum()
  testAutoBoxing_relationalOperations()
  testAutoBoxing_typeInference()
  testAutoBoxing_specialMethods()
  testAutoBoxing_parameterizedLambda()
  // Kotlin doesn't support intersection casts
  // testAutoBoxing_intersectionCasts()
  testSpecializedSuperType()
  testBoxingPartialInlining()
  testNonNativeJsEnumArrays()
  testNonNativeStringJsEnumArrays()
  testNativeJsEnumArray()
  testJsEnumVarargs()
}

@JsEnum(isNative = true, namespace = "test")
enum class NativeEnum {
  @JsProperty(name = "OK") ACCEPT,
  CANCEL,
}

private fun testNativeJsEnum() {
  val v: NativeEnum = NativeEnum.ACCEPT
  when (v) {
    NativeEnum.ACCEPT -> {}
    NativeEnum.CANCEL -> fail()
    else -> fail()
  }

  assertTrue(v === NativeEnum.ACCEPT)
  assertTrue(v !== NativeEnum.CANCEL)
  // Native JsEnums are not boxed.
  assertTrue(v === OK_STRING)
  assertTrue(v === (StringNativeEnum.OK as Any?))

  // No boxing
  val o: Any? = NativeEnum.ACCEPT
  assertTrue(o === NativeEnum.ACCEPT)

  // Object methods calls on a variable of JsEnum type.
  assertTrue(v.hashCode() === NativeEnum.ACCEPT.hashCode())
  assertTrue(v.hashCode() !== NativeEnum.CANCEL.hashCode())
  assertTrue(v.hashCode() === StringNativeEnum.OK.hashCode())
  assertTrue(v.toString().equals(OK_STRING))
  assertTrue(v.equals(NativeEnum.ACCEPT))
  assertTrue(v.equals(OK_STRING))
  assertTrue(v.equals(StringNativeEnum.OK))

  // Kotlin equality on a variable of JsEnum type.
  assertTrue(v == NativeEnum.ACCEPT)
  assertTrue(v == OK_STRING)
  // Kotlin doesn't allow equality of incompatible types, so users cannot write:
  // v == StringNativeEnum.OK

  // Object methods calls on a variable of Object type.
  assertTrue(o!!.hashCode() === NativeEnum.ACCEPT.hashCode())
  assertTrue(o!!.hashCode() !== NativeEnum.CANCEL.hashCode())
  assertTrue(o!!.hashCode() === StringNativeEnum.OK.hashCode())
  assertTrue(o!!.toString() == OK_STRING)
  assertTrue(o!!.equals(NativeEnum.ACCEPT))
  assertTrue(o!!.equals(OK_STRING))
  assertTrue(o!!.equals(StringNativeEnum.OK))

  // Kotlin equality on a variable of Object type.
  assertTrue(o == NativeEnum.ACCEPT)
  assertTrue(o == OK_STRING)
  assertTrue(o == StringNativeEnum.OK)

  assertFalse(v is Enum<*>)
  assertTrue((v as Any?) is String)
  assertTrue(v is Comparable<*>)
  assertTrue(v is Serializable)
  assertFalse((v as Any?) is PlainJsEnum)

  val ne: NativeEnum = o as NativeEnum
  val sne: StringNativeEnum = o as StringNativeEnum
  var ce: Comparable<*> = o as Comparable<*>
  // Kotlin doesn't support intersection casts
  // ce = o as (NativeEnum & Comparable<NativeEnum>)
  val s: Serializable = o as Serializable
  assertThrowsClassCastException(
    {
      val unused = o as Enum<*>
    },
    Enum::class.java,
  )
  assertThrowsClassCastException(
    {
      val unused = o as Boolean
    },
    Boolean::class.javaObjectType,
  )

  assertTrue(asSeenFromJs(NativeEnum.ACCEPT) === OK_STRING)
}

@JsEnum(isNative = true, namespace = "test", name = "NativeEnum")
enum class NativeEnumWitMissingValues {
  OK
}

private fun testNativeJsEnumWithMissingValues() {
  try {
    val e = NativeEnum.CANCEL as Any? as NativeEnumWitMissingValues
    val i =
      when (e) {
        NativeEnumWitMissingValues.OK -> 1
      }
    fail()
  } catch (expected: RuntimeException) {
    // Kotlin throws kotlin.NoWhenBranchMatchedException, but it is not public and will be
    // deprecated.
    assertTrue(expected.message!!.contains("exhaustive"))
  }
}

@JsMethod(name = "passThrough") private external fun asSeenFromJs(s: Any?): Any?

@JsEnum(isNative = true, namespace = "test", name = "NativeEnum", hasCustomValue = true)
enum class StringNativeEnum {
  OK,
  CANCEL;

  private val value: String = definedExternally

  @JsOverlay fun getValue(): String = value
}

private fun testStringNativeJsEnum() {
  val v: StringNativeEnum = StringNativeEnum.OK
  when (v) {
    StringNativeEnum.OK -> {}
    StringNativeEnum.CANCEL -> fail()
    else -> fail()
  }

  assertTrue(v === StringNativeEnum.OK)
  assertTrue(v !== StringNativeEnum.CANCEL)
  assertTrue(v as Any? === OK_STRING)
  assertTrue(v === NativeEnum.ACCEPT as Any?)

  val o: Any? = StringNativeEnum.OK
  assertTrue(o === StringNativeEnum.OK)

  // Object methods calls on a variable of JsEnum type.
  assertTrue(v.hashCode() === StringNativeEnum.OK.hashCode())
  assertTrue(v.hashCode() !== StringNativeEnum.CANCEL.hashCode())
  assertTrue(v.toString() == OK_STRING)
  assertTrue(v.equals(StringNativeEnum.OK))
  assertTrue(v.equals(OK_STRING))

  // Kotlin equality on a variable of JsEnum type.
  assertTrue(v == StringNativeEnum.OK)
  assertTrue(v == OK_STRING)
  // Kotlin doesn't allow equality of incompatible types, so users cannot write:
  // v == NativeEnum.ACCEPT

  // Object methods calls on a variable of Object type.
  assertTrue(o!!.hashCode() === StringNativeEnum.OK.hashCode())
  assertTrue(o!!.hashCode() !== StringNativeEnum.CANCEL.hashCode())
  assertTrue(o!!.toString() == OK_STRING)
  assertTrue(o!!.equals(StringNativeEnum.OK))
  assertTrue(o!!.equals(NativeEnum.ACCEPT))
  assertTrue(o!!.equals(OK_STRING))

  // Kotlin equality on a variable of Object type.
  assertTrue(o == StringNativeEnum.OK)
  assertTrue(o == NativeEnum.ACCEPT)
  assertTrue(o == OK_STRING)

  assertTrue(v.getValue() == v.toString())
  assertTrue(v.getValue() == OK_STRING)

  assertFalse(v is Enum<*>)
  assertTrue((v as Any?) is String)
  assertTrue(v is Comparable<*>)
  assertTrue(v is Serializable)
  assertFalse((v as Any?) is PlainJsEnum)

  val se: Serializable = o as Serializable
  val sne: StringNativeEnum = o as StringNativeEnum
  val ne: NativeEnum = o as NativeEnum
  val ce: Comparable<*> = o as Comparable<*>

  // Kotlin doesn't support intersection casts
  // val seAndC: Comparable<StringNativeEnum> =  o as (StringNativeEnum &
  // Comparable<StringNativeEnum>)
  // assertUnderlyingTypeEquals(String::class.java, seAndC)

  assertThrowsClassCastException(
    {
      val unused: Any? = o as Enum<*>
    },
    Enum::class.java,
  )
  assertThrowsClassCastException(
    {
      val unused: Any? = o as Boolean
    },
    Boolean::class.javaObjectType,
  )

  assertTrue(asSeenFromJs(StringNativeEnum.OK) === OK_STRING)
}

@JsEnum(isNative = true, namespace = "test", name = "NativeEnumOfNumber", hasCustomValue = true)
enum class NumberNativeEnum {
  ONE,
  TWO;

  @JvmField internal val value: Int = definedExternally
}

fun testCastOnNative() {
  castToNativeEnum(NativeEnum.ACCEPT)
  castToNativeEnum(StringNativeEnum.OK)
  castToNativeEnum(NumberNativeEnum.ONE)
  castToNativeEnum(PlainJsEnum.ONE)
  castToNativeEnum(OK_STRING)
  castToNativeEnum(2.0 as Double?)
  castToNativeEnum(1 as Int?)

  castToStringNativeEnum(StringNativeEnum.OK)
  castToStringNativeEnum(NativeEnum.ACCEPT)
  castToStringNativeEnum(OK_STRING)
  assertThrowsClassCastException { castToStringNativeEnum(NumberNativeEnum.ONE) }
  assertThrowsClassCastException { castToStringNativeEnum(PlainJsEnum.ONE) }
  assertThrowsClassCastException { castToStringNativeEnum(1 as Int?) }
  assertThrowsClassCastException { castToStringNativeEnum(2.0 as Double?) }

  castToNumberNativeEnum(NumberNativeEnum.ONE)
  castToNumberNativeEnum(2.0 as Double?)
  assertThrowsClassCastException { castToNumberNativeEnum(NativeEnum.ACCEPT) }
  assertThrowsClassCastException { castToNumberNativeEnum(StringNativeEnum.OK) }
  assertThrowsClassCastException { castToNumberNativeEnum(PlainJsEnum.ONE) }
  assertThrowsClassCastException { castToNumberNativeEnum(1 as Int?) }
  assertThrowsClassCastException { castToNumberNativeEnum(OK_STRING) }
}

private fun castToNativeEnum(o: Any?): NativeEnum {
  return o as NativeEnum
}

private fun castToStringNativeEnum(o: Any?): StringNativeEnum {
  return o as StringNativeEnum
}

private fun castToNumberNativeEnum(o: Any?): NumberNativeEnum {
  return o as NumberNativeEnum
}

@JsMethod(name = "passThrough") private external fun asSeenFromJs(s: StringNativeEnum): Any?

@JsEnum
enum class PlainJsEnum {
  @JsProperty(name = "NAUGHT") ZERO,
  ONE,
  TWO,
  THREE,
  FOUR,
  FIVE,
  SIX,
  SEVEN,
  EIGHT,
  NINE,
  TEN;

  fun getValue(): Int = ordinal
}

@JsEnum
enum class OtherPlainJsEnum {
  NONE,
  UNIT,
}

private fun testComparableJsEnum() {
  val v: PlainJsEnum = PlainJsEnum.ONE
  when (v) {
    PlainJsEnum.ZERO -> fail()
    PlainJsEnum.ONE -> {}
    else -> fail()
  }

  assertTrue(v === PlainJsEnum.ONE)
  assertTrue(v !== PlainJsEnum.ZERO)
  assertTrue((v as Any?) !== ONE_DOUBLE)
  // Boxing preserves equality.
  val o: Any? = PlainJsEnum.ONE
  assertTrue(o === PlainJsEnum.ONE)

  // Object methods calls on a variable of JsEnum type.
  assertTrue(v.hashCode() === PlainJsEnum.ONE.hashCode())
  assertTrue(v.hashCode() !== PlainJsEnum.ZERO.hashCode())
  assertTrue(v.toString() == ONE_DOUBLE.toString())
  assertTrue(v.equals(PlainJsEnum.ONE))
  assertFalse(v.equals(ONE_DOUBLE))
  assertFalse(PlainJsEnum.ZERO.equals(OtherPlainJsEnum.NONE))

  assertThrows(NullPointerException::class.java) {
    val nullJsEnum: PlainJsEnum? = null
    nullJsEnum!!.equals(PlainJsEnum.ZERO)
  }

  // Kotlin equality on a variable of JsEnum type.
  assertTrue(v == PlainJsEnum.ONE)
  assertFalse(v == ONE_DOUBLE)
  // Kotlin doesn't allow equality of incompatible types, so users cannot write:
  // PlainJsEnum.ZERO == OtherPlainJsEnum.NONE

  // Object methods calls on a variable of Object type.
  assertTrue(o!!.hashCode() === PlainJsEnum.ONE.hashCode())
  assertTrue(o!!.hashCode() !== PlainJsEnum.ZERO.hashCode())
  assertTrue(o!!.toString() == ONE_DOUBLE.toString())
  assertTrue(o!!.equals(PlainJsEnum.ONE))
  assertFalse(o!!.equals(PlainJsEnum.TWO))
  assertTrue(o!!.equals(v))
  assertFalse(o!!.equals(ONE_DOUBLE))

  // Kotlin equality on a variable of Object type.
  assertTrue(o == PlainJsEnum.ONE)
  assertFalse(o == PlainJsEnum.TWO)
  assertTrue(o == v)
  assertFalse(o == ONE_DOUBLE)

  assertTrue(v.getValue() == 1)
  assertTrue(v.ordinal == 1)
  assertTrue(PlainJsEnum.ONE.compareTo(v) == 0)
  assertTrue(PlainJsEnum.ZERO.compareTo(v) < 0)
  assertThrows(NullPointerException::class.java) {
    val nullJsEnum: PlainJsEnum? = null
    nullJsEnum!!.compareTo(PlainJsEnum.ZERO)
  }
  assertThrowsClassCastException {
    val comparable = PlainJsEnum.ONE as Comparable<Any?>
    comparable.compareTo(OtherPlainJsEnum.UNIT)
  }
  assertThrowsClassCastException {
    val comparable = PlainJsEnum.ONE as Comparable<Any?>
    comparable.compareTo(ONE_DOUBLE)
  }
  assertThrowsClassCastException {
    val comparable = ONE_DOUBLE as Comparable<Any?>
    comparable.compareTo(PlainJsEnum.ONE)
  }

  // Kotlin doesn't support intersection casts
  // assertThrowsClassCastException(
  //   {
  //     val unused = PlainJsEnum.ONE as (Enum<PlainJsEnum> & Comparable<PlainJsEnum>)
  //   },
  //   Enum::class.java)

  // Test that boxing of special method 'ordinal()' call is not broken by normalization.
  val i: Int? = v.ordinal
  assertTrue(i?.toInt() == 1)

  if (!TestUtils.isWasm()) {
    // JsEnums are still instance of Enum in Wasm.
    assertFalse(v is Enum<*>)
  }
  assertTrue(v is PlainJsEnum)
  assertFalse(v as Any? is Double)
  assertTrue(v is Comparable<*>)
  assertTrue(v is Serializable)

  assertFalse(Any() is PlainJsEnum)
  assertFalse(ONE_DOUBLE as Any? is PlainJsEnum)

  // Kotlin doesn't support intersection casts
  // val pe = o as PlainJsEnum
  // val c = o as Comparable<*>
  // val s = o as Serializable
  //
  // Intersection casts box/or unbox depending on the destination type.
  // val otherC: Comparable<PlainJsEnum> = o as (PlainJsEnum & Comparable<PlainJsEnum>)
  // assertUnderlyingTypeEquals(PlainJsEnum::class.java, otherC)
  // val otherPe: PlainJsEnum = o as (PlainJsEnum & Comparable<PlainJsEnum>)
  // assertUnderlyingTypeEquals(Double::class.javaObjectType, otherPe)

  assertThrowsClassCastException(
    {
      val unused = o as Enum<*>
    },
    Enum::class.java,
  )
  assertThrowsClassCastException(
    {
      val unused = o as Double
    },
    Double::class.javaObjectType,
  )

  assertTrue(asSeenFromJs(PlainJsEnum.ONE) == ONE_DOUBLE)

  // Comparable test.
  val sortedSet = TreeSet(Comparable<Any?>::compareTo)
  sortedSet.add(PlainJsEnum.ONE as Comparable<Any?>)
  sortedSet.add(PlainJsEnum.ZERO as Comparable<Any?>)
  assertTrue(sortedSet.iterator().next() == PlainJsEnum.ZERO)
  assertTrue(sortedSet.iterator().next() is PlainJsEnum)
}

@JsMethod(name = "passThrough") private external fun asSeenFromJs(d: PlainJsEnum): Any?

@JsEnum(hasCustomValue = true)
enum class StringJsEnum(internal val value: String) {
  HELLO("Hello"),
  GOODBYE("Good Bye"),
}

private fun testStringJsEnum() {
  val v: StringJsEnum = StringJsEnum.HELLO
  when (v) {
    StringJsEnum.GOODBYE -> fail()
    StringJsEnum.HELLO -> {}
    else -> fail()
  }

  assertTrue(v === StringJsEnum.HELLO)
  assertTrue(v !== StringJsEnum.GOODBYE)
  assertTrue(v as Any? !== HELLO_STRING)
  // Boxing preserves equality.
  val o: Any? = StringJsEnum.HELLO
  assertTrue(o === StringJsEnum.HELLO)

  // Object methods calls on a variable of JsEnum type.
  assertTrue(v.hashCode() === StringJsEnum.HELLO.hashCode())
  assertTrue(v.hashCode() !== StringJsEnum.GOODBYE.hashCode())
  assertTrue(v == StringJsEnum.HELLO)
  assertFalse(v == HELLO_STRING)

  assertThrows(NullPointerException::class.java) {
    val nullJsEnum: StringJsEnum? = null
    nullJsEnum!!.equals(StringJsEnum.HELLO)
  }

  // Object methods calls on a variable of Object type.
  assertTrue(o.hashCode() === StringJsEnum.HELLO.hashCode())
  assertTrue(o.hashCode() !== StringJsEnum.GOODBYE.hashCode())
  assertTrue(o == StringJsEnum.HELLO)
  assertFalse(o == StringJsEnum.GOODBYE)
  assertTrue(o == v)
  assertFalse(o == HELLO_STRING)

  assertTrue(v.value == HELLO_STRING)

  if (!TestUtils.isWasm()) {
    // JsEnums are still instance of Enum in Wasm.
    assertFalse(v is Enum<*>)
  }
  assertTrue(v is StringJsEnum)
  assertFalse(v as Any? is String)
  if (!TestUtils.isWasm()) {
    // JsEnums are still instance of Enum in Wasm.
    assertFalse(v is Comparable<*>)
  }
  assertTrue(v is Serializable)
  assertFalse(v as Any? is PlainJsEnum)

  assertFalse(Any() is StringJsEnum)
  assertFalse(HELLO_STRING as Any? is StringJsEnum)

  val se = o as StringJsEnum
  val s = o as Serializable
  assertThrowsClassCastException(
    {
      val unused = o as Enum<*>
    },
    Enum::class.java,
  )
  assertThrowsClassCastException(
    {
      val unused = o as Comparable<*>
    },
    Comparable::class.java,
  )
  assertThrowsClassCastException(
    {
      val unused = o as String
    },
    String::class.java,
  )

  // Kotlin doesn't support intersection casts
  // assertThrowsClassCastException(
  //       {
  //         val unused: Any? = o as (StringJsEnum & Comparable<StringJsEnum>)
  //       },
  //       Comparable::class.java)

  if (!TestUtils.isWasm()) {
    // TODO(b/353352388): The value field is not used in toString in Wasm.
    val v: StringJsEnum = StringJsEnum.HELLO
    assertTrue(v.toString() == HELLO_STRING)
    val o: Any? = StringJsEnum.HELLO
    assertTrue(o.toString() == HELLO_STRING)
  }
}

private fun testStringJsEnumAsSeenFromJs() {
  assertTrue(asSeenFromJs(StringJsEnum.HELLO) === HELLO_STRING)
}

@JsMethod(name = "passThrough") private external fun asSeenFromJs(b: StringJsEnum): Any?

private var nonNativeClinitCalled = false

@JsEnum
enum class EnumWithClinit {
  A;

  companion object {
    init {
      nonNativeClinitCalled = true
    }
  }

  internal fun getValue(): Int = ordinal
}

private fun testJsEnumClassInitialization() {
  assertFalse(nonNativeClinitCalled)
  // Access to an enum value does not trigger clinit.
  var o: Any? = EnumWithClinit.A
  assertFalse(nonNativeClinitCalled)

  // Cast and instanceof do not trigger clinit.
  if (o is EnumWithClinit) {
    o = o as EnumWithClinit
  }
  assertFalse(nonNativeClinitCalled)

  // Access to ordinal() does not trigger clinit.
  val n = EnumWithClinit.A.ordinal
  assertFalse(nonNativeClinitCalled)

  // Access to any devirtualized method triggers clinit.
  EnumWithClinit.A.getValue()
  assertTrue(nonNativeClinitCalled)
}

private var nativeClinitCalled = false

@JsEnum(isNative = true, hasCustomValue = true, namespace = "test", name = "NativeEnum")
enum class NativeEnumWithClinit {
  OK;

  companion object {
    init {
      nativeClinitCalled = true
    }
  }

  @JvmField internal val value: String = definedExternally

  @JsOverlay internal fun devirtGetter(): String = value
}

private fun testNativeEnumClassInitialization() {
  assertFalse(nativeClinitCalled)
  // Access to an enum value does not trigger clinit.
  var o: Any? = NativeEnumWithClinit.OK
  assertFalse(nativeClinitCalled)

  // Cast does not trigger clinit.
  o = o as NativeEnumWithClinit
  assertFalse(nativeClinitCalled)

  // Access to value does not trigger clinit.
  val s: String = NativeEnumWithClinit.OK.value
  assertFalse(nativeClinitCalled)

  // Access to any devirtualized method triggers clinit.
  NativeEnumWithClinit.OK.devirtGetter()
  assertTrue(nativeClinitCalled)
}

private fun testDoNotAutoboxJsEnum() {
  assertTrue(returnsAny(StringJsEnum.HELLO) === HELLO_STRING)
  assertTrue(returnsAny(0, StringJsEnum.HELLO) === HELLO_STRING)

  val arr = arrayOf(StringJsEnum.HELLO)
  assertTrue(returnsAny(arr[0]) == HELLO_STRING)
}

private fun returnsAny(@DoNotAutobox obj: Any?): Any? {
  return obj
}

private fun returnsAny(n: Int, @DoNotAutobox vararg params: Any?): Any? {
  return params[0]
}

private fun testUnckeckedCastJsEnum() {
  val s: StringJsEnum = uncheckedCast(HELLO_STRING)
  assertTrue(s === StringJsEnum.HELLO)
}

@UncheckedCast private fun <T> uncheckedCast(@DoNotAutobox obj: Any?): T = obj as T

private fun testAutoBoxing_relationalOperations() {
  val one = PlainJsEnum.ONE
  val boxedOne: Any? = PlainJsEnum.ONE
  assertTrue(one === boxingPassthrough(one))
  assertTrue(boxedOne === boxingPassthrough(one))
  assertTrue(boxingPassthrough(one) === one)
  assertTrue(boxingPassthrough(one) === boxedOne)
  assertFalse(one !== boxedOne)
  assertFalse(boxedOne !== one)
  assertFalse(one !== boxingPassthrough(one))
  assertFalse(boxedOne !== boxingPassthrough(one))
  assertFalse(boxingPassthrough(one) !== one)
  assertFalse(boxingPassthrough(one) !== boxedOne)

  // Comparison with a double object which is unboxed. Many of the comparisons, like
  // `1.0 == PlainJsEnum.ONE` are rejected by the compiler due to type incompatibility.
  assertFalse(1.0 as Any? == PlainJsEnum.ONE)
  assertFalse(1.0 as Any? == boxedOne)
  assertThrowsClassCastException {
    val unused = 1.0 == boxedOne as Double
  }
}

private fun <T> boxingPassthrough(t: T): T = t

private fun testAutoBoxing_specialMethods() {
  assertTrue(PlainJsEnum.ONE.equals(PlainJsEnum.ONE))
  assertTrue(PlainJsEnum.ONE.compareTo(PlainJsEnum.ONE) == 0)
  assertTrue(PlainJsEnum.ONE.compareTo(PlainJsEnum.ZERO) > 0)
  assertTrue(PlainJsEnum.TWO.compareTo(PlainJsEnum.TEN) < 0)

  val jsEnum: PlainJsEnum = PlainJsEnum.ONE
  val nullJsEnum: PlainJsEnum? = null
  val objectJsEnum: Any? = PlainJsEnum.ONE

  val stringJsEnum: StringJsEnum = StringJsEnum.HELLO
  val nullStringJsEnum: StringJsEnum? = null
  val objectStringJsEnum: Any? = StringJsEnum.HELLO

  assertFalse(jsEnum == PlainJsEnum.TWO)
  assertTrue(jsEnum == objectJsEnum)
  assertFalse(jsEnum == nullJsEnum)
  assertFalse(jsEnum == null)

  assertFalse(stringJsEnum == StringJsEnum.GOODBYE)
  assertTrue(stringJsEnum == objectStringJsEnum)
  assertFalse(stringJsEnum.equals(nullJsEnum))
  assertFalse(stringJsEnum == null)

  assertFalse(jsEnum.equals(stringJsEnum))
}

// Kotlin doesn't support intersection casts
// private fun testAutoBoxing_intersectionCasts() {
//   val c: Comparable<Any?> = PlainJsEnum.ONE as (PlainJsEnum & Comparable<PlainJsEnum>)
//   assertTrue(c.compareTo(PlainJsEnum.ZERO) > 0)
//   val e: PlainJsEnum = PlainJsEnum.ONE as (PlainJsEnum & Comparable<PlainJsEnum>)
//   // e correcly holds an unboxed value.
//   assertUnderlyingTypeEquals(Double::class.javaObjectType, e)
//
//   assertTrue(PlainJsEnum.ONE == (PlainJsEnum.ONE as (PlainJsEnum & Comparable<PlainJsEnum>)))
//   // Intersection cast with a JsEnum does not unbox like the simple cast.
//   assertUnderlyingTypeEquals(
//       PlainJsEnum::class.java, PlainJsEnum.ONE as (PlainJsEnum & Comparable<PlainJsEnum>))
// }

private fun testAutoBoxing_typeInference() {
  assertUnderlyingTypeEquals(Double::class.javaObjectType, PlainJsEnum.ONE)
  assertUnderlyingTypeEquals(PlainJsEnum::class.java, boxingIdentity(PlainJsEnum.ONE))

  // Make sure the enum is boxed even when assigned to a field that is inferred to be JsEnum.
  val templatedField = TemplatedField(PlainJsEnum.ONE)
  var unboxed: PlainJsEnum = templatedField.value
  assertUnderlyingTypeEquals(Double::class.javaObjectType, unboxed)
  // Boxing through specialized method parameter assignment.
  assertUnderlyingTypeEquals(PlainJsEnum::class.java, boxingIdentity(unboxed))
  // Unboxing as a qualifier to ordinal.
  assertUnderlyingTypeEquals(Double::class.javaObjectType, templatedField.value.ordinal)

  // Boxing through specialized method parameter assignment.
  assertUnderlyingTypeEquals(PlainJsEnum::class.java, boxingIdentity(templatedField.value))
  // Checks what is actually returned by value.
  assertUnderlyingTypeEquals(PlainJsEnum::class.java, (templatedField as TemplatedField<*>).value)

  unboxed = templatedField.value
  assertUnderlyingTypeEquals(Double::class.javaObjectType, unboxed)

  templatedField.value = PlainJsEnum.ONE
  // Boxing through specialized method parameter assignment.
  assertUnderlyingTypeEquals(PlainJsEnum::class.java, boxingIdentity(templatedField.value))
  // Checks what is actually stored in value.
  assertUnderlyingTypeEquals(PlainJsEnum::class.java, (templatedField as TemplatedField<*>).value)
  // Unboxing as a qualifier to ordinal.
  assertUnderlyingTypeEquals(Double::class.javaObjectType, templatedField.value.ordinal)

  val list: List<*> = java.util.Arrays.asList(PlainJsEnum.ONE)
  assertUnderlyingTypeEquals(PlainJsEnum::class.javaObjectType, list[0])
  unboxed = list[0] as PlainJsEnum
  assertUnderlyingTypeEquals(Double::class.javaObjectType, unboxed)

  // TODO(b/118615488): Rewrite the following checks when JsEnum arrays are allowed.
  // In Java the varargs array will be of the inferred argument type. Since non native JsEnum
  // arrays are not allowed, the created array is of the declared type.
  var arr: Array<*> = varargsToComparableArray(PlainJsEnum.ONE)
  assertUnderlyingTypeEquals(arrayOf<Comparable<*>>().javaClass, arr)
  assertUnderlyingTypeEquals(PlainJsEnum::class.java, arr[0])
  arr = varargsToObjectArray(PlainJsEnum.ONE)
  assertUnderlyingTypeEquals(Array<Any?>::class.java, arr)
  assertUnderlyingTypeEquals(PlainJsEnum::class.java, arr[0])
}

private class TemplatedField<T>(var value: T)

private fun <T> boxingIdentity(o: T): Any? = o

private fun <T : Comparable<T>> varargsToComparableArray(vararg elements: T): Array<Any?> =
  elements as Array<Any?>

private fun <T> varargsToObjectArray(vararg elements: T): Array<Any?> = elements as Array<Any?>

private fun testAutoBoxing_parameterizedLambda() {

  val ordinalWithCast = Function<Any?, Double> { e -> (e as PlainJsEnum).ordinal.toDouble() }
  assertTrue(1.0 == ordinalWithCast.apply(PlainJsEnum.ONE))

  val ordinal = Function<PlainJsEnum, Double> { e -> e.ordinal.toDouble() }
  assertTrue(1.0 == ordinal.apply(PlainJsEnum.ONE))

  val function: Function<in PlainJsEnum, String> =
    Function<PlainJsEnum, String> { e ->
      when (e) {
        PlainJsEnum.ONE -> "ONE"
        else -> "None"
      }
    }
  assertEquals("ONE", function.apply(PlainJsEnum.ONE))

  val supplier = Supplier<PlainJsEnum> { PlainJsEnum.ONE }
  assertEquals(PlainJsEnum.ONE, supplier.get())
}

private open class Container<T> {
  var field: T? = null

  open fun get(): T? = field

  open fun set(t: T?) {
    field = t
  }
}

private class PlainJsEnumContainer : Container<PlainJsEnum>() {
  override fun get(): PlainJsEnum? = super.get()

  override fun set(plainJsEnum: PlainJsEnum?) = super.set(plainJsEnum)
}

@JsType
private open class JsTypeContainer<T> {
  @JvmField var field: T? = null

  open fun get(): T? = field

  open fun set(t: T?) {
    field = t
  }
}

private class JsTypePlainJsEnumContainer @JsConstructor constructor() :
  JsTypeContainer<PlainJsEnum>() {
  override fun get(): PlainJsEnum? = super.get()

  override fun set(plainJsEnum: PlainJsEnum?) = super.set(plainJsEnum)
}

private fun testSpecializedSuperType() {
  val five: PlainJsEnum = PlainJsEnum.FIVE
  val pc: PlainJsEnumContainer = PlainJsEnumContainer()
  val c: Container<PlainJsEnum> = pc
  pc.set(five)
  assertTrue(five == pc.get())
  assertTrue(five == (c as Container<*>).get())
  val six: PlainJsEnum = PlainJsEnum.SIX
  c.set(six)
  assertTrue(six == pc.get())
  assertTrue(six == (c as Container<*>).get())
  assertUnderlyingTypeEquals(PlainJsEnum::class.java, (c as Container<*>).get())
  assertUnderlyingTypeEquals(Double::class.javaObjectType, pc.get())

  val jpc = JsTypePlainJsEnumContainer()
  val jc: JsTypeContainer<PlainJsEnum> = jpc
  jpc.set(five)
  assertTrue(five == jpc.get())
  assertTrue(five == (jc as JsTypeContainer<*>).get())
  jc.set(six)
  assertTrue(six == jpc.get())
  assertTrue(six == (jc as JsTypeContainer<*>).get())
  assertUnderlyingTypeEquals(PlainJsEnum::class.java, (jc as JsTypeContainer<*>).get())
  assertUnderlyingTypeEquals(Double::class.javaObjectType, jpc.get())
}

@JsMethod
// Pass through an enum value as if it were coming from and going to JavaScript.
private fun passThrough(o: Any?): Any? {
  // Supported closure enums can only have number, boolean or string as their underlying type.
  // Make sure that boxed enums are not passing though here.
  assertTrue(o is String || o is Double || o is Boolean)
  return o
}

private fun testBoxingPartialInlining() {
  // TODO(b/315214896) Check the size difference to see if cases such as these take advantage of
  // partial inlining in Wasm to turn this into a simple null check, avoiding boxing.
  val nonnullJsEnum: PlainJsEnum? = PlainJsEnum.ONE
  checkNotNull(nonnullJsEnum)
  // Use the local so it doesn't get removed.
  assertTrue(nonnullJsEnum == PlainJsEnum.ONE)

  val nullJsEnum: PlainJsEnum? = null
  assertThrowsNullPointerException { checkNotNull(nullJsEnum) }
  assertTrue(nullJsEnum == null)
}

private fun checkNotNull(obj: Any?) {
  if (obj == null) {
    throw NullPointerException()
  }
}

fun testNonNativeJsEnumArrays() {
  val arr = arrayOf(PlainJsEnum.THREE, PlainJsEnum.TWO)
  assertTrue(arr.size == 2)
  assertTrue(arr[0] == PlainJsEnum.THREE)
  assertTrue(arr[1] == PlainJsEnum.TWO)

  val arr2 = arrayOfNulls<PlainJsEnum?>(2)
  assertTrue(arr2.size == 2)
  arr2[0] = PlainJsEnum.THREE
  arr2[1] = PlainJsEnum.TWO
  assertTrue(arr2[0] == PlainJsEnum.THREE)
  assertTrue(arr2[1] == PlainJsEnum.TWO)

  val arrayWithNull = arrayOf<PlainJsEnum?>(null)
  assertTrue(arrayWithNull[0] == null)

  val arrayWithDefaults = arrayOfNulls<PlainJsEnum?>(1)
  assertTrue(arrayWithDefaults[0] == null)

  val objArray = arrayOf<Any?>(PlainJsEnum.ONE)
  assertTrue(objArray[0] == PlainJsEnum.ONE)

  val list = arrayListOf<PlainJsEnum>()
  list.add(PlainJsEnum.ONE)
  assertTrue(list.toArray()[0] == PlainJsEnum.ONE)

  val nestedArr = arrayOf(arrayOf(PlainJsEnum.THREE))
  assertTrue(nestedArr.size == 1)
  assertTrue(nestedArr[0].size == 1)
  assertTrue(nestedArr[0][0] == PlainJsEnum.THREE)

  nestedArr[0] = arrayOf(PlainJsEnum.TWO)
  assertTrue(nestedArr[0][0] == PlainJsEnum.TWO)
}

fun testNonNativeStringJsEnumArrays() {
  val arr = arrayOf(StringJsEnum.HELLO, StringJsEnum.GOODBYE)
  assertTrue(arr.size == 2)
  assertTrue(arr[0] == StringJsEnum.HELLO)
  assertTrue(arr[1] == StringJsEnum.GOODBYE)

  val arr2 = arrayOfNulls<StringJsEnum?>(2)
  assertTrue(arr2.size == 2)
  arr2[0] = StringJsEnum.HELLO
  arr2[1] = StringJsEnum.GOODBYE
  assertTrue(arr2[0] == StringJsEnum.HELLO)
  assertTrue(arr2[1] == StringJsEnum.GOODBYE)

  val arrayWithNull = arrayOf<StringJsEnum?>(null)
  assertTrue(arrayWithNull[0] == null)

  val arrayWithDefaults = arrayOfNulls<StringJsEnum?>(1)
  assertTrue(arrayWithDefaults[0] == null)
}

fun testNonNativeJsEnumArrayBoxing() {
  // JsEnums are stored as unboxed in an array.
  val arr = arrayOf(PlainJsEnum.THREE)
  assertUnderlyingTypeEquals(Double::class.javaObjectType, arr[0])

  val arr2 = arrayOf(StringJsEnum.HELLO)
  assertUnderlyingTypeEquals(String::class.java, arr2[0])
}

fun testNativeJsEnumArray() {
  val arr = arrayOf(NativeEnum.ACCEPT, NativeEnum.CANCEL)
  assertTrue(arr.size == 2)
  assertTrue(arr[0] == NativeEnum.ACCEPT)
  assertTrue(arr[1] == NativeEnum.CANCEL)

  val arr2 = arrayOfNulls<NativeEnum?>(2)
  assertTrue(arr2.size == 2)
  arr2[0] = NativeEnum.ACCEPT
  arr2[1] = NativeEnum.CANCEL
  assertTrue(arr2[0] == NativeEnum.ACCEPT)
  assertTrue(arr2[1] == NativeEnum.CANCEL)

  val arrayWithNull = arrayOf<NativeEnum?>(null)
  assertTrue(arrayWithNull[0] == null)

  val arrayWithDefaults = arrayOfNulls<NativeEnum?>(1)
  assertTrue(arrayWithDefaults[0] == null)

  val nestedArr = arrayOf(arrayOf(NativeEnum.ACCEPT))
  assertTrue(nestedArr.size == 1)
  assertTrue(nestedArr[0].size == 1)
  assertTrue(nestedArr[0][0] == NativeEnum.ACCEPT)

  nestedArr[0] = arrayOf(NativeEnum.CANCEL)
  assertTrue(nestedArr[0][0] == NativeEnum.CANCEL)
}

fun testJsEnumVarargs() {
  checkTVarargs(PlainJsEnum.ONE)
  checkJsEnumVarargs(PlainJsEnum.ONE)

  val d = DerivedWithoutJsEnumVarargs()
  d.checkTVarargs(PlainJsEnum.ONE)

  val b: BaseWithTVarargs<PlainJsEnum> = DerivedWithoutJsEnumVarargs()
  b.checkTVarargs(PlainJsEnum.ONE)
}

fun <T> checkTVarargs(vararg t: T) {
  assertTrue(t[0] == PlainJsEnum.ONE)
}

fun checkJsEnumVarargs(vararg t: PlainJsEnum) {
  assertTrue(t[0] == PlainJsEnum.ONE)
}

private open class BaseWithTVarargs<T> {
  open fun checkTVarargs(vararg t: T) {
    assertTrue(t[0] == PlainJsEnum.ONE)
  }
}

private class DerivedWithoutJsEnumVarargs : BaseWithTVarargs<PlainJsEnum>() {}
