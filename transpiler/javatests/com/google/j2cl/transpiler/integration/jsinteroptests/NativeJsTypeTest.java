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

//  public void testClassLiterals() {
//    assertEquals(JavaScriptObject.class, MyNativeJsType.class);
//    assertEquals(JavaScriptObject.class, MyNativeJsTypeInterface.class);
//    assertEquals(JavaScriptObject.class, MyNativeJsType[].class);
//    assertEquals(JavaScriptObject.class, MyNativeJsTypeInterface[].class);
//    assertEquals(JavaScriptObject.class, MyNativeJsType[][].class);
//    assertEquals(JavaScriptObject.class, MyNativeJsTypeInterface[][].class);
//
//    Object nativeObject = createNativeObjectWithoutToString();
//    assertEquals(JavaScriptObject.class, nativeObject.getClass());
//    assertEquals(JavaScriptObject.class, ((MyNativeJsTypeInterface) nativeObject).getClass());
//  }

  public void testToString() {
    Object nativeObjectWithToString = createNativeObjectWithToString();
    assertEquals("Native type", nativeObjectWithToString.toString());

    Object nativeObjectWithoutToString = createNativeObjectWithoutToString();
    assertEquals("[object Object]", nativeObjectWithoutToString.toString());

    // Object nativeArray = createNativeArray();
    // assertEquals("", nativeArray.toString());
  }

  public void testEquals() {
    Object obj1 = createNativeObjectWithoutToString();
    Object obj2 = createNativeObjectWithoutToString();
    assert obj1.equals(obj1);
    assert !obj1.equals(obj2);

    Object nativeArray1 = createNativeArray();
    Object nativeArray2 = createNativeArray();
    assert nativeArray1.equals(nativeArray1);
    assert !nativeArray1.equals(nativeArray2);
  }

  @JsMethod
  private static native MyNativeJsType createNativeObjectWithToString();

  @JsMethod
  private static native MyNativeJsType createNativeObjectWithoutToString();

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
