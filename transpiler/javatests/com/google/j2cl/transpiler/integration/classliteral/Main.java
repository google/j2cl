package com.google.j2cl.transpiler.integration.classliteral;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {

  class Foo {}

  public static void testClass() {
    Object o = new Main();
    assertSame(Main.class, o.getClass());
    assertSame(Object.class, o.getClass().getSuperclass());
    assertSame(null, Object.class.getSuperclass());

    assertEquals("com.google.j2cl.transpiler.integration.classliteral.Main", Main.class.getName());
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$Foo", Foo.class.getName());

    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main", Main.class.getCanonicalName());
    // J2CL doesn't follow JLS here:
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$Foo",
        Foo.class.getCanonicalName());

    assertEquals("Main", Main.class.getSimpleName());
    // J2CL doesn't follow JLS here:
    assertEquals("Main$Foo", Foo.class.getSimpleName());

    assertEquals(
        "class com.google.j2cl.transpiler.integration.classliteral.Main", Main.class.toString());
    assertEquals(
        "class com.google.j2cl.transpiler.integration.classliteral.Main$Foo", Foo.class.toString());

    assert !Foo.class.isArray() : "Foo.class.isArray() returned true";
    assert !Foo.class.isEnum() : "Foo.class.isEnum() returned true";
    assert !Foo.class.isPrimitive() : "Foo.class.isPrimitive() returned true";
    assert !Foo.class.isInterface() : "Foo.class.isArray() returned true";
  }

  interface IFoo {}

  public static void testInterface() {
    assertSame(null, IFoo.class.getSuperclass());

    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$IFoo", IFoo.class.getName());
    // J2CL doesn't follow JLS here:
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$IFoo",
        IFoo.class.getCanonicalName());
    assertEquals("Main$IFoo", IFoo.class.getSimpleName());
    // J2CL doesn't follow JLS here:
    assertEquals(
        "interface com.google.j2cl.transpiler.integration.classliteral.Main$IFoo",
        IFoo.class.toString());

    assert !IFoo.class.isArray() : "IFoo.class.isArray() returned true";
    assert !IFoo.class.isEnum() : "IFoo.class.isEnum() returned true";
    assert !IFoo.class.isPrimitive() : "IFoo.class.isPrimitive() returned true";
    assert IFoo.class.isInterface() : "IFoo.class.isInterface() returned false";
  }

  public static void testPrimitive() {
    assertSame(null, int.class.getSuperclass());

    assertEquals("int", int.class.getName());
    assertEquals("int", int.class.getCanonicalName());
    assertEquals("int", int.class.getSimpleName());
    assertEquals("int", int.class.toString());

    assert !int.class.isArray() : "int.class.isArray() returned true";
    assert !int.class.isEnum() : "int.class.isEnum() returned true";
    assert int.class.isPrimitive() : "int.class.isPrimitive() returned false";
    assert !int.class.isInterface() : "int.class.isInterface() returned true";
  }

  public static void testPrimitivesUnboxed() {
    Object b = true;
    Object d = 0.1;
    assertEquals(Boolean.class, b.getClass());
    assertEquals(Double.class, d.getClass());
  }

  static enum Bar {
    BAR,
    BAZ {};
  }

  public static void testEnum() {
    Object o = Bar.BAR;
    assertSame(Bar.class, o.getClass());
    assertSame(Enum.class, o.getClass().getSuperclass());

    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$Bar", o.getClass().getName());
    // J2CL doesn't follow JLS here:
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$Bar",
        o.getClass().getCanonicalName());
    // J2CL doesn't follow JLS here:
    assertEquals("Main$Bar", o.getClass().getSimpleName());
    assertEquals(
        "class com.google.j2cl.transpiler.integration.classliteral.Main$Bar",
        o.getClass().toString());


    assert !o.getClass().isArray() : "Bar.BAR.class.isArray() returned true";
    assert o.getClass().isEnum() : "Bar.BAR.class.isEnum() returned false";
    assert !o.getClass().isPrimitive() : "Bar.BAR.class.isPrimitive() returned true";
    assert !o.getClass().isInterface() : "Bar.BAR.class.isInterface() returned true";
  }

  public static void testEnumSubclass() {
    Object o = Bar.BAZ;
    assertNotSame(Bar.class, o.getClass());
    assertSame(Bar.class, o.getClass().getSuperclass());

    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$Bar$1", o.getClass().getName());
    assertEquals(
        "class com.google.j2cl.transpiler.integration.classliteral.Main$Bar$1",
        o.getClass().toString());

    assert !o.getClass().isArray() : "Bar.BAZ.class.isArray() returned true";
    assert !o.getClass().isEnum() : "Bar.BAZ.class.isEnum() returned true";
    assert !o.getClass().isPrimitive() : "Bar.BAZ.class.isPrimitive() returned true";
    assert !o.getClass().isInterface() : "Bar.BAZ.class.isInterface() returned true";
  }

  public static void testArray() {
    Object o = new Foo[3];
    assertSame(Foo[].class, o.getClass());
    assertSame(Object.class, o.getClass().getSuperclass());

    assertSame(Foo.class, o.getClass().getComponentType());
    assertSame(Foo[].class, Foo[][].class.getComponentType());

    assertEquals("[L" + Foo.class.getName() + ";", o.getClass().getName());
    assertEquals(Foo.class.getCanonicalName() + "[]", o.getClass().getCanonicalName());
    assertEquals(Foo.class.getSimpleName() + "[]", o.getClass().getSimpleName());
    assertEquals("class [L" + Foo.class.getName() + ";", o.getClass().toString());

    assert o.getClass().isArray() : "Foo[].class.isArray() returned false";
    assert !o.getClass().isEnum() : "Foo[].class.isEnum() returned true";
    assert !o.getClass().isPrimitive() : "Foo[].class.isPrimitive() returned true";
    assert !o.getClass().isInterface() : "Foo[].class.isInterface() returned true";

    Foo[][] f = new Foo[3][3];
    assertSame(Foo[][].class, f.getClass());
    assertSame(Foo[].class, f[0].getClass());
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  interface NativeType {}

  @JsFunction
  interface NativeFunction {
    void f();
  }

  public static void testNative() {
    assertEquals("JavaScriptObject", NativeType.class.getName());
    assertEquals("JavaScriptFunction", NativeFunction.class.getName());
  }

  static class GenericClass<T> {}

  interface GenericInterface<T> {}

  public static void testGeneric() {
    GenericClass<Number> g = new GenericClass<>();
    assertSame(GenericClass.class, g.getClass());
    assertEquals("Main$GenericClass", GenericClass.class.getSimpleName());
    assertEquals("Main$GenericInterface", GenericInterface.class.getSimpleName());
  }

  private static boolean clinitCalled = false;

  static class ClinitTest {
    static {
      clinitCalled = true;
    }
  }

  private static void testClinit() {
    assert clinitCalled == false;
    assert ClinitTest.class != null;
    assert clinitCalled == false;
  }

  @SuppressWarnings("GetClassOnClass")
  public static void testMisc() {
    assertSame(Class.class, Object.class.getClass());
    assertSame(Class.class, int.class.getClass());
    assertSame(Class.class, Object[].class.getClass());

    try {
      Object nullObject = null;
      nullObject.getClass();
      assert false;
    } catch (NullPointerException expected) {
      // expected
    }
  }

  public static void main(String[] args) {
    testClass();
    testInterface();
    testPrimitive();
    testPrimitivesUnboxed();
    testEnum();
    testEnumSubclass();
    testArray();
    testNative();
    testGeneric();
    testClinit();
    testMisc();
  }

  private static void assertEquals(Object expected, Object actual) {
    assert expected == null ? actual == null : expected.equals(actual)
        : getFailureMessage(expected, actual, "should be equal");
  }

  private static void assertSame(Object expected, Object actual) {
    assert expected == actual : getFailureMessage(expected, actual, "should be same");
  }

  private static void assertNotSame(Object expected, Object actual) {
    assert expected != actual : getFailureMessage(expected, actual, "should not be same");
  }

  private static String getFailureMessage(Object expected, Object actual, String msg) {
    String expectedString = expected == null ? null : expected.toString();
    String actualString = actual == null ? null : actual.toString();
    return "<" + actualString + "> " + msg + " to <" + expectedString + ">";
  }
}
