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
package nativeinjection;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@SuppressWarnings("unusable-by-js")
public class NativeClass {
  @JsConstructor
  public NativeClass() {}

  @JsMethod
  public native String nativeInstanceMethod(int a, int b);

  @JsProperty
  public native String getNativeInstanceProperty();

  @JsMethod
  public int instanceMember() {
    return 10;
  }

  @JsMethod
  public native int callsInstanceMember();

  @JsMethod
  public static native String nativeStaticMethod(int a, int b);

  @JsMethod
  public static native String nativeStaticMethodUsingLongAlias();

  @JsProperty
  public static native int getNativeStaticProperty();

  @JsMethod
  public static int staticMember(int value) {
    return 2 * value;
  }

  @JsMethod
  public static native int callsStaticMember(int value);

  @JsType(isNative = true, name = "*", namespace = JsPackage.GLOBAL)
  public interface JsAny {}

  // Implemented by the exposed overload below.
  @JsMethod
  public static native String implementedByExposedMember(Object value);

  @JsMethod
  public static String implementedByExposedMember(JsAny value) {
    return "implementedByExposedMember:" + value;
  }

  @JsProperty
  public static native int getNativeInitializationCount();

  @JsMethod
  public static native int callsConstructor();
}
