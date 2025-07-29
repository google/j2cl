/*
 * Copyright 2025 Google Inc.
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
package interfaces

class AccidentalOverride {
  interface Left<T : Left<T>> {
    fun foo(t: T) {}
  }

  interface Right<T : Right<T>> {
    fun foo(t: T) {}
  }

  interface Bottom<T : Bottom<T>> : Left<T>, Right<T> {
    override fun foo(t: T) {}
  }

  open class A<T : Left<T>, V : Right<V>> : Left<T>, Right<V>

  class B : A<B, B>(), Bottom<B>

  class C : Bottom<C> {
    override fun foo(c: C) {}
  }
}
