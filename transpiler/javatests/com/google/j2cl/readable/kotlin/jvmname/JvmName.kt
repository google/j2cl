/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
@file:kotlin.jvm.JvmName("CustomNameKt")

package jvmname

class SomeClass {
  val a: Int = 10

  var c: Int = 0
    @kotlin.jvm.JvmName("d") get
    @kotlin.jvm.JvmName("d") set

  @kotlin.jvm.JvmName("f") fun e() {}

  companion object {
    @kotlin.jvm.JvmName("h") fun g() {}
  }
}

@kotlin.jvm.JvmName("customTopLevelFun") fun topLevel() {}

var foo: Int = 0
  @kotlin.jvm.JvmName("bar") get
  @kotlin.jvm.JvmName("buzz") set

@kotlin.jvm.JvmName("name-with_special\$chars") fun name_with_special_chars() {}
