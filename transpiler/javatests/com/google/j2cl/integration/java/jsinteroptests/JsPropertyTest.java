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
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/** Tests JsProperty functionality. */
public class JsPropertyTest {
  public static void testAll() {
    testConcreteJsType();
    testJavaClassImplementingMyJsTypeInterfaceWithProperty();
    testJsPropertyAccidentalOverrideSuperCall();
    testJsPropertyBridges();
    testJsPropertyBridgesSubclass();
    testJsPropertyGetX();
    testJsPropertyIsX();
    testJsPropertyRemovedAccidentalOverrideSuperCall();
    testNativeJsType();
    testNativeJsTypeSubclass();
    testNativeJsTypeSubclassNoOverride();
    testNativeJsTypeWithConstructor();
    testNativeJsTypeWithConstructorSubclass();
    testProtectedNames();
  }

  private static final int SET_PARENT_X = 500;
  private static final int GET_PARENT_X = 1000;
  private static final int GET_X = 100;
  private static final int SET_X = 50;

  @JsType
  interface MyJsTypeInterfaceWithProperty {
    @JsProperty
    int getX();

    @JsProperty
    void setX(int x);
  }

  static class MyJavaTypeImplementingMyJsTypeInterfaceWithProperty
      implements MyJsTypeInterfaceWithProperty {
    private int x;

    @Override
    public int getX() {
      return x + GET_X;
    }

    @Override
    public void setX(int x) {
      this.x = x + SET_X;
    }
  }

  private static void testJavaClassImplementingMyJsTypeInterfaceWithProperty() {
    MyJavaTypeImplementingMyJsTypeInterfaceWithProperty obj =
        new MyJavaTypeImplementingMyJsTypeInterfaceWithProperty();
    assertEquals(0 + GET_X, getProperty(obj, "x"));
    assertEquals(0 + GET_X, obj.getX());
    assertEquals(0, obj.x);

    setProperty(obj, "x", 10);
    assertEquals(10 + GET_X + SET_X, getProperty(obj, "x"));
    assertEquals(10 + GET_X + SET_X, obj.getX());
    assertEquals(10 + SET_X, obj.x);

    obj.setX(12);
    assertEquals(12 + GET_X + SET_X, getProperty(obj, "x"));
    assertEquals(12 + GET_X + SET_X, obj.getX());
    assertEquals(12 + SET_X, obj.x);

    MyJsTypeInterfaceWithProperty intf = new MyJavaTypeImplementingMyJsTypeInterfaceWithProperty();
    assertEquals(0 + GET_X, getProperty(intf, "x"));
    assertEquals(0 + GET_X, intf.getX());
    assertEquals(0, ((MyJavaTypeImplementingMyJsTypeInterfaceWithProperty) intf).x);

    setProperty(intf, "x", 10);
    assertEquals(10 + GET_X + SET_X, getProperty(intf, "x"));
    assertEquals(10 + GET_X + SET_X, intf.getX());
    assertEquals(10 + SET_X, ((MyJavaTypeImplementingMyJsTypeInterfaceWithProperty) intf).x);

    intf.setX(12);
    assertEquals(12 + GET_X + SET_X, getProperty(intf, "x"));
    assertEquals(12 + GET_X + SET_X, intf.getX());
    assertEquals(12 + SET_X, ((MyJavaTypeImplementingMyJsTypeInterfaceWithProperty) intf).x);
  }

  @JsType
  public static class MyConcreteJsType {
    private int x;

    @JsProperty
    public int getY() {
      return x + GET_X;
    }

    @JsProperty
    public void setY(int x) {
      this.x = x + SET_X;
    }
  }

  private static void testConcreteJsType() {
    MyConcreteJsType obj = new MyConcreteJsType();
    assertEquals(0 + GET_X, getProperty(obj, "y"));
    assertEquals(0 + GET_X, obj.getY());
    assertEquals(0, obj.x);

    setProperty(obj, "y", 10);
    assertEquals(10 + GET_X + SET_X, getProperty(obj, "y"));
    assertEquals(10 + GET_X + SET_X, obj.getY());
    assertEquals(10 + SET_X, obj.x);

    obj.setY(12);
    assertEquals(12 + GET_X + SET_X, getProperty(obj, "y"));
    assertEquals(12 + GET_X + SET_X, obj.getY());
    assertEquals(12 + SET_X, obj.x);
  }

  @JsType(isNative = true, name = "MyNativeJsType")
  static class MyNativeJsType {
    public MyNativeJsType() {}

    public MyNativeJsType(int n) {}

    public static int staticX;

    public static native int answerToLife();

    public boolean ctorExecuted;

    public int x;

    @JsProperty
    public native int getY();

    @JsProperty
    public native void setY(int x);

    public native int sum(int bias);
  }

  private static void testNativeJsType() {
    MyNativeJsType.staticX = 34;
    assertEquals(34, MyNativeJsType.staticX);
    assertEquals(42, MyNativeJsType.answerToLife());

    MyNativeJsType obj = createMyNativeJsType();
    assertTrue(obj.ctorExecuted);
    assertEquals(obj.x, 0);
    obj.x = 72;
    assertEquals(72, obj.x);
    assertEquals(74, obj.sum(2));

    assertEquals(0, obj.getY());
    obj.setY(91);
    assertEquals(91, obj.getY());
  }
  
  static class MyNativeJsTypeSubclass extends MyNativeJsType {

    @JsConstructor
    MyNativeJsTypeSubclass() {
      super(42);
      setY(52);
    }

    @Override
    public int sum(int bias) {
      return super.sum(bias) + GET_X;
    }
  }

  private static void testNativeJsTypeSubclass() {
    MyNativeJsTypeSubclass mc = new MyNativeJsTypeSubclass();
    assertTrue(mc.ctorExecuted);
    assertEquals(143, mc.sum(1));

    mc.x = -mc.x;
    assertEquals(58, mc.sum(0));

    assertEquals(52, mc.getY());
  }

  static class MyNativeJsTypeSubclassNoOverride extends MyNativeJsType {
    @JsConstructor
    public MyNativeJsTypeSubclassNoOverride() {}
  }

  private static void testNativeJsTypeSubclassNoOverride() {
    MyNativeJsTypeSubclassNoOverride myNativeJsType = new MyNativeJsTypeSubclassNoOverride();
    myNativeJsType.x = 12;
    assertEquals(42, myNativeJsType.sum(30));
  }

  @JsType(isNative = true, name = "MyNativeJsType")
  static class MyNativeJsTypeWithConstructor {
    public MyNativeJsTypeWithConstructor(int x) {}

    public boolean ctorExecuted;
    public int x;
  }

  private static void testNativeJsTypeWithConstructor() {
    MyNativeJsTypeWithConstructor obj = new MyNativeJsTypeWithConstructor(12);
    assertTrue(obj.ctorExecuted);
    assertEquals(12, obj.x);
  }

  static class MyNativeJsTypeWithConstructorSubclass extends MyNativeJsTypeWithConstructor {
    @JsConstructor
    public MyNativeJsTypeWithConstructorSubclass(int x) {
      super(x);
    }
  }

  private static void testNativeJsTypeWithConstructorSubclass() {
    MyNativeJsTypeWithConstructorSubclass obj = new MyNativeJsTypeWithConstructorSubclass(12);
    assertTrue(obj.ctorExecuted);
    assertEquals(12, obj.x);
  }

  @JsType(isNative = true)
  interface MyNativeJsTypeInterface {
    @JsProperty
    int getX();

    @JsProperty
    void setX(int x);

    int sum(int bias);
  }

  static class MyNativeNativeJsTypeTypeInterfaceSubclassNeedingBridge extends AccidentaImplementor
      implements MyNativeJsTypeInterface {}

  abstract static class AccidentaImplementor {
    private int x;

    public int getX() {
      return x + GET_X;
    }

    public void setX(int x) {
      this.x = x + SET_X;
    }

    public int sum(int bias) {
      return bias + x;
    }
  }

  private static void testJsPropertyBridges() {
    MyNativeJsTypeInterface object = new MyNativeNativeJsTypeTypeInterfaceSubclassNeedingBridge();

    object.setX(3);
    assertEquals(3 + 150, object.getX());
    assertEquals(3 + SET_X, ((AccidentaImplementor) object).x);

    AccidentaImplementor accidentaImplementor = (AccidentaImplementor) object;

    accidentaImplementor.setX(3);
    assertEquals(3 + 150, accidentaImplementor.getX());
    assertEquals(3 + 150, getProperty(object, "x"));
    assertEquals(3 + SET_X, accidentaImplementor.x);

    setProperty(object, "x", 4);
    assertEquals(4 + 150, accidentaImplementor.getX());
    assertEquals(4 + 150, getProperty(object, "x"));
    assertEquals(4 + SET_X, accidentaImplementor.x);

    assertEquals(3 + 4 + SET_X, accidentaImplementor.sum(3));
  }

  static class MyNativeJsTypeInterfaceImplNeedingBridgeSubclassed extends OtherAccidentalImplementer
      implements MyNativeJsTypeInterface {}

  abstract static class OtherAccidentalImplementer {
    private int x;

    public int getX() {
      return x + GET_PARENT_X;
    }

    public void setX(int x) {
      this.x = x + SET_PARENT_X;
    }

    public int sum(int bias) {
      return bias + x;
    }
  }

  static class MyNativeJsTypeInterfaceImplNeedingBridgeSubclass
      extends MyNativeJsTypeInterfaceImplNeedingBridgeSubclassed {
    private int y;

    @Override
    public int getX() {
      return y + GET_X;
    }

    @Override
    public void setX(int y) {
      this.y = y + SET_X;
    }

    public void setParentX(int value) {
      super.setX(value);
    }

    public int getXPlusY() {
      return super.getX() + y;
    }
  }

  private static void testJsPropertyBridgesSubclass() {
    MyNativeJsTypeInterface object = new MyNativeJsTypeInterfaceImplNeedingBridgeSubclass();

    object.setX(3);
    assertEquals(3 + 150, object.getX());

    OtherAccidentalImplementer simple = (OtherAccidentalImplementer) object;

    simple.setX(3);
    assertEquals(3 + GET_X + SET_X, simple.getX());
    assertEquals(3 + GET_X + SET_X, getProperty(object, "x"));
    assertEquals(3 + SET_X, ((MyNativeJsTypeInterfaceImplNeedingBridgeSubclass) object).y);
    assertEquals(0, ((OtherAccidentalImplementer) object).x);

    setProperty(object, "x", 4);
    assertEquals(4 + GET_X + SET_X, simple.getX());
    assertEquals(4 + GET_X + SET_X, getProperty(object, "x"));
    assertEquals(4 + SET_X, ((MyNativeJsTypeInterfaceImplNeedingBridgeSubclass) object).y);
    assertEquals(0, ((OtherAccidentalImplementer) object).x);

    MyNativeJsTypeInterfaceImplNeedingBridgeSubclass subclass =
        (MyNativeJsTypeInterfaceImplNeedingBridgeSubclass) object;

    subclass.setParentX(5);
    assertEquals(8 + SET_PARENT_X, simple.sum(3));
    assertEquals(9 + SET_PARENT_X + GET_PARENT_X + SET_X, subclass.getXPlusY());
    assertEquals(4 + SET_X, ((MyNativeJsTypeInterfaceImplNeedingBridgeSubclass) object).y);
    assertEquals(5 + SET_PARENT_X, ((OtherAccidentalImplementer) object).x);
  }
  
  @JsType(isNative = true)
  interface MyJsTypeInterfaceWithProtectedNames {
    String var();

    @JsProperty
    String getNullField(); // Defined in object scope but shouldn't obfuscate

    @JsProperty
    String getImport();

    @JsProperty
    void setImport(String str);
  }

  private static void testProtectedNames() {
    MyJsTypeInterfaceWithProtectedNames obj = createMyJsInterfaceWithProtectedNames();
    assertEquals("var", obj.var());
    assertEquals("nullField", obj.getNullField());
    assertEquals("import", obj.getImport());
    obj.setImport("import2");
    assertEquals("import2", obj.getImport());
  }

  @JsType(isNative = true)
  interface JsTypeIsProperty {

    @JsProperty
    boolean isX();

    @JsProperty
    void setX(boolean x);
  }

  private static void testJsPropertyIsX() {
    JsTypeIsProperty object = createJsTypeIsProperty();

    assertFalse(object.isX());
    object.setX(true);
    assertTrue(object.isX());
    object.setX(false);
    assertFalse(object.isX());
  }
  
  @JsType(isNative = true)
  interface AccidentalOverridePropertyJsTypeInterface {
    @JsProperty
    int getX();
  }

  static class AccidentalOverridePropertyBase {
    public int getX() {
      return 50;
    }
  }

  static class AccidentalOverrideProperty extends AccidentalOverridePropertyBase
      implements AccidentalOverridePropertyJsTypeInterface {}

  private static void testJsPropertyAccidentalOverrideSuperCall() {
    AccidentalOverrideProperty object = new AccidentalOverrideProperty();
    assertEquals(50, object.getX());
    assertEquals(50, getProperty(object, "x"));
  }

  @JsType
  static class RemovedAccidentalOverridePropertyBase {
    private RemovedAccidentalOverridePropertyBase() {}
    @JsProperty
    public int getX() {
      return 55;
    }
  }

  static class RemovedAccidentalOverrideProperty extends RemovedAccidentalOverridePropertyBase
      implements AccidentalOverridePropertyJsTypeInterface {}

  private static void testJsPropertyRemovedAccidentalOverrideSuperCall() {
    RemovedAccidentalOverrideProperty object = new RemovedAccidentalOverrideProperty();
    // If the accidental override here were not removed the access to property x would result in
    // an infinite loop
    assertEquals(55, object.getX());
    assertEquals(55, getProperty(object, "x"));
  }
  
  @JsType(isNative = true)
  interface JsTypeGetProperty {

    @JsProperty
    int getX();

    @JsProperty
    void setX(int x);
  }

  private static void testJsPropertyGetX() {
    JsTypeGetProperty object = createJsTypeGetProperty();

    assertTrue(isUndefined(object.getX()));
    object.setX(10);
    assertEquals(10, object.getX());
    object.setX(0);
    assertEquals(0, object.getX());
  }

  @JsMethod
  private static native MyNativeJsType createMyNativeJsType();

  @JsMethod
  private static native JsTypeGetProperty createJsTypeGetProperty();

  @JsMethod
  private static native JsTypeIsProperty createJsTypeIsProperty();

  @JsMethod
  private static native MyJsTypeInterfaceWithProtectedNames createMyJsInterfaceWithProtectedNames();

  @JsMethod
  private static native boolean isUndefined(int value);

  @JsMethod
  private static native boolean hasField(Object object, String fieldName);

  @JsMethod
  private static native int getProperty(Object object, String name);

  @JsMethod
  private static native void setProperty(Object object, String name, int value);

  public void assertJsTypeHasFields(Object obj, String... fields) {
    for (String field : fields) {
      assertTrue("Field '" + field + "' should be exported", hasField(obj, field));
    }
  }

  public void assertJsTypeDoesntHaveFields(Object obj, String... fields) {
    for (String field : fields) {
      assertFalse("Field '" + field + "' should not be exported", hasField(obj, field));
    }
  }
}
