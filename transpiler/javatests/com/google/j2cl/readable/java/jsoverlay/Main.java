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
package jsoverlay;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {

  @JsType(isNative = true, namespace = "test.foo")
  public interface NativeJsTypeInterfaceWithOverlay {
    @JsOverlay Object staticField = new Object();

    int m();

    @JsOverlay
    default int callM() {
      return m();
    }
  }

  static class NativeJsTypeInterfaceWithOverlayImpl implements NativeJsTypeInterfaceWithOverlay {
    public int m() {
      return 0;
    }
  }

  @JsType(isNative = true, namespace = "test.foo")
  public static class NativeJsTypeWithOverlay {
    public static int nonJsOverlayField;

    @JsOverlay public static Object staticField = new Object();

    public native int m();

    // non JsOverlay static method.
    public static native void n();

    @JsOverlay
    public final int callM() {
      return m();
    }

    @JsOverlay
    public static final int fun() {
      return 1;
    }

    @JsOverlay
    private static final int bar() {
      return 1;
    }

    @JsOverlay
    private final int foo() {
      return 1;
    }

    @JsOverlay
    public static int varargs(int... a) {
      return a[0];
    }

    @JsOverlay
    private int baz() {
      return 1;
    }

    @JsOverlay
    public final void overlayWithJsFunction() {
      new Intf() {
        @Override
        public void run() {}
      }.run();
    }

    @JsOverlay
    public final void overlay() {}

    @JsOverlay
    public static void overlay(NativeJsTypeWithOverlay o) {}
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  static class NativeJsTypeWithOverlayConstant {
    @JsOverlay public static final int COMPILE_TIME_CONSTANT = 10;
    @JsOverlay public static final String STRING_COMPILE_TIME_CONSTANT = "10";
  }

  @JsFunction
  private interface Intf {
    void run();
  }

  public void test() {
    NativeJsTypeWithOverlay n = new NativeJsTypeWithOverlay();
    n.callM();
    NativeJsTypeWithOverlay.fun();
    NativeJsTypeWithOverlay.n();
    NativeJsTypeWithOverlay.bar();
    n.foo();
    int a =
        NativeJsTypeWithOverlayConstant.COMPILE_TIME_CONSTANT
            + NativeJsTypeWithOverlay.nonJsOverlayField;
    NativeJsTypeWithOverlay.staticField = null;
    NativeJsTypeWithOverlay.varargs(1, 2, 3);
    n.baz();

    String b =
        NativeJsTypeWithOverlayConstant.STRING_COMPILE_TIME_CONSTANT
            + NativeJsTypeInterfaceWithOverlay.staticField;
  }

  public void testOverlayInterface(NativeJsTypeInterfaceWithOverlay foo) {
    foo.m();
    foo.callM();
  }

  public static void testOverlayInterfaceImpl() {
    NativeJsTypeInterfaceWithOverlay foo = new NativeJsTypeInterfaceWithOverlayImpl();
    foo.m();
    foo.callM();
  }
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
interface ParameterizedNativeInterface<T> {

  @JsOverlay
  default <T, S> void shadowsTypeVariable(T param1, S param2) {}

  @JsOverlay
  default <T, S> void shadowsTypeVariable(T param1, int param2) {}
}
