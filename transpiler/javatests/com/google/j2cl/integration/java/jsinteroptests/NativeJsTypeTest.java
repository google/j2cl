/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jsinteroptests;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertNotEquals;
import static com.google.j2cl.integration.testing.Asserts.assertNotNull;
import static com.google.j2cl.integration.testing.Asserts.assertNull;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/** Tests native JsType functionality. */
public class NativeJsTypeTest {
  public static void testAll() {
    testClassLiterals();
    testGetClass();
    testEqualityOptimization();
    testClassLiterals();
    testNativeJsTypeWithOverlay();
    testNativeJsTypeWithStaticIntializer();
    testNativeInnerClass();
    testSpecialNativeInstanceOf();
    testForwaringMethodsOnNativeClasses();
    testUninitializedStaticOverlayField();
    testVariableExternCollision();
    testAliasExternCollision();
    testBridgesNativeSubclass();
    testCallNamespaceAsFunction();
    testAccessNamespaceAsProperty();
  }

  @JsType(isNative = true)
  static class MyNativeJsType {
    @Override
    public native int hashCode();
  }

  @JsType(isNative = true)
  private interface MyNativeJsTypeInterface {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private static class NativeObject implements MyNativeJsTypeInterface {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private static final class FinalNativeObject implements MyNativeJsTypeInterface {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private interface MyNativeJsTypeInterfaceOnlyOneConcreteImplementor {}

  private static void testClassLiterals() {
    assertEquals("<native object>", MyNativeJsType.class.getName());
    // Native classes have all the same class literal.
    assertEquals(MyNativeJsType.class, NativeObject.class);

    // native interfaces and native classes have different class literals to satisfy Java semantics
    // with respect to Class.isInterface etc.
    assertNotEquals(MyNativeJsType.class, MyNativeJsTypeInterface.class);
    // Native interfaces have all the same class literal.
    assertEquals(
        MyNativeJsTypeInterface.class, MyNativeJsTypeInterfaceOnlyOneConcreteImplementor.class);

    // Native arrays have all the same class literal, and is Object[].class.
    assertEquals(Object[].class, MyNativeJsType[].class);
    assertEquals(MyNativeJsType[].class, MyNativeJsType[].class);
    assertEquals(MyNativeJsType[].class, MyNativeJsTypeInterface[].class);
    assertEquals(MyNativeJsType[].class, MyNativeJsType[][].class);
    assertEquals(MyNativeJsType[].class, MyNativeJsTypeInterface[][].class);
  }

  private static void testGetClass() {
    Object object = createNativeObjectWithoutToString();
    assertEquals(MyNativeJsType.class, object.getClass());

    MyNativeJsTypeInterface nativeInterface =
        (MyNativeJsTypeInterface) createNativeObjectWithoutToString();
    assertEquals(MyNativeJsType.class, nativeInterface.getClass());

    // Test that the dispatch to getClass in not messed up by incorrectly marking nativeObject1 as
    // exact and inlining Object.getClass() implementation.
    NativeObject nativeObject1 = new NativeObject();
    assertEquals(MyNativeJsType.class, nativeObject1.getClass());

    // Test that the dispatch to getClass in not messed up by incorrectly marking nativeObject2 as
    // exact and inlining Object.getClass() implementation.
    FinalNativeObject nativeObject2 = createNativeObject();
    assertEquals(MyNativeJsType.class, nativeObject2.getClass());

    assertEquals(MyNativeJsType[].class, createNativeArray().getClass());
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  static final class AnotherFinalNativeObject implements MyNativeJsTypeInterface {}

  private static boolean same(Object thisObject, Object thatObject) {
    return thisObject == thatObject;
  }

  private static void testEqualityOptimization() {
    // Makes sure that == does not get optimized away due to static class incompatibility.

    FinalNativeObject finalNativeObject = new FinalNativeObject();

    AnotherFinalNativeObject anotherFinalNativeObject =
        (AnotherFinalNativeObject) (Object) finalNativeObject;
    // DeadCodeElimination could optimize statically to false due to type incompatibility, which
    // could happen if both variables were marked as exact.
    assertTrue(same(anotherFinalNativeObject, finalNativeObject));
  }

  private static void testToString() {
    Object nativeObjectWithToString = createNativeObjectWithToString();
    assertEquals("Native type", nativeObjectWithToString.toString());

    Object nativeObjectWithoutToString = createNativeObjectWithoutToString();
    assertEquals("[object Object]", nativeObjectWithoutToString.toString());

    Object nativeArray = createNativeArray();
    assertEquals("", nativeArray.toString());
  }

  @JsMethod
  private static native FinalNativeObject createNativeObject();

  @JsMethod
  private static native MyNativeJsType createNativeObjectWithToString();

  @JsMethod(name = "createNativeObject")
  private static native MyNativeJsType createNativeObjectWithoutToString();

  @JsMethod
  private static native Object createNativeArray();

  @JsType(isNative = true, name = "NativeJsTypeWithOverlay")
  static class NativeJsTypeWithOverlay {

    @JsOverlay public static final int x = 2;

    @JsMethod(namespace = JsPackage.GLOBAL, name = "Object.keys")
    public static native String[] keys(NativeObject o);

    @JsOverlay
    public static final boolean hasM(Object obj) {
      for (String k : keys((NativeObject) obj)) {
        if ("m".equals(k)) {
          return true;
        }
      }
      return false;
    }

    public native boolean hasOwnProperty(String name);

    @JsOverlay
    public final boolean hasM() {
      return hasOwnProperty("m");
    }

    public int k;

    @JsOverlay
    public final NativeJsTypeWithOverlay setK(int k) {
      this.k = k;
      return this;
    }
  }

  @JsMethod
  private static native NativeJsTypeWithOverlay createNativeJsTypeWithOverlayWithM();

  private static void testNativeJsTypeWithOverlay() {
    NativeJsTypeWithOverlay object = createNativeJsTypeWithOverlayWithM();
    assertTrue(object.hasM());
    assertTrue(NativeJsTypeWithOverlay.hasM(object));
    assertEquals(2, NativeJsTypeWithOverlay.x);
    assertEquals(42, object.setK(3).setK(42).k);
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

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  static class NativeJsTypeWithStaticInitializationAndInstanceOverlayMethod {
    @JsOverlay
    public static Object object = new Integer(5);

    @JsOverlay
    public final Object getObject() {
      return object;
    }
    static {
      clinitCalled++;
    }
  }

  private static int clinitCalled = 0;

  private static void testNativeJsTypeWithStaticIntializer() {
    assertEquals(new Integer(3), NativeJsTypeWithStaticInitializationAndFieldAccess.object);
    assertEquals(0, clinitCalled);
    assertEquals(
        new Integer(4), NativeJsTypeWithStaticInitializationAndStaticOverlayMethod.getObject());
    assertEquals(
        new Integer(5),
        new NativeJsTypeWithStaticInitializationAndInstanceOverlayMethod().getObject());
    assertEquals(1, clinitCalled);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Function")
  static class NativeFunction {}

  @JsMethod
  private static native Object createFunction();

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array")
  static class NativeArray {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Number")
  static class NativeNumber {}

  @JsMethod
  private static native Object createNumber();

  @JsMethod
  private static native Object createBoxedNumber();

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
  static class NativeString {}

  @JsMethod
  private static native Object createBoxedString();

  @JsFunction
  interface SomeFunctionInterface {
    void m();
  }

  static final class SomeFunction implements SomeFunctionInterface {
    public void m() {}
  }

  private static void testSpecialNativeInstanceOf() {
    Object aJsFunction = new SomeFunction();
    // True cases.
    assertTrue(aJsFunction instanceof NativeFunction);
    assertTrue(aJsFunction instanceof SomeFunctionInterface);
    assertTrue(aJsFunction instanceof NativeObject);
    // False cases.
    assertFalse(aJsFunction instanceof NativeArray);
    assertFalse(aJsFunction instanceof NativeNumber);
    assertFalse(aJsFunction instanceof NativeString);

    Object anotherFunction = createFunction();
    // True cases.
    assertTrue(anotherFunction instanceof NativeFunction);
    assertTrue(anotherFunction instanceof SomeFunctionInterface);
    assertTrue(anotherFunction instanceof NativeObject);
    // False cases.
    assertFalse(anotherFunction instanceof NativeArray);
    assertFalse(anotherFunction instanceof NativeNumber);
    assertFalse(anotherFunction instanceof NativeString);

    Object aString = "Hello";
    // True cases.
    // TODO(b/31271239): uncomment
    // assertTrue(aString instanceof NativeString);
    // False cases.
    assertFalse(aString instanceof NativeFunction);
    assertFalse(aString instanceof NativeObject);
    assertFalse(aString instanceof NativeArray);
    assertFalse(aString instanceof NativeNumber);

    Object aBoxedString = createBoxedString();
    // True cases.
    // Note that boxed strings are (surprisingly) not strings but objects.
    assertTrue(aBoxedString instanceof NativeObject);
    // False cases.
    assertFalse(aBoxedString instanceof NativeFunction);
    assertFalse(aBoxedString instanceof NativeArray);
    assertFalse(aBoxedString instanceof NativeNumber);
    // TODO(b/31271239): uncomment
    // assertFalse(aBoxedString instanceof NativeString);

    Object anArray = new String[0];
    // True cases.
    assertTrue(anArray instanceof NativeArray);
    assertTrue(anArray instanceof NativeObject);
    // False cases.
    assertFalse(anArray instanceof NativeFunction);
    assertFalse(anArray instanceof NativeNumber);
    assertFalse(anArray instanceof NativeString);

    Object aNativeArray = createNativeArray();
    // True cases.
    assertTrue(aNativeArray instanceof NativeArray);
    assertTrue(anArray instanceof NativeObject);
    // False cases.
    assertFalse(aNativeArray instanceof NativeFunction);
    assertFalse(aNativeArray instanceof NativeNumber);
    assertFalse(aNativeArray instanceof NativeString);

    Object aNumber = new Double(3);
    // True cases.
    // TODO(b/31271239): uncomment
    // assertTrue(aNumber instanceof NativeNumber);
    // False cases.
    assertFalse(aNumber instanceof NativeArray);
    assertFalse(aNumber instanceof NativeObject);
    assertFalse(aNumber instanceof NativeFunction);
    assertFalse(aNumber instanceof NativeString);

    Object anotherNumber = createNumber();
    // True cases.
    // TODO(b/31271239): uncomment
    // assertTrue(anotherNumber instanceof NativeNumber);
    // False cases.
    assertFalse(anotherNumber instanceof NativeArray);
    assertFalse(anotherNumber instanceof NativeObject);
    assertFalse(anotherNumber instanceof NativeFunction);
    assertFalse(anotherNumber instanceof NativeString);

    Object aBoxedNumber = createBoxedNumber();
    // True cases.
    assertTrue(aBoxedNumber instanceof NativeObject);
    // False cases.
    // TODO(b/31271239): uncomment
    // assertFalse(aBoxedNumber instanceof NativeNumber);
    assertFalse(aBoxedNumber instanceof NativeArray);
    assertFalse(aBoxedNumber instanceof NativeFunction);
    assertFalse(aBoxedNumber instanceof NativeString);

    Object anObject = new Object();
    // True cases.
    assertTrue(anObject instanceof NativeObject);
    // False cases.
    assertFalse(anObject instanceof NativeNumber);
    assertFalse(anObject instanceof NativeArray);
    assertFalse(anObject instanceof NativeFunction);
    assertFalse(anObject instanceof NativeString);

    Object nullObject = null;

    assertFalse(nullObject instanceof NativeObject);
    assertFalse(nullObject instanceof NativeArray);
    assertFalse(nullObject instanceof NativeFunction);
    assertFalse(nullObject instanceof NativeString);
    assertFalse(nullObject instanceof NativeNumber);

    Object undefined = getUndefined();
    assertFalse(undefined instanceof NativeObject);
    assertFalse(undefined instanceof NativeArray);
    assertFalse(undefined instanceof NativeFunction);
    assertFalse(undefined instanceof NativeString);
    assertFalse(undefined instanceof NativeNumber);
  }

  @JsProperty(namespace = JsPackage.GLOBAL)
  private static native Object getUndefined();

  @SuppressWarnings("unused")
  private static void testVariableExternCollision() {
    Object Int8Array = null; // A variable name that would collide with an extern.
    assertNotNull(getInt8ArrayBytesPerElement());
  }

  @JsProperty(namespace = JsPackage.GLOBAL, name = "Int8Array.BYTES_PER_ELEMENT")
  private static native double getInt8ArrayBytesPerElement();

  private static void testAliasExternCollision() {
    Float32Array unused = new Float32Array(); // make sure it is referenced hence aliased.
    assertNotNull(getFloat32ArrayBytesPerElement());
  }

  static class Float32Array {}

  @JsProperty(namespace = JsPackage.GLOBAL, name = "Float32Array.BYTES_PER_ELEMENT")
  private static native Object getFloat32ArrayBytesPerElement();

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  interface NativeInterface {
    void add(String element);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  static class NativeSuperClass {
    public native void add(String element);

    public native boolean remove(String element);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  static class NativeSubClassAccidentalOverride extends NativeSuperClass
      implements NativeInterface {}

  @JsMethod
  private static native NativeSubClassAccidentalOverride createNativeSubclass();

  private static void testForwaringMethodsOnNativeClasses() {
    NativeSubClassAccidentalOverride subClass = createNativeSubclass();
    subClass.add("Hi");
    assertTrue(subClass.remove("Hi"));
    assertFalse(subClass.remove("Hi"));
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  static class NativeClassWithStaticOverlayFields {
    @JsOverlay static String uninitializedString;
    @JsOverlay static int uninitializedInt;
    @JsOverlay static int initializedInt = 5;
  }

  private static void testUninitializedStaticOverlayField() {
    assertEquals(0, NativeClassWithStaticOverlayFields.uninitializedInt);
    assertEquals(5, NativeClassWithStaticOverlayFields.initializedInt);
    assertNull(NativeClassWithStaticOverlayFields.uninitializedString);
  }

  @JsType(isNative = true, name = "MyNativeJsType")
  static class MyNativeJsTypeWithInner {
    @JsType(isNative = true)
    static class Inner {
      public int n;

      public Inner(int n) {}
    }
  }

  // This is implemented in NativeJsTypeTest.native.js on purpose to make sure that the reference to
  // the enclosing module is considered a Java type and all the scheme to avoid dependency
  // cycles is correct.
  @JsType(isNative = true, name = "MyNativeJsType.Inner")
  static class MyNativeJsTypeInner {
    public MyNativeJsTypeInner(int n) {}

    @JsProperty
    public native int getN();
  }

  private static void testNativeInnerClass() {
    MyNativeJsTypeWithInner.Inner object = new MyNativeJsTypeWithInner.Inner(3);
    assertTrue(object instanceof MyNativeJsTypeWithInner.Inner);
    assertTrue((Object) object instanceof MyNativeJsTypeInner);
    assertEquals(3, object.n);
    object = (MyNativeJsTypeWithInner.Inner) (Object) new MyNativeJsTypeInner(4);
    assertEquals(4, object.n);
  }

  interface InterfaceWithDefaultMethods {
    String method();

    default String defaultMethod() {
      return "default-method";
    }
  }

  private abstract static class AbstractSuperWithPackagePrivateMethod {
    @JsConstructor
    AbstractSuperWithPackagePrivateMethod() {}

    String packagePrivateMethod() {
      return "package-private";
    }
  }

  @JsType(namespace = "woo.NativeJsTypeTest", name = "AbstractJavaSuperclass")
  private abstract static class AbstractSuperClassForJs
      extends AbstractSuperWithPackagePrivateMethod implements InterfaceWithDefaultMethods {
    public AbstractSuperClassForJs() {}

    @Override
    public abstract String packagePrivateMethod();

    @Override
    public abstract String method();
  }

  private static void testBridgesNativeSubclass() {
    AbstractSuperClassForJs nativeSubclass = createBridgesNativeSubclass();
    // Tests that the default method is implemented.
    assertEquals("default-method", nativeSubclass.defaultMethod());

    assertEquals("package-private-override", nativeSubclass.packagePrivateMethod());
    // Tests package private bridge.
    assertEquals(
        "package-private-override",
        ((AbstractSuperWithPackagePrivateMethod) (Object) nativeSubclass).packagePrivateMethod());

    assertEquals("method", nativeSubclass.method());
    // Tests JsMethod bridge.
    assertEquals("method", ((InterfaceWithDefaultMethods) (Object) nativeSubclass).method());
  }

  @JsMethod(namespace = "woo.NativeJsTypeTest.NativeBridgesSubclass", name = "create")
  private static native AbstractSuperClassForJs createBridgesNativeSubclass();

  @JsMethod(namespace = "woo.NativeJsTypeTest.FunctionNamespace", name = "")
  private static native String callFunctionNamespace(String input);

  private static void testCallNamespaceAsFunction() {
    assertEquals("foobar", callFunctionNamespace("foo"));
  }

  @JsProperty(namespace = "woo.NativeJsTypeTest.PropertyNamespace", name = "")
  private static native String getPropertyNamespace();

  private static void testAccessNamespaceAsProperty() {
    assertEquals("foo", getPropertyNamespace());
  }
}
