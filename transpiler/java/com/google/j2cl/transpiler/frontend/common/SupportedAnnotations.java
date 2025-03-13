/*
 * Copyright 2025 Google Inc.
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

import com.google.common.collect.ImmutableSet;

/** Utility that provides handling of recognized annotations. */
public class SupportedAnnotations {

  private static final ImmutableSet<String> RECOGNIZED_ANNOTATIONS_QUALIFIED_NAMES =
      ImmutableSet.of(
          // JsInterop annotations.
          FrontendConstants.JS_CONSTRUCTOR_ANNOTATION_NAME,
          FrontendConstants.JS_ASYNC_ANNOTATION_NAME,
          FrontendConstants.JS_ENUM_ANNOTATION_NAME,
          FrontendConstants.JS_FUNCTION_ANNOTATION_NAME,
          FrontendConstants.JS_IGNORE_ANNOTATION_NAME,
          FrontendConstants.JS_METHOD_ANNOTATION_NAME,
          FrontendConstants.JS_OPTIONAL_ANNOTATION_NAME,
          FrontendConstants.JS_OVERLAY_ANNOTATION_NAME,
          FrontendConstants.JS_PACKAGE_ANNOTATION_NAME,
          FrontendConstants.JS_PROPERTY_ANNOTATION_NAME,
          FrontendConstants.JS_TYPE_ANNOTATION_NAME,
          // Test annotations
          "org.junit.runner.RunWith",
          "com.google.apps.xplat.testing.parameterized.RunParameterized",
          // Other
          "java.lang.Deprecated",
          "kotlin.Deprecated",
          FrontendConstants.KT_OBJECTIVE_C_NAME,
          FrontendConstants.J2KT_THROWS_ANNOTATION_NAME,
          FrontendConstants.J2KT_NATIVE_ANNOTATION_NAME);

  public static boolean isSupportedAnnotation(String qualifiedName) {
    return Nullability.isNullableAnnotation(qualifiedName)
        || Nullability.isNonNullAnnotation(qualifiedName)
        || Nullability.isNullMarkedAnnotation(qualifiedName)
        || RECOGNIZED_ANNOTATIONS_QUALIFIED_NAMES.contains(qualifiedName);
  }

  private SupportedAnnotations() {}
}
