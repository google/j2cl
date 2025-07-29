/*
 * Copyright 2017 Google Inc.
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
package jsvarargs

class QualifiedSuperMethodCall : Main(0) {
  inner class InnerClass {
    fun test() {
      // multiple arguments.
      super@QualifiedSuperMethodCall.f3(1, 1, 2)
      // no argument for varargs.
      super@QualifiedSuperMethodCall.f3(1)
      // array spread for varargs.
      super@QualifiedSuperMethodCall.f3(1, *intArrayOf(1, 2))
      // empty array spread for varargs.
      super@QualifiedSuperMethodCall.f3(1, *intArrayOf())
      // array object spread for varargs.
      val ints = intArrayOf(1, 2)
      super@QualifiedSuperMethodCall.f3(1, *ints)
    }
  }
}
