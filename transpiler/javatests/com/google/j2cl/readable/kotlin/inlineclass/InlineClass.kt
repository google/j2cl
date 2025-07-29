/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package inlineclass

interface I {
  val i: Int
}

@JvmInline
value class Foo(override val i: Int) : I {
  val j: Int
    get() = i

  fun plusOne() = Foo(i + 1)

  operator fun plus(other: Foo) = Foo(i + other.i)
}

@JvmInline value class FooNullable(val i: Int?)

@JvmInline value class FooReference(val s: String)

@JvmInline value class FooReferenceNullable(val s: String?)

fun <T> asGeneric(x: T) {
  if (x is Foo) {
    val r = x.i + 1
  }
}

fun asObject(x: Any) {
  if (x is Foo) {
    val r = x.i + 1
  }
}

fun asInterface(i: I) {
  val r = i.i + 1
}

fun getFoo(i: Int): Foo {
  return Foo(i)
}

fun <T> id(x: T): T = x

fun primitiveNonnullFieldNonnullParam(f: Foo) {
  val r = f.i + 1
}

fun primitiveNonnullFieldNullParam(f: Foo?) {
  if (f != null) {
    val r = f.i + 1
  }
}

fun primitiveNullableFieldNonnullParam(f: FooNullable) {
  val r = f.i
}

fun primitiveNullableFieldNullableParam(f: FooNullable?) {
  if (f != null) {
    val r = f.i
  }
}

fun referenceNonnullFieldNonnullParam(f: FooReference) {
  val r = f.s + "1"
}

fun referenceNonnullFieldNullParam(f: FooReference?) {
  if (f != null) {
    val r = f.s + "1"
  }
}

fun referenceNullableFieldNonnullParam(f: FooReferenceNullable) {
  val r = f.s
}

fun referenceNullableFieldNullableParam(f: FooReferenceNullable?) {
  if (f != null) {
    val r = f.s
  }
}

private interface Factory<T> {
  fun create(i: Int): T
}

private class FooFactory : Factory<Foo> {
  // A bridge should be generated for the interface method.
  override fun create(i: Int): Foo = Foo(i)
}

fun main() {
  val f = Foo(42)
  val f2 = FooNullable(null)
  val f3 = FooReference("1234")
  val f4 = FooReferenceNullable(null)

  asGeneric(f) // boxed: used as generic type T
  asObject(f) // boxed: used as Any
  asInterface(f) // boxed: used as type I

  // Unboxed
  val g = getFoo(1)

  // below, 'f' first is boxed (while being passed to 'id') and then unboxed (when returned from
  // 'id')
  // In the end, 'c' contains unboxed representation (just '42'), as 'f'
  val c = id(f)

  primitiveNonnullFieldNonnullParam(f) // unboxed
  primitiveNonnullFieldNullParam(f) // boxed
  primitiveNullableFieldNonnullParam(f2) // unboxed
  primitiveNullableFieldNullableParam(f2) // boxed

  referenceNonnullFieldNonnullParam(f3) // unboxed
  referenceNonnullFieldNullParam(f3) // unboxed
  referenceNullableFieldNonnullParam(f4) // unboxed
  referenceNullableFieldNullableParam(f4) // boxed

  val ff = FooFactory()
  val h = ff.create(1)
}
