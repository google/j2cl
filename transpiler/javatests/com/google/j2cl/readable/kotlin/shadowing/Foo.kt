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
package shadowing

class Foo<T>() : AbstractCollection<T>() {

  override val size: Int = 0
  override fun iterator(): Iterator<T> = throw RuntimeException()

  // I'm abusing the fact that Kotlinc will add stub methods so that we transitively implement the
  // java.util.Collection interface. That function will have the signature `add(T): Boolean` whereas
  // here I have a Unit return type.
  // Kotlin callers will _only_ be able to call this method, however for Java callers it gets a bit
  // more interesting. If they directly call `add(T)` on type Foo they'll invoke this method, but
  // if they call it on the java.util.Collection type they'll invoke the `add(T): Boolean` variant.
  // End up in this state isn't normally allowed by either the Kotlin or Java languages. However,
  // the JVM bytecode does allow for methods that differ only on return type, and on the Kotlin side
  // we're relying on the stub function being added during compilation.
  fun add(value: T) {}
}

fun test() {
  Foo<String>().add("")
}
