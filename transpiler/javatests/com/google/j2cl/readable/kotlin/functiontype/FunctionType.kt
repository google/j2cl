/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package functiontype

class IntTransformer : (Int) -> Int {
  override operator fun invoke(x: Int): Int = 1
}

val intFunction: (Int) -> Int = IntTransformer()
val intIntFunction: (Int, Int) -> Int = { p1, p2 -> p1 + p2 }

fun funWithFunctionType(f: (Int) -> Int): Int {
  return f(2)
}

fun funWithNestedFunctionType(f: (() -> Int) -> ((Int) -> Int)): Int {
  return f { 2 }.invoke(2)
}

fun funWithExtensionFunctionType(f: String.(Int, Int) -> String): String {
  return "foo".f(1, 2)
}

// TODO(b/258286507): Enable when the mapping to FunctionN vararg is supported.
// fun funWith23ParametersFunctionType(
//   f:
//     (
//     p1: String,
//     p2: String,
//     p3: String,
//     p4: String,
//     p5: String,
//     p6: String,
//     p7: String,
//     p8: String,
//     p9: String,
//     p10: String,
//     p11: String,
//     p12: String,
//     p13: String,
//     p14: String,
//     p15: String,
//     p16: String,
//     p17: String,
//     p18: String,
//     p19: String,
//     p20: String,
//     p21: String,
//     p22: String,
//     p23: String
//   ) -> String
// ): String {
//   return f(
//     "1",
//     "2",
//     "3",
//     "4",
//     "5",
//     "6",
//     "7",
//     "8",
//     "9",
//     "10",
//     "11",
//     "12",
//     "13",
//     "14",
//     "15",
//     "16",
//     "17",
//     "18",
//     "19",
//     "20",
//     "21",
//     "22",
//     "23"
//   )
// }

fun foo(i: Number): Number = i

val fooRef: (Int) -> Any = ::foo
