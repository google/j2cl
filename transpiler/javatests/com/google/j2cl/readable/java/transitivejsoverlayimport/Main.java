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
package transitivejsoverlayimport;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
class Immediate extends Transitive {

  @JsOverlay
  final void doImmediateInstanceMethod() {}
}

class NonNativeUpper extends Immediate {
  @JsConstructor
  public NonNativeUpper() {}

  void doNonNativeUpperInstanceMethod() {}
}

class NonNativeLower extends NonNativeUpper {
  @JsConstructor
  public NonNativeLower() {}

  void doNonNativeLowerInstanceMethod() {}
}

@JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
class Transitive {

  @JsProperty
  final native int getJsProperty();

  @JsOverlay
  final void doTransitiveInstanceMethod(String arg1) {}
}

public class Main {

  public static void main(String... args) {
    {
      Transitive transitive = null;
      transitive.doTransitiveInstanceMethod("arg1");
      transitive.getJsProperty();
    }

    {
      Immediate immediate = null;
      immediate.doTransitiveInstanceMethod("arg1");
      immediate.getJsProperty();
      immediate.doImmediateInstanceMethod();
    }

    {
      NonNativeUpper nonNativeUpper = null;
      nonNativeUpper.doTransitiveInstanceMethod("arg1");
      nonNativeUpper.getJsProperty();
      nonNativeUpper.doImmediateInstanceMethod();
      nonNativeUpper.doNonNativeUpperInstanceMethod();
    }

    {
      NonNativeLower nonNativeLower = null;
      nonNativeLower.doTransitiveInstanceMethod("arg1");
      nonNativeLower.getJsProperty();
      nonNativeLower.doImmediateInstanceMethod();
      nonNativeLower.doNonNativeUpperInstanceMethod();
      nonNativeLower.doNonNativeLowerInstanceMethod();
    }
  }
}
