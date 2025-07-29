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
package bridgemethods

internal interface I<T, S> {
  fun `fun`(s: S): T
}

open class A<T, S> {
  open fun `fun`(s: S): T? {
    return null
  }

  fun get(): T? {
    return null
  }
}

internal class B : A<Number?, String?>(), I<Int?, String?> {
  // bridge method for A.fun(String):Number and I.fun(String):Integer should both delegate
  // to this method. Since A.fun(String):Number and I.fun(String):Integer has the same signature
  // (although) different return type, only one bridge method should be created.
  override fun `fun`(s: String?): Int? {
    return 1
  }
}

/** An interface with the same method as A but specialized. */
internal interface SpecializedInterface {
  fun `fun`(s: String?): String?
  fun get(): String?
}

internal class AccidentalOverride : A<String?, String?>(), SpecializedInterface
