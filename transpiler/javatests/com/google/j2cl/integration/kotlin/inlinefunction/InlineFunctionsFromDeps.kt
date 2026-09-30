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
package inlinefunction

class ExternalClass(var f: Int)

inline fun inlineFunctionFromDeps(externalClass: ExternalClass, action: (Int) -> Int): Int {
  return action(externalClass.f)
}

inline fun inlineFunctionFromDepsCallingJava(action: (Int) -> Int): Int {
  return action(
    JavaClass().instanceMethod() +
      JavaClass.staticMethod() +
      JavaClass.NestedClass.staticMethodInNestedClass()
  )
}

private val privateTopLevelProperty = 100

private fun privateTopLevelFunction() = 1000

class OuterClass(private val outerValue: Int) {
  inner class InnerClass(private val innerValue: Int) {
    // Once inlined in another module, the body can only reach the outer `this`, the private members
    // and the private top-level members through the synthetic accessors generated for this module.
    internal inline fun sumWithPrivateMembers(): Int =
      outerValue + innerValue + privateTopLevelProperty + privateTopLevelFunction()
  }
}
