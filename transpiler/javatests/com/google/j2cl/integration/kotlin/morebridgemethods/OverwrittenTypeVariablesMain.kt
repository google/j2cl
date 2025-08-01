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
package morebridgemethods

internal enum class A {
  FIRST,
  SECOND
}

internal interface MyFunction<F, T> {
  fun apply(input: F): T
}

internal interface HashFunction<T> : MyFunction<T, String>

internal object HashFunctions {
  fun <T : Enum<T>> enumHashFunction(): HashFunction<T> {
    return object : HashFunction<T> {
      override fun apply(input: T): String {
        return "" + input.ordinal
      }
    }
  }
}

object OverwrittenTypeVariablesMain {
  fun test() {
    // Might crash.
    HashFunctions.enumHashFunction()
  }
}
