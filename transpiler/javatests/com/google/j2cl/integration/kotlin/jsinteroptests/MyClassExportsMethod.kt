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
package jsinteroptests

import jsinterop.annotations.JsMethod

/** A test class that exhibits a variety of @JsTypes. */
class MyClassExportsMethod {
  companion object {
    @JvmField var calledFromCallMe1 = false

    @JvmField var calledFromCallMe2 = false

    @JvmField var calledFromCallMe3 = false

    @JvmField var calledFromCallMe4 = false

    @JvmField var calledFromCallMe5 = false

    // Deprecated in J2CL, can't export member to different namespace than the class.
    // @JsMethod(namespace = JsPackage.GLOBAL, name = "exported")
    // @JvmStatic
    // fun callMe1() {
    //   calledFromCallMe1 = true;
    // }
    //
    // @JsMethod(namespace = "exportNamespace", name = "exported")
    // @JvmStatic
    // fun callMe2(i: Int) {
    //   calledFromCallMe2 = true;
    // }
    //
    // @JsMethod(namespace = "exportNamespace")
    // @JvmStatic
    // fun callMe3(f: Float) {
    //   calledFromCallMe3 = true;
    // }

    @JsMethod(name = "exported")
    @JvmStatic
    fun callMe4() {
      calledFromCallMe4 = true
    }

    @JsMethod
    @JvmStatic
    fun callMe5() {
      calledFromCallMe5 = true
    }

    @JvmField internal var calledFromFoo = false

    @JvmField internal var calledFromBar = false

    // Deprecated in J2CL, can't export member to different namespace than the class.
    // // There should be no calls to this method from java.
    // @SuppressWarnings("unusable-by-js")
    // @JsMethod(namespace = JsPackage.GLOBAL)
    // @JvmStatic
    // fun callBar(A a) {
    //   a.bar();
    // }
    //
    // // There should be a call to this method from java.
    // @SuppressWarnings("unusable-by-js")
    // @JsMethod(namespace = JsPackage.GLOBAL)
    // @JvmStatic
    // fun callFoo(A a) {
    //   a.foo();
    // }
    //
    // @SuppressWarnings("unusable-by-js")
    // @JsMethod(namespace = JsPackage.GLOBAL)
    // @JvmStatic
    // fun newA(): A = A()
  }

  /** Static inner class A. */
  open class A {
    fun bar() {
      calledFromBar = true
    }

    open fun foo() {
      calledFromFoo = true
    }
  }

  /** Static inner subclass of A. */
  class SubclassOfA : A() {
    override fun foo() {}
  }
}
