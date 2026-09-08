/*
 * Copyright 2026 Google Inc.
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
package jsinteropinstanceof;

import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import java.util.Iterator;
import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {
  public static void main(String... args) {
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
  }

  @JsType(isNative = true, namespace = "test.foo", name = "MyNativeJsTypeInterface")
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
      throw new UnsupportedOperationException();
    }
  }

  @JsType(namespace = JsPackage.GLOBAL, name = "HTMLElement", isNative = true)
  static class HTMLElementConcreteNativeJsType {}

  @JsType(namespace = JsPackage.GLOBAL, name = "HTMLElement", isNative = true)
  static class HTMLElementAnotherConcreteNativeJsType {}

  @JsType(namespace = JsPackage.GLOBAL, name = "HTMLButtonElement", isNative = true)
  static class HTMLButtonElementConcreteNativeJsType extends HTMLElementConcreteNativeJsType {}

  /** Implements ElementLikeJsInterface. */
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
  @JsType(isNative = true, namespace = "qux", name = "JsTypeTest_MyNativeJsType")
  static class AliasToMyNativeJsTypeWithOnlyInstanceofReference {}

  @JsType(isNative = true, namespace = "testfoo.bar")
  static class MyNamespacedNativeJsType {}

  @JsType
  static class ConcreteJsType {}

  private static void testInstanceOf_jsoWithProto() {
    Object object = createMyNativeJsType();

    assertTrue(object instanceof Object);
    assertFalse(object instanceof HTMLElementConcreteNativeJsType);
    assertFalse(object instanceof HTMLElementAnotherConcreteNativeJsType);
    assertFalse(object instanceof HTMLButtonElementConcreteNativeJsType);
    assertFalse(object instanceof Iterator);
    assertTrue(object instanceof MyNativeJsType);
    assertFalse(object instanceof MyNativeJsTypeSubclass);
    assertFalse(object instanceof MyNativeJsTypeSubclassWithIterator);
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
    assertFalse(object instanceof MyNativeJsTypeSubclass);
    assertFalse(object instanceof MyNativeJsTypeSubclassWithIterator);
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
    assertFalse(object instanceof MyNativeJsTypeSubclass);
    assertFalse(object instanceof MyNativeJsTypeSubclassWithIterator);
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
    assertFalse(object instanceof MyNativeJsTypeSubclass);
    assertFalse(object instanceof MyNativeJsTypeSubclassWithIterator);
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
    assertFalse(object instanceof MyNativeJsTypeSubclass);
    assertFalse(object instanceof MyNativeJsTypeSubclassWithIterator);
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
    assertFalse(object instanceof MyNativeJsTypeSubclass);
    assertFalse(object instanceof MyNativeJsTypeSubclassWithIterator);
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

  @JsMethod(namespace = "jsinteropinstanceof.JsTypeTestHelper")
  private static native Object createNativeButton();

  @JsMethod(namespace = "jsinteropinstanceof.JsTypeTestHelper")
  private static native Object createObject();
}
