/*
 * Copyright 2014 Google Inc.
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

import jsinterop.annotations.JsMethod;

/**
 * A test class that exhibits a variety of @JsTypes.
 */
public class MyClassExportsMethod {
  public static boolean calledFromCallMe1 = false;
  public static boolean calledFromCallMe2 = false;
  public static boolean calledFromCallMe3 = false;
  public static boolean calledFromCallMe4 = false;
  public static boolean calledFromCallMe5 = false;

  // Deprecated in J2CL, can't export member to different namespace than the class.
  // @JsMethod(namespace = JsPackage.GLOBAL, name = "exported")
  // public static void callMe1() {
  //   calledFromCallMe1 = true;
  // }
  //
  // @JsMethod(namespace = "exportNamespace", name = "exported")
  // public static void callMe2(int i) {
  //   calledFromCallMe2 = true;
  // }
  //
  // @JsMethod(namespace = "exportNamespace")
  // public static void callMe3(float f) {
  //   calledFromCallMe3 = true;
  // }

  @JsMethod(name = "exported")
  public static void callMe4() {
    calledFromCallMe4 = true;
  }

  @JsMethod
  public static void callMe5() {
    calledFromCallMe5 = true;
  }

  static boolean calledFromFoo = false;
  static boolean calledFromBar = false;

  /**
   * Static inner class A.
   */
  public static class A {
    public void bar() {
      calledFromBar = true;
    }

    public void foo() {
      calledFromFoo = true;
    }
  }

  /**
   * Static inner subclass of A.
   */
  public static class SubclassOfA extends A {
    @Override
    public void foo() {}
  }

  // Deprecated in J2CL, can't export member to different namespace than the class.
  // // There should be no calls to this method from java.
  // @SuppressWarnings("unusable-by-js")
  // @JsMethod(namespace = JsPackage.GLOBAL)
  // public static void callBar(A a) {
  //   a.bar();
  // }
  //
  // // There should be a call to this method from java.
  // @SuppressWarnings("unusable-by-js")
  // @JsMethod(namespace = JsPackage.GLOBAL)
  // public static void callFoo(A a) {
  //   a.foo();
  // }
  //
  // @SuppressWarnings("unusable-by-js")
  // @JsMethod(namespace = JsPackage.GLOBAL)
  // public static A newA() {
  //   return new A();
  // }
}
