package com.google.j2cl.transpiler.integration.jsinteroptests;

import static jsinterop.annotations.JsPackage.GLOBAL;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

/**
 * Tests native JsType functionality.
 */
public class NativeJsTypeTest extends MyTestCase {
  @JsType(isNative = true)
  static class MyNativeJsType {
    // TODO(rluble): these methods should be synthesized by the compiler.
    @Override
    public native String toString();

    @Override
    public native boolean equals(Object o);

    @Override
    public native int hashCode();
  }

  @JsType(isNative = true)
  interface MyNativeJsTypeInterface {}

  public void testClassLiterals() {
    assertEquals("JavaScriptObject", MyNativeJsType.class.getName());
    assertEquals("JavaScriptObject", MyNativeJsTypeInterface.class.getName());
    // Currently in gwt, class literal of native js type array returns JavaScriptObject.class.
    assertEquals(Object[].class, MyNativeJsType[].class);
    assertEquals(Object[].class, MyNativeJsTypeInterface[].class);
    assertEquals(Object[].class, MyNativeJsType[][].class);
    assertEquals(Object[].class, MyNativeJsTypeInterface[][].class);

    Object nativeObject = createEmptyNativeObject();
    assertEquals("JavaScriptObject", nativeObject.getClass().getName());
    assertEquals("JavaScriptObject", ((MyNativeJsTypeInterface) nativeObject).getClass().getName());
  }

  public void testToString() {
    Object nativeObjectWithToString = createNativeObjectWithToString();
    assertEquals("Native type", nativeObjectWithToString.toString());

    Object nativeObjectWithoutToString = createEmptyNativeObject();
    assertEquals("[object Object]", nativeObjectWithoutToString.toString());

    // Different from gwt, in gwt nativeArray.toString() returns "".
    Object nativeArray = createNativeArray();
    assertTrue(nativeArray.toString().startsWith("[Ljava.lang.Object;"));
  }

  public void testEquals() {
    Object obj1 = createEmptyNativeObject();
    Object obj2 = createEmptyNativeObject();
    assert obj1.equals(obj1);
    assert !obj1.equals(obj2);

    MyNativeJsType m1 = createNativeObjectWithEquals(10);
    MyNativeJsType m2 = createNativeObjectWithEquals(10);
    MyNativeJsType m3 = createNativeObjectWithEquals(20);
    assert m1.equals(m2);
    assert !m1.equals(m3);

    Object o1 = createNativeObjectWithEquals(10);
    Object o2 = createNativeObjectWithEquals(10);
    Object o3 = createNativeObjectWithEquals(20);
    assert o1.equals(o2);
    assert !o1.equals(o3);
    
    Object nativeArray1 = createNativeArray();
    Object nativeArray2 = createNativeArray();
    assert nativeArray1.equals(nativeArray1);
    assert !nativeArray1.equals(nativeArray2);
  }

  public void testHashCode() {
    Object obj1 = createEmptyNativeObject();
    Object obj2 = createEmptyNativeObject();
    assert obj1.hashCode() == obj1.hashCode();
    assert obj1.hashCode() != obj2.hashCode();

    MyNativeJsType m = createNativeObjectWithHashCode();
    assert m.hashCode() == 100;
    Object o = createNativeObjectWithHashCode();
    assert o.hashCode() == 100;

    Object nativeArray1 = createNativeArray();
    Object nativeArray2 = createNativeArray();
    assert nativeArray1.hashCode() == nativeArray1.hashCode();
    assert nativeArray1.hashCode() != nativeArray2.hashCode();
  }

  @JsMethod
  private static native MyNativeJsType createNativeObjectWithToString();

  @JsMethod
  private static native MyNativeJsType createEmptyNativeObject();

  @JsMethod
  private static native MyNativeJsType createNativeObjectWithEquals(int x);

  @JsMethod
  private static native MyNativeJsType createNativeObjectWithHashCode();

  @JsMethod
  private static native Object createNativeArray();

  @JsType(isNative = true, namespace = GLOBAL, name = "Object")
  static class NativeJsTypeWithOverlay {

    @JsOverlay
    public static final int X = 2;

    public static native String[] keys(Object o);

    @JsOverlay
    public static final boolean hasM(Object obj) {
      return keys(obj)[0].equals("m");
    }

    public native boolean hasOwnProperty(String name);

    @JsOverlay
    public final boolean hasM() {
      return hasOwnProperty("m");
    }
  }

  @JsMethod
  private static native NativeJsTypeWithOverlay createNativeJsTypeWithOverlay();

  public void testNativeJsTypeWithOverlay() {
    NativeJsTypeWithOverlay object = createNativeJsTypeWithOverlay();
    assertTrue(object.hasM());
    assertTrue(NativeJsTypeWithOverlay.hasM(object));
    assertEquals(2, NativeJsTypeWithOverlay.X);
  }

  @JsType(isNative = true)
  static class NativeJsTypeWithStaticInitializationAndFieldAccess {
    @JsOverlay
    public static Object object = new Integer(3);
  }

  @JsType(isNative = true)
  static class NativeJsTypeWithStaticInitializationAndStaticOverlayMethod {
    @JsOverlay
    public static Object object = new Integer(4);

    @JsOverlay
    public static Object getObject() {
      return object;
    }
  }

  @JsType(isNative = true, namespace = GLOBAL, name = "Object")
  static class NativeJsTypeWithStaticInitializationAndInstanceOverlayMethod {
    @JsOverlay
    public static Object object = new Integer(5);

    @JsOverlay
    public final Object getObject() {
      return object;
    }
  }

  public void testNativeJsTypeWithStaticIntializer() {
    assertEquals(new Integer(3), NativeJsTypeWithStaticInitializationAndFieldAccess.object);
    assertEquals(
        new Integer(4), NativeJsTypeWithStaticInitializationAndStaticOverlayMethod.getObject());
     assertEquals(new Integer(5),
         new NativeJsTypeWithStaticInitializationAndInstanceOverlayMethod().getObject());
  }
}
