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
import static com.google.j2cl.integration.testing.Asserts.assertSame;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import java.util.Iterator;
import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class JsTypeTest {
  public static void testAll() {
    testAbstractJsTypeAccess();
    testCasts();
    testConcreteJsTypeAccess();
    testConcreteJsTypeNoTypeTightenField();
    testConcreteJsTypeSubclassAccess();
    testEnumeration();
    testEnumJsTypeAccess();
    testEnumSubclassEnumeration();
    testInstanceOf_concreteJsType();
    testInstanceOf_extendsJsTypeWithProto();
    testInstanceOf_implementsJsType();
    testInstanceOf_implementsJsTypeWithPrototype();
    testInstanceOf_jsoWithNativeButtonProto();
    testInstanceOf_jsoWithoutProto();
    testInstanceOf_jsoWithProto();
    testInstanceOf_classWithCustomIsInstance();
    testInstanceOf_interfaceWithCustomIsInstance();
    testInstanceOf_withNameSpace();
    testJsMethodWithDifferentVisiblities();
    testJsTypeField();
    testNamedBridge();
    testNativeMethodOverrideNoTypeTightenParam();
    testRevealedOverrideJsType();
    testSingleJavaConcreteInterface();
    testSingleJavaConcreteJsFunction();
    testStar();
    testWildcard();
    testNativeFunctionalInterface();
    testInheritName();
  }

  @JsType(isNative = true, namespace = "test.foo")
  interface MyNativeJsTypeInterface {}

  static class MyNativeJsTypeInterfaceImpl implements MyNativeJsTypeInterface {}

  @JsType(isNative = true, namespace = "qux", name = "JsTypeTest_MyNativeJsType")
  static class MyNativeJsType {}

  static class MyNativeJsTypeSubclass extends MyNativeJsType {
    @JsConstructor
    public MyNativeJsTypeSubclass() {}
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array")
  static class MyNativeClassWithCustomIsInstance {
    @JsOverlay
    static boolean $isInstance(Object o) {
      return isCustomIsInstanceClassSingleton(o);
    }
  }

  private static final String CUSTOM_IS_INSTANCE_CLASS_SINGLETON = "CustomIsInstanceClass";

  // This method was extracted from $isInstance and ONLY called from there to ensure that
  // rta traverses custom isInstance methods.
  private static boolean isCustomIsInstanceClassSingleton(Object o) {
    return o == CUSTOM_IS_INSTANCE_CLASS_SINGLETON;
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array")
  interface MyNativeInterfaceWithCustomIsInstance {
    @JsOverlay
    static boolean $isInstance(Object o) {
      return isCustomIsInstanceInterfaceSingleton(o);
    }
  }

  private static final String CUSTOM_IS_INSTANCE_INTERFACE_SINGLETON = "CustomIsInstanceInterface";

  // This method was extracted from $isInstance and ONLY called from there to ensure that
  // rta traverses custom isInstance methods.
  private static boolean isCustomIsInstanceInterfaceSingleton(Object o) {
    return o == CUSTOM_IS_INSTANCE_INTERFACE_SINGLETON;
  }

  static class MyNativeJsTypeSubclassWithIterator extends MyNativeJsType implements Iterable {
    @JsConstructor
    public MyNativeJsTypeSubclassWithIterator() {}

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

  /** A test class marked with JsType but isn't referenced from any Java code except instanceof. */
  @JsType
  interface MyJsInterfaceWithOnlyInstanceofReference {}

  /** A test class marked with JsType but isn't referenced from any Java code except instanceof. */
  @JsType(isNative = true)
  interface MyNativeJsTypeInterfaceAndOnlyInstanceofReference {}

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

  /** Causes the run() function of any implementor to be exported. */
  @JsType
  public interface JsTypeRunnable {

    void run();
  }

  /** This test class exposes parent jsmethod as non-jsmethod. */
  static class ConcreteJsTypeJsSubclass extends ConcreteJsType implements SubclassInterface {
    @JsConstructor
    public ConcreteJsTypeJsSubclass() {}
  }

  interface SubclassInterface {
    int publicMethodAlsoExposedAsNonJsMethod();
  }

  /** This enum is annotated as a @JsType. */
  @SuppressWarnings("ImmutableEnumChecker")
  @JsType
  public enum MyEnumWithJsType {
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

  /** This enum is annotated exported and has enumerations which cause subclass generation. */
  @SuppressWarnings("ImmutableEnumChecker")
  @JsType
  public enum MyEnumWithSubclassGen {
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

  private static <NI extends MyNativeJsTypeInterface, NC extends HTMLElementConcreteNativeJsType>
      void testCasts() {
    Object myClass;
    assertNotNull(myClass = (ElementLikeNativeInterface) createMyNativeJsType());
    assertNotNull(myClass = (MyNativeJsTypeInterface) createMyNativeJsType());
    assertNotNull(myClass = (NI) createMyNativeJsType());
    assertNotNull(myClass = (HTMLElementConcreteNativeJsType) createNativeButton());
    assertNotNull(myClass = (NC) createNativeButton());

    assertThrowsClassCastException(
        () -> {
          Object unused = (HTMLElementConcreteNativeJsType) createMyNativeJsType();
        });

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

  private static void testInstanceOf_jsoWithProto() {
    Object object = createMyNativeJsType();

    assertTrue(object instanceof Object);
    assertFalse(object instanceof HTMLElementConcreteNativeJsType);
    assertFalse(object instanceof HTMLElementAnotherConcreteNativeJsType);
    assertFalse(object instanceof HTMLButtonElementConcreteNativeJsType);
    assertFalse(object instanceof Iterator);
    assertTrue(object instanceof MyNativeJsType);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertFalse(object instanceof MyNativeClassWithCustomIsInstance);
    assertFalse(object instanceof MyNativeInterfaceWithCustomIsInstance);
    assertFalse(object instanceof ElementLikeNativeInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertTrue(object instanceof AliasToMyNativeJsTypeWithOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
    assertFalse(object instanceof MyNativeJsTypeInterface[]);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl[][]);
  }

  private static void testInstanceOf_jsoWithoutProto() {
    Object object = createObject();

    assertTrue(object instanceof Object);
    assertFalse(object instanceof HTMLElementConcreteNativeJsType);
    assertFalse(object instanceof HTMLElementAnotherConcreteNativeJsType);
    assertFalse(object instanceof HTMLButtonElementConcreteNativeJsType);
    assertFalse(object instanceof Iterator);
    assertFalse(object instanceof MyNativeJsType);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertFalse(object instanceof MyNativeClassWithCustomIsInstance);
    assertFalse(object instanceof MyNativeInterfaceWithCustomIsInstance);
    assertFalse(object instanceof ElementLikeNativeInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertFalse(object instanceof AliasToMyNativeJsTypeWithOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
    assertFalse(object instanceof MyNativeJsTypeInterface[]);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl[][]);
  }

  private static void testInstanceOf_jsoWithNativeButtonProto() {
    Object object = createNativeButton();

    assertTrue(object instanceof Object);
    assertTrue(object instanceof HTMLElementConcreteNativeJsType);
    assertTrue(object instanceof HTMLElementAnotherConcreteNativeJsType);
    assertTrue(object instanceof HTMLButtonElementConcreteNativeJsType);
    assertFalse(object instanceof Iterator);
    assertFalse(object instanceof MyNativeJsType);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertFalse(object instanceof MyNativeClassWithCustomIsInstance);
    assertFalse(object instanceof MyNativeInterfaceWithCustomIsInstance);
    assertFalse(object instanceof ElementLikeNativeInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertFalse(object instanceof AliasToMyNativeJsTypeWithOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
    assertFalse(object instanceof MyNativeJsTypeInterface[]);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl[][]);
  }

  private static void testInstanceOf_implementsJsType() {
    // Foils type tightening.
    Object object = new ElementLikeNativeInterfaceImpl();

    assertTrue(object instanceof Object);
    assertFalse(object instanceof HTMLElementConcreteNativeJsType);
    assertFalse(object instanceof HTMLElementAnotherConcreteNativeJsType);
    assertFalse(object instanceof HTMLButtonElementConcreteNativeJsType);
    assertFalse(object instanceof Iterator);
    assertFalse(object instanceof MyNativeJsType);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertFalse(object instanceof MyNativeClassWithCustomIsInstance);
    assertFalse(object instanceof MyNativeInterfaceWithCustomIsInstance);
    assertTrue(object instanceof ElementLikeNativeInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertFalse(object instanceof AliasToMyNativeJsTypeWithOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
    assertFalse(object instanceof MyNativeJsTypeInterface[]);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl[][]);
  }

  private static void testInstanceOf_implementsJsTypeWithPrototype() {
    // Foils type tightening.
    Object object = new MyNativeJsTypeInterfaceImpl();

    assertTrue(object instanceof Object);
    assertFalse(object instanceof HTMLElementConcreteNativeJsType);
    assertFalse(object instanceof HTMLElementAnotherConcreteNativeJsType);
    assertFalse(object instanceof HTMLButtonElementConcreteNativeJsType);
    assertFalse(object instanceof Iterator);
    assertFalse(object instanceof MyNativeJsType);
    assertTrue(object instanceof MyNativeJsTypeInterfaceImpl);
    assertFalse(object instanceof MyNativeClassWithCustomIsInstance);
    assertFalse(object instanceof MyNativeInterfaceWithCustomIsInstance);
    assertFalse(object instanceof ElementLikeNativeInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertFalse(object instanceof AliasToMyNativeJsTypeWithOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
    assertFalse(object instanceof MyNativeJsTypeInterface[]);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl[][]);
  }

  private static void testInstanceOf_concreteJsType() {
    // Foils type tightening.
    Object object = new ConcreteJsType();

    assertTrue(object instanceof Object);
    assertFalse(object instanceof HTMLElementConcreteNativeJsType);
    assertFalse(object instanceof HTMLElementAnotherConcreteNativeJsType);
    assertFalse(object instanceof HTMLButtonElementConcreteNativeJsType);
    assertFalse(object instanceof Iterator);
    assertFalse(object instanceof MyNativeJsType);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertFalse(object instanceof MyNativeClassWithCustomIsInstance);
    assertFalse(object instanceof MyNativeInterfaceWithCustomIsInstance);
    assertFalse(object instanceof ElementLikeNativeInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertFalse(object instanceof AliasToMyNativeJsTypeWithOnlyInstanceofReference);
    assertTrue(object instanceof ConcreteJsType);
    assertFalse(object instanceof MyNativeJsTypeInterface[]);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl[][]);
  }

  private static void testInstanceOf_extendsJsTypeWithProto() {
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
    assertFalse(object instanceof MyNativeClassWithCustomIsInstance);
    assertFalse(object instanceof MyNativeInterfaceWithCustomIsInstance);
    assertFalse(object instanceof ElementLikeNativeInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertTrue(object instanceof AliasToMyNativeJsTypeWithOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
    assertFalse(object instanceof MyNativeJsTypeInterface[]);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl[][]);
  }

  private static void testInstanceOf_classWithCustomIsInstance() {
    Object object = CUSTOM_IS_INSTANCE_CLASS_SINGLETON;

    assertTrue(object instanceof Object);
    assertTrue(object instanceof String);
    assertFalse(object instanceof HTMLElementConcreteNativeJsType);
    assertFalse(object instanceof HTMLElementAnotherConcreteNativeJsType);
    assertFalse(object instanceof HTMLButtonElementConcreteNativeJsType);
    assertFalse(object instanceof Iterator);
    assertFalse(object instanceof MyNativeJsType);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertTrue(object instanceof MyNativeClassWithCustomIsInstance);
    assertFalse(object instanceof MyNativeInterfaceWithCustomIsInstance);
    assertFalse(object instanceof ElementLikeNativeInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertFalse(object instanceof AliasToMyNativeJsTypeWithOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
    assertFalse(object instanceof MyNativeJsTypeInterface[]);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl[][]);
  }

  private static void testInstanceOf_interfaceWithCustomIsInstance() {
    Object object = CUSTOM_IS_INSTANCE_INTERFACE_SINGLETON;

    assertTrue(object instanceof Object);
    assertTrue(object instanceof String);
    assertFalse(object instanceof HTMLElementConcreteNativeJsType);
    assertFalse(object instanceof HTMLElementAnotherConcreteNativeJsType);
    assertFalse(object instanceof HTMLButtonElementConcreteNativeJsType);
    assertFalse(object instanceof Iterator);
    assertFalse(object instanceof MyNativeJsType);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertFalse(object instanceof MyNativeClassWithCustomIsInstance);
    assertTrue(object instanceof MyNativeInterfaceWithCustomIsInstance);
    assertFalse(object instanceof ElementLikeNativeInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertFalse(object instanceof AliasToMyNativeJsTypeWithOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
    assertFalse(object instanceof MyNativeJsTypeInterface[]);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl[][]);
  }

  private static void testInstanceOf_withNameSpace() {
    Object obj1 = createMyNamespacedJsInterface();

    assertTrue(obj1 instanceof MyNamespacedNativeJsType);
    assertFalse(obj1 instanceof MyNativeJsType);
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

  private static void testConcreteJsTypeAccess() {
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

  private static void testAbstractJsTypeAccess() {
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

  private static void testConcreteJsTypeSubclassAccess() {
    ConcreteJsTypeSubclass concreteJsTypeSubclass = new ConcreteJsTypeSubclass();

    // A subclass of a JsType is not itself a JsType.
    assertFalse(PropertyUtils.hasPublicSubclassMethod(concreteJsTypeSubclass));
    assertFalse(PropertyUtils.hasPublicSubclassField(concreteJsTypeSubclass));
    assertFalse(PropertyUtils.hasPublicStaticSubclassMethod(concreteJsTypeSubclass));
    assertFalse(PropertyUtils.hasPrivateSubclassMethod(concreteJsTypeSubclass));
    assertFalse(PropertyUtils.hasProtectedSubclassMethod(concreteJsTypeSubclass));
    assertFalse(PropertyUtils.hasPackageSubclassMethod(concreteJsTypeSubclass));
    assertFalse(PropertyUtils.hasPublicStaticSubclassField(concreteJsTypeSubclass));
    assertFalse(PropertyUtils.hasPrivateSubclassField(concreteJsTypeSubclass));
    assertFalse(PropertyUtils.hasProtectedSubclassField(concreteJsTypeSubclass));
    assertFalse(PropertyUtils.hasPackageSubclassField(concreteJsTypeSubclass));

    // But if it overrides an exported method then the overriding method will be exported.
    assertTrue(ConcreteJsType.hasPublicMethod(concreteJsTypeSubclass));

    assertEquals(20, callPublicMethod(concreteJsTypeSubclass));
    assertEquals(10, concreteJsTypeSubclass.publicSubclassMethod());
  }

  private static void testConcreteJsTypeNoTypeTightenField() {
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

  private static void testNativeMethodOverrideNoTypeTightenParam() {
    AImpl a = new AImpl();
    assertTrue(a.m(null));
    assertFalse((Boolean) callM(a, new Object()));
  }

  @JsMethod
  private static native Object callM(Object obj, Object param);

  private static void testRevealedOverrideJsType() {
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

  private static void testEnumeration() {
    assertEquals(2, callPublicMethodFromEnumeration(MyEnumWithJsType.TEST1));
    assertEquals(3, callPublicMethodFromEnumeration(MyEnumWithJsType.TEST2));
  }

  private static void testEnumJsTypeAccess() {
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

  private static void testEnumSubclassEnumeration() {
    assertEquals(100, callPublicMethodFromEnumerationSubclass(MyEnumWithSubclassGen.A));
    assertEquals(200, callPublicMethodFromEnumerationSubclass(MyEnumWithSubclassGen.B));
    assertEquals(1, callPublicMethodFromEnumerationSubclass(MyEnumWithSubclassGen.C));
  }

  @JsMethod
  private static native int callPublicMethod(Object object);

  @JsMethod
  private static native boolean isUndefined(Object value);

  @JsMethod
  @SuppressWarnings("unusable-by-js")
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

  private static void testJsTypeField() {
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
    @Override
    public int m() {
      return 5;
    }
  }

  @JsMethod
  private static native Object nativeObjectImplementingM();

  private static void testSingleJavaConcreteInterface() {
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

  static final class JavaConcreteJsFunction implements JsFunctionInterface {
    @Override
    public int m() {
      return 5;
    }
  }

  @JsMethod
  private static native Object nativeJsFunction();

  private static void testSingleJavaConcreteJsFunction() {
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
    @Override
    public abstract SomeZAbstractSubclass m();
  }

  @JsType
  static class SomeConcreteSubclass extends SomeZAbstractSubclass {
    @Override
    public SomeConcreteSubclass m() {
      return this;
    }
  }

  private static void testNamedBridge() {
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

  private static void testJsMethodWithDifferentVisiblities() {
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

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "*")
  interface Star {}

  private static void testStar() {
    Object object = new Object();

    assertNotNull(object);

    object = Double.valueOf(3.0);
    assertNotNull(object);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  interface Wildcard {}

  private static void testWildcard() {
    Object object = new Object();

    assertNotNull(object);

    object = Double.valueOf(3.0);
    assertNotNull(object);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  interface NativeFunctionalInterface<T> {
    int f(T t);
  }

  private static void testNativeFunctionalInterface() {
    NativeFunctionalInterface<String> nativeFunctionalInterface = (s) -> 10;
    assertEquals(10, nativeFunctionalInterface.f(""));
  }

  static class ClassWithJsMethod {
    @JsMethod(name = "name")
    public String className() {
      return ClassWithJsMethod.class.getName();
    }
  }

  static class ClassWithJsMethodInheritingName extends ClassWithJsMethod {
    @JsMethod
    public String className() {
      return ClassWithJsMethodInheritingName.class.getName();
    }
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  private interface HasName {
    String name();
  }

  private static String callName(Object o) {
    return ((HasName) o).name();
  }

  private static void testInheritName() {
    ClassWithJsMethod object = new ClassWithJsMethod();
    assertEquals(ClassWithJsMethod.class.getName(), object.className());
    assertEquals(ClassWithJsMethod.class.getName(), callName(object));

    object = new ClassWithJsMethodInheritingName();
    assertEquals(ClassWithJsMethodInheritingName.class.getName(), object.className());
    assertEquals(ClassWithJsMethodInheritingName.class.getName(), callName(object));
  }
}
