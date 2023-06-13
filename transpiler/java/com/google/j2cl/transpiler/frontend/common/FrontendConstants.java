/*
 * Copyright 2019 Google Inc.
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
package com.google.j2cl.transpiler.frontend.common;



/** Constants common to all frontends. */
public final class FrontendConstants {

  /** Annotations names. */
  public static final String JS_CONSTRUCTOR_ANNOTATION_NAME = "jsinterop.annotations.JsConstructor";

  public static final String JS_ASYNC_ANNOTATION_NAME = "jsinterop.annotations.JsAsync";
  public static final String JS_ENUM_ANNOTATION_NAME = "jsinterop.annotations.JsEnum";
  public static final String JS_FUNCTION_ANNOTATION_NAME = "jsinterop.annotations.JsFunction";
  public static final String JS_IGNORE_ANNOTATION_NAME = "jsinterop.annotations.JsIgnore";
  public static final String JS_METHOD_ANNOTATION_NAME = "jsinterop.annotations.JsMethod";
  public static final String JS_OPTIONAL_ANNOTATION_NAME = "jsinterop.annotations.JsOptional";
  public static final String JS_OVERLAY_ANNOTATION_NAME = "jsinterop.annotations.JsOverlay";
  public static final String JS_PACKAGE_ANNOTATION_NAME = "jsinterop.annotations.JsPackage";
  public static final String JS_PROPERTY_ANNOTATION_NAME = "jsinterop.annotations.JsProperty";
  public static final String JS_TYPE_ANNOTATION_NAME = "jsinterop.annotations.JsType";
  public static final String KT_DISABLED_ANNOTATION_NAME =
      "javaemul.internal.annotations.KtDisabled";
  public static final String KT_IN_ANNOTATION_NAME = "javaemul.internal.annotations.KtIn";
  public static final String KT_NAME_ANNOTATION_NAME = "javaemul.internal.annotations.KtName";
  public static final String KT_NATIVE_ANNOTATION_NAME = "javaemul.internal.annotations.KtNative";
  public static final String KT_OBJECTIVE_C_NAME = "com.google.j2objc.annotations.ObjectiveCName";
  public static final String KT_OUT_ANNOTATION_NAME = "javaemul.internal.annotations.KtOut";
  public static final String KT_PROPERTY_ANNOTATION_NAME =
      "javaemul.internal.annotations.KtProperty";
  public static final String SUPPRESS_WARNINGS_ANNOTATION_NAME = "java.lang.SuppressWarnings";
  public static final String DO_NOT_AUTOBOX_ANNOTATION_NAME =
      "javaemul.internal.annotations.DoNotAutobox";


  private FrontendConstants() {}
}
