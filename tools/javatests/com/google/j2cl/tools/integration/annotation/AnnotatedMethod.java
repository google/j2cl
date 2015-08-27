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
package com.google.j2cl.tools.integration.annotation;

import com.google.j2cl.tools.jsni.JsMethod;

/**
 * Class using JsMethod annotations to rename the javascript method.
 */
public class AnnotatedMethod {

  @JsMethod("staticMethod")
  public static native void annotatedStaticNativeMethod() /*-{
    return "annotatedStaticNativeMethod"
  }-*/;

  @com.google.j2cl.tools.jsni.JsMethod("fqnStaticMethod")
  public static native void fqnAnnotatedStaticNativeMethod() /*-{
      return "fqnAnnotatedStaticNativeMethod"
  }-*/;

  @JsMethod("nativeMethod")
  public native String annotatedNativeMethod() /*-{
      return "annotatedNativeMethod";
  }-*/;

  @com.google.j2cl.tools.jsni.JsMethod("fqnNativeMethod")
  public native String fqnAnnotatedNativeMethod() /*-{
      return "fqnAnnotatedNativeMethod";
  }-*/;

  @JsMethod()
  public native String notRenamedNativeMethod() /*-{
      return "notRenamedNativeMethod";
  }-*/;
}
