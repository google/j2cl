package com.google.j2cl.transpiler.integration.classliteral;

class GenericClass<T> {}

interface GenericInterface<T> {}

public class Main {

  static class Foo {}

  static enum Bar {
    BAR,
    BAZ {};
  }

  public static void testEnum() {
    Object o = Bar.BAR;
    assertSame(Bar.class, o.getClass());

    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$Bar", o.getClass().getName());
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main.Bar",
        o.getClass().getCanonicalName());
    assertEquals("Bar", o.getClass().getSimpleName());
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

  public static void testInterface() {
    assertSame(null, IFoo.class.getSuperclass());
    assertEquals("com.google.j2cl.transpiler.integration.classliteral.IFoo", IFoo.class.getName());
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.IFoo", IFoo.class.getCanonicalName());
    assertEquals("IFoo", IFoo.class.getSimpleName());
    assertEquals(
        "interface com.google.j2cl.transpiler.integration.classliteral.IFoo",
        IFoo.class.toString());

    assert !IFoo.class.isArray() : "IFoo.class.isArray() returned true";
    assert !IFoo.class.isEnum() : "IFoo.class.isEnum() returned true";
    assert !IFoo.class.isPrimitive() : "IFoo.class.isPrimitive() returned true";
    assert IFoo.class.isInterface() : "IFoo.class.isInterface() returned false";
  }

  public static void testClass() {
    Object o = new Main();
    assertEquals(Main.class, o.getClass());

    assertEquals(Object.class, o.getClass().getSuperclass());

    assertEquals("com.google.j2cl.transpiler.integration.classliteral.Main", Main.class.getName());
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$Foo", Foo.class.getName());

    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main", Main.class.getCanonicalName());

    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main.Foo",
        Foo.class.getCanonicalName());

    assertEquals("Main", Main.class.getSimpleName());
    assertEquals("Foo", Foo.class.getSimpleName());

    assertEquals(
        "class com.google.j2cl.transpiler.integration.classliteral.Main", Main.class.toString());
    assertEquals(
        "class com.google.j2cl.transpiler.integration.classliteral.Main$Foo", Foo.class.toString());

    assert !Foo.class.isArray() : "Foo.class.isArray() returned true";
    assert !Foo.class.isEnum() : "Foo.class.isEnum() returned true";
    assert !Foo.class.isPrimitive() : "Foo.class.isPrimitive() returned true";
    assert !Foo.class.isInterface() : "Foo.class.isArray() returned true";
  }

  public static void testPrimitive() {
    assert null == int.class.getSuperclass();

    assertEquals("int", int.class.getName());
    assertEquals("int", int.class.getCanonicalName());
    assertEquals("int", int.class.getSimpleName());
    assertEquals("int", int.class.toString());

    assert !int.class.isArray() : "int.class.isArray() returned true";
    assert !int.class.isEnum() : "int.class.isEnum() returned true";
    assert int.class.isPrimitive() : "int.class.isPrimitive() returned false";
    assert !int.class.isInterface() : "int.class.isInterface() returned true";
  }

  @SuppressWarnings("GetClassOnClass")
  public static void testMisc() {
    assertSame(Class.class, int.class.getClass());
  }

  public static void testGeneric() {
    GenericClass<Number> g = new GenericClass<>();
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.GenericClass",
        GenericClass.class.getName());
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.GenericClass",
        GenericClass.class.getCanonicalName());
    assertEquals("GenericClass", GenericClass.class.getSimpleName());
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.GenericClass", g.getClass().getName());
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.GenericClass",
        g.getClass().getCanonicalName());
    assertEquals("GenericClass", g.getClass().getSimpleName());

    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.GenericInterface",
        GenericInterface.class.getName());
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.GenericInterface",
        GenericInterface.class.getCanonicalName());
    assertEquals("GenericInterface", GenericInterface.class.getSimpleName());
  }

  public static void main(String[] args) {
    testClass();
    testInterface();
    testArray();
    testPrimitive();
    testMisc();
    testEnum();
    testGeneric();
    testEnumSubclass();
  }

  private static void assertEquals(Object expected, Object actual) {
    assertEquals(getFailureMessage(expected, actual), expected, actual);
  }

  private static void assertEquals(String message, Object expected, Object actual) {
    assert expected == null ? actual == null : expected.equals(actual) : message;
  }

  private static void assertSame(Object expected, Object actual) {
    assertSame(getFailureMessage(expected, actual), expected, actual);
  }

  private static void assertNotSame(Object expected, Object actual) {
    String expectedString = expected == null ? null : expected.toString();
    String actualString = actual == null ? null : actual.toString();
    assertNotSame(
        "<" + actualString + "> should not be equal to <" + expectedString + ">", expected, actual);
  }

  private static String getFailureMessage(Object expected, Object actual) {
    String expectedString = expected == null ? null : expected.toString();
    String actualString = actual == null ? null : actual.toString();
    return "Assertion failed: expected<" + expectedString + ">  actual:<" + actualString + ">";
  }

  private static void assertSame(String message, Object expected, Object actual) {
    assert expected == actual : message;
  }

  private static void assertNotSame(String message, Object expected, Object actual) {
    assert expected != actual : message;
  }
}

interface IFoo {}
