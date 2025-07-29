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
package destructuringdeclarations

data class Data(val name: String, val age: Int)

class CustomData(val name: String, val age: Int) {
  operator fun component1(): String {
    return this.name
  }

  operator fun component2(): Int {
    return this.age
  }
}

class DestructuringDeclarations {

  private fun getPersonInfo(): Data {
    return Data("Ma", 50)
  }

  private fun testDestructuringDeclarationsInAssignment(args: Array<String>) {
    val person = Data("Jack", 30)
    val (name, age) = person

    val customPerson = CustomData("Ma", 50)
    val (name2, age2) = customPerson
  }

  private fun testReturnValuesFromFunction() {
    val (name, age) = getPersonInfo()
  }

  private fun testDestructuringDeclarationsInForLoops() {
    val persons = arrayOf(Data("Jack", 30), Data("Ma", 50))
    for ((name, age) in persons) {
      var a = name
      var b = age
    }

    for ((index, value) in persons.withIndex()) {}
  }

  private fun testUnderscoreForUnusedVariable() {
    val (_, age) = Data("Jane", 28)
  }

  private fun testDestructuringDeclarationsInMaps() {
    val map = mapOf<Int, String>(1 to "world")
    for ((key, value) in map) {
      var a = key
      var b = value
    }
  }

  private fun interface DataToString {
    fun apply(i: Data): String
  }

  private fun testDestructuringDeclarationsInLambdas() {
    val createPerson = DataToString { (name, age): Data -> name + " is " + age }
    val person = createPerson.apply(Data("Jack", 30))
  }
}
