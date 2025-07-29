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
package `typealias`

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue

interface IFoo {
  fun f(): Int
}

typealias AliasedInterface = IFoo

class Foo(val i: Int) : AliasedInterface {
  override fun f(): Int = i + 1

  companion object {
    val j = 2

    fun companionFunction() = j + 2
  }
}

class Container<T> {
  var o: T? = null

  fun set(o: T) {
    this.o = o
  }

  fun get(): T = o ?: throw IllegalStateException()
}

class KeyValueContainer<K, V> {
  var k: K? = null
  var v: V? = null

  fun set(k: K, v: V) {
    this.k = k
    this.v = v
  }

  fun getKey(): K = k ?: throw IllegalStateException()

  fun getValue(): V = v ?: throw IllegalStateException()
}

typealias AliasedFoo = Foo

typealias AliasedString = java.lang.String

typealias DoubleAliasedFoo = AliasedFoo

typealias FooContainer = Container<Foo>

typealias FooValueContainer<K> = KeyValueContainer<K, Foo>

typealias FooProvider = (Int) -> Foo

typealias ToInt<T> = (T) -> Int

fun main(vararg unused: String) {
  testInstanceOf()
  testMemberAccess()
  testConsumer()
}

private fun testInstanceOf() {
  assertTrue(Foo(1) is AliasedFoo)
  assertTrue(Foo(1) is DoubleAliasedFoo)
  assertTrue(AliasedFoo(1) is Foo)
  assertTrue(DoubleAliasedFoo(1) is Foo)

  assertTrue(java.lang.String("") is AliasedString)
  assertTrue(AliasedString("Foo") is java.lang.String)

  assertTrue(FooContainer() is Container<*>)
  assertTrue(FooValueContainer<String>() is KeyValueContainer<*, *>)
}

private fun testMemberAccess() {
  assertEquals(AliasedFoo(1).i, 1)
  assertEquals(DoubleAliasedFoo(2).i, 2)
  assertEquals(AliasedFoo(1).f(), 2)
  assertEquals(DoubleAliasedFoo(2).f(), 3)

  assertEquals(AliasedFoo.j, 2)
  assertEquals(DoubleAliasedFoo.j, 2)
  assertEquals(AliasedFoo.companionFunction(), 4)
  assertEquals(DoubleAliasedFoo.companionFunction(), 4)

  assertEquals(AliasedString("Foo").substring(1), "oo")

  val fooContainer = FooContainer()
  val a = Foo(1)
  fooContainer.set(a)
  assertEquals(fooContainer.get(), a)

  val fooString = FooValueContainer<String>()
  fooString.set("a", a)
  assertEquals(fooString.getKey(), "a")
  assertEquals(fooString.getValue(), a)
}

private fun testConsumer() {
  assertEquals(consumeFoo(AliasedFoo(1)), 1)
  assertEquals(consumeFoo(DoubleAliasedFoo(2)), 2)
  assertEquals(consumeAliasedFoo(Foo(3)), 3)
  assertEquals(consumeDoubleAliasedFoo(Foo(4)), 4)
  assertEquals(consumeString(AliasedString("Foo")), "oo")

  val fooContainer = FooContainer()
  val a = Foo(1)
  fooContainer.set(a)
  assertEquals(consumeContainer(fooContainer), a)

  val fooString = FooValueContainer<String>()
  fooString.set("a", a)
  assertEquals(consumeKeyValueContainer(fooString), "a")

  val container = Container<Foo>()
  container.set(a)
  assertEquals(consumeFooContainer(container), a)

  val keyValuecontainer = KeyValueContainer<Int, Foo>()
  keyValuecontainer.set(1, a)
  assertTrue(consumeFooValueContainer(keyValuecontainer) == 1)

  assertEquals((consumeFooProvider(1) { i -> Foo(i) }).i, 1)
  assertEquals(consumeFooToInt(Foo(2)) { f -> f.i }, 2)
}

private fun consumeString(s: java.lang.String) = s.substring(1)

private fun consumeFoo(foo: Foo) = foo.i

private fun consumeAliasedFoo(foo: AliasedFoo) = foo.i

private fun consumeDoubleAliasedFoo(foo: DoubleAliasedFoo) = foo.i

private fun <T> consumeContainer(c: Container<T>): T {
  return c.get()
}

private fun <K, V> consumeKeyValueContainer(c: KeyValueContainer<K, V>): K {
  return c.getKey()
}

private fun consumeFooContainer(c: FooContainer): Foo {
  return c.get()
}

private fun <K> consumeFooValueContainer(c: FooValueContainer<K>): K {
  return c.getKey()
}

private fun consumeFooProvider(i: Int, f: FooProvider): Foo {
  return f(i)
}

private fun consumeFooToInt(foo: Foo, f: ToInt<Foo>): Int {
  return f(foo)
}
