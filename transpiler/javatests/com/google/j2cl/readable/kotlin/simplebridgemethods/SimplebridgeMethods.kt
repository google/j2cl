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
package simplebridgemethods

internal open class Superclass<T : Error> {
  open fun m1(t: T) {}
}

// non generic class extends parameterized class, and overrides generic method.
internal class Subclass : Superclass<AssertionError>() {
  // bridge method: m1(Error a) {m1((AssertionError) a);}
  override fun m1(a: AssertionError) {}
}

// non generic class extends parameterized class, and does not override generic method.
internal class AnotherSubclass : Superclass<AssertionError>() {
  // there should be no bridge method.
}

internal interface Callable<V> {
  fun call(v: V)
}

// generic class implements parameterized interface, overriding methods without changing signature.
internal class Task<T> : Callable<T> {
  // no bridge method, signature does not change.
  override fun call(t: T) {}
}

// generic class implements parameterized interface, overriding methods and changing signature.
internal class AnotherTask<T : AssertionError> : Callable<Superclass<T>> {
  // overrides and changes the signature.
  // bridge method: call(Object) {call((Superclass<T>) t);}
  override fun call(t: Superclass<T>) {}
}
