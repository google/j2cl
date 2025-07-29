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
package instanceinitorder

import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertNotNull
import com.google.j2cl.integration.testing.Asserts.assertNull
import com.google.j2cl.integration.testing.Asserts.assertTrue

/** Test instance initialization order. */
var initStep = 0

fun main(vararg unused: String) {
  initStep = 1
  val c = Child()
  assertTrue(initStep == 7)

  // The values in these properties are written to by the super constructor, but they are
  // overwritten by the constructor due to having an initializer.
  assertTrue(c.doubleNegativeZero == -0.0)
  assertTrue(c.floatNegativeZero == -0.0f)
  assertTrue(c.nullableInt == 0)
  assertTrue(c.field1 == 0)
  assertTrue(c.field6 == 0)
  assertNotNull(c.field2)
  assertTrue(c.field3 == 0)
  assertFalse(c.field4)
  assertNull(c.field5)

  // Fields initialized to their default value during declaration will see the values written by the
  // super constructor.
  assertTrue(c.fieldInitializedToDefault1 == 1)
  assertTrue(c.fieldInitializedToDefault2)
  assertTrue(c.fieldInitializedToDefault3 == 'a')
  assertTrue(c.fieldInitializedToDefault4.toInt() == 1)
  assertTrue(c.fieldInitializedToDefault5.toInt() == 1)
  assertTrue(c.fieldInitializedToDefault6 == 1L)
  assertTrue(c.fieldInitializedToDefault7 == 1.0)
  assertTrue(c.fieldInitializedToDefault8 == 1.0f)
  assertNotNull(c.fieldInitializedToDefault9)
  assertTrue(c.fieldInitializedToDefault10 == true)
}

class Child : Parent() {
  // TODO(b/288869365): Add tests with field initialized to `0.toChar()` `0.toShort(), `0.toByte()`
  // when those expression are evaluated at compile time.
  var fieldInitializedToDefault1 = 0
  var fieldInitializedToDefault2 = false
  var fieldInitializedToDefault3 = '\u0000'
  var fieldInitializedToDefault4: Byte = 0
  var fieldInitializedToDefault5: Short = 0
  var fieldInitializedToDefault6: Long = 0
  var fieldInitializedToDefault7 = 0.0
  var fieldInitializedToDefault8 = 0f
  var fieldInitializedToDefault9: Any? = null
  var fieldInitializedToDefault10: Boolean? = null

  // Test that those ones are not considered as being initialized to the default value.
  var doubleNegativeZero = -0.0
  var floatNegativeZero = -0.0f
  var nullableInt: Int? = 0

  // Those should not be considered as initialized to the default value.
  var field1 = initializeField1()
  var field2: Any? = Any()
  var field3: Int
  var field4: Boolean
  var field5: Any?

  init {
    assertTrue(initStep++ == 3) // #3
    field3 = 0
    field4 = false
    field5 = null
  }

  var field6 = initializefField6()

  init {
    assertTrue(initStep++ == 5) // #5
  }

  init {
    assertTrue(initStep++ == 6) // #6
  }

  fun initializeField1(): Int {
    assertTrue(initStep++ == 2) // #2
    return 0
  }

  fun initializefField6(): Int {
    assertTrue(initStep++ == 4) // #4
    return 0
  }

  override fun assignFromParentConstructor() {
    assertTrue(initStep++ == 1) // #1
    // Assign values to each field that are different from the default to test which assigment is
    // done last.
    fieldInitializedToDefault1 = 1
    fieldInitializedToDefault2 = true
    fieldInitializedToDefault3 = 'a'
    fieldInitializedToDefault4 = 1
    fieldInitializedToDefault5 = 1
    fieldInitializedToDefault6 = 1L
    fieldInitializedToDefault7 = 1.0
    fieldInitializedToDefault8 = 1.0f
    fieldInitializedToDefault9 = Any()
    fieldInitializedToDefault10 = true

    doubleNegativeZero = -1.0
    floatNegativeZero = -1.0f
    nullableInt = null

    field1 = 10
    field2 = null
    field3 = 1
    field4 = true
    field5 = Any()
    field6 = 10
  }
}

open class Parent {
  init {
    assignFromParentConstructor()
  }

  open fun assignFromParentConstructor() {}
}
