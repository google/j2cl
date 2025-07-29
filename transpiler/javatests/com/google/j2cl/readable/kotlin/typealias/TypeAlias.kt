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

interface IFoo {
  fun f(): Int
}

typealias AliasedInterface = IFoo

class Foo(val i: Int) : AliasedInterface {
  override fun f(): Int = i + 1
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

typealias FooContainer = Container<Foo>

typealias FooValueContainer<K> = KeyValueContainer<K, Foo>

typealias FooProvider = (Int) -> Foo

typealias ToInt<T> = (T) -> Int

private fun testMemberAccess() {
  val a = AliasedFoo(1).i
  val b = AliasedFoo(1).f()
  val c = AliasedString("Foo").substring(1)

  FooContainer().set(Foo(2))
  FooValueContainer<String>().set("a", AliasedFoo(2))
}

private fun testConsumer() {
  consumeFoo(AliasedFoo(1))
  consumeAliasedFoo(Foo(3))
  consumeString(AliasedString("Foo"))

  consumeContainer(FooContainer())
  consumeKeyValueContainer(FooValueContainer<String>())
  consumeFooContainer(Container<Foo>())
  consumeFooValueContainer(KeyValueContainer<Int, Foo>())

  consumeFooProvider { Foo(1) }
  consumeFooToInt { f -> f.i }
}

private fun consumeString(s: java.lang.String) {}

private fun consumeFoo(foo: Foo) {}

private fun consumeAliasedFoo(foo: AliasedFoo) {}

private fun <T> consumeContainer(c: Container<T>) {}

private fun <K, V> consumeKeyValueContainer(c: KeyValueContainer<K, V>) {}

private fun consumeFooContainer(c: FooContainer) {}

private fun <K> consumeFooValueContainer(c: FooValueContainer<K>) {}

private fun consumeFooProvider(f: FooProvider) {}

private fun consumeFooToInt(f: ToInt<Foo>) {}
