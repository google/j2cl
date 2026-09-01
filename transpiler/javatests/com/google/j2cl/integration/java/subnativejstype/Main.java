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
package subnativejstype;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class Main {
  public static void main(String... args) {
    testJsTypeSubclassConstructor();
    testJsPropertyNativeJsTypeSubclass();
    testJsPropertyNativeJsTypeSubclassNoOverride();
    testJsPropertyBridges();
    testJsPropertyBridgesSubclass();
    testJsPropertyAccidentalOverrideSuperCall();
    testJsPropertyRemovedAccidentalOverrideSuperCall();
  }

  private static void testJsTypeSubclassConstructor() {
    JsPropertyMyNativeJsTypeSubclass mc = new JsPropertyMyNativeJsTypeSubclass(0, 0);
    assertTrue(mc.ctorExecuted);
  }

  private static final int SET_PARENT_X = 500;
  private static final int GET_PARENT_X = 1000;
  private static final int GET_X = 100;
  private static final int SET_X = 50;

  private static void testJsPropertyNativeJsTypeSubclass() {
    JsPropertyMyNativeJsTypeSubclass mc = new JsPropertyMyNativeJsTypeSubclass(10, 20);
    assertTrue(mc.x == 30);
    assertTrue(mc.y == 200);
    assertTrue(mc.f == 10);
    assertEquals(131, mc.sum(1));

    mc.x = -mc.x;
    assertEquals(70, mc.sum(0));

    assertEquals(52, mc.getZ());
  }

  @JsType(isNative = true, namespace = "subnativejstype.JsPropertyTest", name = "MyNativeJsType")
  static class JsPropertyMyNativeJsType {
    public JsPropertyMyNativeJsType() {}

    public JsPropertyMyNativeJsType(int x, int y) {}

    public static int staticX;

    public static native int answerToLife();

    public boolean ctorExecuted;

    public int x;

    public int y;

    @JsProperty
    public native int getZ();

    @JsProperty
    public native void setZ(int z);

    public native int sum(int bias);
  }

  static class JsPropertyMyNativeJsTypeSubclass extends JsPropertyMyNativeJsType {
    public int f = 20;

    @JsConstructor
    JsPropertyMyNativeJsTypeSubclass(int x, int y) {
      super(x + y, x * y);
      f += x - y;
      setZ(52);
    }

    @Override
    public int sum(int bias) {
      return super.sum(bias) + GET_X;
    }
  }

  private static void testJsPropertyNativeJsTypeSubclassNoOverride() {
    JsPropertyMyNativeJsTypeSubclassNoOverride myNativeJsType =
        new JsPropertyMyNativeJsTypeSubclassNoOverride();
    myNativeJsType.x = 12;
    assertEquals(42, myNativeJsType.sum(30));
  }

  static class JsPropertyMyNativeJsTypeSubclassNoOverride extends JsPropertyMyNativeJsType {
    @JsConstructor
    public JsPropertyMyNativeJsTypeSubclassNoOverride() {}
  }

  private static void testJsPropertyBridges() {
    JsPropertyMyNativeJsTypeInterface object =
        new JsPropertyMyNativeNativeJsTypeTypeInterfaceSubclassNeedingBridge();

    object.setX(3);
    assertEquals(3 + 150, object.getX());
    assertEquals(3 + SET_X, ((JsPropertyAccidentaImplementor) object).x);

    JsPropertyAccidentaImplementor accidentaImplementor = (JsPropertyAccidentaImplementor) object;

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

  @JsType(
      isNative = true,
      namespace = "subnativejstype.JsPropertyTest",
      name = "MyNativeJsTypeInterface")
  interface JsPropertyMyNativeJsTypeInterface {
    @JsProperty
    int getX();

    @JsProperty
    void setX(int x);

    int sum(int bias);
  }

  static class JsPropertyMyNativeNativeJsTypeTypeInterfaceSubclassNeedingBridge
      extends JsPropertyAccidentaImplementor implements JsPropertyMyNativeJsTypeInterface {}

  abstract static class JsPropertyAccidentaImplementor {
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

  private static void testJsPropertyBridgesSubclass() {
    JsPropertyMyNativeJsTypeInterface object =
        new JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass();

    object.setX(3);
    assertEquals(3 + 150, object.getX());

    JsPropertyOtherAccidentalImplementer simple = (JsPropertyOtherAccidentalImplementer) object;

    simple.setX(3);
    assertEquals(3 + GET_X + SET_X, simple.getX());
    assertEquals(3 + GET_X + SET_X, getProperty(object, "x"));
    assertEquals(
        3 + SET_X, ((JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass) object).y);
    assertEquals(0, ((JsPropertyOtherAccidentalImplementer) object).x);

    setProperty(object, "x", 4);
    assertEquals(4 + GET_X + SET_X, simple.getX());
    assertEquals(4 + GET_X + SET_X, getProperty(object, "x"));
    assertEquals(
        4 + SET_X, ((JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass) object).y);
    assertEquals(0, ((JsPropertyOtherAccidentalImplementer) object).x);

    JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass subclass =
        (JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass) object;

    subclass.setParentX(5);
    assertEquals(8 + SET_PARENT_X, simple.sum(3));
    assertEquals(9 + SET_PARENT_X + GET_PARENT_X + SET_X, subclass.getXPlusY());
    assertEquals(
        4 + SET_X, ((JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass) object).y);
    assertEquals(5 + SET_PARENT_X, ((JsPropertyOtherAccidentalImplementer) object).x);
  }

  static class JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclassed
      extends JsPropertyOtherAccidentalImplementer implements JsPropertyMyNativeJsTypeInterface {}

  abstract static class JsPropertyOtherAccidentalImplementer {
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

  static class JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass
      extends JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclassed {
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

  private static void testJsPropertyAccidentalOverrideSuperCall() {
    JsPropertyAccidentalOverrideProperty object = new JsPropertyAccidentalOverrideProperty();
    assertEquals(50, object.getX());
    assertEquals(50, getProperty(object, "x"));
  }

  @JsType(
      isNative = true,
      namespace = "subnativejstype.JsPropertyTest",
      name = "AccidentalOverridePropertyJsTypeInterface")
  interface JsPropertyAccidentalOverridePropertyJsTypeInterface {
    @JsProperty
    int getX();
  }

  static class JsPropertyAccidentalOverridePropertyBase {
    public int getX() {
      return 50;
    }
  }

  static class JsPropertyAccidentalOverrideProperty extends JsPropertyAccidentalOverridePropertyBase
      implements JsPropertyAccidentalOverridePropertyJsTypeInterface {}

  private static void testJsPropertyRemovedAccidentalOverrideSuperCall() {
    JsPropertyRemovedAccidentalOverrideProperty object =
        new JsPropertyRemovedAccidentalOverrideProperty();
    // If the accidental override here were not removed the access to property x would result in
    // an infinite loop
    assertEquals(55, object.getX());
    assertEquals(55, getProperty(object, "x"));
  }

  @JsType
  static class JsPropertyRemovedAccidentalOverridePropertyBase {
    private JsPropertyRemovedAccidentalOverridePropertyBase() {}

    @JsProperty
    public int getX() {
      return 55;
    }
  }

  static class JsPropertyRemovedAccidentalOverrideProperty
      extends JsPropertyRemovedAccidentalOverridePropertyBase
      implements JsPropertyAccidentalOverridePropertyJsTypeInterface {}

  @JsMethod(namespace = "subnativejstype.JsPropertyTestHelper")
  private static native int getProperty(Object object, String name);

  @JsMethod(namespace = "subnativejstype.JsPropertyTestHelper")
  private static native void setProperty(Object object, String name, int value);
}
