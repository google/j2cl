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

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue

data class Data(val name: String, val age: Int)

class CustomData(val name: String, val age: Int) {
  operator fun component1(): String {
    return this.name
  }

  operator fun component2(): Int {
    return this.age
  }
}

fun getPersonInfo(): Data {
  return Data("Jack", 30)
}

fun getCustomPersonInfo(): CustomData {
  return CustomData("Ma", 50)
}

fun main(vararg unused: String) {
  testDestructuringDeclarationsInAssignment()
  testDestructuringDeclarationsInForLoops()
  testUnderscoreForUnusedVariable()
  testDestructuringDeclarationsInMaps()
  testDestructuringDeclarationsInLambdas()
}

private fun testDestructuringDeclarationsInAssignment() {
  // Destructuring declarations are only allowed for local variables/values
  val person = Data("Jack", 30)
  val (name, age) = person
  assertEquals("Jack", name)
  assertTrue(30 == age)

  val (name2, age2) = CustomData("Ma", 50)
  assertEquals("Ma", name2)
  assertTrue(50 == age2)

  val (name3, age3) = getPersonInfo()
  assertEquals("Jack", name3)
  assertTrue(30 == age3)

  val (name4, age4) = getCustomPersonInfo()
  assertEquals("Ma", name4)
  assertTrue(50 == age4)
}

private fun testDestructuringDeclarationsInForLoops() {
  var index = 0
  val persons = arrayOf(Data("0", 0), Data("1", 1))
  for ((name, age) in persons) {
    assertEquals("$index", name)
    assertTrue(index == age)
    index++
  }

  index = 0
  val customPersons = arrayOf(CustomData("0", 0), CustomData("1", 1))
  for ((customName, customAge) in customPersons) {
    assertEquals("$index", customName)
    assertTrue(index == customAge)
    index++
  }

  val map = mutableMapOf(1 to 2, 3 to 4)
  var sum = 0
  for ((first, second) in map) {
    sum += first + second
  }
  assertEquals(10, sum)

  val intArray = arrayOf(1, 2, 3)
  sum = 0
  for ((i, value) in intArray.withIndex()) {
    sum += i + value
  }
  assertEquals(9, sum)
}

private fun testUnderscoreForUnusedVariable() {
  val (_, age) = Data("Jack", 30)
  assertTrue(30 == age)

  val (_, age2) = CustomData("Ma", 50)
  assertTrue(50 == age2)
}

private fun testDestructuringDeclarationsInMaps() {
  val map = mapOf(1 to "world")
  for ((key, value) in map) {
    assertTrue(1 == key)
    assertEquals("world", value)
  }
}

private fun interface DataToString {
  fun apply(i: Data): String
}

private fun interface CustomDataToString {
  fun apply(i: CustomData): String
}

private fun testDestructuringDeclarationsInLambdas() {
  val createPerson = DataToString { (name, age): Data -> name + " is " + age }
  assertEquals("Jack is 30", createPerson.apply(Data("Jack", 30)))

  val createCustomPerson = CustomDataToString { (name, age): CustomData -> name + " is " + age }
  assertEquals("Ma is 50", createCustomPerson.apply(CustomData("Ma", 50)))
}
