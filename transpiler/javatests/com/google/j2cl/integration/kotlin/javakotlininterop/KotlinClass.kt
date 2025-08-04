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
package javakotlininterop

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.TestUtils.isJvm
import javakotlininterop.FromJava.AbstractConsumer
import javakotlininterop.FromJava.TConsumer

var topLevelProperty = 0

fun topLevelFunction(): Int {
  return 1
}

class KotlinClass {
  var instanceProperty = 2

  fun instanceFunction(): Int {
    return 3
  }
}

class WithCompanion {
  // Needed to avoid compilation error: "Classes that contain only a companion object should be
  // replaced with a named object declaration"
  val foo = 0

  companion object {
    val nonJvmStaticProperty = 1
    @JvmStatic val jvmStaticProperty = 2

    fun nonJvmStaticFunction() = 3

    @JvmStatic fun jvmStaticFunction() = 4

    const val CONSTANT = 10
  }
}

object KotlinObject {
  val nonJvmStaticProperty = 1
  @JvmStatic val jvmStaticProperty = 2

  fun nonJvmStaticFunction() = 3

  @JvmStatic fun jvmStaticFunction() = 4
}

interface InterfaceWithCompanion {
  companion object {
    fun nonJvmStaticMethod() = 5

    @JvmStatic fun jvmStaticMethod() = 6
  }
}

open class KotlinExtendsJavaTConsumerWithOverride : TConsumer<Int>() {
  override fun consume(i: Int): String = "KotlinExtendsJavaTConsumerWithOverride.consume(int)"
}

open class KotlinExtendsJavaTConsumerWithoutOverride : TConsumer<Int>() {}

fun testJavaKotlinMixedHierarchyFromKotlinWithExplicitOverride() {
  assertEquals(
    "KotlinExtendsJavaTConsumerWithOverride.consume(int)",
    KotlinExtendsJavaTConsumerWithOverride().consume(1),
  )

  val t: TConsumer<Int> = KotlinExtendsJavaTConsumerWithOverride()
  assertEquals("KotlinExtendsJavaTConsumerWithOverride.consume(int)", t.consume(1))
  assertEquals("KotlinExtendsJavaTConsumerWithOverride.consume(int)", t.consume(1 as Int?))

  val a: AbstractConsumer<Int> = KotlinExtendsJavaTConsumerWithOverride()
  assertEquals("KotlinExtendsJavaTConsumerWithOverride.consume(int)", a.consume(1))
  assertEquals("KotlinExtendsJavaTConsumerWithOverride.consume(int)", a.consume(1 as Int?))

  val c: FromJava.Consumer<Int> = KotlinExtendsJavaTConsumerWithOverride()
  assertEquals("KotlinExtendsJavaTConsumerWithOverride.consume(int)", c.consume(1))
  assertEquals("KotlinExtendsJavaTConsumerWithOverride.consume(int)", c.consume(1 as Int?))
}

fun testJavaKotlinMixedHierarchyFromKotlinWithoutExplicitOverride() {
  assertEquals("TConsumer.consume(int)", KotlinExtendsJavaTConsumerWithoutOverride().consume(1))

  val t: TConsumer<Int> = KotlinExtendsJavaTConsumerWithoutOverride()
  assertEquals("TConsumer.consume(int)", t.consume(1))
  if (isJvm()) {
    // TODO(b/262430319): Decide what to do with the quirky kotlin semantics for overrides.

    // Note since the kotlin class did not implement the method the two super methods are not
    // bridged and both are preserved. In the J2CL model the subclass collapses the two methods
    // together.
    assertEquals("TConsumer.consume(T)", t.consume(1 as Int?))

    // The problem is even worse here where even when passing a primitive it does get dispatched
    // to the reference version because there is no bridge.
    val a: AbstractConsumer<Int> = KotlinExtendsJavaTConsumerWithoutOverride()
    assertEquals("TConsumer.consume(T)", a.consume(1))
    assertEquals("TConsumer.consume(T)", a.consume(1 as Int?))

    val c: FromJava.Consumer<Int> = KotlinExtendsJavaTConsumerWithoutOverride()
    assertEquals("TConsumer.consume(T)", c.consume(1))
    assertEquals("TConsumer.consume(T)", c.consume(1 as Int?))
  }
}

open class SuperTypeWithOptionalParams(x: String? = "defaulted") {
  val str: String = x ?: "null"

  @JvmOverloads open fun openFunWithDefaults(str: String? = "defaulted") = str ?: "null"

  @JvmOverloads fun funWithDefaults(str: String? = "defaulted") = str ?: "null"
}

fun topLevelFunWithDefaults(str: String? = "defaulted") = str ?: null

object KotlinOptionalVarargs {
  @JvmStatic fun optionalVarargs(vararg args: Int = intArrayOf(1, 2, 3)) = args

  @JvmStatic
  fun varargsWithTrailingOptional(vararg args: Int, optional: Int = 10) =
    intArrayOf(*args, optional)

  @JvmStatic
  fun varargsWithLeadingOptional(optional: Int = 10, vararg args: Int) = intArrayOf(optional, *args)

  @JvmStatic
  fun optionalVarargsWithLeadingOptional(
    optional: Int = 10,
    vararg args: Int = intArrayOf(1, 2, 3),
  ) = intArrayOf(optional, *args)
}
