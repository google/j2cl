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
package nestedanonymousclass

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertSame
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.TestUtils.isJvm

/** Test nested anonymous class. */
interface AnonymousInterface {
  fun foo()
}

fun main(vararg unused: String) {
  val intf1 =
    object : AnonymousInterface {
      override fun foo() {
        val intf2 =
          object : AnonymousInterface {
            override fun foo() {}
          }
        assertTrue(intf2 is AnonymousInterface)
        // TODO(b/269324261): J2CL doesn't follow the Kotlin JVM naming scheme for anonymous classes
        if (isJvm()) {
          assertEquals(
            "nestedanonymousclass.MainKt\$main\$intf1$1\$foo\$intf2$1",
            intf2.javaClass.name,
          )
        } else {
          assertEquals("nestedanonymousclass.MainKt$1$1", intf2.javaClass.name)
        }
      }
    }
  assertTrue(intf1 is AnonymousInterface)
  // TODO(b/269324261): J2CL doesn't follow the Kotlin JVM naming scheme for anonymous classes.
  if (isJvm()) {
    assertEquals("nestedanonymousclass.MainKt\$main\$intf1$1", intf1.javaClass.name)
  } else {
    assertEquals("nestedanonymousclass.MainKt$1", intf1.javaClass.name)
  }
  intf1.foo()

  val o = object : InterfaceWithThisReference {}
  assertSame(o, o.foo())
  assertSame(o, o.baz())
}

interface InterfaceWithThisReference {
  fun foo(): Any {
    open class Super : InterfaceWithThisReference {
      fun bar(): Any {
        return this@InterfaceWithThisReference
      }
    }
    return object : Super() {}.bar()
  }

  fun baz(): Any {
    open class Super : InterfaceWithThisReference {
      fun bar(): Any {
        return this@InterfaceWithThisReference
      }
    }

    class Sub : Super()
    return Sub().bar()
  }
}
