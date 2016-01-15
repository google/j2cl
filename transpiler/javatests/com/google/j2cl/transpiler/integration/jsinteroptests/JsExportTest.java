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
package com.google.j2cl.transpiler.integration.jsinteroptests;

import com.google.j2cl.transpiler.integration.jsinteroptests.MyExportedClass.InnerClass;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Tests presence and naming of exported classes, fields, and methods.
 */
public class JsExportTest extends MyTestCase {

  public void testMethodExport() {
    // Deprecated in J2CL, these methods attempt to be a calling interface in front of methods in
    // MyClassExportsMethod that have been exported to a different namespace than the class. But
    // J2CL does not allow members to be exported to a different namespace than their own class.
    // myClassExportsMethodCallMe1();
    // assertTrue(MyClassExportsMethod.calledFromCallMe1);
    //
    // myClassExportsMethodCallMe2();
    // assertTrue(MyClassExportsMethod.calledFromCallMe2);
    //
    // myClassExportsMethodCallMe3();
    // assertTrue(MyClassExportsMethod.calledFromCallMe3);

    myClassExportsMethodCallMe4();
    assertTrue(MyClassExportsMethod.calledFromCallMe4);

    myClassExportsMethodCallMe5();
    assertTrue(MyClassExportsMethod.calledFromCallMe5);
  }

  // Deprecated in J2CL, these methods attempt to be a calling interface in front of methods in
  // MyClassExportsMethod that have been exported to a different namespace than the class. But
  // J2CL does not allow members to be exported to a different namespace than their own class.
  // @JsMethod(namespace = JsPackage.GLOBAL, name = "exported")
  // private static native void myClassExportsMethodCallMe1();
  //
  // @JsMethod(namespace = "exportNamespace", name = "exported")
  // private static native void myClassExportsMethodCallMe2();
  //
  // @JsMethod(namespace = "exportNamespace", name = "callMe3")
  // private static native void myClassExportsMethodCallMe3();

  @JsMethod(namespace = "woo.MyClassExportsMethod", name = "exported")
  private static native void myClassExportsMethodCallMe4();

  @JsMethod(namespace = "woo.MyClassExportsMethod", name = "callMe5")
  private static native void myClassExportsMethodCallMe5();

  public void testMethodExportWithLong() {
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

  public void testMethodExport_notReferencedFromJava() {
    // Exported by MyClassExportsMethodWithoutReference which is not referenced by Java. This
    // ensures that we correctly collect root types.
    assertEquals(42, onlyCalledFromJs());
  }

  @JsMethod(
      namespace = "woo.MyClassExportsMethodWithoutReference", name = "onlyCalledFromJs")
  private static native int onlyCalledFromJs();

//  public void testClinit() {
//    new NativeMyClassExportsMethodWithClinit();
//    assertEquals(23, MyClassExportsMethodWithClinit.magicNumber);
//  }
//
//  /**
//   * Native interface to type MyClassExportsMethodWithClinit which has been exported to a
//   * particular namespaces.
//   */
//  @JsType(isNative = true, namespace = "woo", name = "MyClassExportsMethodWithClinit")
//  private static class NativeMyClassExportsMethodWithClinit { }

  public void testClinit_staticField() {
    assertNotNull(getStaticInitializerStaticFieldExported1());
    assertNotNull(getStaticInitializerStaticFieldExported2());
    assertNotNull(getStaticInitializerStaticFieldInterfaceStatic());
  }

  @JsProperty(namespace = "woo.StaticInitializerStaticField", name = "EXPORTED_1")
  private static native Object getStaticInitializerStaticFieldExported1();

  @JsProperty(namespace = "woo.StaticInitializerStaticField", name = "EXPORTED_2")
  private static native Object getStaticInitializerStaticFieldExported2();

  @JsProperty(namespace = "woo.InterfaceWithField", name = "STATIC")
  private static native Object getStaticInitializerStaticFieldInterfaceStatic();

  public void testClinit_staticMethod() {
    assertNotNull(getStaticInitializerStaticMethod());
  }

  @JsMethod(namespace = "woo.StaticInitializerStaticMethod", name = "getInstance")
  private static native int getStaticInitializerStaticMethod();

//  public void testClinit_virtualMethod() {
//    NativeStaticInitializerVirtualMethod instance1 = new NativeStaticInitializerVirtualMethod();
//    assertNotNull(instance1);
//    NativeStaticInitializerVirtualMethod instance2 = instance1.getInstance();
//    assertNotNull(instance2);
//  }
//
//  /**
//   * Native interface to type StaticInitializerVirtualMethod which has been exported to a
//   * particular namespaces.
//   */
//  @JsType(isNative = true, namespace = "woo", name = "StaticInitializerVirtualMethod")
//  private static class NativeStaticInitializerVirtualMethod {
//    public native NativeStaticInitializerVirtualMethod getInstance();
//  }

  @JsType(namespace = "bar.foo.baz")
  class MyExportedClassCorrectNamespace {
    public MyExportedClassCorrectNamespace() { }
  }

  public void testExportClass_correctNamespace() {
    // Check is deprecated in J2CL because these result in an attempt to import a module that
    // does not exist and this attempt is a compile error in JSCompiler.
    // assertNull(getBarMyExportedClassCorrectNamespace());
    // assertNull(getBarFooMyExportedClassCorrectNamespace());

    Object o = new NativeMyExportedClassCorrectNamespace();
    assertTrue(o instanceof MyExportedClassCorrectNamespace);
  }

  // Check is deprecated in J2CL because these result in an attempt to import a module that does not
  // exist and this attempt is a compile error in JSCompiler.
  // @JsProperty(namespace = "bar", name = "MyExportedClassCorrectNamespace")
  // private static native Object getBarMyExportedClassCorrectNamespace();
  //
  // @JsProperty(namespace = "bar.foo", name = "MyExportedClassCorrectNamespace")
  // private static native Object getBarFooMyExportedClassCorrectNamespace();

  @JsType(isNative = true, namespace = "bar.foo.baz", name = "MyExportedClassCorrectNamespace")
  private static class NativeMyExportedClassCorrectNamespace { }

  public void testExportClass_implicitConstructor() {
    Object o = new NativeMyExportedClassWithImplicitConstructor();
    assertNotNull(o);
    assertTrue(o instanceof MyExportedClassWithImplicitConstructor);
  }

  @JsType(isNative = true, namespace = "woo", name = "MyExportedClassWithImplicitConstructor")
  private static class NativeMyExportedClassWithImplicitConstructor { }

//  public void testExportConstructors() {
//    MyClassExportsConstructor nativeMyClassExportsConstructor =
//        (MyClassExportsConstructor) (Object) new NativeMyClassExportsConstructor(2);
//    assertEquals(4, nativeMyClassExportsConstructor.foo());
//    assertEquals(2, new MyClassExportsConstructor().foo());
//  }
//
//  @JsType(isNative = true, namespace = "woo", name = "MyClassExportsConstructor")
//  private static class NativeMyClassExportsConstructor {
//    public NativeMyClassExportsConstructor(@SuppressWarnings("unused") int a) { }
//  }

  public void testExportedField() {
    assertEquals(100, MyExportedClass.EXPORTED_1);
    assertEquals(100, getExportedField());

    // Different than GWT, exported fields and methods are not an immutable copy.
    setExportedField(1000);
    assertEquals(1000, MyExportedClass.EXPORTED_1);
    assertEquals(1000, getExportedField());
  }

  @JsProperty(namespace = "woo.MyExportedClass", name = "EXPORTED_1")
  private static native int getExportedField();

  @JsProperty(namespace = "woo.MyExportedClass", name = "EXPORTED_1")
  private static native void setExportedField(int value);

  public void testExportedMethod() {
    assertEquals(200, MyExportedClass.foo());
    assertEquals(200, callExportedMethod());

    // Different than GWT, exported fields and methods are not an immutable copy.
    setExportedMethod(getReplacementExportedMethod());
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

  public void testExportedFieldRefInExportedMethod() {
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
  private static native void setExportedField2(InnerClass a);

  @JsMethod(namespace = "woo.MyExportedClass", name = "newInnerClass")
  private static native InnerClass myExportedClassNewInnerClass(int a);

  @JsProperty(namespace = "woo.MyExportedClass", name = "EXPORTED_2.field")
  private static native int getExportedField2();

  public void testNoExport() {
    assertNull(getNotExportedMethod1());
    assertNull(getNotExportedMethod2());

    assertNull(getNotExported1());
    assertNull(getNotExported2());
    assertNull(getNotExported3());
    assertNull(getNotExported4());
    assertNull(getNotExported5());
  }

  @JsProperty(namespace = "woo.StaticInitializerStaticMethod", name = "notExported_1")
  private static native Object getNotExportedMethod1();

  @JsProperty(namespace = "woo.StaticInitializerStaticMethod", name = "notExported_2")
  private static native Object getNotExportedMethod2();

  @JsProperty(namespace = "woo.StaticInitializerStaticField", name = "NOT_EXPORTED_1")
  private static native Object getNotExported1();

  @JsProperty(namespace = "woo.StaticInitializerStaticField", name = "NOT_EXPORTED_2")
  private static native Object getNotExported2();

  @JsProperty(namespace = "woo.StaticInitializerStaticField", name = "NOT_EXPORTED_3")
  private static native Object getNotExported3();

  @JsProperty(namespace = "woo.StaticInitializerStaticField", name = "NOT_EXPORTED_4")
  private static native Object getNotExported4();

  @JsProperty(namespace = "woo.StaticInitializerStaticField", name = "NOT_EXPORTED_5")
  private static native Object getNotExported5();

  public void testInheritClassNamespace() {
    assertEquals(42, getBAR());
  }

  @JsProperty(namespace = "foo.MyExportedClassWithNamespace", name = "BAR")
  private static native int getBAR();

  public void testInheritClassNamespace_empty() {
    assertEquals(82, getDAN());
    assertNotNull(new NativeMyClassWithEmptyNamespace());
  }

  @JsProperty(namespace = "MyClassWithEmptyNamespace", name = "DAN")
  private static native int getDAN();

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "MyClassWithEmptyNamespace")
  private static class NativeMyClassWithEmptyNamespace { }

  public void testInheritClassNamespace_withName() {
    assertEquals(42, getBooBAR());
  }

  @JsProperty(namespace = "foo.boo", name = "BAR")
  private static native int getBooBAR();

  public void testInheritClassNamespace_noExport() {
    assertEquals(99, getBAZ());
  }

  @JsProperty(namespace = "foobaz.MyClassWithNamespace", name = "BAZ")
  private static native int getBAZ();

//  public void testInheritClassNamespace_nested() {
//    assertEquals(99, getLOO());
//    assertNotNull(new BlooInner());
//  }
//
//  @JsProperty(namespace = "woo.Bloo", name = "Inner.LOO")
//  private static native int getLOO();
//
//  @JsType(isNative = true, namespace = "woo.Bloo", name = "Inner")
//  private static class BlooInner { }

  public void testInheritClassNamespace_nestedNoExport() {
    assertEquals(999, getWOOZ());
    assertNotNull(new NativeInnerWithNamespace());
  }

  @JsProperty(namespace = "zoo.InnerWithNamespace", name = "WOOZ")
  private static native int getWOOZ();

  @JsType(isNative = true, namespace = "zoo", name = "InnerWithNamespace")
  private static class NativeInnerWithNamespace { }

  public void testInheritPackageNamespace() {
    assertEquals(1001, getWOO());
  }

  @JsProperty(namespace = "woo.MyExportedClassWithPackageNamespace", name = "WOO")
  private static native int getWOO();

//  public void testInheritPackageNamespace_nestedClass() {
//    assertEquals(99, getNestedWOO());
//    assertNotNull(new NativeMyClassWithNestedExportedClassInner());
//  }
//
//  @JsProperty(namespace = "woo.MyClassWithNestedExportedClass.Inner", name = "WOO")
//  private static native int getNestedWOO();
//
//  @JsType(isNative = true, namespace = "woo.MyClassWithNestedExportedClass", name = "Inner")
//  private static class NativeMyClassWithNestedExportedClassInner { }
//
//  public void testInheritPackageNamespace_nestedEnum() {
//    assertNotNull(getNestedEnum());
//  }
//
//  @JsProperty(namespace = "woo.MyClassWithNestedExportedClass", name = "InnerEnum.AA")
//  private static native Object getNestedEnum();

  public void testInheritPackageNamespace_subpackage() {
    // This is different than GWT. Sub packages can't be imported or referenced in a goog.module()
    // world. To attempt it is a compile error.
    // assertNull(getNestedSubpackage());

    assertNotNull(new NativeMyNestedExportedClassSansPackageNamespace());
  }

//  @JsProperty(namespace = "woo", name = "subpackage")
//  private static native Object getNestedSubpackage();

  @JsType(
      isNative = true,
      namespace = "com.google.j2cl.transpiler.integration.jsinteroptests.subpackage",
      name = "MyNestedExportedClassSansPackageNamespace")
  private static class NativeMyNestedExportedClassSansPackageNamespace {}

  public void testEnum_enumerations() {
    assertNotNull(getEnumerationTEST1());
    assertNotNull(getEnumerationTEST2());
  }

  @JsProperty(namespace = "woo.MyExportedEnum", name = "TEST1")
  private static native Object getEnumerationTEST1();

  @JsProperty(namespace = "woo.MyExportedEnum", name = "TEST2")
  private static native Object getEnumerationTEST2();

  public void testEnum_exportedMethods() {
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

  public void testEnum_exportedFields() {
    assertEquals(1, getPublicStaticFinalFieldInEnum());

    // explicitly marked @JsType() fields must be final
    // but ones that are in a @JsType()ed class don't need to be final
    assertEquals(2, getPublicStaticFieldInEnum());
  }

  @JsProperty(namespace = "woo.MyExportedEnum", name = "publicStaticFinalField")
  private static native int getPublicStaticFinalFieldInEnum();

  @JsProperty(namespace = "woo.MyExportedEnum", name = "publicStaticField")
  private static native int getPublicStaticFieldInEnum();

  public void testEnum_notExported() {
    assertNull(myExportedEnumPublicFinalField());
    assertNull(myExportedEnumPrivateStaticFinalField());
    assertNull(myExportedEnumProtectedStaticFinalField());
    assertNull(myExportedEnumDefaultStaticFinalField());

    assertNull(myExportedEnumPublicMethod());
    assertNull(myExportedEnumProtectedStaticMethod());
    assertNull(myExportedEnumPrivateStaticMethod());
    assertNull(myExportedEnumDefaultStaticMethod());
  }

  @JsProperty(namespace = "woo.MyExportedEnum", name = "publicFinalField")
  private static native Object myExportedEnumPublicFinalField();

  @JsProperty(namespace = "woo.MyExportedEnum", name = "privateStaticFinalField")
  private static native Object myExportedEnumPrivateStaticFinalField();

  @JsProperty(namespace = "woo.MyExportedEnum", name = "protectedStaticFinalField")
  private static native Object myExportedEnumProtectedStaticFinalField();

  @JsProperty(namespace = "woo.MyExportedEnum", name = "defaultStaticFinalField")
  private static native Object myExportedEnumDefaultStaticFinalField();

  @JsProperty(namespace = "woo.MyExportedEnum", name = "publicMethod")
  private static native Object myExportedEnumPublicMethod();

  @JsProperty(namespace = "woo.MyExportedEnum", name = "protectedStaticMethod")
  private static native Object myExportedEnumProtectedStaticMethod();

  @JsProperty(namespace = "woo.MyExportedEnum", name = "privateStaticMethod")
  private static native Object myExportedEnumPrivateStaticMethod();

  @JsProperty(namespace = "woo.MyExportedEnum", name = "defaultStaticMethod")
  private static native Object myExportedEnumDefaultStaticMethod();

  public void testEnum_subclassEnumerations() {
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

  public void testEnum_subclassMethodCallFromExportedEnumerations() {
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
