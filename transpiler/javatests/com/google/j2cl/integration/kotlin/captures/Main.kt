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
package captures

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertNotEquals
import com.google.j2cl.integration.testing.Asserts.assertNull
import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testVariableCapture()
  testVariableCapture_nested()
  testVariableCapture_indirect()
  testVariableCapture_nameClashes_inheritance()
  testVariableCapture_nameClashes_nested()
  testVariableCapture_mutation()
  testVariableCapture_mutation_outerScope()
  testParameterCapture()
  testOuterCapture()
  testOuterCapture_nested()
  testOuterCapture_indirect()
  testOuterCapture_defaultMethodSuper()
  testOuterCapture_inLambda()
  testOuterCapture_viaSuper()
  testOuterCapture_viaSuper_inLambda()
  testCaptures_constructor()
  testCaptures_parent()
  testCaptures_anonymous()
  testCaptures_uninitializedNotObservable()
  testCaptures_fieldReferences()
}

private fun testVariableCapture() {
  val variable = 1

  class Inner {
    fun variable(): Int {
      return variable
    }
  }
  assertEquals(variable, Inner().variable())
}

private fun testVariableCapture_nested() {
  val variable = 1
  val anotherVariable = 2

  open class Outer {
    // TODO(b/215282471): anonymous class needs to pass the capture anotherVariable to Outer, but
    // when computing the captures it has not seen that Outer captures anotherVariable yet.
    // public Outer anonymous() {
    //    return new Outer() {};
    // }

    inner class Inner : Outer() {
      fun variable(): Int {
        return variable
      }
    }

    // Keep the only reference to the captured variable AFTER the nested classes to test that
    // the AST traversal in the resolution of captures handles the case.
    val field = anotherVariable
  }
  assertEquals(variable, Outer().Inner().variable())
  assertEquals(anotherVariable, Outer().Inner().field)
  // assertEquals(anotherVariable, new Outer().anonymous().field);
}

private fun testVariableCapture_indirect() {
  val local = 3

  class ClassCapturingLocal {
    fun returnLocal(): Int {
      return local
    }
  }

  class ClassIndirectlyCapturingLocal {
    fun returnIndirectCapture(): Int {
      return ClassCapturingLocal().returnLocal()
    }
  }
  assertTrue(ClassIndirectlyCapturingLocal().returnIndirectCapture() == 3)
}

private fun testVariableCapture_nameClashes_inheritance() {
  // TODO(b/165431807): Uncomment when bug is fixed
  //   val local = 3
  //   class ClassCapturingLocal {
  //     open fun returnLocal() = local
  //
  //     fun returnOriginalLocal() = local
  //   }
  //   class UnrelatedClass {
  //     fun getInnerClass(): ClassCapturingLocal {
  //       val local = 4;
  //       return object : ClassCapturingLocal() {
  //         override fun returnLocal() = local
  //       }
  //     }
  //   }
  //
  //   val outer = ClassCapturingLocal()
  //   val inner = UnrelatedClass().getInnerClass()
  //   assertEquals(3, outer.returnLocal())
  //   assertEquals(4, inner.returnLocal())
  //   assertEquals(3, inner.returnOriginalLocal())
}

private fun testVariableCapture_nameClashes_nested() {
  // TODO(b/165431807): Uncomment when bug is fixed
  //   val local = 3
  //   class ClassCapturingLocal {
  //     open fun returnLocal() = local
  //
  //     fun returnOriginalLocal() = local
  //
  //     fun getInnerClass(): ClassCapturingLocal {
  //       val local = 4
  //       return object : ClassCapturingLocal() {
  //         override fun returnLocal() = local
  //       }
  //     }
  //   }
  //   val outer = ClassCapturingLocal()
  //   val inner = outer.getInnerClass()
  //   assertEquals(3, outer.returnLocal())
  //   assertEquals(4, inner.returnLocal())
  //   assertEquals(3, inner.returnOriginalLocal())
}

private interface CapturingVariableModifier {
  fun modify()
}

private fun testVariableCapture_mutation() {
  // Declare the variable of a more general type than the initial value to make sure the wrapping
  // variable that will be introduce is of the right type and allows an assignment of a different
  // subtype.
  var s: CharSequence = "initial"
  var i = 0
  val modifier =
    object : CapturingVariableModifier {
      override fun modify() {
        s = StringBuilder("modified")
        i = 10
      }
    }
  modifier.modify()

  assertEquals("modified", s.toString())
  assertTrue(10 == i)
}

private fun testVariableCapture_mutation_outerScope() {
  var i = 0
  var s: CharSequence = "initial"

  class Supplier {
    fun getInt(): Int = i

    fun getCharSequence(): CharSequence = s
  }

  val supplier = Supplier()
  assertEquals(0, supplier.getInt())
  assertEquals("initial", supplier.getCharSequence().toString())

  i = 10
  s = StringBuilder().append("modified")

  // TODO(b/468966706): Re-enable once the bug is fixed.
  // assertEquals(10, supplier.getInt())
  // assertEquals("modified", supplier.getCharSequence().toString())
}

private fun testCaptures_constructor() {
  val variable = 1

  // local class with non-default constructor and this() call
  class Local(a: Int, b: Int) {
    constructor(a: Int) : this(a, variable)

    var field: Int

    init {
      field = variable + a + b
    }
  }

  val a = 10
  val b = 20
  assertEquals(variable + a + b, Local(a, b).field)
  assertEquals(variable + a + variable, Local(a).field)
}

private fun testParameterCapture() {
  class Outer {
    fun captureInInner(parameter: Int): Int {
      class Inner {
        fun parameter(): Int {
          return parameter
        }
      }
      return Inner().parameter()
    }
  }

  val parameter = 10
  assertEquals(parameter, Outer().captureInInner(10))
}

private fun testOuterCapture() {
  class Outer {
    inner class Inner {
      fun captured(): Outer {
        return this@Outer
      }
    }
  }

  val outer = Outer()
  assertEquals(outer, outer.Inner().captured())
}

private const val TEN = 10
private const val TWENTY = 20

private class Outer {
  var outerField = TEN

  constructor() {}

  constructor(outherField: Int) {
    outerField = outherField
  }

  open inner class Intermediate {
    internal inner class Inner {
      fun captured(): Int {
        return outerField
      }
    }
  }

  // In Kotlin inner classes can not be extended by static classes
  // Even though this class extends Intermediate that captures the enclosing class, this class
  // itself does not capture it.
  // class IntermediateChild : Intermediate()  {
  //
  // }
}

private fun testOuterCapture_nested() {
  val outer = Outer()
  assertEquals(TEN, outer.Intermediate().Inner().captured())
  // In Kotlin inner classes can not be extended by static classes
  // assertEquals(TWENTY, Outer.IntermediateChild().Inner().captured())
}

private fun testOuterCapture_indirect() {
  class Outer {
    inner class ClassCapturingOuter {
      fun capturedOuter(): Outer {
        return this@Outer
      }
    }

    inner class ClassIndirectlyCapturingOuter {
      fun returnIndirectCapture(): Outer {
        return ClassCapturingOuter().capturedOuter()
      }
    }

    inner class ClassNotIndirectlyCapturingOuter {
      fun returnIndirectCapture(): Outer {
        return Outer().ClassCapturingOuter().capturedOuter()
      }
    }
  }

  val outer = Outer()
  assertEquals(outer, outer.ClassIndirectlyCapturingOuter().returnIndirectCapture())
  assertNotEquals(outer, outer.ClassNotIndirectlyCapturingOuter().returnIndirectCapture())
}

fun interface Supplier {
  fun get(): String
}

private fun testOuterCapture_inLambda() {
  class Outer {
    fun m(): String {
      assertTrue(this is Outer)
      return "Outer"
    }

    fun n(): String {
      val s = Supplier { m() }
      return s.get()
    }
  }
  assertEquals("Outer", Outer().n())
}

private fun testOuterCapture_viaSuper() {
  open class Super {
    open fun m(): String {
      assertTrue(this is Super)
      return "Super"
    }
  }
  class Sub : Super() {
    override fun m(): String {
      return "Sub"
    }

    fun n(): String {
      val s: Supplier =
        object : Supplier {
          override fun get(): String {
            return super@Sub.m()
          }
        }
      return s.get()
    }
  }
  assertEquals("Super", Sub().n())
}

private fun testOuterCapture_viaSuper_inLambda() {
  open class Super {
    open fun m(): String {
      assertTrue(this is Super)
      return "Super"
    }
  }
  class Sub : Super() {
    override fun m(): String {
      return "Sub"
    }

    fun n(): String {
      val s = Supplier { super.m() }
      return s.get()
    }
  }
  assertEquals("Super", Sub().n())
}

private fun testCaptures_parent() {
  val variable = 10

  open class Parent {
    fun variablePlusPar(parameter: Int): Int {
      return variable + parameter
    }
  }

  class AChild : Parent()
  class AnotherChild : Parent()

  val parameter = 15
  assertEquals(variable + parameter, AChild().variablePlusPar(parameter))
  assertEquals(variable + parameter, AnotherChild().variablePlusPar(parameter))
}

internal interface AnonymousInterface {
  fun foo()
}

private abstract class SomeClass constructor(var f: Int) {
  fun foo(): Int {
    return f
  }
}

fun testCaptures_anonymous() {
  val instances = Array<Any?>(3) { null }

  class Outer {
    var i: AnonymousInterface =
      object : AnonymousInterface {
        var cachedThis: Any = this

        override fun foo() {
          instances[0] = this
          instances[1] = cachedThis
          instances[2] = this@Outer
        }
      }
  }

  val outer = Outer()
  outer.i.foo()

  assertTrue(instances[0] === outer.i)
  assertTrue(instances[1] === outer.i)
  assertTrue(instances[2] === outer)

  assertTrue(object : SomeClass(3) {}.foo() == 3)
}

fun testCaptures_uninitializedNotObservable() {
  val o = Any()

  abstract class Parent(oValue: Any?, outerValue: Any?) {
    abstract fun observe(oValue: Any?, outerValue: Any?)

    init {
      observe(oValue, outerValue)
    }
  }

  class Outer {
    // Pass the values on construction to avoid relying on captures which is what we will test.
    inner class Capturer(oValue: Any?, outerValue: Any?) : Parent(oValue, outerValue) {
      // Sentinel to make sure that observe() is called before getting the chance to initialize
      // the instance.
      var initializedField = Any()

      override fun observe(oValue: Any?, outerValue: Any?) {
        assertEquals(oValue, o)
        assertEquals(outerValue, this@Outer)
        // Observed uninitialized. Make sure that instance initialization has not been run.
        assertNull(initializedField)
      }
    }
  }

  val outer = Outer()
  outer.Capturer(o, outer)
}

private open class FieldReferencesOuter(val text: String) {
  // Refers to FieldReferencesOuter.text explicitly.
  // Refers to Inner.super.text implicitly.

  constructor() : this("Outer")

  inner class Inner : FieldReferencesOuter("InnerSuper") {
    inner class InnerInner {
      val implicitText: String
        get() = text

      val outerText: String
        get() = this@FieldReferencesOuter.text
    }
  }
}

fun testCaptures_fieldReferences() {
  assertEquals("InnerSuper", FieldReferencesOuter().Inner().InnerInner().implicitText)
  assertEquals("Outer", FieldReferencesOuter().Inner().InnerInner().outerText)
}

internal interface InterfaceWithDefaultMethod {
  fun realName(): String

  fun name(): String {
    return realName()
  }
}

fun testOuterCapture_defaultMethodSuper() {
  abstract class SuperOuter : InterfaceWithDefaultMethod

  open class Outer : SuperOuter() {
    override fun realName(): String {
      return "Outer"
    }

    override fun name(): String {
      throw AssertionError("Outer.name() should never be called.")
    }

    inner class Inner : Outer() {
      override fun realName(): String {
        return "Inner"
      }

      fun outerName(): String {
        return super@Outer.name()
      }
    }
  }

  assertEquals("Outer", Outer().Inner().outerName())
}
