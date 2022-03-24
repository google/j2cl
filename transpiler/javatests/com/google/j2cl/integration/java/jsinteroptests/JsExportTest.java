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
package jsinteroptests;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertNotNull;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinteroptests.MyExportedClass.InnerClass;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/** Tests presence and naming of exported classes, fields, and methods. */
public class JsExportTest {
  public static void testAll() {
    testClinit_staticField();
    testClinit_staticMethod();
    testClinit_virtualMethod();
    testClinit();
    testEnum_enumerations();
    testEnum_exportedFields();
    testEnum_exportedMethods();
    testEnum_notExported();
    testEnum_subclassEnumerations();
    testEnum_subclassMethodCallFromExportedEnumerations();
    testExportClass_correctNamespace();
    testExportClass_implicitConstructor();
    testExportConstructors();
    testExportedField();
    testExportedFieldRefInExportedMethod();
    testExportedMethod();
    testInheritClassNamespace();
    testInheritClassNamespace_nested();
    testInheritClassNamespace_nestedNoExport();
    testInheritClassNamespace_noExport();
    testInheritClassNamespace_withName();
    testInheritPackageNamespace_nestedClass();
    testInheritPackageNamespace_nestedEnum();
    testInheritPackageNamespace_subpackage();
    testInheritPackageNamespace();
    testMethodExport_notReferencedFromJava();
    testMethodExport();
    testMethodExportWithLong();
    testNoExport();
  }

  private static void testMethodExport() {
    myClassExportsMethodCallMe4();
    assertTrue(MyClassExportsMethod.calledFromCallMe4);

    myClassExportsMethodCallMe5();
    assertTrue(MyClassExportsMethod.calledFromCallMe5);
  }

  @JsMethod(namespace = "woo.MyClassExportsMethod", name = "exported")
  private static native void myClassExportsMethodCallMe4();

  @JsMethod(namespace = "woo.MyClassExportsMethod", name = "callMe5")
  private static native void myClassExportsMethodCallMe5();

  private static void testMethodExportWithLong() {
    NativeMyJsTypeThatUsesLongType obj = new NativeMyJsTypeThatUsesLongType();

    assertEquals(42L, obj.addLong(40L, 2L));
    assertEquals(82L, NativeMyJsTypeThatUsesLongType.addLongStatic(80L, 2L));
  }

  /**
   * Native interface to type MyJsTypeThatUsesLongType which has been exported to a particular
   * namespaces.
   */
  @JsType(isNative = true, namespace = "woo", name = "MyJsTypeThatUsesLongType")
  private static class NativeMyJsTypeThatUsesLongType {
    public native long addLong(long a, long b);

    public static native long addLongStatic(long a, long b);
  }

  private static void testMethodExport_notReferencedFromJava() {
    // Exported by MyClassExportsMethodWithoutReference which is not referenced by Java. This
    // ensures that we correctly collect root types.
    assertEquals(42, onlyCalledFromJs());
  }

  @JsMethod(
      namespace = "woo.MyClassExportsMethodWithoutReference", name = "onlyCalledFromJs")
  private static native int onlyCalledFromJs();

  private static void testClinit() {
    new NativeMyClassExportsMethodWithClinit();
    assertEquals(23, MyClassExportsMethodWithClinit.magicNumber);
  }

  /**
   * Native interface to type MyClassExportsMethodWithClinit which has been exported to a
   * particular namespaces.
   */
  @JsType(isNative = true, namespace = "woo", name = "MyClassExportsMethodWithClinit")
  private static class NativeMyClassExportsMethodWithClinit {}

  private static void testClinit_staticField() {
    assertNotNull(getStaticInitializerStaticFieldExported1());
    assertNotNull(getStaticInitializerStaticFieldExported2());
    assertNotNull(getStaticInitializerStaticFieldInterfaceStatic());
  }

  @JsProperty(namespace = "woo.StaticInitializerStaticField", name = "EXPORTED_1")
  private static native Object getStaticInitializerStaticFieldExported1();

  @JsProperty(namespace = "woo.StaticInitializerStaticField", name = "EXPORTED_2")
  private static native Object getStaticInitializerStaticFieldExported2();

  @JsProperty(namespace = "woo.StaticInitializerStaticField.InterfaceWithField", name = "STATIC")
  private static native Object getStaticInitializerStaticFieldInterfaceStatic();

  private static void testClinit_staticMethod() {
    assertNotNull(getStaticInitializerStaticMethod());
  }

  @JsMethod(namespace = "woo.StaticInitializerStaticMethod", name = "getInstance")
  private static native Object getStaticInitializerStaticMethod();

  private static void testClinit_virtualMethod() {
    NativeStaticInitializerVirtualMethod instance1 = new NativeStaticInitializerVirtualMethod();
    assertNotNull(instance1);
    NativeStaticInitializerVirtualMethod instance2 = instance1.getInstance();
    assertNotNull(instance2);
  }

  /**
   * Native interface to type StaticInitializerVirtualMethod which has been exported to a
   * particular namespaces.
   */
  @JsType(isNative = true, namespace = "woo", name = "StaticInitializerVirtualMethod")
  private static class NativeStaticInitializerVirtualMethod {
    public native NativeStaticInitializerVirtualMethod getInstance();
  }

  @JsType(namespace = "bar.foo.baz")
  static class MyExportedClassCorrectNamespace {
    public MyExportedClassCorrectNamespace() { }
  }

  private static void testExportClass_correctNamespace() {
    Object o = new NativeMyExportedClassCorrectNamespace();
    assertTrue(o instanceof MyExportedClassCorrectNamespace);
  }

  @JsType(isNative = true, namespace = "bar.foo.baz", name = "MyExportedClassCorrectNamespace")
  private static class NativeMyExportedClassCorrectNamespace { }

  private static void testExportClass_implicitConstructor() {
    Object o = new NativeMyExportedClassWithImplicitConstructor();
    assertNotNull(o);
    assertTrue(o instanceof MyExportedClassWithImplicitConstructor);
  }

  @JsType(isNative = true, namespace = "woo", name = "MyExportedClassWithImplicitConstructor")
  private static class NativeMyExportedClassWithImplicitConstructor { }

  private static void testExportConstructors() {
    MyClassExportsConstructor nativeMyClassExportsConstructor =
        (MyClassExportsConstructor) (Object) new NativeMyClassExportsConstructor(2);
    assertEquals(4, nativeMyClassExportsConstructor.foo());
    assertEquals(2, new MyClassExportsConstructor().foo());
  }

  @JsType(isNative = true, namespace = "woo", name = "MyClassExportsConstructor")
  private static class NativeMyClassExportsConstructor {
    public NativeMyClassExportsConstructor(@SuppressWarnings("unused") int a) {}
  }

  private static void testExportedField() {
    assertEquals(100, MyExportedClass.EXPORTED_1);
    assertEquals(100, getExportedField());

    setExportedField(1000);
    // Different than GWT, fields are not copied to export namespace hence call reflects the change
    assertEquals(1000, MyExportedClass.EXPORTED_1);
    assertEquals(1000, getExportedField());
  }

  @JsProperty(namespace = "woo.MyExportedClass", name = "EXPORTED_1")
  private static native int getExportedField();

  @JsProperty(namespace = "woo.MyExportedClass", name = "EXPORTED_1")
  private static native void setExportedField(int value);

  private static void testExportedMethod() {
    assertEquals(200, MyExportedClass.foo());
    assertEquals(200, callExportedMethod());

    setExportedMethod(getReplacementExportedMethod());
    // Different than GWT, methods are not copied to export namespace hence call reflects the change
    assertEquals(1000, MyExportedClass.foo());
    assertEquals(1000, callExportedMethod());
  }

  @JsMethod(
      namespace = "woo.MyExportedClass", name = "foo")
  private static native int callExportedMethod();

  @JsProperty(
      namespace = "woo.MyExportedClass", name = "foo")
  private static native void setExportedMethod(Object object);

  @JsProperty(
      namespace = "woo.MyExportedClass", name = "replacementFoo")
  private static native Object getReplacementExportedMethod();

  private static void testExportedFieldRefInExportedMethod() {
    assertEquals(5, MyExportedClass.bar(0, 0));
    assertEquals(5, callExportedFieldByExportedMethod(0, 0));
    setExportedField2(myExportedClassNewInnerClass(10));

    // Different than GWT, exported fields and methods are not an immutable copy.
    assertEquals(10, getExportedField2());
    assertEquals(12, MyExportedClass.bar(1, 1));
    assertEquals(12, callExportedFieldByExportedMethod(1, 1));
  }

  @JsMethod(namespace = "woo.MyExportedClass", name = "bar")
  private static native int callExportedFieldByExportedMethod(int a, int b);

  @JsProperty(namespace = "woo.MyExportedClass", name = "EXPORTED_2")
  private static native void setExportedField2(Object a);

  @JsMethod(namespace = "woo.MyExportedClass", name = "newInnerClass")
  private static native InnerClass myExportedClassNewInnerClass(int a);

  @JsProperty(namespace = "woo.MyExportedClass", name = "EXPORTED_2.field")
  private static native int getExportedField2();

  private static void testNoExport() {
    Object staticInitializerStaticMethodCtor =
        PropertyUtils.toCtor(StaticInitializerStaticMethod.class);
    assertFalse(PropertyUtils.hasNotExported_1(staticInitializerStaticMethodCtor));
    assertFalse(PropertyUtils.hasNotExported_2(staticInitializerStaticMethodCtor));

    Object staticInitializerStaticFieldCtor =
        PropertyUtils.toCtor(StaticInitializerStaticField.class);
    assertFalse(PropertyUtils.hasNOT_EXPORTED_1(staticInitializerStaticFieldCtor));
    assertFalse(PropertyUtils.hasNOT_EXPORTED_2(staticInitializerStaticFieldCtor));
    assertFalse(PropertyUtils.hasNOT_EXPORTED_3(staticInitializerStaticFieldCtor));
    assertFalse(PropertyUtils.hasNOT_EXPORTED_4(staticInitializerStaticFieldCtor));
    assertFalse(PropertyUtils.hasNOT_EXPORTED_5(staticInitializerStaticFieldCtor));
  }

  private static void testInheritClassNamespace() {
    assertEquals(42, getBAR());
  }

  @JsProperty(namespace = "foo.MyExportedClassWithNamespace", name = "BAR")
  private static native int getBAR();

  private static void testInheritClassNamespace_withName() {
    assertEquals(42, getBooBAR());
  }

  @JsProperty(namespace = "foo.boo", name = "BAR")
  private static native int getBooBAR();

  private static void testInheritClassNamespace_noExport() {
    assertEquals(99, getBAZ());
  }

  @JsProperty(namespace = "foobaz.MyClassWithNamespace", name = "BAZ")
  private static native int getBAZ();

  private static void testInheritClassNamespace_nested() {
    assertEquals(99, getLOO());
    assertNotNull(new BlooInner());
  }

  @JsProperty(namespace = "woo.Bloo.Inner", name = "LOO")
  private static native int getLOO();

  @JsType(isNative = true, namespace = "woo.Bloo", name = "Inner")
  private static class BlooInner {}

  private static void testInheritClassNamespace_nestedNoExport() {
    assertEquals(999, getWOOZ());
    assertNotNull(new NativeInnerWithNamespace());
  }

  @JsProperty(namespace = "zoo.InnerWithNamespace", name = "WOOZ")
  private static native int getWOOZ();

  @JsType(isNative = true, namespace = "zoo", name = "InnerWithNamespace")
  private static class NativeInnerWithNamespace { }

  private static void testInheritPackageNamespace() {
    assertEquals(1001, getWOO());
  }

  @JsProperty(namespace = "woo.MyExportedClassWithPackageNamespace", name = "WOO")
  private static native int getWOO();

  private static void testInheritPackageNamespace_nestedClass() {
    assertEquals(99, getNestedWOO());
    assertNotNull(new NativeMyClassWithNestedExportedClassInner());
  }

  @JsProperty(namespace = "woo.MyClassWithNestedExportedClass.Inner", name = "WOO")
  private static native int getNestedWOO();

  @JsType(isNative = true, namespace = "woo.MyClassWithNestedExportedClass", name = "Inner")
  private static class NativeMyClassWithNestedExportedClassInner {}

  private static void testInheritPackageNamespace_nestedEnum() {
    assertNotNull(getNestedEnum());
  }

  @JsProperty(namespace = "woo.MyClassWithNestedExportedClass.InnerEnum", name = "AA")
  private static native Object getNestedEnum();

  private static void testInheritPackageNamespace_subpackage() {
    assertNotNull(new NativeMyNestedExportedClassSansPackageNamespace());
  }

  @JsType(
      isNative = true,
      namespace = "jsinteroptests.subpackage",
      name = "MyNestedExportedClassSansPackageNamespace")
  private static class NativeMyNestedExportedClassSansPackageNamespace {}

  private static void testEnum_enumerations() {
    assertNotNull(getEnumerationTEST1());
    assertNotNull(getEnumerationTEST2());
  }

  @JsProperty(namespace = "woo.MyExportedEnum", name = "TEST1")
  private static native Object getEnumerationTEST1();

  @JsProperty(namespace = "woo.MyExportedEnum", name = "TEST2")
  private static native Object getEnumerationTEST2();

  private static void testEnum_exportedMethods() {
    assertNotNull(getPublicStaticMethodInEnum());
    assertNotNull(getValuesMethodInEnum());
    assertNotNull(getValueOfMethodInEnum());
  }

  @JsProperty(namespace = "woo.MyExportedEnum", name = "publicStaticMethod")
  private static native Object getPublicStaticMethodInEnum();

  @JsProperty(namespace = "woo.MyExportedEnum", name = "values")
  private static native Object getValuesMethodInEnum();

  @JsProperty(namespace = "woo.MyExportedEnum", name = "valueOf")
  private static native Object getValueOfMethodInEnum();

  private static void testEnum_exportedFields() {
    assertEquals(1, getPublicStaticFinalFieldInEnum());

    // explicitly marked @JsType() fields must be final
    // but ones that are in a @JsType()ed class don't need to be final
    assertEquals(2, getPublicStaticFieldInEnum());
  }

  @JsProperty(namespace = "woo.MyExportedEnum", name = "publicStaticFinalField")
  private static native int getPublicStaticFinalFieldInEnum();

  @JsProperty(namespace = "woo.MyExportedEnum", name = "publicStaticField")
  private static native int getPublicStaticFieldInEnum();

  private static void testEnum_notExported() {
    Object enumClassCtor = PropertyUtils.toCtor(MyExportedEnum.class);
    assertFalse(PropertyUtils.hasPublicFinalField(enumClassCtor));
    assertFalse(PropertyUtils.hasPrivateStaticFinalField(enumClassCtor));
    assertFalse(PropertyUtils.hasProtectedStaticFinalField(enumClassCtor));
    assertFalse(PropertyUtils.hasDefaultStaticFinalField(enumClassCtor));

    assertFalse(PropertyUtils.hasPublicMethod(enumClassCtor));
    assertFalse(PropertyUtils.hasProtectedStaticMethod(enumClassCtor));
    assertFalse(PropertyUtils.hasPrivateStaticMethod(enumClassCtor));
    assertFalse(PropertyUtils.hasDefaultStaticMethod(enumClassCtor));
  }

  private static void testEnum_subclassEnumerations() {
    assertNotNull(getEnumerationA());
    assertNotNull(getEnumerationB());
    assertNotNull(getEnumerationC());
  }

  @JsProperty(namespace = "woo.MyEnumWithSubclassGen", name = "A")
  private static native Object getEnumerationA();

  @JsProperty(namespace = "woo.MyEnumWithSubclassGen", name = "B")
  private static native Object getEnumerationB();

  @JsProperty(namespace = "woo.MyEnumWithSubclassGen", name = "C")
  private static native Object getEnumerationC();

  private static void testEnum_subclassMethodCallFromExportedEnumerations() {
    assertEquals(100, callPublicMethodFromEnumerationA());
    assertEquals(200, callPublicMethodFromEnumerationB());
    assertEquals(1, callPublicMethodFromEnumerationC());
  }

  @JsMethod(namespace = "woo.MyEnumWithSubclassGen", name = "A.foo")
  private static native int callPublicMethodFromEnumerationA();

  @JsMethod(namespace = "woo.MyEnumWithSubclassGen", name = "B.foo")
  private static native int callPublicMethodFromEnumerationB();

  @JsMethod(namespace = "woo.MyEnumWithSubclassGen", name = "C.foo")
  private static native int callPublicMethodFromEnumerationC();
}
