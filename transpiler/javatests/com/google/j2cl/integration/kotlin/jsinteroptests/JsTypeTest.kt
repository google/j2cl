/*
 * Copyright 2015 Google Inc.
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
@file:Suppress("KotlinConstantConditions")

package jsinteroptests

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertNotNull
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsConstructor
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

object JsTypeTest {
  fun testAll() {
    testAbstractJsTypeAccess()
    testConcreteJsTypeAccess()
    testConcreteJsTypeNoTypeTightenField()
    testConcreteJsTypeSubclassAccess()
    testEnumeration()
    testEnumJsTypeAccess()
    testEnumSubclassEnumeration()
    testJsMethodWithDifferentVisiblities()
    testJsTypeField()
    testNamedBridge()
    testNativeMethodOverrideNoTypeTightenParam()
    testRevealedOverrideJsType()
    testInheritName()
  }

  /** This concrete test class is *directly* annotated as a @JsType. */
  @JsType
  internal abstract class AbstractJsType {
    abstract fun publicMethod(): Int
  }

  /** A regular class with no exports. */
  open class PlainParentType {

    /** A simple function that is not itself exported. */
    fun run() {}
  }

  /**
   * This type implements no functionality but should still export run() because of the @JsType
   * interface that it implements.
   */
  class RevealedOverrideSubType : PlainParentType(), JsTypeRunnable {}

  /** Causes the run() function of any implementor to be exported. */
  @JsType
  interface JsTypeRunnable {
    fun run()
  }

  /** This test class exposes parent jsmethod as non-jsmethod. */
  internal class ConcreteJsTypeJsSubclass @JsConstructor constructor() :
    ConcreteJsType(), SubclassInterface

  interface SubclassInterface {
    fun publicMethodAlsoExposedAsNonJsMethod(): Int
  }

  /** This enum is annotated as a @JsType. */
  @SuppressWarnings("ImmutableEnumChecker")
  @JsType
  enum class MyEnumWithJsType constructor(val idx: Int) {
    TEST1(1),
    TEST2(2);

    fun idxAddOne(): Int = idx + 1

    fun publicMethod(): Int = 10

    private fun privateMethod() {}

    protected fun protectedMethod() {}

    internal fun packageMethod() {}

    val publicField: Int = 10

    private val privateField: Int = 10

    protected val protectedField: Int = 10

    internal val packageField: Int = 10

    companion object {
      @JvmStatic fun publicStaticMethod() {}

      @JvmField val publicStaticField: Int = 10
    }
  }

  /** This enum is annotated exported and has enumerations which cause subclass generation. */
  @SuppressWarnings("ImmutableEnumChecker")
  @JsType
  enum class MyEnumWithSubclassGen {
    A {
      override fun foo(): Int = 100
    },
    B {
      override fun foo(): Int = 200
    },
    C;

    open fun foo(): Int = 1
  }

  private fun testConcreteJsTypeAccess() {
    val concreteJsType = ConcreteJsType()

    assertTrue(ConcreteJsType.hasPublicField(concreteJsType))
    assertTrue(ConcreteJsType.hasPublicMethod(concreteJsType))

    assertFalse(ConcreteJsType.hasPublicStaticMethod(concreteJsType))
    assertFalse(ConcreteJsType.hasPrivateMethod(concreteJsType))
    assertFalse(ConcreteJsType.hasProtectedMethod(concreteJsType))
    assertFalse(ConcreteJsType.hasPackageMethod(concreteJsType))
    assertFalse(ConcreteJsType.hasPublicStaticField(concreteJsType))
    assertFalse(ConcreteJsType.hasPrivateField(concreteJsType))
    assertFalse(ConcreteJsType.hasProtectedField(concreteJsType))
    assertFalse(ConcreteJsType.hasPackageField(concreteJsType))

    assertEquals(10, callPublicMethod(concreteJsType))
  }

  private fun testAbstractJsTypeAccess() {
    val jsType =
      object : AbstractJsType() {
        override fun publicMethod(): Int = 32
      }

    assertTrue(ConcreteJsType.hasPublicMethod(jsType))
    assertEquals(32, callPublicMethod(jsType))
    assertEquals(32, jsType.publicMethod())
  }

  private fun testConcreteJsTypeSubclassAccess() {
    val concreteJsTypeSubclass = ConcreteJsTypeSubclass()

    // A subclass of a JsType is not itself a JsType.
    assertFalse(PropertyUtils.hasPublicSubclassMethod(concreteJsTypeSubclass))
    assertFalse(PropertyUtils.hasPublicSubclassField(concreteJsTypeSubclass))
    assertFalse(PropertyUtils.hasPublicStaticSubclassMethod(concreteJsTypeSubclass))
    assertFalse(PropertyUtils.hasPrivateSubclassMethod(concreteJsTypeSubclass))
    assertFalse(PropertyUtils.hasProtectedSubclassMethod(concreteJsTypeSubclass))
    assertFalse(PropertyUtils.hasPackageSubclassMethod(concreteJsTypeSubclass))
    assertFalse(PropertyUtils.hasPublicStaticSubclassField(concreteJsTypeSubclass))
    assertFalse(PropertyUtils.hasPrivateSubclassField(concreteJsTypeSubclass))
    assertFalse(PropertyUtils.hasProtectedSubclassField(concreteJsTypeSubclass))
    assertFalse(PropertyUtils.hasPackageSubclassField(concreteJsTypeSubclass))

    // But if it overrides an exported method then the overriding method will be exported.
    assertTrue(ConcreteJsType.hasPublicMethod(concreteJsTypeSubclass))

    assertEquals(20, callPublicMethod(concreteJsTypeSubclass))
    assertEquals(10, concreteJsTypeSubclass.publicSubclassMethod())
  }

  private fun testConcreteJsTypeNoTypeTightenField() {
    // If we type-tighten, java side will see no calls and think that field could only AImpl1.
    val concreteJsType = ConcreteJsType()
    setTheField(concreteJsType, ConcreteJsType.AImpl2())
    assertEquals(101, concreteJsType.notTypeTightenedField.x())
  }

  @JsType
  internal interface A {
    fun m(o: Any?): Boolean
  }

  private class AImpl : A {
    override fun m(o: Any?): Boolean = o == null
  }

  private fun testNativeMethodOverrideNoTypeTightenParam() {
    val a = AImpl()
    assertTrue(a.m(null))
    assertFalse(callM(a, Any()) as Boolean)
  }

  @JsMethod(namespace = "jsinteroptests.JsTypeTestHelper")
  @JvmStatic
  private external fun callM(obj: Any?, param: Any?): Any?

  private fun testRevealedOverrideJsType() {
    val plainParentType = PlainParentType()
    val revealedOverrideSubType = RevealedOverrideSubType()

    // PlainParentType is neither @JsType or @JsType and so exports no functions.
    assertFalse(hasFieldRun(plainParentType))

    // RevealedOverrideSubType defines no functions itself, it only inherits them, but it still
    // exports run() because it implements the @JsType interface JsTypeRunnable.
    assertTrue(hasFieldRun(revealedOverrideSubType))

    val subclass = ConcreteJsTypeJsSubclass()
    assertEquals(100, subclass.publicMethodAlsoExposedAsNonJsMethod())
    val subclassInterface: SubclassInterface = subclass
    assertEquals(100, subclassInterface.publicMethodAlsoExposedAsNonJsMethod())
  }

  @JsMethod(namespace = "jsinteroptests.JsTypeTestHelper")
  @JvmStatic
  private external fun hasFieldRun(obj: Any?): Boolean

  private fun testEnumeration() {
    assertEquals(2, callPublicMethodFromEnumeration(MyEnumWithJsType.TEST1))
    assertEquals(3, callPublicMethodFromEnumeration(MyEnumWithJsType.TEST2))
  }

  private fun testEnumJsTypeAccess() {
    assertTrue(ConcreteJsType.hasPublicField(MyEnumWithJsType.TEST2))
    assertTrue(ConcreteJsType.hasPublicMethod(MyEnumWithJsType.TEST2))

    assertFalse(ConcreteJsType.hasPublicStaticMethod(MyEnumWithJsType.TEST2))
    assertFalse(ConcreteJsType.hasPrivateMethod(MyEnumWithJsType.TEST2))
    assertFalse(ConcreteJsType.hasProtectedMethod(MyEnumWithJsType.TEST2))
    assertFalse(ConcreteJsType.hasPackageMethod(MyEnumWithJsType.TEST2))
    assertFalse(ConcreteJsType.hasPublicStaticField(MyEnumWithJsType.TEST2))
    assertFalse(ConcreteJsType.hasPrivateField(MyEnumWithJsType.TEST2))
    assertFalse(ConcreteJsType.hasProtectedField(MyEnumWithJsType.TEST2))
    assertFalse(ConcreteJsType.hasPackageField(MyEnumWithJsType.TEST2))
  }

  private fun testEnumSubclassEnumeration() {
    assertEquals(100, callPublicMethodFromEnumerationSubclass(MyEnumWithSubclassGen.A))
    assertEquals(200, callPublicMethodFromEnumerationSubclass(MyEnumWithSubclassGen.B))
    assertEquals(1, callPublicMethodFromEnumerationSubclass(MyEnumWithSubclassGen.C))
  }

  @JsMethod(namespace = "jsinteroptests.JsTypeTestHelper")
  @JvmStatic
  private external fun callPublicMethod(o: Any?): Int

  @JsMethod(namespace = "jsinteroptests.JsTypeTestHelper")
  @JvmStatic
  private external fun isUndefined(value: Any?): Boolean

  @JsMethod(namespace = "jsinteroptests.JsTypeTestHelper")
  @SuppressWarnings("unusable-by-js")
  @JvmStatic
  private external fun setTheField(obj: ConcreteJsType?, value: ConcreteJsType.A?)

  @JsMethod(namespace = "jsinteroptests.JsTypeTestHelper")
  @JvmStatic
  private external fun callPublicMethodFromEnumeration(enumeration: MyEnumWithJsType?): Int

  @JsMethod(namespace = "jsinteroptests.JsTypeTestHelper")
  @JvmStatic
  private external fun callPublicMethodFromEnumerationSubclass(e: MyEnumWithSubclassGen?): Int

  @JsType interface SimpleJsTypeFieldInterface {}

  internal class SimpleJsTypeFieldClass : SimpleJsTypeFieldInterface {}

  internal class SimpleJsTypeWithField {
    @JsProperty @JvmField val someField: SimpleJsTypeFieldInterface? = null
  }

  private fun testJsTypeField() {
    assertTrue(SimpleJsTypeFieldClass() != SimpleJsTypeFieldClass())
    val holder = SimpleJsTypeWithField()
    fillJsTypeField(holder)
    val someField: SimpleJsTypeFieldInterface? = holder.someField
    assertNotNull(someField)
  }

  @JsMethod(namespace = "jsinteroptests.JsTypeTestHelper")
  @JvmStatic
  internal external fun fillJsTypeField(jstype: SimpleJsTypeWithField)

  @JsType
  internal abstract class SomeAbstractClass {
    abstract fun m(): SomeAbstractClass
  }

  // Do not rename this class.
  @JsType
  internal abstract class SomeZAbstractSubclass : SomeAbstractClass() {
    override abstract fun m(): SomeAbstractClass
  }

  @JsType
  internal class SomeConcreteSubclass : SomeZAbstractSubclass() {
    override fun m(): SomeAbstractClass = this
  }

  private fun testNamedBridge() {
    // Bridges are sorted by signature in the JDT. Make sure that the bridge method appears second.
    // GWT specific test.
    // assertTrue(
    //  SomeConcreteSubclass.class.getName().compareTo(SomeZAbstractSubclass.class.getName()) < 0);
    val o = SomeConcreteSubclass()
    assertEquals(o, o.m())
  }

  internal class NonPublicJsMethodClass {
    @JsMethod private fun foo(): String = "foo"

    internal fun fooProxy() = foo()

    @JsMethod internal fun bar(): String = "bar"
  }

  private fun testJsMethodWithDifferentVisiblities() {
    val instance = NonPublicJsMethodClass()
    assertEquals("foo", instance.fooProxy())
    assertEquals("bar", instance.bar())
    assertEquals("foo", callFoo(instance, null))
    assertEquals("bar", callBar(instance, null))
  }

  @JsMethod(namespace = "jsinteroptests.JsTypeTestHelper")
  @JvmStatic
  public external fun callFoo(obj: Any?, param: Any?): Any?

  @JsMethod(namespace = "jsinteroptests.JsTypeTestHelper")
  @JvmStatic
  public external fun callBar(obj: Any?, param: Any?): Any?

  internal open class ClassWithJsMethod {
    @JsMethod(name = "name") open fun className(): String = ClassWithJsMethod::class.java.name
  }

  internal class ClassWithJsMethodInheritingName : ClassWithJsMethod() {
    @JsMethod override fun className(): String = ClassWithJsMethodInheritingName::class.java.name
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  private interface HasName {
    fun name(): String
  }

  @JvmStatic private fun callName(o: Any?): String = (o as HasName).name()

  private fun testInheritName() {
    var o = ClassWithJsMethod()
    assertEquals(ClassWithJsMethod::class.java.name, o.className())
    assertEquals(ClassWithJsMethod::class.java.name, callName(o))

    o = ClassWithJsMethodInheritingName()
    assertEquals(ClassWithJsMethodInheritingName::class.java.name, o.className())
    assertEquals(ClassWithJsMethodInheritingName::class.java.name, callName(o))
  }
}
