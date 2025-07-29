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
package enums

import java.util.function.Function

enum class Enum1 {
  V1,
  V2
}

internal enum class Enum2(foo: Int) {
  VALUE1(2),
  VALUE2(Enum1.V1),
  VALUE3,
  VALUE4(5) {
    override fun bar(): Int {
      return 10
    }
  };

  var foo = Enum1.V1.ordinal

  init {
    this.foo = foo
  }

  constructor(foo: Enum1) : this(foo.ordinal)

  constructor(vararg somePars: Any) : this(somePars.size) {}

  open fun bar(): Int = ordinal

  companion object {
    var C = f(Any())

    internal fun f(o: Any): Enum2? {
      return null
    }
  }
}

internal enum class Enum3(function: Function<String, String>) {
  VALUE1({ _: String -> "foo" })
}
