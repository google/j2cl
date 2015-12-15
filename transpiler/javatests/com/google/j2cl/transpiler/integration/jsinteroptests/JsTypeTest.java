package com.google.j2cl.transpiler.integration.jsinteroptests;

import static jsinterop.annotations.JsPackage.GLOBAL;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

import java.util.Iterator;

public class JsTypeTest extends MyTestCase {
  @JsType(isNative = true, namespace = "test.foo")
  static interface ElementLikeNativeInterface {
    @JsProperty
    String getTagName();
  }

  @JsType(isNative = true, namespace = "test.foo")
  static interface MyNativeJsTypeInterface {}

  static class MyNativeJsTypeInterfaceImpl implements MyNativeJsTypeInterface {}

  @JsType(isNative = true, namespace = GLOBAL, name = "JsTypeTest_MyNativeJsType")
  static class MyNativeJsType {}

  static class MyNativeJsTypeSubclass extends MyNativeJsType {}

  static class MyNativeJsTypeSubclassWithIterator extends MyNativeJsType implements Iterable {
    @Override
    public Iterator iterator() {
      return null;
    }
  }

  @JsType(namespace = JsPackage.GLOBAL, name = "HTMLElement", isNative = true)
  static class HTMLElementConcreteNativeJsType {}

  @JsType(namespace = JsPackage.GLOBAL, name = "HTMLElement", isNative = true)
  static class HTMLElementAnotherConcreteNativeJsType {}

  @JsType(namespace = JsPackage.GLOBAL, name = "HTMLButtonElement", isNative = true)
  static class HTMLButtonElementConcreteNativeJsType extends HTMLElementConcreteNativeJsType {}

  /**
   * Implements ElementLikeJsInterface.
   */
  static class ElementLikeNativeInterfaceImpl implements ElementLikeNativeInterface {
    @Override
    public String getTagName() {
      return "mytag";
    }
  }

  /**
   * A test class marked with JsType but isn't referenced from any Java code except instanceof.
   */
  @JsType
  static interface MyJsInterfaceWithOnlyInstanceofReference {}

  /**
   * A test class marked with JsType but isn't referenced from any Java code except instanceof.
   */
  @JsType(isNative = true)
  static interface MyNativeJsTypeInterfaceAndOnlyInstanceofReference {}

  /**
   * A test class marked with JsType but isn't referenced from any Java code except instanceof.
   */
  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "JsTypeTest_MyNativeJsType")
  static class AliasToMyNativeJsTypeWithOnlyInstanceofReference {}

  @JsType(isNative = true, namespace = "testfoo.bar")
  static class MyNamespacedNativeJsType {}

  /**
   * This concrete test class is *directly* annotated as a @JsType.
   */
  @JsType
  static class ConcreteJsType {
    public int publicMethod() {
      return 10;
    }

    public int publicMethodAlsoExposedAsNonJsMethod() {
      return 100;
    }

    public static void publicStaticMethod() {}

    private void privateMethod() {}

    protected void protectedMethod() {}

    void packageMethod() {}

    public int publicField = 10;

    public static int publicStaticField = 10;

    private int privateField = 10;

    protected int protectedField = 10;

    int packageField = 10;

    interface A {
      int x();
    }

    public static class AImpl1 implements A {
      @Override
      public int x() {
        return 42;
      }
    }

    public static class AImpl2 implements A {
      @Override
      public int x() {
        return 101;
      }
    }

    @SuppressWarnings("unusable-by-js")
    public A notTypeTightenedField = new AImpl1();
  }

  public void testCasts() {
    Object myClass;
    assertNotNull(myClass = (ElementLikeNativeInterface) createMyNativeJsType());
    assertNotNull(myClass = (MyNativeJsTypeInterface) createMyNativeJsType());
    assertNotNull(myClass = (HTMLElementConcreteNativeJsType) createNativeButton());

    try {
      assertNotNull(myClass = (HTMLElementConcreteNativeJsType) createMyNativeJsType());
      assert false;
    } catch (ClassCastException cce) {
      // Expected.
    }

    // Test cross cast for native types
    Object nativeButton1 = (HTMLElementConcreteNativeJsType) createNativeButton();
    Object nativeButton2 = (HTMLElementAnotherConcreteNativeJsType) nativeButton1;

    /*
     * If the optimizations are turned on, it is possible for the compiler to dead-strip the
     * variables since they are not used. Therefore the casts could potentially be stripped.
     */
    assertNotNull(myClass);
    assertNotNull(nativeButton1);
    assertNotNull(nativeButton2);
  }

  public void testInstanceOf_jsoWithProto() {
    Object object = createMyNativeJsType();

    assertTrue(object instanceof Object);
    assertFalse(object instanceof HTMLElementConcreteNativeJsType);
    assertFalse(object instanceof HTMLElementAnotherConcreteNativeJsType);
    assertFalse(object instanceof HTMLButtonElementConcreteNativeJsType);
    assertFalse(object instanceof Iterator);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertFalse(object instanceof ElementLikeNativeInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertTrue(object instanceof AliasToMyNativeJsTypeWithOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
    assertFalse(object instanceof MyNativeJsTypeInterface[]);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl[][]);
  }

  public void testInstanceOf_jsoWithoutProto() {
    Object object = createObject();

    assertTrue(object instanceof Object);
    assertFalse(object instanceof HTMLElementConcreteNativeJsType);
    assertFalse(object instanceof HTMLElementAnotherConcreteNativeJsType);
    assertFalse(object instanceof HTMLButtonElementConcreteNativeJsType);
    assertFalse(object instanceof Iterator);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertFalse(object instanceof ElementLikeNativeInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertFalse(object instanceof AliasToMyNativeJsTypeWithOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
    assertFalse(object instanceof MyNativeJsTypeInterface[]);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl[][]);
  }

  public void testInstanceOf_jsoWithNativeButtonProto() {
    Object object = createNativeButton();

    assertTrue(object instanceof Object);
    assertTrue(object instanceof HTMLElementConcreteNativeJsType);
    assertTrue(object instanceof HTMLElementAnotherConcreteNativeJsType);
    assertTrue(object instanceof HTMLButtonElementConcreteNativeJsType);
    assertFalse(object instanceof Iterator);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertFalse(object instanceof ElementLikeNativeInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertFalse(object instanceof AliasToMyNativeJsTypeWithOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
    assertFalse(object instanceof MyNativeJsTypeInterface[]);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl[][]);
  }

  public void testInstanceOf_implementsJsType() {
    // Foils type tightening.
    Object object = new ElementLikeNativeInterfaceImpl();

    assertTrue(object instanceof Object);
    assertFalse(object instanceof HTMLElementConcreteNativeJsType);
    assertFalse(object instanceof HTMLElementAnotherConcreteNativeJsType);
    assertFalse(object instanceof HTMLButtonElementConcreteNativeJsType);
    assertFalse(object instanceof Iterator);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertTrue(object instanceof ElementLikeNativeInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertFalse(object instanceof AliasToMyNativeJsTypeWithOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
    assertFalse(object instanceof MyNativeJsTypeInterface[]);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl[][]);
  }

  public void testInstanceOf_implementsJsTypeWithPrototype() {
    // Foils type tightening.
    Object object = new MyNativeJsTypeInterfaceImpl();

    assertTrue(object instanceof Object);
    assertFalse(object instanceof HTMLElementConcreteNativeJsType);
    assertFalse(object instanceof HTMLElementAnotherConcreteNativeJsType);
    assertFalse(object instanceof HTMLButtonElementConcreteNativeJsType);
    assertFalse(object instanceof Iterator);
    assertTrue(object instanceof MyNativeJsTypeInterfaceImpl);
    assertFalse(object instanceof ElementLikeNativeInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertFalse(object instanceof AliasToMyNativeJsTypeWithOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
    assertFalse(object instanceof MyNativeJsTypeInterface[]);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl[][]);
  }

  public void testInstanceOf_concreteJsType() {
    // Foils type tightening.
    Object object = new ConcreteJsType();

    assertTrue(object instanceof Object);
    assertFalse(object instanceof HTMLElementConcreteNativeJsType);
    assertFalse(object instanceof HTMLElementAnotherConcreteNativeJsType);
    assertFalse(object instanceof HTMLButtonElementConcreteNativeJsType);
    assertFalse(object instanceof Iterator);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertFalse(object instanceof ElementLikeNativeInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertFalse(object instanceof AliasToMyNativeJsTypeWithOnlyInstanceofReference);
    assertTrue(object instanceof ConcreteJsType);
    assertFalse(object instanceof MyNativeJsTypeInterface[]);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl[][]);
  }

  public void testInstanceOf_extendsJsTypeWithProto() {
    // Foils type tightening.
    Object object = new MyNativeJsTypeSubclassWithIterator();

    assertTrue(object instanceof Object);
    assertTrue(object instanceof MyNativeJsType);
    assertFalse(object instanceof MyNativeJsTypeSubclass);
    assertTrue(object instanceof MyNativeJsTypeSubclassWithIterator);
    assertFalse(object instanceof HTMLElementConcreteNativeJsType);
    assertFalse(object instanceof HTMLElementAnotherConcreteNativeJsType);
    assertFalse(object instanceof HTMLButtonElementConcreteNativeJsType);
    assertTrue(object instanceof Iterable);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertFalse(object instanceof ElementLikeNativeInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertTrue(object instanceof AliasToMyNativeJsTypeWithOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
    assertFalse(object instanceof MyNativeJsTypeInterface[]);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl[][]);
  }

  public void testInstanceOf_withNameSpace() {
    Object obj1 = createMyNamespacedJsInterface();
    // Object obj2 = createMyWrongNamespacedJsInterface();

    assertTrue(obj1 instanceof MyNamespacedNativeJsType);
    assertFalse(obj1 instanceof MyNativeJsType);

    // assertFalse(obj2 instanceof MyNamespacedNativeJsType);
  }

  private static Object createMyNativeJsType() {
    return new MyNativeJsType();
  }

  private static Object createMyNamespacedJsInterface() {
    return new MyNamespacedNativeJsType();
  }

  @JsMethod
  public static native Object createNativeButton();

  @JsMethod
  public static native Object createObject();
}
