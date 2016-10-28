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

import java.util.Iterator;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class JsTypeTest extends MyTestCase {
  public static void testAll() {
    JsTypeTest test = new JsTypeTest();
    test.testAbstractJsTypeAccess();
    test.testCasts();
    test.testConcreteJsTypeAccess();
    test.testConcreteJsTypeNoTypeTightenField();
    test.testConcreteJsTypeSubclassAccess();
    test.testEnumeration();
    test.testEnumJsTypeAccess();
    test.testEnumSubclassEnumeration();
    test.testInstanceOf_concreteJsType();
    test.testInstanceOf_extendsJsTypeWithProto();
    test.testInstanceOf_implementsJsType();
    test.testInstanceOf_implementsJsTypeWithPrototype();
    test.testInstanceOf_jsoWithNativeButtonProto();
    test.testInstanceOf_jsoWithoutProto();
    test.testInstanceOf_jsoWithProto();
    test.testInstanceOf_withNameSpace();
    test.testJsMethodWithDifferentVisiblities();
    test.testJsTypeField();
    test.testNamedBridge();
    test.testNativeMethodOverrideNoTypeTightenParam();
    test.testRevealedOverrideJsType();
    test.testSingleJavaConcreteInterface();
    test.testSingleJavaConcreteJsFunction();
  }

  @JsType(isNative = true, namespace = "test.foo")
  static interface MyNativeJsTypeInterface {}

  static class MyNativeJsTypeInterfaceImpl implements MyNativeJsTypeInterface {}

  @JsType(isNative = true, namespace = "qux", name = "JsTypeTest_MyNativeJsType")
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
  @JsType(isNative = true, namespace = "qux", name = "JsTypeTest_MyNativeJsType")
  static class AliasToMyNativeJsTypeWithOnlyInstanceofReference {}

  @JsType(isNative = true, namespace = "testfoo.bar")
  static class MyNamespacedNativeJsType {}

  /**
   * This concrete test class is *directly* annotated as a @JsType.
   */
  @JsType
  abstract static class AbstractJsType {
    public abstract int publicMethod();
  }

  /**
   * A regular class with no exports.
   */
  public static class PlainParentType {

    /**
     * A simple function that is not itself exported.
     */
    public void run() {}
  }

  /**
   * This type implements no functionality but should still export run() because of the @JsType
   * interface that it implements.
   */
  public static class RevealedOverrideSubType extends PlainParentType implements JsTypeRunnable {}

  /**
   * Causes the run() function of any implementor to be exported.
   */
  @JsType
  public static interface JsTypeRunnable {

    void run();
  }

  /**
   * This test class exposes parent jsmethod as non-jsmethod.
   */
  static class ConcreteJsTypeJsSubclass extends ConcreteJsType implements SubclassInterface {}

  static interface SubclassInterface {
    int publicMethodAlsoExposedAsNonJsMethod();
  }

  /**
   * This enum is annotated as a @JsType.
   */
  @JsType
  public static enum MyEnumWithJsType {
    TEST1(1),
    TEST2(2);

    public int idx;

    MyEnumWithJsType(int a) {
      this.idx = a;
    }

    public int idxAddOne() {
      return idx + 1;
    }

    public int publicMethod() {
      return 10;
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
  }

  /**
   * This enum is annotated exported and has enumerations which cause subclass generation.
   */
  @JsType
  public static enum MyEnumWithSubclassGen {
    A {
      @Override
      public int foo() {
        return 100;
      }
    },
    B {
      @Override
      public int foo() {
        return 200;
      }
    },
    C;

    public int foo() {
      return 1;
    }
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

  public void testConcreteJsTypeAccess() {
    ConcreteJsType concreteJsType = new ConcreteJsType();

    assertTrue(ConcreteJsType.hasPublicField(concreteJsType));
    assertTrue(ConcreteJsType.hasPublicMethod(concreteJsType));

    assertFalse(ConcreteJsType.hasPublicStaticMethod(concreteJsType));
    assertFalse(ConcreteJsType.hasPrivateMethod(concreteJsType));
    assertFalse(ConcreteJsType.hasProtectedMethod(concreteJsType));
    assertFalse(ConcreteJsType.hasPackageMethod(concreteJsType));
    assertFalse(ConcreteJsType.hasPublicStaticField(concreteJsType));
    assertFalse(ConcreteJsType.hasPrivateField(concreteJsType));
    assertFalse(ConcreteJsType.hasProtectedField(concreteJsType));
    assertFalse(ConcreteJsType.hasPackageField(concreteJsType));

    assertEquals(10, callPublicMethod(concreteJsType));
  }

  public void testAbstractJsTypeAccess() {
    AbstractJsType jsType =
        new AbstractJsType() {
          @Override
          public int publicMethod() {
            return 32;
          }
        };

    assertTrue(ConcreteJsType.hasPublicMethod(jsType));
    assertEquals(32, callPublicMethod(jsType));
    assertEquals(32, jsType.publicMethod());
  }

  public void testConcreteJsTypeSubclassAccess() {
    ConcreteJsTypeSubclass concreteJsTypeSubclass = new ConcreteJsTypeSubclass();

    // A subclass of a JsType is not itself a JsType.
    assertFalse(ConcreteJsTypeSubclass.hasPublicSubclassMethod(concreteJsTypeSubclass));
    assertFalse(ConcreteJsTypeSubclass.hasPublicSubclassField(concreteJsTypeSubclass));
    assertFalse(ConcreteJsTypeSubclass.hasPublicStaticSubclassMethod(concreteJsTypeSubclass));
    assertFalse(ConcreteJsTypeSubclass.hasPrivateSubclassMethod(concreteJsTypeSubclass));
    assertFalse(ConcreteJsTypeSubclass.hasProtectedSubclassMethod(concreteJsTypeSubclass));
    assertFalse(ConcreteJsTypeSubclass.hasPackageSubclassMethod(concreteJsTypeSubclass));
    assertFalse(ConcreteJsTypeSubclass.hasPublicStaticSubclassField(concreteJsTypeSubclass));
    assertFalse(ConcreteJsTypeSubclass.hasPrivateSubclassField(concreteJsTypeSubclass));
    assertFalse(ConcreteJsTypeSubclass.hasProtectedSubclassField(concreteJsTypeSubclass));
    assertFalse(ConcreteJsTypeSubclass.hasPackageSubclassField(concreteJsTypeSubclass));

    // But if it overrides an exported method then the overriding method will be exported.
    assertTrue(ConcreteJsType.hasPublicMethod(concreteJsTypeSubclass));

    assertEquals(20, callPublicMethod(concreteJsTypeSubclass));
    assertEquals(10, concreteJsTypeSubclass.publicSubclassMethod());
  }

  public void testConcreteJsTypeNoTypeTightenField() {
    // If we type-tighten, java side will see no calls and think that field could only AImpl1.
    ConcreteJsType concreteJsType = new ConcreteJsType();
    setTheField(concreteJsType, new ConcreteJsType.AImpl2());
    assertEquals(101, concreteJsType.notTypeTightenedField.x());
  }

  @JsType
  interface A {
    boolean m(Object o);
  }

  private static class AImpl implements A {
    @Override
    public boolean m(Object o) {
      return o == null;
    }
  }

  public void testNativeMethodOverrideNoTypeTightenParam() {
    AImpl a = new AImpl();
    assertTrue(a.m(null));
    assertFalse((Boolean) callM(a, new Object()));
  }

  @JsMethod
  private static native Object callM(Object obj, Object param);

  public void testRevealedOverrideJsType() {
    PlainParentType plainParentType = new PlainParentType();
    RevealedOverrideSubType revealedOverrideSubType = new RevealedOverrideSubType();

    // PlainParentType is neither @JsType or @JsType and so exports no functions.
    assertFalse(hasFieldRun(plainParentType));

    // RevealedOverrideSubType defines no functions itself, it only inherits them, but it still
    // exports run() because it implements the @JsType interface JsTypeRunnable.
    assertTrue(hasFieldRun(revealedOverrideSubType));

    ConcreteJsTypeJsSubclass subclass = new ConcreteJsTypeJsSubclass();
    assertEquals(100, subclass.publicMethodAlsoExposedAsNonJsMethod());
    SubclassInterface subclassInterface = subclass;
    assertEquals(100, subclassInterface.publicMethodAlsoExposedAsNonJsMethod());
  }

  @JsMethod
  private static native boolean hasFieldRun(Object obj);

  public void testEnumeration() {
    assertEquals(2, callPublicMethodFromEnumeration(MyEnumWithJsType.TEST1));
    assertEquals(3, callPublicMethodFromEnumeration(MyEnumWithJsType.TEST2));
  }

  public void testEnumJsTypeAccess() {
    assertTrue(ConcreteJsType.hasPublicField(MyEnumWithJsType.TEST2));
    assertTrue(ConcreteJsType.hasPublicMethod(MyEnumWithJsType.TEST2));

    assertFalse(ConcreteJsType.hasPublicStaticMethod(MyEnumWithJsType.TEST2));
    assertFalse(ConcreteJsType.hasPrivateMethod(MyEnumWithJsType.TEST2));
    assertFalse(ConcreteJsType.hasProtectedMethod(MyEnumWithJsType.TEST2));
    assertFalse(ConcreteJsType.hasPackageMethod(MyEnumWithJsType.TEST2));
    assertFalse(ConcreteJsType.hasPublicStaticField(MyEnumWithJsType.TEST2));
    assertFalse(ConcreteJsType.hasPrivateField(MyEnumWithJsType.TEST2));
    assertFalse(ConcreteJsType.hasProtectedField(MyEnumWithJsType.TEST2));
    assertFalse(ConcreteJsType.hasPackageField(MyEnumWithJsType.TEST2));
  }

  public void testEnumSubclassEnumeration() {
    assertEquals(100, callPublicMethodFromEnumerationSubclass(MyEnumWithSubclassGen.A));
    assertEquals(200, callPublicMethodFromEnumerationSubclass(MyEnumWithSubclassGen.B));
    assertEquals(1, callPublicMethodFromEnumerationSubclass(MyEnumWithSubclassGen.C));
  }

  @JsMethod
  private static native int callPublicMethod(Object object);

  @JsMethod
  private static native boolean isUndefined(Object value);

  @JsMethod
  private static native void setTheField(ConcreteJsType obj, ConcreteJsType.A value);

  @JsMethod
  private static native int callPublicMethodFromEnumeration(MyEnumWithJsType enumeration);

  @JsMethod
  private static native int callPublicMethodFromEnumerationSubclass(MyEnumWithSubclassGen e);

  @JsType
  interface SimpleJsTypeFieldInterface {}

  static class SimpleJsTypeFieldClass implements SimpleJsTypeFieldInterface {}

  static class SimpleJsTypeWithField {
    @JsProperty public SimpleJsTypeFieldInterface someField;
  }

  public void testJsTypeField() {
    assertTrue(new SimpleJsTypeFieldClass() != new SimpleJsTypeFieldClass());
    SimpleJsTypeWithField holder = new SimpleJsTypeWithField();
    fillJsTypeField(holder);
    SimpleJsTypeFieldInterface someField = holder.someField;
    assertNotNull(someField);
  }

  @JsMethod
  public static native void fillJsTypeField(SimpleJsTypeWithField jstype);

  @JsType(isNative = true)
  interface InterfaceWithSingleJavaConcrete {
    int m();
  }

  static class JavaConcrete implements InterfaceWithSingleJavaConcrete {
    public int m() {
      return 5;
    }
  }

  @JsMethod
  private static native Object nativeObjectImplementingM();

  public void testSingleJavaConcreteInterface() {
    // Create a couple of instances and use the objects in some way to avoid complete pruning
    // of JavaConcrete
    assertTrue(new JavaConcrete() != new JavaConcrete());
    assertSame(5, new JavaConcrete().m());
    assertSame(3, ((InterfaceWithSingleJavaConcrete) nativeObjectImplementingM()).m());
  }

  @JsFunction
  interface JsFunctionInterface {
    int m();
  }

  static class JavaConcreteJsFunction implements JsFunctionInterface {
    public int m() {
      return 5;
    }
  }

  @JsMethod
  private static native Object nativeJsFunction();

  public void testSingleJavaConcreteJsFunction() {
    // Create a couple of instances and use the objects in some way to avoid complete pruning
    // of JavaConcrete
    assertTrue(new JavaConcreteJsFunction() != new JavaConcreteJsFunction());
    assertSame(5, new JavaConcreteJsFunction().m());
    assertSame(3, ((JsFunctionInterface) nativeJsFunction()).m());
  }

  @JsType
  abstract static class SomeAbstractClass {
    public abstract SomeAbstractClass m();
  }

  // Do not rename this class.
  @JsType
  abstract static class SomeZAbstractSubclass extends SomeAbstractClass {
    public abstract SomeZAbstractSubclass m();
  }

  @JsType
  static class SomeConcreteSubclass extends SomeZAbstractSubclass {
    public SomeConcreteSubclass m() {
      return this;
    }
  }

  public void testNamedBridge() {
    // Bridges are sorted by signature in the JDT. Make sure that the bridge method appears second.
    // GWT specific test.
    // assertTrue(
    //  SomeConcreteSubclass.class.getName().compareTo(SomeZAbstractSubclass.class.getName()) < 0);
    SomeConcreteSubclass o = new SomeConcreteSubclass();
    assertEquals(o, o.m());
  }

  static class NonPublicJsMethodClass {
    @JsMethod
    private String foo() {
      return "foo";
    }

    @JsMethod
    String bar() {
      return "bar";
    }
  }

  public void testJsMethodWithDifferentVisiblities() {
    NonPublicJsMethodClass instance = new NonPublicJsMethodClass();
    assertEquals("foo", instance.foo());
    assertEquals("bar", instance.bar());
    assertEquals("foo", callFoo(instance, null));
    assertEquals("bar", callBar(instance, null));
  }

  @JsMethod
  public static native Object callFoo(Object obj, Object param);

  @JsMethod
  public static native Object callBar(Object obj, Object param);
}
