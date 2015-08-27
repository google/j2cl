/*
 * Copyright 2015 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */
package com.google.j2cl.tools.integration.classmethod;

/**
 * Class containing native instance and static methods.
 */
public class ClassWithNativeMethod {
  public native void nativeMethod(String fieldValue) /*-{
    this.field = fieldValue;
  }-*/;

  public native String nativeMethodWithResult() /*-{
      return "nativeMethodWithResult";
  }-*/;

  public void nonNativeMethod(String fieldValue){
    // do nothing
  }

  public static native void staticNative(String value) /*-{
    ClassWithNativeMethod.staticField = value;
  }-*/;

  public static native String staticNativeWithResult() /*-{
      return "staticNativeWithResult";
  }-*/;

  public static void staticNotNative(String value) {
      // do nothing
  }
}
