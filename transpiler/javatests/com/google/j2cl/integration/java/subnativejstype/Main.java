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
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class Main {
  public static void main(String... args) {
    testJsTypeSubclassConstructor();
    testJsPropertyNativeJsTypeSubclass();
    testJsPropertyNativeJsTypeSubclassNoOverride();
  }

  private static void testJsTypeSubclassConstructor() {
    JsPropertyMyNativeJsTypeSubclass mc = new JsPropertyMyNativeJsTypeSubclass(0, 0);
    assertTrue(mc.ctorExecuted);
  }

  private static final int GET_X = 100;

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
}
