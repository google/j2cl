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
package rawstring

fun main() {
  val rawString = """Tell me and I forget.
  Teach me and I remember.
  Involve me and I learn."""
  val rawStringWithEscapedCharacters = """\n will not be substituted."""
  val rawStringWithSymbol = """This ${'$'}{variable} will not be substituted."""
  val variable = "number"
  val rawStringWithStringTemplate = """This ${variable} will be substituted."""
}
