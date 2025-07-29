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
package jsinteroptests

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertNotNull
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType
import jsinteroptests.MyExportedClass.InnerClass

/** Tests presence and naming of exported classes, fields, and methods. */
object JsExportTest {
  fun testAll() {
    testClinit_staticField()
    testClinit_staticMethod()
    testClinit_virtualMethod()
    testClinit()
    testEnum_enumerations()
    testEnum_exportedFields()
    testEnum_exportedMethods()
    testEnum_notExported()
    testEnum_subclassEnumerations()
    testEnum_subclassMethodCallFromExportedEnumerations()
    testExportClass_correctNamespace()
    testExportClass_implicitConstructor()
    testExportConstructors()
    testExportedField()
    testExportedFieldRefInExportedMethod()
    testExportedMethod()
    testInheritClassNamespace()
    testInheritClassNamespace_nested()
    testInheritClassNamespace_nestedNoExport()
    testInheritClassNamespace_noExport()
    testInheritClassNamespace_withName()
    testInheritPackageNamespace_nestedClass()
    testInheritPackageNamespace_nestedEnum()
    testInheritPackageNamespace_subpackage()
    testInheritPackageNamespace()
    testMethodExport_notReferencedFromJava()
    testMethodExport()
    testMethodExportWithLong()
    testNoExport()
  }

  private fun testMethodExport() {
    myClassExportsMethodCallMe4()
    assertTrue(MyClassExportsMethod.calledFromCallMe4)

    myClassExportsMethodCallMe5()
    assertTrue(MyClassExportsMethod.calledFromCallMe5)
  }

  @JsMethod(namespace = "woo.MyClassExportsMethod", name = "exported")
  @JvmStatic
  private external fun myClassExportsMethodCallMe4()

  @JsMethod(namespace = "woo.MyClassExportsMethod", name = "callMe5")
  @JvmStatic
  private external fun myClassExportsMethodCallMe5()

  private fun testMethodExportWithLong() {
    val obj: NativeMyJsTypeThatUsesLongType = NativeMyJsTypeThatUsesLongType()

    assertEquals(42L, obj.addLong(40L, 2L))
    assertEquals(82L, NativeMyJsTypeThatUsesLongType.addLongStatic(80L, 2L))
  }

  /**
   * Native interface to type MyJsTypeThatUsesLongType which has been exported to a particular
   * namespaces.
   */
  @JsType(isNative = true, namespace = "woo", name = "MyJsTypeThatUsesLongType")
  private class NativeMyJsTypeThatUsesLongType {
    external fun addLong(a: Long, b: Long): Long

    companion object {
      @JvmStatic external fun addLongStatic(a: Long, b: Long): Long
    }
  }

  private fun testMethodExport_notReferencedFromJava() {
    // Exported by MyClassExportsMethodWithoutReference which is not referenced by Java. This
    // ensures that we correctly collect root types.
    assertEquals(42, onlyCalledFromJs())
  }

  @JsMethod(namespace = "woo.MyClassExportsMethodWithoutReference", name = "onlyCalledFromJs")
  @JvmStatic
  private external fun onlyCalledFromJs(): Int

  private fun testClinit() {
    val unused = NativeMyClassExportsMethodWithClinit()
    assertEquals(23, MyClassExportsMethodWithClinit.magicNumber)
  }

  /**
   * Native interface to type MyClassExportsMethodWithClinit which has been exported to a particular
   * namespaces.
   */
  @JsType(isNative = true, namespace = "woo", name = "MyClassExportsMethodWithClinit")
  private class NativeMyClassExportsMethodWithClinit

  private fun testClinit_staticField() {
    assertNotNull(getStaticInitializerStaticFieldExported1())
    assertNotNull(getStaticInitializerStaticFieldExported2())
    assertNotNull(getStaticInitializerStaticFieldInterfaceStatic())
  }

  @JsProperty(namespace = "woo.StaticInitializerStaticField", name = "EXPORTED_1")
  @JvmStatic
  private external fun getStaticInitializerStaticFieldExported1(): Any?

  @JsProperty(namespace = "woo.StaticInitializerStaticField", name = "EXPORTED_2")
  @JvmStatic
  private external fun getStaticInitializerStaticFieldExported2(): Any?

  @JsProperty(namespace = "woo.StaticInitializerStaticField.InterfaceWithField", name = "STATIC")
  @JvmStatic
  private external fun getStaticInitializerStaticFieldInterfaceStatic(): Any?

  private fun testClinit_staticMethod() {
    assertNotNull(getStaticInitializerStaticMethod())
  }

  @JsMethod(namespace = "woo.StaticInitializerStaticMethod", name = "getInstance")
  @JvmStatic
  private external fun getStaticInitializerStaticMethod(): Any?

  private fun testClinit_virtualMethod() {
    val instance1: NativeStaticInitializerVirtualMethod = NativeStaticInitializerVirtualMethod()
    assertNotNull(instance1)
    val instance2: NativeStaticInitializerVirtualMethod = instance1.getInstance()
    assertNotNull(instance2)
  }

  /**
   * Native interface to type StaticInitializerVirtualMethod which has been exported to a particular
   * namespaces.
   */
  @JsType(isNative = true, namespace = "woo", name = "StaticInitializerVirtualMethod")
  private class NativeStaticInitializerVirtualMethod {
    external fun getInstance(): NativeStaticInitializerVirtualMethod
  }

  @JsType(namespace = "bar.foo.baz") internal class MyExportedClassCorrectNamespace constructor()

  private fun testExportClass_correctNamespace() {
    val o: Any? = NativeMyExportedClassCorrectNamespace()
    assertTrue(o is MyExportedClassCorrectNamespace)
  }

  @JsType(isNative = true, namespace = "bar.foo.baz", name = "MyExportedClassCorrectNamespace")
  private class NativeMyExportedClassCorrectNamespace

  private fun testExportClass_implicitConstructor() {
    val o: Any? = NativeMyExportedClassWithImplicitConstructor()
    assertNotNull(o)
    assertTrue(o is MyExportedClassWithImplicitConstructor)
  }

  @JsType(isNative = true, namespace = "woo", name = "MyExportedClassWithImplicitConstructor")
  private class NativeMyExportedClassWithImplicitConstructor

  private fun testExportConstructors() {
    val nativeMyClassExportsConstructor: MyClassExportsConstructor =
      (NativeMyClassExportsConstructor(2) as Any) as MyClassExportsConstructor
    assertEquals(4, nativeMyClassExportsConstructor.foo())
    assertEquals(2, MyClassExportsConstructor().foo())
  }

  @JsType(isNative = true, namespace = "woo", name = "MyClassExportsConstructor")
  private class NativeMyClassExportsConstructor constructor(a: Int)

  private fun testExportedField() {
    assertEquals(100, MyExportedClass.EXPORTED_1)
    assertEquals(100, getExportedField())

    setExportedField(1000)
    // Different than GWT, fields are not copied to export namespace hence call reflects the change
    assertEquals(1000, MyExportedClass.EXPORTED_1)
    assertEquals(1000, getExportedField())
  }

  @JsProperty(namespace = "woo.MyExportedClass", name = "EXPORTED_1")
  @JvmStatic
  private external fun getExportedField(): Int

  @JsProperty(namespace = "woo.MyExportedClass", name = "EXPORTED_1")
  @JvmStatic
  private external fun setExportedField(value: Int)

  private fun testExportedMethod() {
    assertEquals(200, MyExportedClass.foo())
    assertEquals(200, callExportedMethod())

    setExportedMethod(getReplacementExportedMethod())
    // Different than GWT, methods are not copied to export namespace hence call reflects the change
    assertEquals(1000, MyExportedClass.foo())
    assertEquals(1000, callExportedMethod())
  }

  @JsMethod(namespace = "woo.MyExportedClass", name = "foo")
  @JvmStatic
  private external fun callExportedMethod(): Int

  @JsProperty(namespace = "woo.MyExportedClass", name = "foo")
  @JvmStatic
  private external fun setExportedMethod(o: Any?)

  @JsProperty(namespace = "woo.MyExportedClass", name = "replacementFoo")
  @JvmStatic
  private external fun getReplacementExportedMethod(): Any?

  private fun testExportedFieldRefInExportedMethod() {
    assertEquals(5, MyExportedClass.bar(0, 0))
    assertEquals(5, callExportedFieldByExportedMethod(0, 0))
    setExportedField2(myExportedClassNewInnerClass(10))

    // Different than GWT, exported fields and methods are not an immutable copy.
    assertEquals(10, getExportedField2())
    assertEquals(12, MyExportedClass.bar(1, 1))
    assertEquals(12, callExportedFieldByExportedMethod(1, 1))
  }

  @JsMethod(namespace = "woo.MyExportedClass", name = "bar")
  @JvmStatic
  private external fun callExportedFieldByExportedMethod(a: Int, b: Int): Int

  @JsProperty(namespace = "woo.MyExportedClass", name = "EXPORTED_2")
  @JvmStatic
  private external fun setExportedField2(a: Any?)

  @JsMethod(namespace = "woo.MyExportedClass", name = "newInnerClass")
  @JvmStatic
  private external fun myExportedClassNewInnerClass(a: Int): InnerClass

  @JsProperty(namespace = "woo.MyExportedClass", name = "EXPORTED_2.field")
  @JvmStatic
  private external fun getExportedField2(): Int

  private fun testNoExport() {
    val staticInitializerStaticMethodCtor: Any =
      PropertyUtils.toCtor(StaticInitializerStaticMethod::class.java)
    assertFalse(PropertyUtils.hasNotExported_1(staticInitializerStaticMethodCtor))
    assertFalse(PropertyUtils.hasNotExported_2(staticInitializerStaticMethodCtor))

    val staticInitializerStaticFieldCtor: Any =
      PropertyUtils.toCtor(StaticInitializerStaticField::class.java)
    assertFalse(PropertyUtils.hasNOT_EXPORTED_1(staticInitializerStaticFieldCtor))
    assertFalse(PropertyUtils.hasNOT_EXPORTED_2(staticInitializerStaticFieldCtor))
    assertFalse(PropertyUtils.hasNOT_EXPORTED_3(staticInitializerStaticFieldCtor))
    assertFalse(PropertyUtils.hasNOT_EXPORTED_4(staticInitializerStaticFieldCtor))
    assertFalse(PropertyUtils.hasNOT_EXPORTED_5(staticInitializerStaticFieldCtor))
  }

  private fun testInheritClassNamespace() {
    assertEquals(42, getBAR())
  }

  @JsProperty(namespace = "foo.MyExportedClassWithNamespace", name = "BAR")
  @JvmStatic
  private external fun getBAR(): Int

  private fun testInheritClassNamespace_withName() {
    assertEquals(42, getBooBAR())
  }

  @JsProperty(namespace = "foo.boo", name = "BAR") @JvmStatic private external fun getBooBAR(): Int

  private fun testInheritClassNamespace_noExport() {
    assertEquals(99, getBAZ())
  }

  @JsProperty(namespace = "foobaz.MyClassWithNamespace", name = "BAZ")
  @JvmStatic
  private external fun getBAZ(): Int

  private fun testInheritClassNamespace_nested() {
    assertEquals(99, getLOO())
    assertNotNull(BlooInner())
  }

  @JsProperty(namespace = "woo.Bloo.Inner", name = "LOO")
  @JvmStatic
  private external fun getLOO(): Int

  @JsType(isNative = true, namespace = "woo.Bloo", name = "Inner") private class BlooInner

  private fun testInheritClassNamespace_nestedNoExport() {
    assertEquals(999, getWOOZ())
    assertNotNull(NativeInnerWithNamespace())
  }

  @JsProperty(namespace = "zoo.InnerWithNamespace", name = "WOOZ")
  @JvmStatic
  private external fun getWOOZ(): Int

  @JsType(isNative = true, namespace = "zoo", name = "InnerWithNamespace")
  private class NativeInnerWithNamespace

  private fun testInheritPackageNamespace() {
    assertEquals(1001, getWOO())
  }

  @JsProperty(namespace = "woo.MyExportedClassWithPackageNamespace", name = "WOO")
  @JvmStatic
  private external fun getWOO(): Int

  private fun testInheritPackageNamespace_nestedClass() {
    assertEquals(99, getNestedWOO())
    assertNotNull(NativeMyClassWithNestedExportedClassInner())
  }

  @JsProperty(namespace = "woo.MyClassWithNestedExportedClass.Inner", name = "WOO")
  @JvmStatic
  private external fun getNestedWOO(): Int

  @JsType(isNative = true, namespace = "woo.MyClassWithNestedExportedClass", name = "Inner")
  private class NativeMyClassWithNestedExportedClassInner

  private fun testInheritPackageNamespace_nestedEnum() {
    assertNotNull(getNestedEnum())
  }

  @JsProperty(namespace = "woo.MyClassWithNestedExportedClass.InnerEnum", name = "AA")
  @JvmStatic
  private external fun getNestedEnum(): Any?

  private fun testInheritPackageNamespace_subpackage() {
    assertNotNull(NativeMyNestedExportedClassSansPackageNamespace())
  }

  @JsType(
    isNative = true,
    namespace = "jsinteroptests.subpackage",
    name = "MyNestedExportedClassSansPackageNamespace",
  )
  private class NativeMyNestedExportedClassSansPackageNamespace

  private fun testEnum_enumerations() {
    assertNotNull(getEnumerationTEST1())
    assertNotNull(getEnumerationTEST2())
  }

  @JsProperty(namespace = "woo.MyExportedEnum", name = "TEST1")
  @JvmStatic
  private external fun getEnumerationTEST1(): Any?

  @JsProperty(namespace = "woo.MyExportedEnum", name = "TEST2")
  @JvmStatic
  private external fun getEnumerationTEST2(): Any?

  private fun testEnum_exportedMethods() {
    assertNotNull(getPublicStaticMethodInEnum())
    assertNotNull(getValuesMethodInEnum())
    assertNotNull(getValueOfMethodInEnum())
  }

  @JsProperty(namespace = "woo.MyExportedEnum", name = "publicStaticMethod")
  @JvmStatic
  private external fun getPublicStaticMethodInEnum(): Any?

  @JsProperty(namespace = "woo.MyExportedEnum", name = "values")
  @JvmStatic
  private external fun getValuesMethodInEnum(): Any?

  @JsProperty(namespace = "woo.MyExportedEnum", name = "valueOf")
  @JvmStatic
  private external fun getValueOfMethodInEnum(): Any?

  private fun testEnum_exportedFields() {
    assertEquals(1, getPublicStaticFinalFieldInEnum())

    // explicitly marked @JsType() fields must be final
    // but ones that are in a @JsType()ed class don't need to be final
    assertEquals(2, getPublicStaticFieldInEnum())
  }

  @JsProperty(namespace = "woo.MyExportedEnum", name = "publicStaticFinalField")
  @JvmStatic
  private external fun getPublicStaticFinalFieldInEnum(): Int

  @JsProperty(namespace = "woo.MyExportedEnum", name = "publicStaticField")
  @JvmStatic
  private external fun getPublicStaticFieldInEnum(): Int

  private fun testEnum_notExported() {
    val enumClassCtor: Any = PropertyUtils.toCtor(MyExportedEnum::class.java)
    assertFalse(PropertyUtils.hasPublicFinalField(enumClassCtor))
    assertFalse(PropertyUtils.hasPrivateStaticFinalField(enumClassCtor))
    assertFalse(PropertyUtils.hasProtectedStaticFinalField(enumClassCtor))
    assertFalse(PropertyUtils.hasDefaultStaticFinalField(enumClassCtor))

    assertFalse(PropertyUtils.hasPublicMethod(enumClassCtor))
    assertFalse(PropertyUtils.hasProtectedStaticMethod(enumClassCtor))
    assertFalse(PropertyUtils.hasPrivateStaticMethod(enumClassCtor))
    assertFalse(PropertyUtils.hasDefaultStaticMethod(enumClassCtor))
  }

  private fun testEnum_subclassEnumerations() {
    assertNotNull(getEnumerationA())
    assertNotNull(getEnumerationB())
    assertNotNull(getEnumerationC())
  }

  @JsProperty(namespace = "woo.MyEnumWithSubclassGen", name = "A")
  @JvmStatic
  private external fun getEnumerationA(): Any?

  @JsProperty(namespace = "woo.MyEnumWithSubclassGen", name = "B")
  @JvmStatic
  private external fun getEnumerationB(): Any?

  @JsProperty(namespace = "woo.MyEnumWithSubclassGen", name = "C")
  @JvmStatic
  private external fun getEnumerationC(): Any?

  private fun testEnum_subclassMethodCallFromExportedEnumerations() {
    assertEquals(100, callPublicMethodFromEnumerationA())
    assertEquals(200, callPublicMethodFromEnumerationB())
    assertEquals(1, callPublicMethodFromEnumerationC())
  }

  @JsMethod(namespace = "woo.MyEnumWithSubclassGen", name = "A.foo")
  @JvmStatic
  private external fun callPublicMethodFromEnumerationA(): Int

  @JsMethod(namespace = "woo.MyEnumWithSubclassGen", name = "B.foo")
  @JvmStatic
  private external fun callPublicMethodFromEnumerationB(): Int

  @JsMethod(namespace = "woo.MyEnumWithSubclassGen", name = "C.foo")
  @JvmStatic
  private external fun callPublicMethodFromEnumerationC(): Int
}
